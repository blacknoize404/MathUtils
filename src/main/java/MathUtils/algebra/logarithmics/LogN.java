package MathUtils.algebra.logarithmics;

import MathUtils.algebra.Const;
import MathUtils.algebra.primitives.MathElement;

/**
 * Representa la función del logaritmo natural o neperiano
 */
public final class LogN extends Log {

    public LogN(MathElement argument) {
        super(argument, new Const(Math.E));
    }

}
