package MathUtils.special;

/**
 * Funciones especiales.
 */
public final class Special {

    private Special() {
    }

    /**
     * Función de error {@code erf(x) = 2/√π ∫₀ˣ e^(-t²) dt}.
     * <p>Usa la serie {@code 2/√π · e^(-x²) · Σ 2ⁿ x^(2n+1) / (2n+1)!!}, cuyos términos son
     * todos positivos y por tanto no sufre cancelación.
     */
    public static double erf(double x) {
        if (Double.isNaN(x)) return Double.NaN;
        if (x < 0) return -erf(-x);
        if (x > 6) return 1;

        double term = x;
        double sum = x;
        for (int n = 0; n < 500 && term > 1e-17 * sum; n++) {
            term *= 2 * x * x / (2 * n + 3);
            sum += term;
        }
        return 2 / Math.sqrt(Math.PI) * Math.exp(-x * x) * sum;
    }

    /**
     * Función de error complementaria {@code erfc(x) = 1 - erf(x)}.
     */
    public static double erfc(double x) {
        return 1 - erf(x);
    }

    /**
     * Logaritmo natural del factorial, {@code ln(n!)}, sumando logaritmos.
     * @throws IllegalArgumentException si {@code n} es negativo
     */
    public static double logFactorial(int n) {
        if (n < 0) throw new IllegalArgumentException("n debe ser mayor o igual que 0");
        double sum = 0;
        for (int i = 2; i <= n; i++) sum += Math.log(i);
        return sum;
    }

    /**
     * {@code ln(C(n, k))}.
     */
    public static double logBinomial(int n, int k) {
        if (k < 0 || k > n) throw new IllegalArgumentException("Se requiere 0 <= k <= n");
        return logFactorial(n) - logFactorial(k) - logFactorial(n - k);
    }
}
