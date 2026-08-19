package barnesinside.mathutils.discrete;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

class DiscreteTest {

    private static BigInteger big(long v) {
        return BigInteger.valueOf(v);
    }

    @Test
    void gcdAndLcm() {
        assert NumberTheory.gcd(12, 18) == 6;
        assert NumberTheory.gcd(-12, 18) == 6;
        assert NumberTheory.gcd(0, 5) == 5;
        assert NumberTheory.gcd(0, 0) == 0;
        assert NumberTheory.lcm(4, 6) == 12;
        assert NumberTheory.lcm(0, 6) == 0;
        assert NumberTheory.lcm(-4, 6) == 12;
    }

    @Test
    void extendedGcd() {
        for (long[] pair : new long[][]{{240, 46}, {17, 5}, {-30, 12}, {7, 0}, {0, 9}}) {
            NumberTheory.Bezout b = NumberTheory.extendedGcd(pair[0], pair[1]);
            assert b.gcd() == NumberTheory.gcd(pair[0], pair[1]);
            assert pair[0] * b.x() + pair[1] * b.y() == b.gcd() : pair[0] + "," + pair[1];
        }
    }

    @Test
    void primality() {
        assert !NumberTheory.isPrime(-7) && !NumberTheory.isPrime(0) && !NumberTheory.isPrime(1);
        assert NumberTheory.isPrime(2) && NumberTheory.isPrime(3) && NumberTheory.isPrime(97);
        assert !NumberTheory.isPrime(91) && !NumberTheory.isPrime(1_000_000);
        assert NumberTheory.isPrime(1_000_000_007L);
        assert NumberTheory.isPrime(2_147_483_647L);
        assert NumberTheory.isPrime(Long.MAX_VALUE - 24); // 9223372036854775783, el mayor primo de 63 bits
        assert !NumberTheory.isPrime(3_215_031_751L); // pseudoprimo fuerte en bases 2, 3, 5 y 7
        assert !NumberTheory.isPrime(561); // número de Carmichael
    }

    @Test
    void sieve() {
        assert NumberTheory.sieve(1).isEmpty();
        assert NumberTheory.sieve(30).equals(List.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29));
        assert NumberTheory.sieve(100).size() == 25;
        assert NumberTheory.sieve(100_000).size() == 9592;
    }

    @Test
    void factorize() {
        assert NumberTheory.factorize(1).isEmpty();
        assert NumberTheory.factorize(360).equals(Map.of(2L, 3, 3L, 2, 5L, 1));
        assert NumberTheory.factorize(97).equals(Map.of(97L, 1));
        assert NumberTheory.factorize(1_000_000_007L * 3).equals(Map.of(3L, 1, 1_000_000_007L, 1));
        try {
            NumberTheory.factorize(0);
            assert false;
        } catch (IllegalArgumentException e) {
            assert true;
        }
    }

    @Test
    void totient() {
        assert NumberTheory.totient(1) == 1;
        assert NumberTheory.totient(9) == 6;
        assert NumberTheory.totient(10) == 4;
        assert NumberTheory.totient(97) == 96;
        assert NumberTheory.totient(360) == 96;
    }

    @Test
    void modPowAndInverse() {
        assert NumberTheory.modPow(2, 10, 1000) == 24;
        assert NumberTheory.modPow(5, 0, 7) == 1;
        assert NumberTheory.modPow(-2, 3, 5) == 2;
        assert NumberTheory.modInverse(3, 11) == 4;
        assert NumberTheory.modInverse(-3, 11) == 7;
        assert (NumberTheory.modInverse(17, 3120) * 17) % 3120 == 1;
        try {
            NumberTheory.modInverse(4, 8);
            assert false;
        } catch (ArithmeticException e) {
            assert true;
        }
    }

    @Test
    void chineseRemainderCoprime() {
        // x = 2 (3), x = 3 (5), x = 2 (7) -> 23 (mod 105)
        NumberTheory.Congruence c = NumberTheory.chineseRemainder(new long[]{2, 3, 2}, new long[]{3, 5, 7});
        assert c.remainder() == 23 && c.modulus() == 105;
    }

    @Test
    void chineseRemainderNonCoprime() {
        // x = 2 (6), x = 4 (8) -> x = 20 (24)
        NumberTheory.Congruence c = NumberTheory.chineseRemainder(new long[]{2, 4}, new long[]{6, 8});
        assert c.modulus() == 24;
        assert c.remainder() % 6 == 2 && c.remainder() % 8 == 4;
    }

    @Test
    void chineseRemainderIncompatible() {
        try {
            NumberTheory.chineseRemainder(new long[]{1, 2}, new long[]{4, 6});
            assert false;
        } catch (ArithmeticException e) {
            assert true;
        }
    }

    @Test
    void factorialPermutationsCombinations() {
        assert Counting.factorial(0).equals(BigInteger.ONE);
        assert Counting.factorial(20).equals(big(2432902008176640000L));
        assert Counting.factorial(25).toString().equals("15511210043330985984000000");
        assert Counting.permutations(5, 2).equals(big(20));
        assert Counting.permutations(5, 0).equals(BigInteger.ONE);
        assert Counting.combinations(5, 2).equals(big(10));
        assert Counting.combinations(52, 5).equals(big(2598960));
        assert Counting.combinations(100, 50).toString().equals("100891344545564193334812497256");
        assert Counting.combinations(7, 7).equals(BigInteger.ONE);
    }

    @Test
    void catalanStirlingPartitions() {
        long[] catalan = {1, 1, 2, 5, 14, 42, 132, 429};
        for (int i = 0; i < catalan.length; i++) assert Counting.catalan(i).equals(big(catalan[i])) : i;

        assert Counting.stirlingSecond(4, 2).equals(big(7));
        assert Counting.stirlingSecond(5, 3).equals(big(25));
        assert Counting.stirlingSecond(0, 0).equals(BigInteger.ONE);
        assert Counting.stirlingSecond(3, 5).equals(BigInteger.ZERO);
        assert Counting.stirlingSecond(6, 1).equals(BigInteger.ONE);

        assert Counting.partitions(0).equals(BigInteger.ONE);
        assert Counting.partitions(5).equals(big(7));
        assert Counting.partitions(10).equals(big(42));
        assert Counting.partitions(100).toString().equals("190569292");
    }

    @Test
    void invalidArgumentsThrow() {
        for (Runnable r : new Runnable[]{
                () -> Counting.factorial(-1),
                () -> Counting.combinations(3, 5),
                () -> Counting.permutations(3, -1),
                () -> Counting.catalan(-2)}) {
            try {
                r.run();
                assert false;
            } catch (IllegalArgumentException e) {
                assert true;
            }
        }
    }
}
