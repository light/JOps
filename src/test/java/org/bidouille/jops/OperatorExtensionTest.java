package org.bidouille.jops;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class OperatorExtensionTest {

    @Operator( "+" )
    public static List concat( List a, List b ) {
        List res = new ArrayList( a.size() + b.size() );
        res.addAll( a );
        res.addAll( b );
        return res;
    }

    @Operator( "%" )
    public static List<String> concatString( List<String> a, List<String> b ) {
        List<String> res = new ArrayList<>( a.size() + b.size() );
        res.addAll( a );
        res.addAll( b );
        return res;
    }

    @Test
    public void test_extended_operator() {
        List<String> cola = Arrays.asList( "hello", "," );
        List<String> colb = Arrays.asList( "world", "!" );

        Assert.<List<String>>assertThat( cola + colb, contains( "hello", ",", "world", "!" ) );
    }

    @Test
    public void test_extended_operator_raw() {
        List cola = Arrays.asList( 1 );
        List colb = Arrays.asList( 2, 3 );

        Assert.<List<Object>>assertThat( cola + colb, contains( 1, 2, 3 ) );
    }

    @Test
    public void test_extended_operator_generic() {
        List<String> cola = Arrays.asList( "hello", "," );
        List<String> colb = Arrays.asList( "world", "!" );

//        assertThat( concatString( cola, colb ), contains( "hello", ",", "world", "!" ) );
        Assert.<List<String>>assertThat( cola % colb, contains( "hello", ",", "world", "!" ) );
    }

}
