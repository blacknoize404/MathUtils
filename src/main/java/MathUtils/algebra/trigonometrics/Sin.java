package MathUtils.algebra.trigonometrics;

import MathUtils.algebra.Evaluation;
import MathUtils.exceptions.FunctionEvaluationException;
import MathUtils.algebra.primitives.Expression;
import MathUtils.algebra.primitives.MathElement;

public final class Sin extends Expression<MathElement> {

    public Sin(MathElement body) {
        super(body);
    }

    // TODO: 11/12/2023 Las trigonométricas tienen errores a la hora de evaluar
    //  valores como el seno de 2pi, debería ser 0.
    @Override
    public double eval(Evaluation... variables) throws FunctionEvaluationException {
        return evalWithMods(variables);
    }

    private double evalWithMods(Evaluation... evaluations) throws FunctionEvaluationException {
        double value = argument.eval(evaluations);
        return Math.sin(value) - Math.sin(Math.toRadians(180d)) * (Math.abs(Math.toDegrees(value)) / 180d);
    }

}
