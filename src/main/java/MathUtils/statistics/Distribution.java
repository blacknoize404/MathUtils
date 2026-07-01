package MathUtils.statistics;

import java.util.Random;

/**
 * Distribución de probabilidad continua.
 */
public interface Distribution {

    /** Función de densidad de probabilidad. */
    double pdf(double x);

    /** Función de distribución acumulada {@code P(X ≤ x)}. */
    double cdf(double x);

    /**
     * Cuantil (inversa de la acumulada).
     * @param p probabilidad en [0, 1]
     * @throws IllegalArgumentException si {@code p} está fuera de [0, 1]
     */
    double quantile(double p);

    double mean();

    double variance();

    /** Genera una muestra aleatoria. */
    double sample(Random random);

    default double standardDeviation() {
        return Math.sqrt(variance());
    }

    /** Valida que {@code p} sea una probabilidad. */
    static void requireProbability(double p) {
        if (!(p >= 0 && p <= 1)) throw new IllegalArgumentException("La probabilidad debe estar en [0, 1]");
    }
}
