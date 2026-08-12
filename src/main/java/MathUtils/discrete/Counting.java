package MathUtils.discrete;

import java.math.BigInteger;

/**
 * Conteo combinatorio exacto con {@link BigInteger}: factoriales, permutaciones, combinaciones,
 * números de Catalan, de Stirling de segunda especie y particiones.
 */
public final class Counting {

    private Counting() {
    }

    public static BigInteger factorial(int n) {
        requireNonNegative(n);
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) result = result.multiply(BigInteger.valueOf(i));
        return result;
    }

    /**
     * Permutaciones de {@code k} elementos tomados de {@code n}: {@code n! / (n-k)!}.
     */
    public static BigInteger permutations(int n, int k) {
        requireChoose(n, k);
        BigInteger result = BigInteger.ONE;
        for (int i = n - k + 1; i <= n; i++) result = result.multiply(BigInteger.valueOf(i));
        return result;
    }

    /**
     * Coeficiente binomial {@code C(n, k)}.
     */
    public static BigInteger combinations(int n, int k) {
        requireChoose(n, k);
        k = Math.min(k, n - k);
        BigInteger result = BigInteger.ONE;
        for (int i = 1; i <= k; i++) {
            result = result.multiply(BigInteger.valueOf(n - k + i)).divide(BigInteger.valueOf(i));
        }
        return result;
    }

    /**
     * Número de Catalan {@code C(2n, n) / (n + 1)}.
     */
    public static BigInteger catalan(int n) {
        requireNonNegative(n);
        return combinations(2 * n, n).divide(BigInteger.valueOf(n + 1));
    }

    /**
     * Número de Stirling de segunda especie {@code S(n, k)}: formas de repartir {@code n}
     * elementos en {@code k} subconjuntos no vacíos.
     */
    public static BigInteger stirlingSecond(int n, int k) {
        requireNonNegative(n);
        requireNonNegative(k);
        if (k > n) return BigInteger.ZERO;

        // S(i, j) = j·S(i-1, j) + S(i-1, j-1)
        BigInteger[] row = new BigInteger[k + 1];
        java.util.Arrays.fill(row, BigInteger.ZERO);
        row[0] = BigInteger.ONE;
        for (int i = 1; i <= n; i++) {
            for (int j = Math.min(i, k); j >= 1; j--) {
                row[j] = row[j].multiply(BigInteger.valueOf(j)).add(row[j - 1]);
            }
            row[0] = BigInteger.ZERO;
        }
        return row[k];
    }

    /**
     * Número de particiones de {@code n} (formas de escribirlo como suma de enteros positivos
     * sin importar el orden), por programación dinámica.
     */
    public static BigInteger partitions(int n) {
        requireNonNegative(n);
        BigInteger[] ways = new BigInteger[n + 1];
        java.util.Arrays.fill(ways, BigInteger.ZERO);
        ways[0] = BigInteger.ONE;
        for (int part = 1; part <= n; part++) {
            for (int total = part; total <= n; total++) {
                ways[total] = ways[total].add(ways[total - part]);
            }
        }
        return ways[n];
    }

    private static void requireNonNegative(int n) {
        if (n < 0) throw new IllegalArgumentException("El argumento debe ser mayor o igual que 0");
    }

    private static void requireChoose(int n, int k) {
        if (n < 0 || k < 0 || k > n) throw new IllegalArgumentException("Se requiere 0 <= k <= n");
    }
}
