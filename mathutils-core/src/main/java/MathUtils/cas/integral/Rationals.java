package MathUtils.cas.integral;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.Rational;
import MathUtils.cas.poly.Polynomial;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Álgebra exacta de funciones racionales: factorización de un denominador sobre los racionales y
 * descomposición en fracciones parciales. No registra pasos; los usa el {@link Integrator}.
 * <p>Solo factoriza lo que puede garantizar: factores lineales (raíces racionales) y cuadráticos
 * sin raíces racionales. Cualquier otro resto se declara no factorizable.
 */
final class Rationals {

    private static final BigInteger SEARCH_LIMIT = BigInteger.TEN.pow(12);

    /** Factor mónico (lineal, o cuadrático sin raíces racionales) con su multiplicidad. */
    record Factor(Polynomial base, int multiplicity) {
        boolean isLinear() {
            return base.degree() == 1;
        }
    }

    /** {@code constant · ∏ base^multiplicity}. */
    record Factorization(Rational constant, List<Factor> factors) {
        Expr toExpr(String v) {
            List<Expr> parts = new ArrayList<>();
            if (!constant.isOne()) parts.add(Expr.num(constant));
            for (Factor f : factors) parts.add(Expr.pow(f.base().toExpr(v), Expr.num(f.multiplicity())));
            return Expr.mul(parts);
        }
    }

    /** Término {@code numerator / base^power} de una descomposición. */
    record Term(Polynomial numerator, Polynomial base, int power) {
        Expr toExpr(String v) {
            return Expr.div(numerator.toExpr(v), Expr.pow(base.toExpr(v), Expr.num(power)));
        }
    }

    private Rationals() {
    }

    // ================================================================ Factorización

    static Optional<Factorization> factor(Polynomial p) {
        if (p.degree() < 1) return Optional.empty();
        Rational constant = p.leading();
        Polynomial monic = p.scale(constant.reciprocal());

        List<Factor> factors = new ArrayList<>();
        List<Polynomial> squareFree = squareFreeParts(monic);
        for (int i = 0; i < squareFree.size(); i++) {
            Polynomial part = squareFree.get(i);
            if (part.degree() < 1) continue;
            int multiplicity = i + 1;
            if (!splitSquareFree(part, multiplicity, factors)) return Optional.empty();
        }
        factors.sort((a, b) -> a.base().degree() != b.base().degree()
                ? Integer.compare(a.base().degree(), b.base().degree())
                : a.base().coefficient(0).compareTo(b.base().coefficient(0)));
        return Optional.of(new Factorization(constant, factors));
    }

    /** Descomposición de Yun: {@code p = ∏ a_i^i} con cada {@code a_i} sin raíces repetidas. */
    private static List<Polynomial> squareFreeParts(Polynomial p) {
        List<Polynomial> result = new ArrayList<>();
        Polynomial derivative = p.derivative();
        Polynomial a0 = p.gcd(derivative);
        Polynomial b = p.divide(a0).quotient();
        Polynomial c = derivative.divide(a0).quotient();
        Polynomial d = c.subtract(b.derivative());
        while (b.degree() >= 1) {
            Polynomial a = b.gcd(d);
            result.add(a);
            b = b.divide(a).quotient();
            c = d.divide(a).quotient();
            d = c.subtract(b.derivative());
        }
        return result;
    }

    /** Separa un polinomio mónico sin raíces repetidas en factores lineales y un cuadrático. */
    private static boolean splitSquareFree(Polynomial part, int multiplicity, List<Factor> out) {
        Polynomial rest = part;
        while (rest.degree() >= 1) {
            Optional<Rational> root = rationalRoot(rest);
            if (root.isEmpty()) break;
            Polynomial linear = Polynomial.of(root.get().negate(), Rational.ONE);
            out.add(new Factor(linear, multiplicity));
            rest = rest.divide(linear).quotient();
        }
        if (rest.degree() < 1) return true;
        if (rest.degree() == 2) {
            Rational a = rest.coefficient(2);
            Rational discriminant = rest.coefficient(1).multiply(rest.coefficient(1))
                    .subtract(Rational.of(4).multiply(a).multiply(rest.coefficient(0)));
            // Sin raíces racionales: el discriminante es negativo o positivo sin raíz exacta
            if (discriminant.signum() != 0) {
                out.add(new Factor(rest.scale(a.reciprocal()), multiplicity));
                return true;
            }
        }
        return false;
    }

