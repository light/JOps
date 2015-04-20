package org.bidouille.jops;

import static org.bidouille.jops.ReflectUtil.getField;
import static org.bidouille.jops.ReflectUtil.setField;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.comp.TransTypes;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

@SupportedAnnotationTypes( "org.bidouille.jops.Operator" )
@SupportedSourceVersion( SourceVersion.RELEASE_7 )
public class JOpsProcessor extends AbstractProcessor {
    private java.util.List<OperatorMethod> ops = new ArrayList<>();

    @Override
    public synchronized void init( ProcessingEnvironment processingEnv ) {
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        MultiTaskListener listener = MultiTaskListener.instance( context );
        // Listen to the compilation phases so that we can insert ourselves in the ANALYZE step later
        listener.add( new ReplaceOnAnalyzeTaskListener( context ) );
        super.init( processingEnv );
    }

    @Override
    public boolean process( Set<? extends TypeElement> annotations, RoundEnvironment roundEnv ) {
        for( Element elem : roundEnv.getElementsAnnotatedWith( Operator.class ) ) {
            Operator annotation = elem.getAnnotation( Operator.class );
            String op = annotation.value();
            Tag tag = getBinOpTag( op );
            if( tag == null ) {
                processingEnv.getMessager().printMessage( Kind.ERROR, "Invalid operator '" + op + "'", elem, elem.getAnnotationMirrors().get( 0 ) );
            } else {
                ops.add( new OperatorMethod( tag, (ExecutableElement) elem ) );
                note( "Added operator '" + op + "' -> " + elem.getEnclosingElement() + "." + elem );
            }
        }
        return false; // Indicate that the annotation is fully handled by us
    }

    private void note( String note ) {
        System.out.println( note );
        processingEnv.getMessager().printMessage( Kind.NOTE, note );
    }

    private void debug( String message ) {
        note( message );
    }

    private static Tag getBinOpTag( String op ) {
        switch( op ) {
        //@formatter:off
            case "||"  : return Tag.OR;
            case "&&"  : return Tag.AND;
            case "|"   : return Tag.BITOR;
            case "^"   : return Tag.BITXOR;
            case "&"   : return Tag.BITAND;
            case "=="  : return Tag.EQ;
            case "!="  : return Tag.NE;
            case "<"   : return Tag.LT;
            case ">"   : return Tag.GT;
            case "<="  : return Tag.LE;
            case ">="  : return Tag.GE;
            case "<<"  : return Tag.SL;
            case ">>"  : return Tag.SR;
            case ">>>" : return Tag.USR;
            case "+"   : return Tag.PLUS;
            case "-"   : return Tag.MINUS;
            case "*"   : return Tag.MUL;
            case "/"   : return Tag.DIV;
            case "%"   : return Tag.MOD;
            //@formatter:on
        }
        return null;
    }

    private class ReplaceOnAnalyzeTaskListener implements TaskListener {
        private final JavaCompiler compiler;
        private boolean replaced;

        private ReplaceOnAnalyzeTaskListener( Context context ) {
            this.compiler = JavaCompiler.instance( context );
        }

        @Override
        public void started( TaskEvent e ) {
            if( e.getKind() == TaskEvent.Kind.ANALYZE && !replaced ) {
                // We replace the compilation steps at this point because we have a different Context
                Context context = getField( compiler, "delegateCompiler.context" );

                // Replace the Attribute step to remove errors when encoutering overloaded operators
                ReflectUtil.<Map> getField( context, "ht" ).values().remove( getField( compiler, "delegateCompiler.attr" ) );
                JOpsAttr attr2 = new JOpsAttr( context );
                setField( compiler, "delegateCompiler.attr", attr2 );

                // Replace the TransTypes step to "desugar" operators into method calls.
                ReflectUtil.<Map> getField( context, "ht" ).values().remove( getField( compiler, "delegateCompiler.transTypes" ) );
                JOpsTransTypes transTypes2 = new JOpsTransTypes( context );
                setField( compiler, "delegateCompiler.transTypes", transTypes2 );

                replaced = true;
            }
        }

        @Override
        public void finished( TaskEvent e ) {}
    }

    private class OperatorMethod {
        private JCTree.Tag op;
        private ExecutableElement elem;
        private Type res;
        private Type arg0;
        private Type arg1;
        private MethodType mt;
        private MethodSymbol sym;

