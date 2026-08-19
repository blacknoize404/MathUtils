package barnesinside.mathutils.statistics;

import java.util.Random;

/**
 * Distribución de probabilidad discreta sobre los enteros no negativos.
 */
public interface DiscreteDistribution {

    /** Función de masa de probabilidad {@code P(X = k)}. */
    double pmf(int k);

    /** Función de distribución acumulada {@code P(X ≤ k)}. */
    double cdf(int k);

    /**
     * Cuantil: el menor {@code k} tal que {@code P(X ≤ k) ≥ p}.
     * @throws IllegalArgumentException si {@code p} está fuera del rango admitido
     */
    int quantile(double p);

    double mean();

    double variance();

    /** Genera una muestra aleatoria. */
    int sample(Random random);

    default double standardDeviation() {
        return Math.sqrt(variance());
    }
}
