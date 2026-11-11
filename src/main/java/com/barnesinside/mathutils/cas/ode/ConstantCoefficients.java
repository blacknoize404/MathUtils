package com.barnesinside.mathutils.cas.ode;

import com.barnesinside.mathutils.cas.expr.Algebra;
import com.barnesinside.mathutils.cas.expr.Eval;
import com.barnesinside.mathutils.cas.expr.Expand;
import com.barnesinside.mathutils.cas.expr.Expr;
import com.barnesinside.mathutils.cas.expr.Fn;
import com.barnesinside.mathutils.cas.expr.Rational;
import com.barnesinside.mathutils.cas.ode.OdeSupport.Rec;
import com.barnesinside.mathutils.cas.poly.Polynomial;
import com.barnesinside.mathutils.cas.steps.Rule;
import com.barnesinside.mathutils.cas.steps.Step;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.barnesinside.mathutils.cas.ode.OdeSupport.eq;
import static com.barnesinside.mathutils.cas.ode.OdeSupport.polish;
import static com.barnesinside.mathutils.cas.ode.OdeSupport.primed;

/**
 * Ecuaciones lineales de orden n con coeficientes constantes, {@code aₙ·y⁽ⁿ⁾ + … + a₁·y' + a₀·y = g(x)}: la ecuación característica da la
 * solución de la homogénea y, si {@code g} no es cero, se le suma una solución particular (coeficientes indeterminados o variación de
 * parámetros).
 */
final class ConstantCoefficients {

    private ConstantCoefficients() {
    }

    /** La ecuación: los coeficientes de {@code y⁽⁰⁾} a {@code y⁽ⁿ⁾} y el término independiente. */
    record Equation(Rational[] a, Expr g, int order) {
    }

    /** Una raíz de la ecuación característica: {@code re + im·i} (con {@code im ≥ 0}; si no es cero, su conjugada también es raíz). */
    record Root(Expr re, Expr im, int multiplicity) {
        boolean isComplex() {
            return !im.isZero();
        }
    }

    // ================================================================ Lectura de la ecuación

    static Optional<Equation> parse(Expr difference, String x, String y, int order) {
        Expr expanded = Expand.expand(difference);
        Rational[] a = new Rational[order + 1];
        Map<String, Expr> zeros = new LinkedHashMap<>();
        List<Expr> reconstructed = new ArrayList<>();
        for (int k = 0; k <= order; k++) {
            Expr yk = k == 0 ? Expr.sym(y) : primed(y, k);
            Expr coefficient = OdeSupport.diff(expanded, ((Expr.Sym) yk).name());
            if (!(coefficient instanceof Expr.Num n)) return Optional.empty();
            a[k] = n.value();
            zeros.put(((Expr.Sym) yk).name(), Expr.ZERO);
            reconstructed.add(Expr.mul(coefficient, yk));
        }
        if (a[order].isZero()) return Optional.empty();
        Expr g = polish(Expr.neg(expanded.substitute(zeros)));
        if (!g.isFreeOf(y) || g.variables().stream().anyMatch(name -> name.startsWith(y + "'"))) return Optional.empty();
        if (!OdeSupport.isZero(Expr.sub(expanded, Expr.sub(Expr.add(reconstructed), g)))) return Optional.empty();
        return Optional.of(new Equation(a, g, order));
    }

    // ================================================================ Raíces

    private static BigInteger lcmOfDenominators(Rational[] c) {
        BigInteger lcm = BigInteger.ONE;
        for (Rational r : c) lcm = lcm.multiply(r.denominator()).divide(lcm.gcd(r.denominator()));
        return lcm;
    }

    private static List<BigInteger> divisors(BigInteger n) {
        n = n.abs();
        if (n.bitLength() > 40) return List.of();
        long value = n.longValueExact();
        List<BigInteger> result = new ArrayList<>();
        for (long d = 1; d * d <= value; d++) {
            if (value % d == 0) {
                result.add(BigInteger.valueOf(d));
                if (d * d != value) result.add(BigInteger.valueOf(value / d));
            }
        }
        return result;
    }

