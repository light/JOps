package org.bidouille.jops;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

public class OperatorOverloadingTest {

    @Test
    public void test_binary_op_simple() {
        Complex a = new Complex( 1, 2 );
        Complex b = new Complex( 3, 4 );

        assertThat( a + b, isComplexValue( 4, 6 ) );
    }

    @Test
    public void test_binary_op_complex() {
        Complex a = new Complex( 1, 2 );
        Complex b = new Complex( 3, 4 );
        Complex c = new Complex( 2, 0 );

        assertThat( a + b * c, isComplexValue( 7, 10 ) );
    }

    @Test
    public void test_binary_op_precedence() {
        Complex a = new Complex( 1, 2 );
        Complex b = new Complex( 3, 4 );
        Complex c = new Complex( 2, 0 );

        assertThat( a + b * c, isComplexValue( 7, 10 ) );
        assertThat( b * c + a, isComplexValue( 7, 10 ) );
    }

    @Test
    public void test_binary_op_parenthesis() {
        Complex a = new Complex( 1, 2 );
        Complex b = new Complex( 3, 4 );
        Complex c = new Complex( 2, 0 );

        assertThat( (a + b) * c, isComplexValue( 8, 12 ) );
    }

    private Matcher<Complex> isComplexValue( double re, double im ) {
        return new ComplexMatcher( re, im );
    }

    private final class ComplexMatcher extends BaseMatcher<Complex> {
        private static final double TOL = 1e-5;
        double re;
        double im;

        public ComplexMatcher( double re, double im ) {
            this.re = re;
            this.im = im;
        }

        @Override
        public boolean matches( Object item ) {
            return item instanceof Complex
                    && Math.abs( ((Complex) item).re - re ) < TOL
                    && Math.abs( ((Complex) item).im - im ) < TOL;
        }

        @Override
        public void describeTo( Description description ) {
            description.appendText( "Complex with value " + re + " + " + im + "i" );
        }
    }
}
