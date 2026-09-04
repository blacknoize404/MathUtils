package barnesinside.mathutils.cas.integral;

import barnesinside.mathutils.cas.steps.Messages;

import java.util.Locale;

/**
 * Técnica con la que se empieza a resolver una integral. {@link #AUTO} prueba todas por orden y usa la primera que
 * sirve; las demás obligan a usar esa técnica en el primer paso (las integrales que salgan después se resuelven
 * con cualquiera).
 */
public enum IntegralMethod {
    AUTO,
    /** Integral de una suma o de una constante por una función. */
    LINEARITY,
    /** Integrales inmediatas de la tabla. */
    TABLE,
    SUBSTITUTION,
    BY_PARTS,
    PARTIAL_FRACTIONS,
    /** Identidades trigonométricas: potencias de seno y coseno, ángulo mitad, producto a suma. */
    TRIGONOMETRIC,
    /** Raíces: sustitución de la raíz, completar el cuadrado y sustitución trigonométrica. */
    RADICAL,
    /** Denominador de segundo grado: completar el cuadrado. */
    COMPLETE_SQUARE,
    /** Fórmulas de reducción de potencias. */
    REDUCTION,
    POLYNOMIAL_DIVISION,
    /** Desarrollar productos y potencias e integrar término a término. */
    EXPAND;

    public String title(Locale locale) {
        return Messages.get("method.integral." + name() + ".title", locale);
    }

    public String description(Locale locale) {
        return Messages.get("method.integral." + name() + ".desc", locale);
    }
}
