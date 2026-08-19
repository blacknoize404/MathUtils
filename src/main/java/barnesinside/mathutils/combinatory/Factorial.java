package barnesinside.mathutils.combinatory;

import barnesinside.mathutils.algebra.Evaluation;
import barnesinside.mathutils.algebra.primitives.Expression;
import barnesinside.mathutils.exceptions.FunctionEvaluationException;

/**
 * Representa el factorial de una expresión matemática
 */
public class Factorial extends Expression<Number> {

    public Factorial(Integer argument) {
        super(argument);
    }

    public static Factorial of(Integer argument)     {
        return new Factorial(argument);
    }

    @Override
    public double eval(Evaluation... evaluations) throws FunctionEvaluationException {

        int value = argument.intValue();
        if (value < 0) throw new IllegalArgumentException("El factorial no está definido para negativos");

        double result = 1;
        for (int i = 2; i <= value; i++) {
            result *= i;
        }

        return result;
    }
}
