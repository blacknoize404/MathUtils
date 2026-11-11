package com.barnesinside.mathutils.algebra.exponentials;

import com.barnesinside.mathutils.algebra.Const;
import com.barnesinside.mathutils.algebra.primitives.MathElement;

public final class Sqrt extends Root {

    public Sqrt(Number argument) {
        super(new Const(argument), new Const(2));
    }

    public Sqrt(MathElement argument) {
        super(argument, new Const(2));
    }


}
