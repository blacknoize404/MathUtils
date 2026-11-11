package com.barnesinside.mathutils.algebra.trigonometrics;

import com.barnesinside.mathutils.algebra.Evaluation;
import com.barnesinside.mathutils.algebra.primitives.Expression;
import com.barnesinside.mathutils.algebra.primitives.MathElement;
import com.barnesinside.mathutils.exceptions.FunctionEvaluationException;

public final class Tan extends Expression<MathElement> {
    public Tan(MathElement body) {
        super(body);
    }
    @Override
    public double eval(Evaluation... variables) throws FunctionEvaluationException {

        return Math.tan(argument.eval(variables));
    }

    @Override
    public String toString() {
        return "tan(" + argument + ")";
    }
}