    /** Las raíces de la ecuación característica, o vacío si no se sabe escribirlas (más de dos sin raíz racional). */
    static Optional<List<Root>> roots(Rational[] coefficients) {
        Polynomial p = Polynomial.of(coefficients);
        List<Root> found = new ArrayList<>();

        int zeroRoots = 0;
        while (p.degree() > 0 && p.coefficient(0).isZero()) {
            p = p.divide(Polynomial.of(Rational.ZERO, Rational.ONE)).quotient();
            zeroRoots++;
        }
        if (zeroRoots > 0) found.add(new Root(Expr.ZERO, Expr.ZERO, zeroRoots));

        while (p.degree() >= 1) {
            Rational[] scaled = new Rational[p.degree() + 1];
            BigInteger lcm = lcmOfDenominators(coefficientsOf(p));
            for (int i = 0; i < scaled.length; i++) scaled[i] = p.coefficient(i).multiply(Rational.of(lcm));
            List<BigInteger> numerators = divisors(scaled[0].numerator());
            List<BigInteger> denominators = divisors(scaled[scaled.length - 1].numerator());
            Rational root = null;
            search:
            for (BigInteger num : numerators) {
                for (BigInteger den : denominators) {
                    for (int sign : new int[]{1, -1}) {
                        Rational candidate = Rational.of(num.multiply(BigInteger.valueOf(sign)), den);
                        if (p.evaluate(candidate).isZero()) {
                            root = candidate;
                            break search;
                        }
                    }
                }
            }
            if (root == null) break;
            Polynomial linear = Polynomial.of(root.negate(), Rational.ONE);
            int multiplicity = 0;
            while (p.degree() >= 1 && p.evaluate(root).isZero()) {
                p = p.divide(linear).quotient();
                multiplicity++;
            }
            found.add(new Root(Expr.num(root), Expr.ZERO, multiplicity));
        }

        if (p.degree() == 2) {
            Rational a = p.coefficient(2);
            Rational b = p.coefficient(1);
            Rational c = p.coefficient(0);
            Rational disc = b.multiply(b).subtract(Rational.of(4).multiply(a).multiply(c));
            Expr twoA = Expr.num(Rational.of(2).multiply(a));
            if (disc.signum() > 0) {
                Expr s = Expr.sqrt(Expr.num(disc));
                found.add(new Root(polish(Expr.div(Expr.add(Expr.num(b.negate()), s), twoA)), Expr.ZERO, 1));
                found.add(new Root(polish(Expr.div(Expr.sub(Expr.num(b.negate()), s), twoA)), Expr.ZERO, 1));
            } else if (disc.signum() < 0) {
                Expr re = Expr.num(b.negate().divide(Rational.of(2).multiply(a)));
                Expr im = polish(Expr.div(Expr.sqrt(Expr.num(disc.negate())), Expr.num(Rational.of(2).multiply(a).abs())));
                found.add(new Root(re, im, 1));
            } else {
                return Optional.empty();
            }
        } else if (p.degree() > 2) {
            return Optional.empty();
        }
        found.sort(Comparator.comparing(Root::isComplex).thenComparingDouble(r -> Eval.value(r.re())));
        return Optional.of(found);
    }

    private static Rational[] coefficientsOf(Polynomial p) {
        Rational[] c = new Rational[p.degree() + 1];
        for (int i = 0; i < c.length; i++) c[i] = p.coefficient(i);
        return c;
    }

    // ================================================================ Soluciones fundamentales

    private static Expr power(Expr x, int k) {
        return k == 0 ? Expr.ONE : Expr.pow(x, k);
    }

    static List<Expr> fundamental(List<Root> roots, String variable) {
        Expr x = Expr.sym(variable);
        List<Expr> phi = new ArrayList<>();
        for (Root root : roots) {
            Expr e = Expr.exp(Expr.mul(root.re(), x));
            for (int k = 0; k < root.multiplicity(); k++) {
                Expr base = polish(Expr.mul(power(x, k), e));
                if (!root.isComplex()) {
                    phi.add(base);
                } else {
                    phi.add(polish(Expr.mul(base, Expr.cos(Expr.mul(root.im(), x)))));
                    phi.add(polish(Expr.mul(base, Expr.sin(Expr.mul(root.im(), x)))));
                }
            }
        }
        return phi;
    }

    // ================================================================ Resolución

