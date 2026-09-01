package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Algebra;
import barnesinside.mathutils.cas.expr.Display;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.poly.Factor;
import barnesinside.mathutils.cas.poly.Polynomial;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Trace;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Factorización y desarrollo de polinomios paso a paso, con la posibilidad de elegir el método.
 * <pre>{@code
 * Factoring.applicableMethods(ExprParser.parse("x^2 + 5x + 6"));        // factor común no, trinomio sí, Ruffini sí...
 * Solution s = Factoring.factor(ExprParser.parse("x^2 + 5x + 6"), FactorMethod.TRINOMIAL);
 * s.result();                                                            // (x + 2)(x + 3)
 * }</pre>
 * Todos los métodos trabajan con coeficientes racionales; {@link FactorMethod#QUADRATIC_FORMULA} y
 * {@link FactorMethod#COMPLETE_SQUARE} también dan factores con raíces cuando el discriminante no es un cuadrado.
 */
public final class Factoring {

    private Factoring() {
    }

    // ================================================================ API

    /**
     * Los métodos con los que se puede factorizar la expresión (siempre incluye {@link FactorMethod#AUTO} si alguno sirve).
     * Cada método se prueba de verdad, así que si aparece en la lista es porque factoriza algo.
     */
    public static List<FactorMethod> applicableMethods(Expr e) {
        Expr expanded = Expand.expand(e);
        List<FactorMethod> result = new ArrayList<>();
        for (FactorMethod m : FactorMethod.values()) {
            if (m == FactorMethod.AUTO) continue;
            if (expanded instanceof Expr.Add sum && apply(Rec.scratch(), m, sum).isPresent()) result.add(m);
        }
        if (!result.isEmpty()) result.add(0, FactorMethod.AUTO);
        return result;
    }

    public static Solution factor(Expr e) {
        return factor(e, FactorMethod.AUTO);
    }

    /**
     * Factoriza con el método pedido. Si el método no sirve para la expresión, el estado es
     * {@link Solution.Status#UNSOLVED} y hay un paso que lo dice.
     */
    public static Solution factor(Expr e, FactorMethod method) {
        Rec rec = new Rec(new Trace(120, 6000));
        String v = mainVariable(e);
        Expr expanded = Expand.expand(e);
        Expr result;
        try {
            result = method == FactorMethod.AUTO ? auto(rec, e) : single(rec, method, expanded);
        } catch (Trace.BudgetExceededException | ArithmeticException ex) {
            result = null;
        }
        boolean factored = result != null && !result.equals(expanded) && Expand.expand(result).equals(expanded);
        if (factored) {
            return new Solution(Solution.Kind.FACTORIZATION, Solution.Status.SOLVED, e, result, rec.steps(), v, 1);
        }
        if (method == FactorMethod.AUTO) {
            // No hay nada que factorizar con racionales: es un resultado válido, no un fallo
            rec.note(Rule.POLY_IRREDUCIBLE, "f", e);
            return new Solution(Solution.Kind.FACTORIZATION, Solution.Status.SOLVED, e, e, rec.steps(), v, 1);
        }
        rec.note(Rule.POLY_NOT_APPLICABLE, "f", e);
        return new Solution(Solution.Kind.FACTORIZATION, Solution.Status.UNSOLVED, e, null, rec.steps(), v, 1);
    }

    /** Desarrolla productos y potencias de sumas, con el método que corresponda (distributiva, binomio, multinomio). */
    public static Solution expand(Expr e) {
        Rec rec = new Rec(new Trace(60, 4000));
        String v = mainVariable(e);
        Expr result = Expand.expand(e);
        if (!result.equals(e)) {
            Rule rule = expansionRule(e);
            switch (rule) {
                case POLY_BINOMIAL -> {
                    Expr.Pow p = firstSumPower(e);
                    Expr.Add base = (Expr.Add) p.base();
                    rec.note(rule, "f", e, "a", base.terms().get(0), "b", base.terms().get(1), "n", p.exponent(), "g", result);
                }
                case POLY_MULTINOMIAL -> rec.note(rule, "f", e, "n", firstSumPower(e).exponent(), "g", result);
                default -> rec.note(rule, "f", e, "g", result);
            }
            Expr collected = Expr.add(Algebra.terms(result));
            if (!collected.equals(result)) rec.note(Rule.POLY_COLLECT, "f", result, "g", collected);
        }
        return new Solution(Solution.Kind.EXPANSION, Solution.Status.SOLVED, e, result, rec.steps(), v, 1);
    }

    private static Rule expansionRule(Expr e) {
        Expr.Pow p = firstSumPower(e);
        if (p == null) return Rule.POLY_DISTRIBUTE;
        return ((Expr.Add) p.base()).terms().size() == 2 ? Rule.POLY_BINOMIAL : Rule.POLY_MULTINOMIAL;
    }

    /** La primera potencia entera de una suma en la expresión, o {@code null}. */
    private static Expr.Pow firstSumPower(Expr e) {
        if (e instanceof Expr.Pow p && p.base() instanceof Expr.Add && p.exponent() instanceof Expr.Num n
                && n.value().isInteger() && n.value().signum() > 0) return p;
        if (e instanceof Expr.Mul m) {
            for (Expr f : m.factors()) {
                Expr.Pow p = firstSumPower(f);
                if (p != null) return p;
            }
        }
        if (e instanceof Expr.Add a) {
            for (Expr t : a.terms()) {
                Expr.Pow p = firstSumPower(t);
                if (p != null) return p;
            }
        }
        return null;
    }

    private static String mainVariable(Expr e) {
        List<String> variables = List.copyOf(e.variables());
        if (variables.size() == 1) return variables.get(0);
        return variables.contains("x") ? "x" : variables.isEmpty() ? "x" : variables.get(0);
    }

    // ================================================================ Automático

    /** Factoriza del todo registrando los pasos en {@code rec}; la usan también las ecuaciones. */
    static Expr auto(Rec rec, Expr e) {
        Expr expanded = Expand.expand(e);
        if (!e.equals(expanded) && !(e instanceof Expr.Add)) {
            // Un producto o una potencia sin desarrollar: se desarrolla primero para trabajar con la suma
            rec.note(Rule.POLY_DISTRIBUTE, "f", e, "g", expanded);
        }
        return autoSum(rec, expanded, 0);
    }

    private static Expr autoSum(Rec rec, Expr e, int depth) {
        if (!(e instanceof Expr.Add sum) || depth > 12) return e;
        for (FactorMethod m : List.of(FactorMethod.COMMON_FACTOR, FactorMethod.DIFFERENCE_OF_SQUARES, FactorMethod.PERFECT_SQUARE,
                FactorMethod.SUM_DIFFERENCE_OF_CUBES, FactorMethod.TRINOMIAL, FactorMethod.GROUPING,
                FactorMethod.RATIONAL_ROOTS, FactorMethod.SUBSTITUTION)) {
            Rec trial = Rec.scratch();
            if (apply(trial, m, sum).isEmpty()) continue;
            Expr product = apply(rec, m, sum).orElseThrow();
            return refine(rec, product, depth + 1);
        }
        Expr fallback = Factor.factor(sum);
        if (!fallback.equals(sum)) {
            rec.note(Rule.ALG_FACTOR, "f", sum, "g", fallback);
            return fallback;
        }
        return sum;
    }

    /** Sigue factorizando los factores de un producto que son sumas. */
    private static Expr refine(Rec rec, Expr product, int depth) {
        List<Expr> parts = new ArrayList<>();
        for (Expr factor : Algebra.factors(product)) {
            Expr base = factor;
            Expr exponent = Expr.ONE;
            if (factor instanceof Expr.Pow p && p.exponent() instanceof Expr.Num n && n.value().isInteger() && p.base() instanceof Expr.Add) {
                base = p.base();
                exponent = p.exponent();
            }
            Expr refined = base instanceof Expr.Add ? autoSum(rec, base, depth) : base;
            parts.add(exponent.isOne() ? refined : Expr.pow(refined, exponent));
        }
        return Expr.mul(parts);
    }

    private static Expr single(Rec rec, FactorMethod method, Expr expanded) {
        if (!(expanded instanceof Expr.Add sum)) return expanded;
        Optional<Expr> product = apply(rec, method, sum);
        if (product.isEmpty()) return expanded;
        // Con un método elegido se aplica primero ese método; lo que quede sin factorizar se sigue factorizando
        return refine(rec, product.get(), 1);
    }

    // ================================================================ Métodos

    private static Optional<Expr> apply(Rec rec, FactorMethod method, Expr.Add sum) {
        return switch (method) {
            case COMMON_FACTOR -> commonFactor(rec, sum);
            case DIFFERENCE_OF_SQUARES -> differenceOfSquares(rec, sum);
            case PERFECT_SQUARE -> perfectSquare(rec, sum);
            case SUM_DIFFERENCE_OF_CUBES -> cubes(rec, sum);
            case TRINOMIAL -> trinomial(rec, sum);
            case GROUPING -> grouping(rec, sum);
            case RATIONAL_ROOTS -> rationalRoots(rec, sum);
            case QUADRATIC_FORMULA -> quadraticRoots(rec, sum, false);
            case COMPLETE_SQUARE -> quadraticRoots(rec, sum, true);
            case SUBSTITUTION -> substitution(rec, sum);
            case AUTO -> Optional.empty();
        };
    }

    private static Optional<Expr> commonFactor(Rec rec, Expr.Add sum) {
        Optional<Factor.CommonFactor> cf = Factor.commonFactor(sum.terms());
        if (cf.isEmpty()) return Optional.empty();
        Expr product = Expr.mul(cf.get().factor(), cf.get().quotient());
        rec.note(Rule.POLY_COMMON_FACTOR, "f", sum, "g", cf.get().factor(), "rest", cf.get().quotient());
        return Optional.of(product);
    }

    private static Optional<Expr> differenceOfSquares(Rec rec, Expr.Add sum) {
        if (sum.terms().size() != 2) return Optional.empty();
        for (int i = 0; i < 2; i++) {
            Expr positive = sum.terms().get(i);
            Expr negative = sum.terms().get(1 - i);
            if (!Display.isNegative(negative) || Display.isNegative(positive)) continue;
            Optional<Expr> a = Factor.exactRoot(positive, 2);
            Optional<Expr> b = Factor.exactRoot(Expr.neg(negative), 2);
            if (a.isPresent() && b.isPresent()) {
                Expr product = Expr.mul(Expr.sub(a.get(), b.get()), Expr.add(a.get(), b.get()));
                rec.note(Rule.POLY_DIFF_SQUARES, "f", sum, "a", a.get(), "b", b.get(), "g", product);
                return Optional.of(product);
            }
        }
        return Optional.empty();
    }

    private static Optional<Expr> perfectSquare(Rec rec, Expr.Add sum) {
        List<Expr> terms = sum.terms();
        if (terms.size() != 3) return Optional.empty();
        for (int middle = 0; middle < 3; middle++) {
            Expr first = terms.get((middle + 1) % 3);
            Expr last = terms.get((middle + 2) % 3);
            if (Display.isNegative(first) || Display.isNegative(last)) continue;
            Optional<Expr> a = Factor.exactRoot(first, 2);
            Optional<Expr> b = Factor.exactRoot(last, 2);
            if (a.isEmpty() || b.isEmpty()) continue;

            Expr cross = Expand.expand(Expr.mul(Expr.num(2), a.get(), b.get()));
            Expr actual = terms.get(middle);
            Expr product;
            if (actual.equals(cross)) product = Expr.pow(Expr.add(a.get(), b.get()), 2);
            else if (actual.equals(Expr.neg(cross))) product = Expr.pow(Expr.sub(a.get(), b.get()), 2);
            else continue;
            rec.note(Rule.POLY_PERFECT_SQUARE, "f", sum, "a", a.get(), "b", b.get(), "m", actual, "g", product);
            return Optional.of(product);
        }
        return Optional.empty();
    }

    private static Optional<Expr> cubes(Rec rec, Expr.Add sum) {
        if (sum.terms().size() != 2) return Optional.empty();
        Optional<Expr> r1 = Factor.exactRoot(sum.terms().get(0), 3);
        Optional<Expr> r2 = Factor.exactRoot(sum.terms().get(1), 3);
        if (r1.isEmpty() || r2.isEmpty()) return Optional.empty();
        Expr a = r1.get();
        Expr b = r2.get();
        // a debe ser el que sale positivo: si no, se intercambian
        if (Display.isNegative(a) && !Display.isNegative(b)) {
            Expr t = a;
            a = b;
            b = t;
        }
        if (Display.isNegative(b)) {
            Expr positive = Expr.neg(b);
            Expr product = Expr.mul(Expr.sub(a, positive),
                    Expr.add(Expr.pow(a, 2), Expr.mul(a, positive), Expr.pow(positive, 2)));
            rec.note(Rule.POLY_DIFF_CUBES, "f", sum, "a", a, "b", positive, "g", product);
            return Optional.of(product);
        }
        if (Display.isNegative(a)) return Optional.empty();
        Expr product = Expr.mul(Expr.add(a, b),
                Expr.add(Expr.pow(a, 2), Expr.neg(Expr.mul(a, b)), Expr.pow(b, 2)));
        rec.note(Rule.POLY_SUM_CUBES, "f", sum, "a", a, "b", b, "g", product);
        return Optional.of(product);
    }

    /** Trinomio {@code ax² + bx + c}: dos números que suman {@code b} y multiplican {@code ac}, y se agrupa. */
    private static Optional<Expr> trinomial(Rec rec, Expr.Add sum) {
        Optional<String> variable = singleVariable(sum);
        if (variable.isEmpty()) return Optional.empty();
        String v = variable.get();
        Optional<Polynomial> polynomial = Polynomial.from(sum, v);
        if (polynomial.isEmpty() || polynomial.get().degree() != 2) return Optional.empty();
        Polynomial p = polynomial.get();

        BigInteger lcm = BigInteger.ONE;
        for (int k = 0; k <= 2; k++) lcm = lcm.multiply(p.coefficient(k).denominator()).divide(lcm.gcd(p.coefficient(k).denominator()));
        BigInteger a = p.coefficient(2).multiply(Rational.of(lcm)).numerator();
        BigInteger b = p.coefficient(1).multiply(Rational.of(lcm)).numerator();
        BigInteger c = p.coefficient(0).multiply(Rational.of(lcm)).numerator();
        if (a.signum() == 0 || c.signum() == 0 || b.signum() == 0) return Optional.empty();
        BigInteger g = a.gcd(b).gcd(c);
        if (!g.equals(BigInteger.ONE)) return Optional.empty(); // el factor común se saca antes

        BigInteger product = a.multiply(c);
        BigInteger[] pair = findPair(product, b);
        if (pair == null) return Optional.empty();
        BigInteger m = pair[0];
        BigInteger n = pair[1];

        Expr x = Expr.sym(v);
        Expr scale = Expr.num(Rational.of(BigInteger.ONE, lcm));
        Expr result;
        rec.note(Rule.POLY_TRINOMIAL_NUMBERS, "b", num(b), "ac", num(product), "m", num(m), "n", num(n));
        if (a.equals(BigInteger.ONE)) {
            result = Expr.mul(scale, Expr.add(x, num(m)), Expr.add(x, num(n)));
            rec.note(Rule.ALG_FACTOR, "f", sum, "g", result);
            return Optional.of(result);
        }
        // Se parte el término central y se agrupa
        Expr split = Expr.add(Expr.mul(num(a), Expr.pow(x, 2)), Expr.mul(num(m), x), Expr.mul(num(n), x), num(c));
        rec.note(Rule.POLY_TRINOMIAL_SPLIT, "f", sum, "g", split);
        Optional<Expr> grouped = groupedPairs(rec, split, x, a, m, n, c);
        if (grouped.isEmpty()) return Optional.empty();
        return Optional.of(Expr.mul(scale, grouped.get()));
    }

    /** El factor común de un par de términos, o el par entero (con factor 1) si no comparten nada. */
    private static Factor.CommonFactor groupOf(Expr first, Expr second) {
        List<Expr> pair = List.of(first, second);
        return Factor.commonFactor(pair).orElseGet(() -> new Factor.CommonFactor(Expr.ONE, Expr.add(pair)));
    }

    /** Agrupa {@code ax² + mx} y {@code nx + c} sacando factor común a cada grupo. */
    private static Optional<Expr> groupedPairs(Rec rec, Expr split, Expr x, BigInteger a, BigInteger m, BigInteger n, BigInteger c) {
        Factor.CommonFactor first = groupOf(Expr.mul(num(a), Expr.pow(x, 2)), Expr.mul(num(m), x));
        Factor.CommonFactor second = groupOf(Expr.mul(num(n), x), num(c));
        return combine(rec, split, first, second);
    }

    /** Si los dos grupos dejan el mismo binomio (o su opuesto), lo saca como factor común. */
    private static Optional<Expr> combine(Rec rec, Expr whole, Factor.CommonFactor first, Factor.CommonFactor second) {
        Expr q1 = first.quotient();
        Expr q2 = second.quotient();
        boolean same = q1.equals(q2);
        boolean opposite = !same && Expand.expand(Expr.neg(q2)).equals(q1);
        if (!same && !opposite) return Optional.empty();

        Expr grouped = Expr.sym(first.factor() + "·(" + q1 + ") + (" + second.factor() + ")·(" + q2 + ")");
        rec.note(Rule.POLY_GROUPING, "g", grouped);
        Expr factor = same ? Expr.add(first.factor(), second.factor()) : Expr.sub(first.factor(), second.factor());
        Expr result = Expr.mul(q1, factor);
        rec.note(Rule.POLY_COMMON_BINOMIAL, "b", q1, "f", Expr.sym(whole.toString()), "g", result);
        return Optional.of(result);
    }

    /** Dos enteros con producto {@code product} y suma {@code sum}, o {@code null}. */
    private static BigInteger[] findPair(BigInteger product, BigInteger sum) {
        BigInteger absolute = product.abs();
        if (absolute.bitLength() > 40) return null;
        long value = absolute.longValueExact();
        for (long d = 1; d * d <= value; d++) {
            if (value % d != 0) continue;
            long e = value / d;
            for (long[] candidate : new long[][]{{d, e}, {-d, -e}, {d, -e}, {-d, e}, {e, d}, {-e, -d}, {e, -d}, {-e, d}}) {
                BigInteger m = BigInteger.valueOf(candidate[0]);
                BigInteger n = BigInteger.valueOf(candidate[1]);
                if (m.multiply(n).equals(product) && m.add(n).equals(sum)) return new BigInteger[]{m, n};
            }
        }
        return null;
    }

    private static Expr num(BigInteger value) {
        return Expr.num(Rational.of(value));
    }

    private static Optional<Expr> grouping(Rec rec, Expr.Add sum) {
        List<Expr> terms = sum.terms();
        if (terms.size() != 4) return Optional.empty();
        int[][] pairings = {{0, 1, 2, 3}, {0, 2, 1, 3}, {0, 3, 1, 2}};
        for (int[] p : pairings) {
            Factor.CommonFactor left = groupOf(terms.get(p[0]), terms.get(p[1]));
            Factor.CommonFactor right = groupOf(terms.get(p[2]), terms.get(p[3]));
            // Si ninguno de los dos grupos saca nada, no hay agrupación
            if (left.factor().isOne() && right.factor().isOne()) continue;
            Optional<Expr> result = combine(rec, sum, left, right);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    /** Raíces racionales con el teorema de la raíz racional y división sintética (Ruffini). */
    private static Optional<Expr> rationalRoots(Rec rec, Expr.Add sum) {
        Optional<String> variable = singleVariable(sum);
        if (variable.isEmpty()) return Optional.empty();
        String v = variable.get();
        Optional<Polynomial> polynomial = Polynomial.from(sum, v);
        if (polynomial.isEmpty() || polynomial.get().degree() < 2) return Optional.empty();

        Polynomial rest = polynomial.get();
        if (Factor.rationalRoot(rest).isEmpty()) return Optional.empty();

        Expr x = Expr.sym(v);
        List<Expr> factors = new ArrayList<>();
        boolean first = true;
        while (rest.degree() >= 2) {
            Optional<Rational> root = Factor.rationalRoot(rest);
            if (root.isEmpty()) break;
            if (first) {
                List<Rational> candidates = Factor.rationalRootCandidates(rest);
                if (rest.coefficient(0).isZero()) candidates = List.of();
                rec.note(Rule.ALG_RATIONAL_ROOT_CANDIDATES, "a0", Expr.num(lastNonZero(rest, true)), "an", Expr.num(rest.leading()),
                        "list", Expr.sym(candidatesText(candidates)));
                first = false;
            }
            Rational r = root.get();
            Expr linear = Expr.sub(Expr.mul(num(r.denominator()), x), num(r.numerator()));
            Polynomial divisor = Polynomial.of(r.numerator().negate().longValue(), r.denominator().longValue());
            Polynomial quotient = rest.divide(divisor).quotient();
            rec.note(Rule.ALG_ROOT_FOUND, "r", Expr.num(r), "factor", linear);
            rec.note(Rule.ALG_SYNTHETIC_DIVISION, "factor", linear, "q", quotient.toExpr(v));
            factors.add(linear);
            rest = quotient;
        }
        Expr remaining = rest.toExpr(v);
        // La parte constante que queda se lleva el coeficiente principal y los signos
        Expr result = Expr.mul(Expr.mul(factors), remaining);
        rec.note(Rule.ALG_FACTOR, "f", sum, "g", result);
        return Optional.of(result);
    }

    private static Rational lastNonZero(Polynomial p, boolean constantTerm) {
        if (constantTerm) {
            for (int k = 0; k <= p.degree(); k++) if (!p.coefficient(k).isZero()) return p.coefficient(k);
        }
        return p.leading();
    }

    private static String candidatesText(List<Rational> candidates) {
        if (candidates.isEmpty()) return "0";
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (Rational c : candidates) {
            if (shown++ >= 16) {
                sb.append(", …");
                break;
            }
            if (sb.length() > 0) sb.append(", ");
            sb.append(c);
        }
        return sb.toString();
    }

    /** {@code ax² + bx + c} con raíces reales: {@code a(x − r₁)(x − r₂)}, a partir de la fórmula o de completar el cuadrado. */
    private static Optional<Expr> quadraticRoots(Rec rec, Expr.Add sum, boolean completeSquare) {
        Optional<String> variable = singleVariable(sum);
        if (variable.isEmpty()) return Optional.empty();
        String v = variable.get();
        Optional<Polynomial> polynomial = Polynomial.from(sum, v);
        if (polynomial.isEmpty() || polynomial.get().degree() != 2) return Optional.empty();
        Polynomial p = polynomial.get();
        Rational a = p.coefficient(2);
        Rational b = p.coefficient(1);
        Rational c = p.coefficient(0);
        Rational discriminant = b.multiply(b).subtract(Rational.of(4).multiply(a).multiply(c));
        if (discriminant.signum() < 0) return Optional.empty();

        Expr x = Expr.sym(v);
        Expr sqrtD = Expr.sqrt(Expr.num(discriminant));
        Expr twoA = Expr.num(a.multiply(Rational.of(2)));
        Expr r1 = Expr.div(Expr.add(Expr.num(b.negate()), Expr.neg(sqrtD)), twoA);
        Expr r2 = Expr.div(Expr.add(Expr.num(b.negate()), sqrtD), twoA);

        Expr result;
        if (completeSquare) {
            Expr h = Expr.num(b.divide(a.multiply(Rational.of(2))));
            Expr k = Expr.num(discriminant.divide(a.multiply(Rational.of(4)).multiply(a)).multiply(a));
            Expr squared = Expr.mul(Expr.num(a), Expr.pow(Expr.add(x, h), 2));
            rec.note(Rule.POLY_COMPLETE_SQUARE, "k", Expr.num(b.multiply(b).divide(a.multiply(Rational.of(4)))), "f", sum,
                    "g", Expr.sub(squared, k));
            Expr half = Expr.div(sqrtD, twoA);
            Expr product = Expr.mul(Expr.num(a), Expr.sub(Expr.add(x, h), half), Expr.add(Expr.add(x, h), half));
            rec.note(Rule.POLY_DIFF_SQUARES, "f", Expr.sub(squared, k), "a", Expr.add(x, h), "b", half, "g", product);
            result = product;
        } else {
            rec.note(Rule.ALG_DISCRIMINANT, "d", Expr.num(discriminant));
            result = Expr.mul(Expr.num(a), Expr.sub(x, r2), Expr.sub(x, r1));
            rec.note(Rule.POLY_QUADRATIC_ROOTS, "r1", r1, "r2", r2, "a", Expr.num(a), "f", sum, "g", result);
        }
        return Optional.of(result);
    }

    /** Polinomio en {@code x^k}: se factoriza como polinomio en {@code u = x^k} y se vuelve a {@code x}. */
    private static Optional<Expr> substitution(Rec rec, Expr.Add sum) {
        Optional<String> variable = singleVariable(sum);
        if (variable.isEmpty()) return Optional.empty();
        String v = variable.get();
        Optional<Polynomial> polynomial = Polynomial.from(sum, v);
        if (polynomial.isEmpty() || polynomial.get().degree() < 4) return Optional.empty();
        Polynomial p = polynomial.get();

        int k = 0;
        for (int i = 1; i <= p.degree(); i++) if (!p.coefficient(i).isZero()) k = gcd(k, i);
        if (k < 2) return Optional.empty();

        Rational[] reduced = new Rational[p.degree() / k + 1];
        for (int i = 0; i < reduced.length; i++) reduced[i] = p.coefficient(i * k);
        Polynomial inner = Polynomial.of(reduced);
        String u = v.equals("u") ? "w" : "u";
        Expr innerExpr = inner.toExpr(u);

        Rec scratch = Rec.scratch();
        Expr innerFactored = autoSum(scratch, innerExpr, 0);
        if (innerFactored.equals(innerExpr)) return Optional.empty();

        Expr x = Expr.sym(v);
        Expr power = Expr.pow(x, k);
        rec.note(Rule.POLY_SUBSTITUTION, "x", x, "k", Expr.num(k), "u", Expr.sym(u), "e", power, "p", innerExpr);
        Expr back = innerFactored.substitute(u, power);
        Expr result = Expand.expand(back).equals(Expand.expand(sum)) ? back : null;
        if (result == null) return Optional.empty();
        rec.note(Rule.ALG_FACTOR, "f", sum, "g", result);
        return Optional.of(result);
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private static Optional<String> singleVariable(Expr e) {
        List<String> variables = List.copyOf(e.variables());
        return variables.size() == 1 ? Optional.of(variables.get(0)) : Optional.empty();
    }
}
