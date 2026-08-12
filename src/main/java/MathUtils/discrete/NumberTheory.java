package MathUtils.discrete;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Teoría de números: divisibilidad, primos, factorización y congruencias.
 * Las operaciones con desbordamiento de {@code long} lanzan {@link ArithmeticException}.
 */
public final class NumberTheory {

    private static final long[] MILLER_RABIN_BASES = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};

    private NumberTheory() {
    }

    /**
     * Resultado de {@link #extendedGcd(long, long)}: {@code gcd = a·x + b·y}.
     */
    public record Bezout(long gcd, long x, long y) {
    }

    /**
     * Resultado de {@link #chineseRemainder(long[], long[])}: {@code x ≡ remainder (mod modulus)}.
     */
    public record Congruence(long remainder, long modulus) {
    }

    /**
     * Máximo común divisor (no negativo).
     */
    public static long gcd(long a, long b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a;
    }

    /**
     * Mínimo común múltiplo (no negativo); 0 si alguno es 0.
     */
    public static long lcm(long a, long b) {
        if (a == 0 || b == 0) return 0;
        return Math.abs(Math.multiplyExact(a / gcd(a, b), b));
    }

    /**
     * MCD extendido: coeficientes de Bézout tales que {@code gcd = a·x + b·y}.
     */
    public static Bezout extendedGcd(long a, long b) {
        long oldR = a, r = b;
        long oldX = 1, x = 0;
        long oldY = 0, y = 1;
        while (r != 0) {
            long q = oldR / r;
            long t = oldR - q * r;
            oldR = r;
            r = t;
            t = oldX - q * x;
            oldX = x;
            x = t;
            t = oldY - q * y;
            oldY = y;
            y = t;
        }
        return oldR < 0 ? new Bezout(-oldR, -oldX, -oldY) : new Bezout(oldR, oldX, oldY);
    }

    /**
     * Test de primalidad determinista (Miller-Rabin con las 12 primeras bases primas,
     * válido para todo {@code long}).
     */
    public static boolean isPrime(long n) {
        if (n < 2) return false;
        for (long p : MILLER_RABIN_BASES) {
            if (n == p) return true;
            if (n % p == 0) return false;
        }

        long d = n - 1;
        int s = Long.numberOfTrailingZeros(d);
        d >>= s;
        BigInteger bigN = BigInteger.valueOf(n);

        for (long base : MILLER_RABIN_BASES) {
            BigInteger x = BigInteger.valueOf(base).modPow(BigInteger.valueOf(d), bigN);
            if (x.equals(BigInteger.ONE) || x.equals(bigN.subtract(BigInteger.ONE))) continue;

            boolean composite = true;
            for (int i = 1; i < s; i++) {
                x = x.multiply(x).mod(bigN);
                if (x.equals(bigN.subtract(BigInteger.ONE))) {
                    composite = false;
                    break;
                }
            }
            if (composite) return false;
        }
        return true;
    }

    /**
     * Criba de Eratóstenes.
     * @return los primos menores o iguales que {@code limit}
     */
    public static List<Integer> sieve(int limit) {
        List<Integer> primes = new ArrayList<>();
        if (limit < 2) return primes;

        boolean[] composite = new boolean[limit + 1];
        for (int i = 2; i <= limit; i++) {
            if (composite[i]) continue;
            primes.add(i);
            for (long j = (long) i * i; j <= limit; j += i) composite[(int) j] = true;
        }
        return primes;
    }

    /**
     * Factorización en primos por división de prueba.
     * @param n entero mayor o igual que 1
     * @return mapa primo → exponente, ordenado por primo (vacío para 1)
     */
    public static Map<Long, Integer> factorize(long n) {
        if (n < 1) throw new IllegalArgumentException("n debe ser mayor o igual que 1");

        Map<Long, Integer> factors = new TreeMap<>();
        for (long p = 2; p <= n / p; p += (p == 2 ? 1 : 2)) {
            while (n % p == 0) {
                factors.merge(p, 1, Integer::sum);
                n /= p;
            }
        }
        if (n > 1) factors.merge(n, 1, Integer::sum);
        return factors;
    }

    /**
     * Función φ de Euler: cantidad de enteros en [1, n] coprimos con n.
     */
    public static long totient(long n) {
        long result = n;
        for (long p : factorize(n).keySet()) result -= result / p;
        return result;
    }

    /**
     * {@code base^exponent mod modulus} por exponenciación binaria.
     * @param exponent exponente (≥ 0)
     * @param modulus módulo (> 0)
     */
    public static long modPow(long base, long exponent, long modulus) {
        if (modulus < 1) throw new IllegalArgumentException("El módulo debe ser positivo");
        if (exponent < 0) throw new IllegalArgumentException("El exponente debe ser mayor o igual que 0");
        return BigInteger.valueOf(base).modPow(BigInteger.valueOf(exponent), BigInteger.valueOf(modulus)).longValue();
    }

    /**
     * Inverso multiplicativo de {@code a} módulo {@code modulus}.
     * @throws ArithmeticException si {@code a} y el módulo no son coprimos
     */
    public static long modInverse(long a, long modulus) {
        if (modulus < 1) throw new IllegalArgumentException("El módulo debe ser positivo");
        Bezout b = extendedGcd(Math.floorMod(a, modulus), modulus);
        if (b.gcd() != 1) throw new ArithmeticException(a + " no tiene inverso módulo " + modulus);
        return Math.floorMod(b.x(), modulus);
    }

    /**
     * Teorema chino del resto: resuelve {@code x ≡ remainders[i] (mod moduli[i])}.
     * Admite módulos no coprimos siempre que el sistema sea compatible.
     * @return la solución única módulo el mínimo común múltiplo de los módulos
     * @throws ArithmeticException si el sistema no tiene solución
     */
    public static Congruence chineseRemainder(long[] remainders, long[] moduli) {
        if (remainders.length != moduli.length || remainders.length == 0) {
            throw new IllegalArgumentException("Se requieren listas no vacías del mismo tamaño");
        }

        long x = Math.floorMod(remainders[0], moduli[0]);
        long m = moduli[0];
        if (m < 1) throw new IllegalArgumentException("Los módulos deben ser positivos");

        for (int i = 1; i < moduli.length; i++) {
            long mi = moduli[i];
            if (mi < 1) throw new IllegalArgumentException("Los módulos deben ser positivos");
            long ai = Math.floorMod(remainders[i], mi);

            Bezout b = extendedGcd(m, mi);
            long difference = ai - x;
            if (difference % b.gcd() != 0) throw new ArithmeticException("El sistema de congruencias no tiene solución");

            long newModulus = Math.multiplyExact(m / b.gcd(), mi);
            long step = Math.floorMod(Math.multiplyExact(difference / b.gcd() % (mi / b.gcd()), b.x()), mi / b.gcd());
            x = Math.floorMod(Math.addExact(x, Math.multiplyExact(m, step)), newModulus);
            m = newModulus;
        }
        return new Congruence(x, m);
    }
}
