package MathUtils.cas.poly;

import MathUtils.cas.expr.Expand;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.Rational;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Polinomio de una variable con coeficientes racionales exactos e inmutable.
 */
public final class Polynomial {

    /** Cociente y resto de una división. */
    public record DivMod(Polynomial quotient, Polynomial remainder) {
    }

    public static final Polynomial ZERO = new Polynomial(new Rational[0]);
    public static final Polynomial ONE = new Polynomial(new Rational[]{Rational.ONE});

    /** Coeficientes en orden ascendente de grado, sin ceros finales. */
    private final Rational[] coefficients;

    private Polynomial(Rational[] coefficients) {
        int length = coefficients.length;
        while (length > 0 && coefficients[length - 1].isZero()) length--;
        this.coefficients = Arrays.copyOf(coefficients, length);
    }

    /**
     * Polinomio con los coeficientes dados en orden ascendente: {@code of(1, 2, 3)} es 1 + 2x + 3x².
     */
    public static Polynomial of(Rational... ascending) {
        return new Polynomial(ascending.clone());
    }

    public static Polynomial of(long... ascending) {
        Rational[] c = new Rational[ascending.length];
        for (int i = 0; i < c.length; i++) c[i] = Rational.of(ascending[i]);
        return new Polynomial(c);
    }

    /** El monomio {@code coefficient · x^degree}. */
    public static Polynomial monomial(Rational coefficient, int degree) {
        Rational[] c = new Rational[degree + 1];
        Arrays.fill(c, Rational.ZERO);
        c[degree] = coefficient;
        return new Polynomial(c);
    }

    /**
     * Interpreta una expresión como polinomio en {@code variable} con coeficientes racionales.
     * Devuelve vacío si no lo es (otras variables, funciones, exponentes no enteros…).
     */
    public static Optional<Polynomial> from(Expr expr, String variable) {
        return convert(Expand.expand(expr), variable);
    }

    private static Optional<Polynomial> convert(Expr e, String x) {
        switch (e) {
            case Expr.Num n -> {
                return Optional.of(new Polynomial(new Rational[]{n.value()}));
            }
            case Expr.Sym s -> {
                return s.name().equals(x) ? Optional.of(monomial(Rational.ONE, 1)) : Optional.empty();
            }
            case Expr.Pow p -> {
                if (p.base() instanceof Expr.Sym s && s.name().equals(x)
                        && p.exponent() instanceof Expr.Num n && n.value().fitsInt() && n.value().signum() > 0) {
                    return Optional.of(monomial(Rational.ONE, n.value().intValueExact()));
                }
                return Optional.empty();
            }
            case Expr.Add a -> {
                Polynomial sum = ZERO;
                for (Expr term : a.terms()) {
                    Optional<Polynomial> t = convert(term, x);
                    if (t.isEmpty()) return Optional.empty();
                    sum = sum.add(t.get());
                }
                return Optional.of(sum);
            }
            case Expr.Mul m -> {
                Polynomial product = ONE;
                for (Expr factor : m.factors()) {
                    Optional<Polynomial> f = convert(factor, x);
                    if (f.isEmpty()) return Optional.empty();
                    product = product.multiply(f.get());
                }
                return Optional.of(product);
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    // ---------------------------------------------------------------- Consulta

    /** Grado del polinomio; {@code -1} para el polinomio cero. */
    public int degree() {
        return coefficients.length - 1;
    }

    public boolean isZero() {
        return coefficients.length == 0;
    }

    /** Coeficiente de {@code x^k} (cero si {@code k} supera el grado). */
    public Rational coefficient(int k) {
        return k >= 0 && k < coefficients.length ? coefficients[k] : Rational.ZERO;
    }

    /** Coeficiente principal (cero para el polinomio cero). */
    public Rational leading() {
        return isZero() ? Rational.ZERO : coefficients[coefficients.length - 1];
    }

    public Rational evaluate(Rational x) {
        Rational result = Rational.ZERO;
        for (int i = coefficients.length - 1; i >= 0; i--) result = result.multiply(x).add(coefficients[i]);
        return result;
    }

    // ---------------------------------------------------------------- Aritmética

    public Polynomial add(Polynomial o) {
        int n = Math.max(coefficients.length, o.coefficients.length);
        Rational[] c = new Rational[n];
        for (int i = 0; i < n; i++) c[i] = coefficient(i).add(o.coefficient(i));
        return new Polynomial(c);
    }

    public Polynomial subtract(Polynomial o) {
        return add(o.negate());
    }

    public Polynomial negate() {
        return scale(Rational.MINUS_ONE);
    }

    public Polynomial scale(Rational k) {
        Rational[] c = new Rational[coefficients.length];
        for (int i = 0; i < c.length; i++) c[i] = coefficients[i].multiply(k);
        return new Polynomial(c);
    }

    public Polynomial multiply(Polynomial o) {
        if (isZero() || o.isZero()) return ZERO;
        Rational[] c = new Rational[coefficients.length + o.coefficients.length - 1];
        Arrays.fill(c, Rational.ZERO);
        for (int i = 0; i < coefficients.length; i++)
            for (int j = 0; j < o.coefficients.length; j++)
                c[i + j] = c[i + j].add(coefficients[i].multiply(o.coefficients[j]));
        return new Polynomial(c);
    }

    public Polynomial pow(int exponent) {
        Polynomial result = ONE;
        for (int i = 0; i < exponent; i++) result = result.multiply(this);
        return result;
    }

    public Polynomial derivative() {
        if (coefficients.length <= 1) return ZERO;
        Rational[] c = new Rational[coefficients.length - 1];
        for (int i = 1; i < coefficients.length; i++) c[i - 1] = coefficients[i].multiply(Rational.of(i));
        return new Polynomial(c);
    }

    /**
     * División con resto: {@code this = quotient · divisor + remainder} con grado del resto menor
     * que el del divisor.
     * @throws ArithmeticException si el divisor es cero
     */
    public DivMod divide(Polynomial divisor) {
        if (divisor.isZero()) throw new ArithmeticException("División entre el polinomio cero");

        Polynomial quotient = ZERO;
        Polynomial remainder = this;
        while (!remainder.isZero() && remainder.degree() >= divisor.degree()) {
            Rational factor = remainder.leading().divide(divisor.leading());
            Polynomial term = monomial(factor, remainder.degree() - divisor.degree());
            quotient = quotient.add(term);
            remainder = remainder.subtract(term.multiply(divisor));
        }
        return new DivMod(quotient, remainder);
    }

    /** Máximo común divisor mónico (el cero si ambos son cero). */
    public Polynomial gcd(Polynomial o) {
        Polynomial a = this;
        Polynomial b = o;
        while (!b.isZero()) {
            Polynomial r = a.divide(b).remainder();
            a = b;
            b = r;
        }
        return a.isZero() ? a : a.scale(a.leading().reciprocal());
    }

    // ---------------------------------------------------------------- Conversión

    /** Expresión simbólica del polinomio en la variable dada. */
    public Expr toExpr(String variable) {
        List<Expr> terms = new ArrayList<>();
        Expr x = Expr.sym(variable);
        for (int i = 0; i < coefficients.length; i++) {
            if (coefficients[i].isZero()) continue;
            terms.add(Expr.mul(Expr.num(coefficients[i]), Expr.pow(x, Expr.num(i))));
        }
        return Expr.add(terms);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Polynomial p && Arrays.equals(coefficients, p.coefficients);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(coefficients);
    }

    @Override
    public String toString() {
        return toExpr("x").toString();
    }
}
