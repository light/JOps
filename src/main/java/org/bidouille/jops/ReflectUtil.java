package org.bidouille.jops;

import java.lang.reflect.Field;

public class ReflectUtil {

    public static <T> T getField( Object o, String path ) {
        try {
            return findField( o, path ).get();
        } catch( ReflectiveOperationException e ) {
            e.printStackTrace();
            throw new RuntimeException( e );
        }
    }

    public static void setField( Object o, String path, Object value ) {
        try {
            findField( o, path ).set( value );
        } catch( ReflectiveOperationException e ) {
            e.printStackTrace();
            throw new RuntimeException( e );
        }
    }

    public static FieldRef findField( Object o, String path ) throws ReflectiveOperationException {
        Field field = null;
        for( String name : path.split( "\\." ) ) {
            if( field != null ) {
                o = field.get( o );
            }
            field = null;
            Class<?> clazz = o.getClass();
            do {
                try {
                    field = clazz.getDeclaredField( name );
                } catch( NoSuchFieldException e ) {
                    clazz = clazz.getSuperclass();
                }
            } while( field == null && clazz != null );
            if( field == null ) {
                throw new NoSuchFieldException( name );
            }
            field.setAccessible( true );
        }
        if( field != null ) {
            return new FieldRef( o, field );
        }
        throw new NoSuchFieldException();
    }

    public static class FieldRef {
        private final Object obj;
        private final Field field;

        public FieldRef( Object obj, Field field ) {
            this.obj = obj;
            this.field = field;
        }

        public <T> T get() throws ReflectiveOperationException {
            return (T) field.get( obj );
        }

        public void set( Object value ) throws ReflectiveOperationException {
            field.set( obj, value );
        }

    }

}
