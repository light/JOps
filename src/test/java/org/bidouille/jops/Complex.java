package org.bidouille.jops;

public class Complex {

    public double re;
    public double im;

    public Complex() {}

    public Complex( double re, double im ) {
        this.re = re;
        this.im = im;
    }

    public void add( Complex other ) {
        re += other.re;
        im += other.im;
    }

    @Override
    public String toString() {
        return re + " + " + im + "i";
    }

    @Operator( "+" )
    public static Complex add( Complex a, Complex b ) {
        return new Complex( a.re + b.re, a.im + b.im );
    }

    @Operator( "*" )
    public static Complex mul( Complex a, Complex b ) {
        return new Complex( a.re * b.re - a.im * b.im, a.re * b.im + a.im * b.re );
    }

}
