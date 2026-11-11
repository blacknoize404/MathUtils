package com.barnesinside.mathutils.algebra.logarithmics;

import com.barnesinside.mathutils.algebra.Const;
import com.barnesinside.mathutils.algebra.primitives.MathElement;

/**
 * Representa la función del logaritmo natural o neperiano
 */
public final class LogN extends Log {

    public LogN(MathElement argument) {
        super(argument, new Const(Math.E));
    }

}
