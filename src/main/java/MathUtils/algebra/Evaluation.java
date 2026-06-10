package MathUtils.algebra;

/**
 * Representa el concepto de sustitución de la letra de una variable por su valor
 * para así poder evaluar funciones con variables.
 */
public record Evaluation(String var, Number value) {

    @Override
    public String toString() {
        return "Evaluation{" + var + " = " + value + "}";
    }
}
