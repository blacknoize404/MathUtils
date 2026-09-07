package barnesinside.mathutils.cas.diff;

import barnesinside.mathutils.cas.steps.Messages;

import java.util.Locale;

/**
 * Cómo preparar o plantear la derivación. {@link #AUTO} aplica directamente las reglas (suma, producto, cociente,
 * cadena...); los demás cambian el planteamiento y llegan al mismo resultado por otro camino.
 */
public enum DerivativeMethod {
    AUTO,
    /** Desarrollar productos y potencias de sumas y derivar término a término. */
    EXPAND_FIRST,
    /** Juntar fracciones y cancelar factores comunes antes de derivar. */
    SIMPLIFY_FIRST,
    /** No usar la regla del cociente: tratar el denominador como una potencia negativa (producto y cadena). */
    QUOTIENT_AS_PRODUCT,
    /** Tomar logaritmos, derivar {@code ln f} y despejar. Útil para productos largos y potencias variables. */
    LOGARITHMIC;

    public String title(Locale locale) {
        return Messages.get("method.derivative." + name() + ".title", locale);
    }

    public String description(Locale locale) {
        return Messages.get("method.derivative." + name() + ".desc", locale);
    }
}
