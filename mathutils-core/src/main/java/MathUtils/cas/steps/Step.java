package MathUtils.cas.steps;

import MathUtils.cas.expr.Expr;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Un paso de una resolución: la regla aplicada, la expresión antes y después, los parámetros
 * de la regla (por ejemplo {@code f} y {@code g} en la regla del producto) y los subpasos
 * que lo justifican. Un conjunto de pasos forma un árbol de justificación.
 *
 * @param rule    regla o transformación aplicada
 * @param before  expresión sobre la que se aplica la regla (puede ser {@code null})
 * @param after   resultado del paso (puede ser {@code null})
 * @param params  parámetros con nombre usados por el texto de la regla
 * @param substeps pasos intermedios, en orden
 */
public record Step(Rule rule, Expr before, Expr after, Map<String, Expr> params, List<Step> substeps) {

    public Step {
        params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
        substeps = List.copyOf(substeps);
    }

    /** Título corto de la regla en el idioma pedido (por ejemplo, "Regla del producto"). */
    public String title(Locale locale) {
        return Messages.title(rule, locale);
    }

    /** Explicación de la regla con las expresiones intercaladas como segmentos. */
    public List<Segment> explanation(Locale locale) {
        return Messages.explanation(rule, params, locale);
    }

    /** Variable de la operación (parámetro {@code x}), o {@code null} si no hay. */
    public Expr variable() {
        return params.get("x");
    }
}
