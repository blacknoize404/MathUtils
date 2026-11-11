package com.barnesinside.mathutils.algebra.trigonometrics;

import com.barnesinside.mathutils.algebra.Evaluation;
import com.barnesinside.mathutils.exceptions.FunctionEvaluationException;
import com.barnesinside.mathutils.algebra.primitives.Expression;
import com.barnesinside.mathutils.algebra.primitives.MathElement;

public class Cot extends Expression<MathElement> {

    public Cot(MathElement content) {
        super(content);
    }

    /**
     * Evaluación de la cotangente usando la identidad trigonométrica.
     */
    @Override
    public double eval(Evaluation... evaluations) throws FunctionEvaluationException {
        double value = argument.eval(evaluations);

        return Math.cos(value) / Math.sin(value);
    }

    @Override
    public String toString() {
        return "cot(" + argument + ")";
    }
}
