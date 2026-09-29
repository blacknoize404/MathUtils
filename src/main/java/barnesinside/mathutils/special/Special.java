package barnesinside.mathutils.special;

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

    private static final double[] LANCZOS = {0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
            -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7};

    /** La función gamma {@code Γ(x)} (aproximación de Lanczos); en los enteros no positivos no está definida (NaN). */
    public static double gamma(double x) {
        if (Double.isNaN(x)) return Double.NaN;
        if (x <= 0 && x == Math.rint(x)) return Double.NaN;
        if (x < 0.5) return Math.PI / (Math.sin(Math.PI * x) * gamma(1 - x));
        if (x > 171.7) return Double.POSITIVE_INFINITY;
        double shifted = x - 1;
        double sum = LANCZOS[0];
        double t = shifted + 7.5;
        for (int i = 1; i < 9; i++) sum += LANCZOS[i] / (shifted + i);
        return Math.sqrt(2 * Math.PI) * Math.pow(t, shifted + 0.5) * Math.exp(-t) * sum;
    }

    /** El factorial {@code x! = Γ(x + 1)}: en los enteros no negativos es el producto de 1 a x. */
    public static double factorial(double x) {
        if (x >= 0 && x == Math.rint(x) && x <= 170) {
            double result = 1;
            for (int i = 2; i <= (int) x; i++) result *= i;
            return result;
        }
        return gamma(x + 1);
    }

    /** La constante de Euler–Mascheroni {@code γ}. */
    public static final double EULER_GAMMA = 0.5772156649015329;

    /**
     * La función digamma {@code ψ(x) = Γ'(x) / Γ(x)}; no está definida (NaN) en los enteros no positivos.
     * Usa la recurrencia {@code ψ(x) = ψ(x + 1) − 1/x} hasta {@code x ≥ 10} y la serie asintótica, y la
     * reflexión {@code ψ(1 − x) − ψ(x) = π cot(πx)} para argumentos negativos.
     */
    public static double digamma(double x) {
        if (Double.isNaN(x)) return Double.NaN;
        if (x <= 0 && x == Math.rint(x)) return Double.NaN;
        if (x < 0) return digamma(1 - x) - Math.PI / Math.tan(Math.PI * x);
        double result = 0;
        while (x < 10) {
            result -= 1 / x;
            x += 1;
        }
        double inverse = 1 / x;
        double square = inverse * inverse;
        return result + Math.log(x) - inverse / 2
                - square * (1.0 / 12 - square * (1.0 / 120 - square * (1.0 / 252 - square * (1.0 / 240 - square / 132))));
    }

    /** Seno integral {@code Si(x) = ∫₀ˣ sen t / t dt}. */
    public static double si(double x) {
        if (Double.isNaN(x)) return Double.NaN;
        return simpson(t -> t == 0 ? 1 : Math.sin(t) / t, x);
    }

    /** Coseno integral {@code Ci(x) = γ + ln x + ∫₀ˣ (cos t − 1) / t dt}, definido para {@code x > 0}. */
    public static double ci(double x) {
        if (Double.isNaN(x) || x <= 0) return Double.NaN;
        return EULER_GAMMA + Math.log(x) + simpson(t -> t == 0 ? 0 : (Math.cos(t) - 1) / t, x);
    }

    /** Exponencial integral {@code Ei(x) = γ + ln|x| + ∫₀ˣ (eᵗ − 1) / t dt} (valor principal), con {@code x ≠ 0}. */
    public static double ei(double x) {
        if (Double.isNaN(x)) return Double.NaN;
        if (x == 0) return Double.NEGATIVE_INFINITY;
        return EULER_GAMMA + Math.log(Math.abs(x)) + simpson(t -> t == 0 ? 1 : (Math.exp(t) - 1) / t, x);
    }

    /** Logaritmo integral {@code li(x) = Ei(ln x)}, definido para {@code x > 0}. */
    public static double li(double x) {
        if (Double.isNaN(x) || x <= 0) return Double.NaN;
        if (x == 1) return Double.NEGATIVE_INFINITY;
        return ei(Math.log(x));
    }

    /** Integral de 0 a {@code x} con la regla de Simpson compuesta, que basta para integrandos analíticos. */
    private static double simpson(java.util.function.DoubleUnaryOperator f, double x) {
        if (x == 0) return 0;
        int n = (int) Math.min(400_000, 2 * Math.max(100, Math.ceil(100 * Math.abs(x))));
        double h = x / n;
        double sum = f.applyAsDouble(0) + f.applyAsDouble(x);
        for (int i = 1; i < n; i++) sum += f.applyAsDouble(i * h) * (i % 2 == 0 ? 2 : 4);
        return sum * h / 3;
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
