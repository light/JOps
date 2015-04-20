import org.bidouille.jops.Operator;
import org.bidouille.jops.GenericTest.X;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class unknown_operator {

    @Operator( "%%" )
    public static String op( String a, String b ) {
        return a + b;
    }

    public void test() {
        System.out.println( "a" %% "b"  );
    }
}
