package barnesinside.mathutils.cas.steps;

import barnesinside.mathutils.cas.expr.Expr;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resultado de resolver un problema paso a paso.
 *
 * @param kind     tipo de problema
 * @param status   si se resolvió o por qué no
 * @param problem  expresión del problema (la función que se deriva, el integrando, la función de un límite)
 * @param result   resultado, o {@code null} si el estado no es {@link Status#SOLVED}
 * @param steps    árbol de pasos de la resolución
 * @param variable variable respecto a la que se opera
 * @param order    orden de la derivada (1 para el resto de problemas)
 * @param extras   datos propios del tipo de problema: {@code a} (punto de un límite) y
 *                 {@code direction} (1 por la derecha, -1 por la izquierda, 0 por ambos lados)
 */
public record Solution(Kind kind, Status status, Expr problem, Expr result, List<Step> steps, String variable,
                       int order, Map<String, Expr> extras) {

    /** Tipo de problema. */
    public enum Kind {DERIVATIVE, LIMIT, INTEGRAL}

    /** Estado final de la resolución. */
    public enum Status {
        /** Se obtuvo un resultado verificado. */
        SOLVED,
        /** El resultado existe pero no se puede escribir con funciones elementales. */
        NON_ELEMENTAL,
        /** El límite no existe (por ejemplo, los límites laterales son distintos). */
        DOES_NOT_EXIST,
        /** El motor no supo resolverlo (nunca devuelve un resultado dudoso). */
        UNSOLVED
    }

    public Solution {
        steps = List.copyOf(steps);
        extras = Collections.unmodifiableMap(new LinkedHashMap<>(extras));
    }

    public Solution(Kind kind, Status status, Expr problem, Expr result, List<Step> steps, String variable,
                    int order) {
        this(kind, status, problem, result, steps, variable, order, Map.of());
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
