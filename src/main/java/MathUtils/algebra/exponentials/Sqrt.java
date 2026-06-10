package MathUtils.algebra.exponentials;

import MathUtils.algebra.Const;
import MathUtils.algebra.primitives.MathElement;

public final class Sqrt extends Root {

    public Sqrt(Number argument) {
        super(new Const(argument), new Const(2));
    }

    public Sqrt(MathElement argument) {
        super(argument, new Const(2));
    }


}
