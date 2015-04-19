package org.bidouille.jops;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Supported right now : binary operators defined in JLS §3.12.
 * </p>
 *
 * <h3>JLS Excerpt :</h3>
 *
 * <h4>3.11. Separator: one of</h4>
 *
 * <pre>
 *   (    )    {    }    [    ]    ;    ,    .
 * </pre>
 *
 * <h4>3.12. Operator: one of</h4>
 *
 * <pre>
 * =   >   <   !   ~   ?   :
 * ==  <=  >=  !=  &&  ||  ++  --
 * +   -   *   /   &   |   ^   %   <<   >>   >>>
 * +=  -=  *=  /=  &=  |=  ^=  %=  <<=  >>=  >>>=
 * </pre>
 */
@Retention( RetentionPolicy.RUNTIME )
@Documented
@Inherited
@Target( { ElementType.METHOD } )
public @interface Operator {

    String value();

}
