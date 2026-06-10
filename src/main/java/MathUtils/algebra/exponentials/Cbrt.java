package MathUtils.algebra.exponentials;

import MathUtils.algebra.Const;
import MathUtils.algebra.primitives.MathElement;

/**
 * Representa la raíz cúbica
 */
public final class Cbrt extends Root {

    public Cbrt(MathElement argument) {
        super(argument, new Const(3));
    }

}
