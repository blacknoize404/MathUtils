package com.barnesinside.mathutils.algebra.exponentials;

import com.barnesinside.mathutils.algebra.Const;
import com.barnesinside.mathutils.algebra.primitives.MathElement;

/**
 * Representa la raíz cúbica
 */
public final class Cbrt extends Root {

    public Cbrt(MathElement argument) {
        super(argument, new Const(3));
    }

}
