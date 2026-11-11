package com.barnesinside.mathutils.algebra.trigonometrics;

import com.barnesinside.mathutils.algebra.Evaluation;
import com.barnesinside.mathutils.exceptions.FunctionEvaluationException;
import com.barnesinside.mathutils.algebra.primitives.Expression;
import com.barnesinside.mathutils.algebra.primitives.MathElement;

public final class Cos extends Expression<MathElement> {

    public Cos(MathElement body) {
        super(body);
    }

    @Override
    public double eval(Evaluation... variables) throws FunctionEvaluationException {
        return Math.cos(argument.eval(variables));
    }

    @Override
    public String toString() {
        return "cos(" + argument + ")";
    }
}
