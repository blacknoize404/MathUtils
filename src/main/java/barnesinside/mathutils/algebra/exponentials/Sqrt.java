package barnesinside.mathutils.algebra.exponentials;

import barnesinside.mathutils.algebra.Const;
import barnesinside.mathutils.algebra.primitives.MathElement;

public final class Sqrt extends Root {

    public Sqrt(Number argument) {
        super(new Const(argument), new Const(2));
    }

    public Sqrt(MathElement argument) {
        super(argument, new Const(2));
    }


}