    static Optional<OdeSupport.Rec> solve(Rec rec, Expr difference, String x, String y, int order, OdeMethod method,
                                          Expr[] answer) {
        Optional<Equation> parsed = parse(difference, x, y, order);
        if (parsed.isEmpty()) return Optional.empty();
        Equation equation = parsed.get();
        Expr xx = Expr.sym(x);
        Expr yy = Expr.sym(y);
        Expr r = Expr.sym("r");

        List<Expr> terms = new ArrayList<>();
        for (int k = 0; k <= order; k++) {
            if (!equation.a()[k].isZero()) terms.add(Expr.mul(Expr.num(equation.a()[k]), power(r, k)));
        }
        Expr characteristic = Expr.add(terms);
        rec.rel(Rule.ODE_CHAR_EQUATION, eq(characteristic, Expr.ZERO), "s", r, "y", yy, "x", xx);

        Optional<List<Root>> roots = roots(equation.a());
        if (roots.isEmpty()) return Optional.empty();
        for (Root root : roots.get()) {
            if (root.isComplex()) {
                rec.note(Rule.ODE_ROOT_COMPLEX, "a", root.re(), "b", root.im(), "m", Expr.num(root.multiplicity()));
            } else if (root.multiplicity() > 1) {
                rec.note(Rule.ODE_ROOT_REPEATED, "r", root.re(), "m", Expr.num(root.multiplicity()));
            } else {
                rec.note(Rule.ODE_ROOT_REAL, "r", root.re());
            }
        }

        List<Expr> phi = fundamental(roots.get(), x);
        List<Expr> parts = new ArrayList<>();
        for (int i = 0; i < phi.size(); i++) {
            Expr constant = Expr.sym(order == 1 ? OdeSupport.C : "C" + (i + 1));
            parts.add(Expr.mul(constant, phi.get(i)));
        }
        Expr homogeneous = polish(Expr.add(parts));
        rec.rel(Rule.ODE_HOMOGENEOUS_SOLUTION, eq(yy, homogeneous), "y", yy);

        Expr solution = homogeneous;
        if (!equation.g().isZero()) {
            Optional<Expr> particular = particular(rec, equation, phi, roots.get(), x, y, method);
            if (particular.isEmpty()) return Optional.empty();
            solution = polish(Expr.add(homogeneous, particular.get()));
            rec.rel(Rule.ODE_GENERAL_SOLUTION, eq(yy, solution), "y", yy);
        } else if (method == OdeMethod.UNDETERMINED_COEFFICIENTS || method == OdeMethod.VARIATION_OF_PARAMETERS) {
            return Optional.empty();
        }
        if (!residualOk(equation, solution, x)) return Optional.empty();
        answer[0] = solution;
        return Optional.of(rec);
    }

    private static boolean residualOk(Equation equation, Expr solution, String x) {
        List<Expr> terms = new ArrayList<>();
        Expr derivative = solution;
        for (int k = 0; k <= equation.order(); k++) {
            if (k > 0) derivative = OdeSupport.diff(derivative, x);
            terms.add(Expr.mul(Expr.num(equation.a()[k]), derivative));
        }
        return !OdeSupport.different(Expr.add(terms), equation.g());
    }

    // ================================================================ Solución particular

    private static Optional<Expr> particular(Rec rec, Equation equation, List<Expr> phi, List<Root> roots, String x, String y,
                                             OdeMethod method) {
        if (method != OdeMethod.VARIATION_OF_PARAMETERS) {
            Rec attempt = new Rec();
            Optional<Expr> found = undetermined(attempt, equation, roots, x);
            if (found.isPresent()) {
                for (Step s : attempt.steps()) rec.trace().add(s);
                return found;
            }
            if (method == OdeMethod.UNDETERMINED_COEFFICIENTS) return Optional.empty();
        }
        Rec attempt = new Rec();
        Optional<Expr> found = variation(attempt, equation, phi, x);
        found.ifPresent(e -> {
            for (Step s : attempt.steps()) rec.trace().add(s);
        });
        return found;
    }

    private static Optional<Expr> variation(Rec rec, Equation equation, List<Expr> phi, String x) {
        if (equation.order() != 2 || phi.size() != 2) return Optional.empty();
        Expr y1 = phi.get(0);
        Expr y2 = phi.get(1);
        Expr w = polish(Expr.sub(Expr.mul(y1, OdeSupport.diff(y2, x)), Expr.mul(OdeSupport.diff(y1, x), y2)));
        if (OdeSupport.isZero(w)) return Optional.empty();
        Expr g = polish(Expr.div(equation.g(), Expr.num(equation.a()[2])));
        Expr u1 = polish(Expr.div(Expr.neg(Expr.mul(y2, g)), w));
        Expr u2 = polish(Expr.div(Expr.mul(y1, g), w));
        rec.rel(Rule.ODE_WRONSKIAN, eq(Expr.sym("W"), w), "y1", y1, "y2", y2);
        Optional<Expr> i1 = rec.integral(u1, x);
        Optional<Expr> i2 = rec.integral(u2, x);
        if (i1.isEmpty() || i2.isEmpty()) return Optional.empty();
        Expr yp = polish(Expr.add(Expr.mul(i1.get(), y1), Expr.mul(i2.get(), y2)));
        rec.rel(Rule.ODE_VARIATION, eq(Expr.sym("y_p"), yp), "u1", u1, "u2", u2, "g", g, "y1", y1, "y2", y2);
        return residualOk(equation, yp, x) ? Optional.of(yp) : Optional.empty();
    }

