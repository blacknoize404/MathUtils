package barnesinside.mathutils.algebra.logarithmics;

import barnesinside.mathutils.algebra.Const;
import barnesinside.mathutils.algebra.primitives.MathElement;

/**
 * Representa la función del logaritmo natural o neperiano
 */
public final class LogN extends Log {

    public LogN(MathElement argument) {
        super(argument, new Const(Math.E));
    }

}
