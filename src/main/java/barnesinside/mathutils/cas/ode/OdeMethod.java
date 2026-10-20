package barnesinside.mathutils.cas.ode;

import barnesinside.mathutils.cas.steps.Messages;

import java.util.Locale;

/**
 * Método para resolver una ecuación diferencial. {@link #AUTO} prueba los demás por orden y se queda con el primero que sirve.
 *
 * @since 0.11
 */
public enum OdeMethod {
    AUTO,
    /** {@code y' = g(x)·h(y)}: se separan las variables y se integra cada lado. */
    SEPARABLE,
    /** {@code y' + P(x)·y = Q(x)}: se multiplica por el factor integrante. */
    LINEAR,
    /** {@code M dx + N dy = 0} con {@code ∂M/∂y = ∂N/∂x} (o con un factor integrante que dependa de una sola variable). */
    EXACT,
    /** {@code y' = F(y/x)}: el cambio {@code y = v·x} la vuelve separable. */
    HOMOGENEOUS,
    /** {@code y' + P(x)·y = Q(x)·yⁿ}: el cambio {@code z = y^(1-n)} la vuelve lineal. */
    BERNOULLI,
    /** Lineal de orden n con coeficientes constantes: ecuación característica (y, si no es homogénea, una solución particular). */
    CONSTANT_COEFFICIENTS,
    /** La solución particular de la no homogénea por coeficientes indeterminados (el término independiente es polinomio por exponencial por seno o coseno). */
    UNDETERMINED_COEFFICIENTS,
    /** La solución particular de la no homogénea de segundo orden por variación de parámetros (sirve para cualquier término independiente integrable). */
    VARIATION_OF_PARAMETERS;

    public String title(Locale locale) {
        return Messages.get("method.ode." + name() + ".title", locale);
    }

    public String description(Locale locale) {
        return Messages.get("method.ode." + name() + ".desc", locale);
    }
}