    /** Un término de {@code g}: {@code c·x^k·e^(a·x)·cos(b·x)} o {@code …·sin(b·x)}. */
    private record Term(Rational c, int k, Rational a, Rational b) {
    }

    private static Optional<Rational> linearIn(Expr arg, Expr x) {
        if (arg.equals(x)) return Optional.of(Rational.ONE);
        if (arg instanceof Expr.Mul m && m.factors().size() == 2 && m.factors().get(0) instanceof Expr.Num n && m.factors().get(1).equals(x)) {
            return Optional.of(n.value());
        }
        return Optional.empty();
    }

    private static Optional<Term> analyze(Expr term, Expr x) {
        Rational c = Rational.ONE;
        int k = 0;
        Rational a = Rational.ZERO;
        Rational b = Rational.ZERO;
        boolean trig = false;
        boolean sin = false;
        List<Expr> factors = term instanceof Expr.Mul m ? m.factors() : List.of(term);
        for (Expr f : factors) {
            if (f instanceof Expr.Num n) {
                c = c.multiply(n.value());
            } else if (f.equals(x)) {
                k++;
            } else if (f instanceof Expr.Pow p && p.base().equals(x) && p.exponent() instanceof Expr.Num n && n.value().isInteger()
                    && n.value().signum() > 0) {
                k += n.value().intValueExact();
            } else if (f instanceof Expr.Pow p && p.base().equals(Expr.E)) {
                Optional<Rational> lin = linearIn(p.exponent(), x);
                if (lin.isEmpty()) return Optional.empty();
                a = a.add(lin.get());
            } else if (f instanceof Expr.Func fn && (fn.fn() == Fn.COS || fn.fn() == Fn.SIN) && !trig) {
                Optional<Rational> lin = linearIn(fn.arg(), x);
                if (lin.isEmpty()) return Optional.empty();
                trig = true;
                sin = fn.fn() == Fn.SIN;
                b = lin.get();
                if (b.signum() < 0) {
                    b = b.negate();
                    if (sin) c = c.negate();
                }
            } else {
                return Optional.empty();
            }
        }
        if (trig && !sin) {
            // coseno: la parte en seno de ese grupo es cero; se marca con b > 0
            return Optional.of(new Term(c, k, a, b));
        }
        if (trig) return Optional.of(new Term(c, k, a, b.negate())); // seno: b negativo en la marca
        return Optional.of(new Term(c, k, a, Rational.ZERO));
    }

    private static int multiplicityOf(List<Root> roots, double re, double im) {
        for (Root r : roots) {
            if (Math.abs(Eval.value(r.re()) - re) < 1e-9 && Math.abs(Eval.value(r.im()) - im) < 1e-9) return r.multiplicity();
        }
        return 0;
    }