        public OperatorMethod( Tag op, ExecutableElement elem ) {
            this.op = op;
            this.elem = elem;
        }

        public void resolve( ClassReader reader, Names names, Resolve rs, Types types, Env<AttrContext> env ) {
            if( sym == null ) {
                // Re-resolve names etc since the context is not the same as during the annotation phase
                MethodSymbol sym_ = (MethodSymbol) elem;
                MethodType type = (MethodType) sym_.type;
                String name = sym_.name.toString(); // correct ?
                ClassSymbol recvsym = resolveClassSym( reader, names, sym_.owner.type );
                res = getType( types, reader, names, type.restype );
                arg0 = getType( types, reader, names, type.argtypes.get( 0 ) );
                arg1 = getType( types, reader, names, type.argtypes.get( 1 ) );
                mt = new MethodType( List.of( arg0, arg1 ), res, List.<Type> nil(), recvsym );
                Name methodName = names.fromString( name );
                sym = rs.resolveInternalMethod( null, env, recvsym.type, methodName, List.of( arg0, arg1 ), null );
            }
        }

        // Recursively resolves a type and its type params
        private Type getType( Types types, ClassReader reader, Names names, Type oldType ) {
            if( oldType instanceof ClassType ) {
                ClassSymbol sym = resolveClassSym( reader, names, oldType );
                List<Type> typarams = List.<Type> nil();
                for( Type type : ((ClassType) oldType).typarams_field ) {
                    typarams = typarams.prepend( getType( types, reader, names, type ) );
                }
                return new ClassType( sym.type.getEnclosingType(), typarams.reverse(), sym );
            }
            return oldType;
        }

        private ClassSymbol resolveClassSym( ClassReader reader, Names names, Type classType ) {
            String className = ((ClassSymbol) classType.tsym).flatname.toString();
            return reader.enterClass( names.fromString( className ) );
        }

    }

    private class JOpsAttr extends Attr {
        private Names names;
        private ClassReader reader;
        private Types types;
        private Resolve rs;

        protected JOpsAttr( Context context ) {
            super( context );
            names = Names.instance( context );
            reader = ClassReader.instance( context );
            types = Types.instance( context );
            rs = Resolve.instance( context );
        }

        @Override
        public void visitBinary( JCBinary tree ) {
            Env<AttrContext> env = getField( this, "env" );
            Type left = attribExpr( tree.lhs, env );
            Type right = attribExpr( tree.rhs, env );

            OperatorMethod foundOpMeth = null;
            for( OperatorMethod opMeth : ops ) {
                opMeth.resolve( reader, names, rs, types, env );
                if( tree.getTag() == opMeth.op
                        && types.isAssignable( opMeth.arg0, left )
                        && types.isAssignable( opMeth.arg1, right ) ) {
                    foundOpMeth = opMeth;
                    break;
                }
            }

            if( foundOpMeth != null ) {
                tree.type = foundOpMeth.res;
                tree.operator = foundOpMeth.sym;
                setField( this, "result", tree.type );
                debug( "Attributing overloaded operator '" + foundOpMeth.op + "' tree type to '" + tree.type + "'" );
            } else {
                super.visitBinary( tree );
            }
        }

    }

    private class JOpsTransTypes extends TransTypes {
        private TreeMaker make;
        private Types types;

        protected JOpsTransTypes( Context context ) {
            super( context );
            make = TreeMaker.instance( context );
            types = Types.instance( context );
        }

        @Override
        public void visitBinary( JCBinary tree ) {
            super.visitBinary( tree );

            OperatorMethod foundOpMeth = null;
            for( OperatorMethod opMeth : ops ) {
                if( tree.getTag() == opMeth.op
                        && types.isAssignable( opMeth.arg0, tree.lhs.type )
                        && types.isAssignable( opMeth.arg1, tree.rhs.type ) ) {
                    foundOpMeth = opMeth;
                    break;
                }
            }

            if( foundOpMeth != null ) {
                result = make.Apply(
                        List.<JCExpression> nil(),
                        make.Select( make.Ident( foundOpMeth.sym.owner ), foundOpMeth.sym ).setType( foundOpMeth.mt ),
                        List.of( tree.lhs, tree.rhs ) )
                        .setType( foundOpMeth.res );
                debug( "Replacing overloaded operator '" + foundOpMeth.op + "' by call to  '" + foundOpMeth.sym + "'" );
            }
        }

    }

}