    /** Una raíz racional (teorema de la raíz racional), o vacío. */
    static Optional<Rational> rationalRoot(Polynomial p) {
        if (p.isZero()) return Optional.empty();
        if (p.coefficient(0).isZero()) return Optional.of(Rational.ZERO);

        BigInteger scale = BigInteger.ONE;
        for (int i = 0; i <= p.degree(); i++) {
            BigInteger den = p.coefficient(i).denominator();
            scale = scale.divide(scale.gcd(den)).multiply(den);
        }
        BigInteger constant = p.coefficient(0).multiply(Rational.of(scale)).numerator().abs();
        BigInteger leading = p.leading().multiply(Rational.of(scale)).numerator().abs();
        if (constant.compareTo(SEARCH_LIMIT) > 0 || leading.compareTo(SEARCH_LIMIT) > 0) return Optional.empty();

        List<BigInteger> numerators = divisors(constant);
        List<BigInteger> denominators = divisors(leading);
        for (BigInteger n : numerators) {
            for (BigInteger d : denominators) {
                if (!n.gcd(d).equals(BigInteger.ONE)) continue;
                Rational candidate = Rational.of(n, d);
                if (p.evaluate(candidate).isZero()) return Optional.of(candidate);
                if (p.evaluate(candidate.negate()).isZero()) return Optional.of(candidate.negate());
            }
        }
        return Optional.empty();
    }

    private static List<BigInteger> divisors(BigInteger n) {
        List<BigInteger> result = new ArrayList<>();
        long value = n.longValueExact();
        for (long i = 1; i * i <= value; i++) {
            if (value % i != 0) continue;
            result.add(BigInteger.valueOf(i));
            if (i != value / i) result.add(BigInteger.valueOf(value / i));
        }
        return result;
    }

    // ================================================================ Fracciones parciales

    /**
     * Descompone {@code numerator / (constant · ∏ base^m)} (con el grado del numerador menor que
     * el del denominador) en términos con denominador {@code base^k}. Los coeficientes salen de un
     * sistema lineal exacto.
     */
    static Optional<List<Term>> partialFractions(Polynomial numerator, Factorization denominator) {
        Polynomial full = Polynomial.ONE;
        for (Factor f : denominator.factors()) full = full.multiply(f.base().pow(f.multiplicity()));
        int n = full.degree();
        if (numerator.degree() >= n) return Optional.empty();

        // Una columna por cada incógnita: x^j · full / base^k
        List<Polynomial> columns = new ArrayList<>();
        List<Term> shapes = new ArrayList<>();
        for (Factor f : denominator.factors()) {
            for (int k = 1; k <= f.multiplicity(); k++) {
                Polynomial cofactor = full.divide(f.base().pow(k)).quotient();
                for (int j = 0; j < f.base().degree(); j++) {
                    columns.add(cofactor.multiply(Polynomial.monomial(Rational.ONE, j)));
                    shapes.add(new Term(Polynomial.monomial(Rational.ONE, j), f.base(), k));
                }
            }
        }
        if (columns.size() != n) return Optional.empty();

        Rational[][] matrix = new Rational[n][n + 1];
        Polynomial target = numerator.scale(denominator.constant().reciprocal());
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) matrix[row][col] = columns.get(col).coefficient(row);
            matrix[row][n] = target.coefficient(row);
        }
        Optional<Rational[]> solution = solve(matrix, n);
        if (solution.isEmpty()) return Optional.empty();

        // Se agrupan los monomios de un mismo (base, k) en un único numerador
        List<Term> terms = new ArrayList<>();
        int index = 0;
        for (Factor f : denominator.factors()) {
            for (int k = 1; k <= f.multiplicity(); k++) {
                Polynomial coefficients = Polynomial.ZERO;
                for (int j = 0; j < f.base().degree(); j++) {
                    coefficients = coefficients.add(Polynomial.monomial(solution.get()[index++], j));
                }
                if (!coefficients.isZero()) terms.add(new Term(coefficients, f.base(), k));
            }
        }
        return Optional.of(terms);
    }

    /** Eliminación de Gauss-Jordan exacta sobre una matriz aumentada {@code n × (n+1)}. */
    private static Optional<Rational[]> solve(Rational[][] m, int n) {
        for (int col = 0; col < n; col++) {
            int pivot = -1;
            for (int row = col; row < n; row++) {
                if (!m[row][col].isZero()) {
                    pivot = row;
                    break;
                }
            }
            if (pivot < 0) return Optional.empty();
            Rational[] swap = m[col];
            m[col] = m[pivot];
            m[pivot] = swap;

            Rational inverse = m[col][col].reciprocal();
            for (int j = col; j <= n; j++) m[col][j] = m[col][j].multiply(inverse);
            for (int row = 0; row < n; row++) {
                if (row == col || m[row][col].isZero()) continue;
                Rational factor = m[row][col];
                for (int j = col; j <= n; j++) m[row][j] = m[row][j].subtract(factor.multiply(m[col][j]));
            }
        }
        Rational[] result = new Rational[n];
        for (int i = 0; i < n; i++) result[i] = m[i][n];
        return Optional.of(result);
    }
}
