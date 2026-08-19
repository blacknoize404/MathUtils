package barnesinside.mathutils.algebra.exponentials;

import barnesinside.mathutils.algebra.Const;
import barnesinside.mathutils.algebra.primitives.MathElement;

/**
 * Representa la raíz cúbica
 */
public final class Cbrt extends Root {

    public Cbrt(MathElement argument) {
        super(argument, new Const(3));
    }

}
