package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.steps.Messages;

import java.util.Locale;

/**
 * Método para resolver una ecuación. {@link #AUTO} elige el más natural para el tipo de ecuación.
 */
public enum SolveMethod {
    AUTO,
    /** Despejar la incógnita en una ecuación de primer grado. */
    ISOLATE,
    /** Factorizar y usar que un producto es cero solo si algún factor lo es. */
    FACTORING,
    /** {@code x² = k}: raíz cuadrada en los dos lados. */
    SQUARE_ROOT,
    COMPLETE_SQUARE,
    QUADRATIC_FORMULA,
    /** Teorema de la raíz racional y división sintética (Ruffini). */
    RATIONAL_ROOTS,
    /** Cambio de variable en ecuaciones con potencias {@code x^k}, o en una exponencial o trigonométrica. */
    SUBSTITUTION,
    /** Ecuaciones con fracciones: el numerador es cero y el denominador no. */
    ZERO_NUMERATOR,
    /** Aislar la raíz, elevar y comprobar. */
    ISOLATE_RADICAL,
    /** Resolver por separado los casos de un valor absoluto. */
    ABSOLUTE_CASES,
    /** Logaritmos y exponenciales: tomar logaritmos o pasar a forma exponencial. */
    LOGARITHMS,
    /** Ecuaciones trigonométricas con las funciones inversas y la periodicidad. */
    INVERSE_TRIG,
    /** Inecuaciones: puntos críticos y signo de la expresión en cada intervalo. */
    SIGN_CHART,
    /** Búsqueda numérica de raíces cuando no hay fórmula. */
    NUMERIC;

    public String title(Locale locale) {
        return Messages.get("method.solve." + name() + ".title", locale);
    }

    public String description(Locale locale) {
        return Messages.get("method.solve." + name() + ".desc", locale);
    }
}
