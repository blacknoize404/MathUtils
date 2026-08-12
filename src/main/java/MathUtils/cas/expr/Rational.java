package MathUtils.cas.expr;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Número racional exacto e inmutable, siempre normalizado: denominador positivo y
 * fracción irreducible.
 */
public final class Rational implements Comparable<Rational> {

    public static final Rational ZERO = new Rational(BigInteger.ZERO, BigInteger.ONE);
    public static final Rational ONE = new Rational(BigInteger.ONE, BigInteger.ONE);
    public static final Rational MINUS_ONE = new Rational(BigInteger.ONE.negate(), BigInteger.ONE);

    private final BigInteger numerator;
    private final BigInteger denominator;

    private Rational(BigInteger numerator, BigInteger denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public static Rational of(long value) {
        return of(BigInteger.valueOf(value), BigInteger.ONE);
    }

    public static Rational of(long numerator, long denominator) {
        return of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public static Rational of(BigInteger value) {
        return of(value, BigInteger.ONE);
    }

    /**
     * @throws ArithmeticException si el denominador es cero
     */
    public static Rational of(BigInteger numerator, BigInteger denominator) {
        if (denominator.signum() == 0) throw new ArithmeticException("El denominador no puede ser 0");
        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        BigInteger gcd = numerator.gcd(denominator);
        if (!gcd.equals(BigInteger.ONE) && gcd.signum() != 0) {
            numerator = numerator.divide(gcd);
            denominator = denominator.divide(gcd);
        }
        return new Rational(numerator, denominator);
    }

    /**
     * Convierte un decimal de texto ({@code "0.25"}, {@code "1e-3"}) a su valor racional exacto.
     */
    public static Rational parse(String text) {
        BigDecimal d = new BigDecimal(text);
        BigInteger unscaled = d.unscaledValue();
        int scale = d.scale();
        return scale >= 0
                ? of(unscaled, BigInteger.TEN.pow(scale))
                : of(unscaled.multiply(BigInteger.TEN.pow(-scale)), BigInteger.ONE);
    }

    /**
     * Convierte un {@code double} a racional usando su representación decimal más corta
     * (así {@code 0.1} da {@code 1/10}).
     * @throws IllegalArgumentException si no es un número finito
     */
    public static Rational fromDouble(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("El valor debe ser finito");
        return parse(Double.toString(value));
    }

    public BigInteger numerator() {
        return numerator;
    }

    public BigInteger denominator() {
        return denominator;
    }

    public Rational add(Rational o) {
        return of(numerator.multiply(o.denominator).add(o.numerator.multiply(denominator)),
                denominator.multiply(o.denominator));
    }

    public Rational subtract(Rational o) {
        return add(o.negate());
    }

    public Rational multiply(Rational o) {
        return of(numerator.multiply(o.numerator), denominator.multiply(o.denominator));
    }

    /**
     * @throws ArithmeticException si el divisor es cero
     */
    public Rational divide(Rational o) {
        if (o.isZero()) throw new ArithmeticException("División por 0");
        return of(numerator.multiply(o.denominator), denominator.multiply(o.numerator));
    }

    public Rational negate() {
        return new Rational(numerator.negate(), denominator);
    }

    public Rational abs() {
        return signum() < 0 ? negate() : this;
    }

    public Rational reciprocal() {
        return ONE.divide(this);
    }

    /**
     * Potencia entera (el exponente puede ser negativo).
     */
    public Rational pow(int exponent) {
        if (exponent == 0) return ONE;
        if (exponent < 0) return reciprocal().pow(-exponent);
        return new Rational(numerator.pow(exponent), denominator.pow(exponent));
    }

    public int signum() {
        return numerator.signum();
    }

    public boolean isZero() {
        return numerator.signum() == 0;
    }

    public boolean isOne() {
        return numerator.equals(BigInteger.ONE) && denominator.equals(BigInteger.ONE);
    }

    public boolean isInteger() {
        return denominator.equals(BigInteger.ONE);
    }

    /**
     * @throws ArithmeticException si no es un entero que quepa en un {@code int}
     */
    public int intValueExact() {
        if (!isInteger()) throw new ArithmeticException(this + " no es un entero");
        return numerator.intValueExact();
    }

    public boolean fitsInt() {
        return isInteger() && numerator.bitLength() < 32;
    }

    public Rational floor() {
        return of(numerator.divide(denominator).subtract(
                numerator.signum() < 0 && !isInteger() ? BigInteger.ONE : BigInteger.ZERO));
    }

    public double doubleValue() {
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), java.math.MathContext.DECIMAL64).doubleValue();
    }

    @Override
    public int compareTo(Rational o) {
        return numerator.multiply(o.denominator).compareTo(o.numerator.multiply(denominator));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Rational r && numerator.equals(r.numerator) && denominator.equals(r.denominator);
    }

    @Override
    public int hashCode() {
        return 31 * numerator.hashCode() + denominator.hashCode();
    }

    @Override
    public String toString() {
        return isInteger() ? numerator.toString() : numerator + "/" + denominator;
    }
}
