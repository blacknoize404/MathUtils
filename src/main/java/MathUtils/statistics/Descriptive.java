package MathUtils.statistics;

import java.util.Arrays;
import java.util.stream.DoubleStream;

/**
 * Estadísticos descriptivos sobre un conjunto de datos.
 * Todos los métodos lanzan {@link IllegalArgumentException} si los datos están vacíos
 * (o no son suficientes para el estadístico).
 */
public final class Descriptive {

    private Descriptive() {
    }

    public static double mean(double[] data) {
        requireNonEmpty(data);
        return DoubleStream.of(data).sum() / data.length;
    }

    public static double min(double[] data) {
        requireNonEmpty(data);
        return DoubleStream.of(data).min().getAsDouble();
    }

    public static double max(double[] data) {
        requireNonEmpty(data);
        return DoubleStream.of(data).max().getAsDouble();
    }

    /**
     * Mediana (percentil 50).
     */
    public static double median(double[] data) {
        return percentile(data, 50);
    }

    /**
     * Valores más frecuentes, en orden creciente (más de uno si hay empate).
     */
    public static double[] modes(double[] data) {
        requireNonEmpty(data);
        double[] sorted = data.clone();
        Arrays.sort(sorted);

        int best = 0;
        double[] result = new double[sorted.length];
        int count = 0;
        for (int i = 0; i < sorted.length; ) {
            int j = i;
            while (j < sorted.length && sorted[j] == sorted[i]) j++;
            int frequency = j - i;
            if (frequency > best) {
                best = frequency;
                count = 0;
            }
            if (frequency == best) result[count++] = sorted[i];
            i = j;
        }
        return Arrays.copyOf(result, count);
    }

    /**
     * Varianza muestral (divide entre n - 1). Requiere al menos 2 datos.
     */
    public static double variance(double[] data) {
        return variance(data, true);
    }

    /**
     * @param sample true para la varianza muestral (n - 1), false para la poblacional (n)
     */
    public static double variance(double[] data, boolean sample) {
        requireNonEmpty(data);
        if (sample && data.length < 2) throw new IllegalArgumentException("La varianza muestral requiere al menos 2 datos");
        return sumOfSquares(data) / (sample ? data.length - 1 : data.length);
    }

    /**
     * Desviación estándar muestral.
     */
    public static double standardDeviation(double[] data) {
        return Math.sqrt(variance(data, true));
    }

    public static double standardDeviation(double[] data, boolean sample) {
        return Math.sqrt(variance(data, sample));
    }

    /**
     * Coeficiente de asimetría poblacional {@code m3 / m2^(3/2)}.
     * @throws ArithmeticException si todos los datos son iguales
     */
    public static double skewness(double[] data) {
        double m2 = centralMoment(data, 2);
        if (m2 == 0) throw new ArithmeticException("La asimetría no está definida cuando la varianza es 0");
        return centralMoment(data, 3) / Math.pow(m2, 1.5);
    }

    /**
     * Curtosis en exceso poblacional {@code m4 / m2² - 3} (0 para una normal).
     * @throws ArithmeticException si todos los datos son iguales
     */
    public static double kurtosis(double[] data) {
        double m2 = centralMoment(data, 2);
        if (m2 == 0) throw new ArithmeticException("La curtosis no está definida cuando la varianza es 0");
        return centralMoment(data, 4) / (m2 * m2) - 3;
    }

    /**
     * Percentil con interpolación lineal entre rangos (método 7 de Hyndman y Fan).
     * @param p percentil en [0, 100]
     */
    public static double percentile(double[] data, double p) {
        requireNonEmpty(data);
        if (p < 0 || p > 100) throw new IllegalArgumentException("El percentil debe estar entre 0 y 100");

        double[] sorted = data.clone();
        Arrays.sort(sorted);
        double rank = p / 100 * (sorted.length - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        return sorted[lower] + (rank - lower) * (sorted[upper] - sorted[lower]);
    }

    /**
     * Covarianza muestral. Requiere dos conjuntos del mismo tamaño y al menos 2 datos.
     */
    public static double covariance(double[] x, double[] y) {
        requireSameSize(x, y);
        if (x.length < 2) throw new IllegalArgumentException("La covarianza requiere al menos 2 datos");
        double meanX = mean(x);
        double meanY = mean(y);
        double sum = 0;
        for (int i = 0; i < x.length; i++) sum += (x[i] - meanX) * (y[i] - meanY);
        return sum / (x.length - 1);
    }

    /**
     * Coeficiente de correlación de Pearson.
     * @throws ArithmeticException si alguno de los conjuntos tiene varianza 0
     */
    public static double correlation(double[] x, double[] y) {
        double denominator = standardDeviation(x) * standardDeviation(y);
        if (denominator == 0) throw new ArithmeticException("La correlación no está definida cuando una varianza es 0");
        return covariance(x, y) / denominator;
    }

    private static double sumOfSquares(double[] data) {
        double mean = mean(data);
        double sum = 0;
        for (double v : data) sum += (v - mean) * (v - mean);
        return sum;
    }

    private static double centralMoment(double[] data, int order) {
        double mean = mean(data);
        double sum = 0;
        for (double v : data) sum += Math.pow(v - mean, order);
        return sum / data.length;
    }

    private static void requireNonEmpty(double[] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("Los datos no pueden estar vacíos");
    }

    private static void requireSameSize(double[] x, double[] y) {
        requireNonEmpty(x);
        requireNonEmpty(y);
        if (x.length != y.length) throw new IllegalArgumentException("Los conjuntos deben tener el mismo tamaño");
    }
}
