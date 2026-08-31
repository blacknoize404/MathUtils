package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.steps.Messages;

import java.util.Locale;

/**
 * Método para factorizar un polinomio. {@link #AUTO} prueba los demás por orden hasta factorizar del todo.
 */
public enum FactorMethod {
    AUTO,
    COMMON_FACTOR,
    DIFFERENCE_OF_SQUARES,
    PERFECT_SQUARE,
    SUM_DIFFERENCE_OF_CUBES,
    TRINOMIAL,
    GROUPING,
    RATIONAL_ROOTS,
    QUADRATIC_FORMULA,
    COMPLETE_SQUARE,
    SUBSTITUTION;

    /** Nombre corto del método en el idioma pedido. */
    public String title(Locale locale) {
        return Messages.get("method.factor." + name() + ".title", locale);
    }

    /** Cuándo sirve el método y en qué consiste, en una frase. */
    public String description(Locale locale) {
        return Messages.get("method.factor." + name() + ".desc", locale);
    }
}
