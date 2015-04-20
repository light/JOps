package org.bidouille.jops;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.sun.tools.javac.Main;

public class CompilationTest {

    @Test
    public void test_inner_static_class() throws IOException {
        assertHasNoError( "src/test/resources/inner_static_class.java" );
    }

    @Test
    public void test_private_class() throws IOException {
        assertHasNoError( "src/test/resources/private_class.java" );
    }

    @Test
    public void test_parameterized_return_type() throws IOException {
        assertHasNoError( "src/test/resources/parameterized_return_type.java" );
    }

    private void assertHasNoError( String path ) throws IOException {
        int res = compile( path );
        assertThat( res, is( 0 ) );
    }

    private void assertHasErrors( String path ) throws IOException {
        int res = compile( path );
        assertThat( res, is( not( 0 ) ) );
    }

    private int compile( String path ) throws IOException {
        File target = new File( "target", getClass().getSimpleName() );
        if( !target.exists() ) {
            target.mkdirs();
        } else {
            FileUtils.cleanDirectory( target );
        }

        return Main.compile( new String[] {
                "-d", target.getAbsolutePath(),
                "-classpath", "target/classes;target/test-classes",
                "-target", "1.7",
                "-source", "1.7",
                "-encoding", "UTF-8",
                path
        } );
    }

}
