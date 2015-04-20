package org.bidouille.jops;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class MixedOperationTest {

    public static class X {
        //
    }

    public static class Y {
        //
    }

    @Operator( "%" )
    public static String op( X a, Y b ) {
        return "Hi.";
    }

    @Operator( "*" )
    public static String op( X a, int b ) {
        return "op" + b;
    }

    @Test
    public void test_mixed() {
        X a = new X();
        Y b = new Y();
        String s = a % b;
        assertThat( s, is( "Hi." ) );
    }

    @Test
    public void test_mixed_primitive() {
        X a = new X();
        Y b = new Y();
        String s = a * 3;
        assertThat( s, is( "op3" ) );
    }

}
