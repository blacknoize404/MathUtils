package MathUtils.exceptions;

/**
 * Ocurre cuando la variable a evaluar no está registrada en el diccionario
 * de variables.
 */
public class FunctionEvaluationException extends Exception {
    public FunctionEvaluationException(String message) {
        super(message);
    }
}