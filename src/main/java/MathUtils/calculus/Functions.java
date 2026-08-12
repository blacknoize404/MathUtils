package MathUtils.calculus;

import MathUtils.algebra.Evaluation;
import MathUtils.algebra.primitives.MathElement;
import MathUtils.exceptions.FunctionEvaluationException;

import java.util.function.DoubleUnaryOperator;

/**
 * Adaptadores entre elementos matemáticos y funciones numéricas de Java.
 */
public final class Functions {

    private Functions() {
    }

    /**
     * Convierte un elemento matemático en una función numérica de una variable.
     * Si el elemento depende de otras variables, la evaluación lanzará
     * {@link IllegalArgumentException}.
     * @param element expresión a evaluar
     * @param variable nombre de la variable independiente
     */
    public static DoubleUnaryOperator of(MathElement element, String variable) {
        return x -> {
            try {
                return element.eval(new Evaluation(variable, x));
            } catch (FunctionEvaluationException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        };
    }
}
