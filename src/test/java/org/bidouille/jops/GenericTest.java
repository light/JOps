package org.bidouille.jops;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class GenericTest {

    public static class X<T> {
        public T x;

        public X( T x ) {
            this.x = x;
        }

        @Override
        public String toString() {
            return String.valueOf( x );
        }
    }

    @Operator( "%" )
    public static X<String> op( X<String> a, X<String> b ) {
        return new X<String>( b.x );
    }

    @Test
    public void test() {
        X<String> a = new X<>( "1" );
        X<String> b = new X<>( "2" );
        String s = (a % b).x; // Expression type should be X<String>
        assertThat( s, is( "2" ) );
    }

}