    private static Optional<Expr> undetermined(Rec rec, Equation equation, List<Root> roots, String x) {
        Expr xx = Expr.sym(x);
        // Los grupos (a, |b|) con el mayor grado de polinomio que aparece en cada uno
        Map<String, int[]> groups = new LinkedHashMap<>();
        Map<String, Rational[]> exponents = new LinkedHashMap<>();
        for (Expr t : Algebra.terms(Expand.expand(equation.g()))) {
            Optional<Term> term = analyze(t, xx);
            if (term.isEmpty()) return Optional.empty();
            Rational b = term.get().b().abs();
            String key = term.get().a() + "|" + b;
            groups.merge(key, new int[]{term.get().k()}, (p, q) -> new int[]{Math.max(p[0], q[0])});
            exponents.put(key, new Rational[]{term.get().a(), b});
        }

        List<Expr> basis = new ArrayList<>();
        for (Map.Entry<String, int[]> group : groups.entrySet()) {
            Rational a = exponents.get(group.getKey())[0];
            Rational b = exponents.get(group.getKey())[1];
            int s = multiplicityOf(roots, a.doubleValue(), b.doubleValue());
            Expr e = Expr.exp(Expr.mul(Expr.num(a), xx));
            for (int p = 0; p <= group.getValue()[0]; p++) {
                Expr base = Expr.mul(power(xx, p + s), e);
                basis.add(polish(b.isZero() ? base : Expr.mul(base, Expr.cos(Expr.mul(Expr.num(b), xx)))));
                if (!b.isZero()) basis.add(polish(Expr.mul(base, Expr.sin(Expr.mul(Expr.num(b), xx)))));
            }
        }
        int unknowns = basis.size();
        if (unknowns == 0 || unknowns > 24) return Optional.empty();

        List<Expr> trial = new ArrayList<>();
        for (int i = 0; i < unknowns; i++) trial.add(Expr.mul(Expr.sym("A" + (i + 1)), basis.get(i)));
        rec.rel(Rule.ODE_UC_FORM, eq(Expr.sym("y_p"), Expr.add(trial)), "g", equation.g());

        // L[base_j] en cada base, y se resuelve Σ A_j·L[base_j] = g en varios puntos
        List<Expr> images = new ArrayList<>();
        for (Expr base : basis) {
            List<Expr> terms = new ArrayList<>();
            Expr derivative = base;
            for (int k = 0; k <= equation.order(); k++) {
                if (k > 0) derivative = OdeSupport.diff(derivative, x);
                terms.add(Expr.mul(Expr.num(equation.a()[k]), derivative));
            }
            images.add(Expr.add(terms));
        }
        int rows = Math.max(2 * unknowns + 4, 8);
        double[][] normal = new double[unknowns][unknowns + 1];
        for (int r = 0; r < rows; r++) {
            double point = 0.15 + 1.7 * r / (rows - 1) + 0.013 * r * r / rows;
            double[] row = new double[unknowns];
            try {
                for (int j = 0; j < unknowns; j++) row[j] = Eval.value(images.get(j), Map.of(x, point));
                double target = Eval.value(equation.g(), Map.of(x, point));
                for (int i = 0; i < unknowns; i++) {
                    for (int j = 0; j < unknowns; j++) normal[i][j] += row[i] * row[j];
                    normal[i][unknowns] += row[i] * target;
                }
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        }
        double[] coefficients = gauss(normal);
        if (coefficients == null) return Optional.empty();

        List<Expr> solved = new ArrayList<>();
        for (int i = 0; i < unknowns; i++) {
            Rational c = approximate(coefficients[i]);
            if (c == null) return Optional.empty();
            if (!c.isZero()) solved.add(Expr.mul(Expr.num(c), basis.get(i)));
        }
        Expr yp = polish(Expr.add(solved));
        rec.rel(Rule.ODE_UC_SOLVE, eq(Expr.sym("y_p"), yp), "g", equation.g());
        return residualOk(equation, yp, x) ? Optional.of(yp) : Optional.empty();
    }

    private static double[] gauss(double[][] m) {
        int n = m.length;
        for (int c = 0; c < n; c++) {
            int pivot = c;
            for (int r = c + 1; r < n; r++) if (Math.abs(m[r][c]) > Math.abs(m[pivot][c])) pivot = r;
            if (Math.abs(m[pivot][c]) < 1e-12) return null;
            double[] swap = m[c];
            m[c] = m[pivot];
            m[pivot] = swap;
            for (int r = 0; r < n; r++) {
                if (r == c) continue;
                double factor = m[r][c] / m[c][c];
                for (int k = c; k <= n; k++) m[r][k] -= factor * m[c][k];
            }
        }
        double[] result = new double[n];
        for (int i = 0; i < n; i++) result[i] = m[i][n] / m[i][i];
        return result;
    }

    /** El racional sencillo más cercano (fracción continua), o {@code null} si no hay uno de denominador pequeño. */
    private static Rational approximate(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || Math.abs(value) > 1e9) return null;
        if (Math.abs(value) < 1e-9) return Rational.ZERO;
        long h1 = 1, h0 = 0, k1 = 0, k0 = 1;
        double rest = value;
        for (int i = 0; i < 20; i++) {
            long a = (long) Math.floor(rest);
            long h2 = a * h1 + h0;
            long k2 = a * k1 + k0;
            h0 = h1;
            h1 = h2;
            k0 = k1;
            k1 = k2;
            if (Math.abs(value - (double) h1 / k1) < 1e-8 * Math.max(1, Math.abs(value))) {
                return k1 > 100_000 ? null : Rational.of(h1, k1);
            }
            double frac = rest - a;
            if (Math.abs(frac) < 1e-12) break;
            rest = 1 / frac;
        }
        return null;
    }
}
