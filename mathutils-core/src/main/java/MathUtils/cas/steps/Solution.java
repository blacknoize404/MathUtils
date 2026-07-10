package MathUtils.cas.steps;

import MathUtils.cas.expr.Expr;

import java.util.List;
import java.util.Locale;

/**
 * Resultado de resolver un problema paso a paso.
 *
 * @param kind     tipo de problema
 * @param status   si se resolvió o por qué no
 * @param problem  expresión del problema (por ejemplo, la función que se deriva)
 * @param result   resultado, o {@code null} si el estado no es {@link Status#SOLVED}
 * @param steps    árbol de pasos de la resolución
 * @param variable variable respecto a la que se opera
 * @param order    orden de la derivada (1 para el resto de problemas)
 */
public record Solution(Kind kind, Status status, Expr problem, Expr result, List<Step> steps, String variable,
                       int order) {

    /** Tipo de problema. */
    public enum Kind {DERIVATIVE}

    /** Estado final de la resolución. */
    public enum Status {
        /** Se obtuvo un resultado verificado. */
        SOLVED,
        /** El resultado existe pero no se puede escribir con funciones elementales. */
        NON_ELEMENTAL,
        /** El motor no supo resolverlo (nunca devuelve un resultado dudoso). */
        UNSOLVED
    }

    public Solution {
        steps = List.copyOf(steps);
    }

    public boolean isSolved() {
        return status == Status.SOLVED;
    }

    /** Renderiza la resolución completa (enunciado, pasos y resultado). */
    public String render(Locale locale, Format format) {
        return StepRenderer.render(this, locale, format);
    }

    @Override
    public String toString() {
        return render(Locale.forLanguageTag("es"), Format.PLAIN);
    }
}
