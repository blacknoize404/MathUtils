package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Algebra;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Fn;
import barnesinside.mathutils.cas.expr.Op;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.expr.Relation;
import barnesinside.mathutils.cas.expr.SolutionSet;
import barnesinside.mathutils.cas.poly.Factor;
import barnesinside.mathutils.cas.poly.Polynomial;
import barnesinside.mathutils.cas.steps.Rule;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resuelve ecuaciones en una variable real registrando los pasos: primer grado, segundo grado (con varios métodos),
 * polinomios de grado mayor, fracciones algebraicas, radicales, valor absoluto, exponenciales, logaritmos,
 * trigonometría y, si nada de eso sirve, búsqueda numérica de raíces.
 */
final class EquationSolver {

    private static final int MAX_DEPTH = 8;

    private final Rec rec;
    private final String v;
    private final Expr x;
    private final SolveMethod method;
    /** Si alguna solución se encontró numéricamente. */
    private boolean approximate;

    EquationSolver(Rec rec, String v, SolveMethod method) {
        this.rec = rec;
        this.v = v;
        this.x = Expr.sym(v);
        this.method = method;
    }

    boolean approximate() {
        return approximate;
    }

    SolutionSet solve(Relation r) {
        return solveEquation(r, 0);
    }

    private static Relation zero(Expr f) {
        return new Relation(f, Op.EQ, Expr.ZERO);
    }

    // ================================================================ Despacho

    private SolutionSet solveEquation(Relation r, int depth) {
        if (depth > MAX_DEPTH) return numeric(r.difference());
        Expr f = r.difference();
        if (f.isFreeOf(v)) return constant(r, f);

        Optional<Polynomial> polynomial = Polynomial.from(f, v);
        if (polynomial.isPresent()) {
            Polynomial p = polynomial.get();
            if (p.degree() <= 0) return constant(r, p.isZero() ? Expr.ZERO : p.toExpr(v));
            if (p.degree() == 1) {
                if (Linear.applies(r, v)) return Linear.solve(rec, r, v);
                return Linear.solve(rec, oneSide(r, f), v);
            }
            Relation z = oneSide(r, f);
            return p.degree() == 2 ? quadratic(p) : polynomial(z, p, depth);
        }
        Optional<Expr[]> linear = symbolicLinear(f);
        if (linear.isPresent()) {
            Expr[] ab = linear.get();
            Expr solution = Expand.expand(Expr.div(Expr.neg(ab[1]), ab[0]));
            rec.rel(Rule.ALG_ISOLATE, new Relation(x, Op.EQ, solution), "x", x);
            return SolutionSet.finite(List.of(solution));
        }
        return nonPolynomial(r, f, depth);
    }

    /** {@code f = a·x + b} con {@code a} y {@code b} sin la incógnita pero no necesariamente racionales (por ejemplo {@code ln 5}). */
    private Optional<Expr[]> symbolicLinear(Expr f) {
        Expr a = Expr.ZERO;
        Expr b = Expr.ZERO;
        for (Expr term : Algebra.terms(Expand.expand(f))) {
            if (term.isFreeOf(v)) {
                b = Expr.add(b, term);
                continue;
            }
            Expr coefficient = Expand.expand(Expr.div(term, x));
            if (!coefficient.isFreeOf(v)) return Optional.empty();
            a = Expr.add(a, coefficient);
        }
        if (a.isZero() || !a.variables().isEmpty() || !b.variables().isEmpty()) return Optional.empty();
        return Optional.of(new Expr[]{a, b});
    }

    /** {@code f = 0} con {@code f} sin la incógnita: o se cumple siempre o no se cumple nunca. */
    private SolutionSet constant(Relation r, Expr f) {
        boolean holds = f.isZero() || (f.variables().isEmpty() && Math.abs(Eval.value(f)) < 1e-12);
        Relation shown = new Relation(f, Op.EQ, Expr.ZERO);
        rec.rel(holds ? Rule.ALG_IDENTITY : Rule.ALG_CONTRADICTION, shown, "x", x);
        return holds ? SolutionSet.allReals() : SolutionSet.empty();
    }

    /** Pasa todo a la izquierda y desarrolla; registra el paso si cambia algo. */
    private Relation oneSide(Relation r, Expr f) {
        Expr shown = Expand.expand(f);
        Relation z = zero(shown);
        boolean sameAsBefore = r.right().isZero() && r.left().equals(shown);
        if (!sameAsBefore) rec.rel(r.right().isZero() ? Rule.ALG_SIMPLIFY : Rule.ALG_MOVE_TO_ONE_SIDE, z);
        return z;
    }

    // ================================================================ Segundo grado

    private SolutionSet quadratic(Polynomial p) {
        return quadratic(p, method);
    }

    private SolutionSet quadratic(Polynomial p, SolveMethod requested) {
        Rational a = p.coefficient(2);
        Rational b = p.coefficient(1);
        Rational c = p.coefficient(0);
        Rational discriminant = b.multiply(b).subtract(Rational.of(4).multiply(a).multiply(c));
        boolean perfect = discriminant.signum() >= 0 && isRationalSquare(discriminant);

        SolveMethod m = requested;
        boolean chosen = m == SolveMethod.SQUARE_ROOT && b.isZero() || m == SolveMethod.FACTORING
                || m == SolveMethod.COMPLETE_SQUARE || m == SolveMethod.QUADRATIC_FORMULA;
        if (!chosen) m = b.isZero() ? SolveMethod.SQUARE_ROOT : perfect ? SolveMethod.FACTORING : SolveMethod.QUADRATIC_FORMULA;

        return switch (m) {
            case SQUARE_ROOT -> squareRoot(a, c);
            case FACTORING -> factoring(p, 1);
            case COMPLETE_SQUARE -> completeSquare(a, b, c);
            default -> quadraticFormula(a, b, c, discriminant);
        };
    }

    static boolean isRationalSquare(Rational r) {
        return r.signum() >= 0 && Factor.exactRoot(Expr.num(r), 2).isPresent();
    }

    private SolutionSet squareRoot(Rational a, Rational c) {
        Expr x2 = Expr.pow(x, 2);
        if (!c.isZero()) {
            rec.rel(Rule.ALG_ADD_TO_BOTH, new Relation(Expr.mul(Expr.num(a), x2), Op.EQ, Expr.num(c.negate())), "t", Expr.num(c.negate()));
        }
        Rational k = c.negate().divide(a);
        if (!a.isOne()) rec.rel(Rule.ALG_DIVIDE_BOTH, new Relation(x2, Op.EQ, Expr.num(k)), "t", Expr.num(a));
        if (k.signum() < 0) {
            rec.rel(Rule.ALG_CONTRADICTION, new Relation(x2, Op.EQ, Expr.num(k)), "x", x);
            return SolutionSet.empty();
        }
        if (k.isZero()) {
            rec.rel(Rule.ALG_SQUARE_ROOT, new Relation(x, Op.EQ, Expr.ZERO));
            return SolutionSet.finite(List.of(Expr.ZERO));
        }
        Expr root = Expr.sqrt(Expr.num(k));
        rec.rel(Rule.ALG_SQUARE_ROOT, new Relation(x, Op.EQ, root));
        rec.rel(Rule.ALG_OTHER_SOLUTION, new Relation(x, Op.EQ, Expr.neg(root)));
        return SolutionSet.finite(List.of(root, Expr.neg(root)));
    }

    private SolutionSet quadraticFormula(Rational a, Rational b, Rational c, Rational d) {
        rec.note(Rule.ALG_QUADRATIC_FORM, "x", x, "a", Expr.num(a), "b", Expr.num(b), "c", Expr.num(c));
        rec.note(Rule.ALG_DISCRIMINANT, "d", Expr.num(d));
        if (d.signum() < 0) {
            rec.note(Rule.ALG_DISCRIMINANT_NEGATIVE, "d", Expr.num(d));
            return SolutionSet.empty();
        }
        rec.note(d.isZero() ? Rule.ALG_DISCRIMINANT_ZERO : Rule.ALG_DISCRIMINANT_POSITIVE, "d", Expr.num(d));
        rec.note(Rule.ALG_QUADRATIC_FORMULA, "a", Expr.num(a), "b", Expr.num(b), "c", Expr.num(c));

        Expr sqrtD = Expr.sqrt(Expr.num(d));
        Expr twoA = Expr.num(a.multiply(Rational.of(2)));
        Expr r1 = Expr.div(Expr.add(Expr.num(b.negate()), sqrtD), twoA);
        Expr r2 = Expr.div(Expr.sub(Expr.num(b.negate()), sqrtD), twoA);
        if (d.isZero()) {
            rec.rel(Rule.ALG_ISOLATE, new Relation(x, Op.EQ, r1), "x", x);
            return SolutionSet.finite(List.of(r1));
        }
        rec.rel(Rule.ALG_ISOLATE, new Relation(x, Op.EQ, r1), "x", x);
        rec.rel(Rule.ALG_OTHER_SOLUTION, new Relation(x, Op.EQ, r2));
        return SolutionSet.finite(List.of(r1, r2));
    }

    private SolutionSet completeSquare(Rational a, Rational b, Rational c) {
        Expr x2 = Expr.pow(x, 2);
        if (!c.isZero()) {
            rec.rel(Rule.ALG_ADD_TO_BOTH, new Relation(Expr.add(Expr.mul(Expr.num(a), x2), Expr.mul(Expr.num(b), x)), Op.EQ,
                    Expr.num(c.negate())), "t", Expr.num(c.negate()));
        }
        Rational bb = b.divide(a);
        Rational cc = c.negate().divide(a);
        if (!a.isOne()) {
            rec.rel(Rule.ALG_DIVIDE_BOTH, new Relation(Expr.add(x2, Expr.mul(Expr.num(bb), x)), Op.EQ, Expr.num(cc)), "t", Expr.num(a));
        }
        Rational k = bb.divide(Rational.of(2)).multiply(bb.divide(Rational.of(2)));
        Rational right = cc.add(k);
        rec.rel(Rule.ALG_COMPLETE_SQUARE, new Relation(Expr.add(x2, Expr.mul(Expr.num(bb), x), Expr.num(k)), Op.EQ, Expr.num(right)),
                "a", Expr.num(a), "k", Expr.num(k));
        Expr h = Expr.num(bb.divide(Rational.of(2)));
        Expr shifted = Expr.add(x, h);
        rec.rel(Rule.ALG_PERFECT_SQUARE_FORM, new Relation(Expr.pow(shifted, 2), Op.EQ, Expr.num(right)));
        if (right.signum() < 0) {
            rec.rel(Rule.ALG_CONTRADICTION, new Relation(Expr.pow(shifted, 2), Op.EQ, Expr.num(right)), "x", x);
            return SolutionSet.empty();
        }
        Expr center = Expr.neg(h);
        if (right.isZero()) {
            rec.rel(Rule.ALG_SQUARE_ROOT, new Relation(shifted, Op.EQ, Expr.ZERO));
            rec.rel(Rule.ALG_ISOLATE, new Relation(x, Op.EQ, center), "x", x);
            return SolutionSet.finite(List.of(center));
        }
        Expr root = Expr.sqrt(Expr.num(right));
        rec.rel(Rule.ALG_SQUARE_ROOT, new Relation(shifted, Op.EQ, root));
        Expr s1 = Expr.add(center, root);
        Expr s2 = Expr.sub(center, root);
        rec.rel(Rule.ALG_ISOLATE, new Relation(x, Op.EQ, s1), "x", x);
        rec.rel(Rule.ALG_OTHER_SOLUTION, new Relation(x, Op.EQ, s2));
        return SolutionSet.finite(List.of(s1, s2));
    }

    // ================================================================ Factorizar y producto cero

    private SolutionSet factoring(Polynomial p, int depth) {
        Expr f = Expand.expand(p.toExpr(v));
        Expr factored = Factoring.auto(Rec.scratch(), f);
        if (factored.equals(f) || nonConstantFactors(factored).isEmpty()) {
            // No se factoriza: queda un polinomio irreducible para este método
            return remainder(p, depth);
        }
        rec.group(Rule.ALG_FACTOR, null, () -> {
            Factoring.auto(rec, f);
            return null;
        }, "f", f, "g", factored);
        return zeroProduct(factored, depth);
    }

    /** Los factores distintos de un producto que dependen de la incógnita. */
    private List<Expr> nonConstantFactors(Expr product) {
        List<Expr> result = new ArrayList<>();
        for (Expr factor : Algebra.factors(product)) {
            Expr base = factor instanceof Expr.Pow pw && pw.exponent() instanceof Expr.Num n && n.value().isInteger()
                    && n.value().signum() > 0 ? pw.base() : factor;
            if (base.isFreeOf(v) || result.contains(base)) continue;
            result.add(base);
        }
        return result;
    }

    private SolutionSet zeroProduct(Expr product, int depth) {
        List<Expr> factors = nonConstantFactors(product);
        Expr list = Expr.sym(factors.stream().map(f -> f + " = 0").collect(Collectors.joining(",  ")));
        return rec.group(Rule.ALG_ZERO_PRODUCT, null, () -> {
            List<SolutionSet> parts = new ArrayList<>();
            for (Expr factor : factors) {
                Relation fr = zero(factor);
                parts.add(rec.group(Rule.ALG_ZERO_FACTOR, fr, () -> solveEquation(fr, depth + 1)));
            }
            return union(parts);
        }, "list", list);
    }

    /** Une conjuntos de soluciones de factores distintos. */
    private SolutionSet union(List<SolutionSet> parts) {
        List<Expr> values = new ArrayList<>();
        List<Expr> families = new ArrayList<>();
        String parameter = null;
        boolean approx = false;
        for (SolutionSet s : parts) {
            switch (s) {
                case SolutionSet.Finite f -> {
                    values.addAll(f.values());
                    approx |= f.approximate();
                }
                case SolutionSet.Periodic p -> {
                    families.addAll(p.families());
                    parameter = p.parameter();
                }
                case SolutionSet.AllReals a -> {
                    return SolutionSet.allReals();
                }
                default -> { }
            }
        }
        if (!families.isEmpty()) {
            families.addAll(values);
            return new SolutionSet.Periodic(families, parameter);
        }
        return approx ? SolutionSet.approximate(values) : SolutionSet.finite(values);
    }

    // ================================================================ Polinomios de grado alto

    private SolutionSet polynomial(Relation z, Polynomial p, int depth) {
        return switch (method) {
            case SUBSTITUTION -> substitution(p, depth).orElseGet(() -> remainder(p, depth));
            case RATIONAL_ROOTS -> rationalRoots(p, depth);
            case FACTORING -> factoring(p, depth);
            case NUMERIC -> numeric(p.toExpr(v));
            default -> {
                Expr f = Expand.expand(p.toExpr(v));
                List<Expr> factors = nonConstantFactors(Factoring.auto(Rec.scratch(), f));
                if (factors.size() >= 2) yield factoring(p, depth);
                if (Factor.rationalRoot(p).isPresent()) yield rationalRoots(p, depth);
                Optional<SolutionSet> substituted = substitution(p, depth);
                if (substituted.isPresent()) yield substituted.get();
                yield numeric(f);
            }
        };
    }

    /** Lo que queda sin resolver exactamente de un polinomio: se busca numéricamente. */
    private SolutionSet remainder(Polynomial p, int depth) {
        if (p.degree() == 1) return Linear.solve(rec, zero(p.toExpr(v)), v);
        if (p.degree() == 2) return quadratic(p, SolveMethod.QUADRATIC_FORMULA);
        return numeric(p.toExpr(v));
    }

    private SolutionSet rationalRoots(Polynomial p, int depth) {
        Polynomial rest = p;
        List<Expr> roots = new ArrayList<>();
        boolean first = true;
        List<SolutionSet> parts = new ArrayList<>();
        while (rest.degree() >= 3) {
            Optional<Rational> root = Factor.rationalRoot(rest);
            if (root.isEmpty()) break;
            if (first) {
                List<Rational> candidates = rest.coefficient(0).isZero() ? List.of() : Factor.rationalRootCandidates(rest);
                Rational a0 = rest.coefficient(0).isZero() ? Rational.ZERO : rest.coefficient(0);
                rec.note(Rule.ALG_RATIONAL_ROOT_CANDIDATES, "a0", Expr.num(a0), "an", Expr.num(rest.leading()),
                        "list", Expr.sym(candidatesText(candidates)));
                first = false;
            }
            Rational r = root.get();
            Expr linear = Expr.sub(Expr.mul(Expr.num(Rational.of(r.denominator())), x), Expr.num(Rational.of(r.numerator())));
            Polynomial divisor = Polynomial.of(r.numerator().negate().longValue(), r.denominator().longValue());
            Polynomial quotient = rest.divide(divisor).quotient();
            rec.note(Rule.ALG_ROOT_FOUND, "r", Expr.num(r), "factor", linear);
            rec.note(Rule.ALG_SYNTHETIC_DIVISION, "factor", linear, "q", Expand.expand(quotient.toExpr(v)));
            roots.add(Expr.num(r));
            rest = quotient;
        }
        parts.add(SolutionSet.finite(roots));
        Polynomial remaining = rest;
        if (rest.degree() >= 3) {
            parts.add(substitution(remaining, depth).orElseGet(() -> numeric(Expand.expand(remaining.toExpr(v)))));
        } else if (rest.degree() == 2) {
            rec.rel(Rule.ALG_SIMPLIFY, zero(Expand.expand(rest.toExpr(v))));
            parts.add(quadratic(rest));
        } else if (rest.degree() == 1) {
            parts.add(Linear.solve(rec, zero(rest.toExpr(v)), v));
        }
        return union(parts);
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

    /** Polinomio en {@code x^k}: se resuelve en {@code u = x^k} y se vuelve a {@code x}. */
    private Optional<SolutionSet> substitution(Polynomial p, int depth) {
        int k = 0;
        for (int i = 1; i <= p.degree(); i++) if (!p.coefficient(i).isZero()) k = gcd(k, i);
        if (k < 2) return Optional.empty();

        Rational[] reduced = new Rational[p.degree() / k + 1];
        for (int i = 0; i < reduced.length; i++) reduced[i] = p.coefficient(i * k);
        String u = v.equals("u") ? "w" : "u";
        Expr uSym = Expr.sym(u);
        Expr inner = Expand.expand(Polynomial.of(reduced).toExpr(u));
        Expr power = Expr.pow(x, k);

        rec.rel(Rule.ALG_SUBSTITUTION, zero(inner), "u", uSym, "e", power);
        SolutionSet innerSet = new EquationSolver(rec, u, SolveMethod.AUTO).solve(zero(inner));
        if (!(innerSet instanceof SolutionSet.Finite finite)) return Optional.of(innerSet.isEmpty() ? innerSet : SolutionSet.empty());

        List<Expr> values = new ArrayList<>();
        for (Expr u0 : finite.values()) {
            Relation back = new Relation(power, Op.EQ, u0);
            rec.rel(Rule.ALG_BACK_SUBSTITUTION, back, "u", uSym, "e", power, "x", x);
            double value = Eval.value(u0);
            if (value < 0 && k % 2 == 0) {
                rec.rel(Rule.ALG_CONTRADICTION, back, "x", x);
                continue;
            }
            if (value == 0) {
                values.add(Expr.ZERO);
                continue;
            }
            Expr root = Expr.pow(Expr.abs(u0).equals(u0) ? u0 : Expr.neg(u0), Expr.num(1, k));
            if (k % 2 == 0) {
                values.add(root);
                values.add(Expr.neg(root));
            } else {
                values.add(value > 0 ? root : Expr.neg(root));
            }
        }
        return Optional.of(finite.approximate() ? SolutionSet.approximate(values) : SolutionSet.finite(values));
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    // ================================================================ No polinómicas

    /** La clase de ecuación de {@code f = 0} según su forma. */
    Equations.Kind kind(Expr f) {
        if (f.isFreeOf(v)) return Equations.Kind.CONSTANT;
        if (Polynomial.from(f, v).isPresent()) return Equations.Kind.POLYNOMIAL;
        if (contains(f, e -> e instanceof Expr.Func fn && fn.fn() == Fn.ABS && !fn.arg().isFreeOf(v))) return Equations.Kind.ABSOLUTE;
        if (hasRadical(f)) return Equations.Kind.RADICAL;
        if (hasLogOrExp(f)) return Equations.Kind.LOG_EXP;
        if (hasTrig(f)) return Equations.Kind.TRIG;
        Algebra.Fraction fraction = Algebra.fraction(Algebra.together(f));
        Expr numerator = Expand.expand(fraction.numerator());
        Expr denominator = Expand.expand(fraction.denominator());
        if (!denominator.isFreeOf(v) && Polynomial.from(numerator, v).isPresent() && Polynomial.from(denominator, v).isPresent()) {
            return Equations.Kind.RATIONAL;
        }
        return Equations.Kind.OTHER;
    }

    private SolutionSet nonPolynomial(Relation r, Expr f, int depth) {
        if (contains(f, e -> e instanceof Expr.Func fn && fn.fn() == Fn.ABS && !fn.arg().isFreeOf(v))) return absolute(r, f, depth);
        if (hasRadical(f)) return radical(r, f, depth);
        if (hasLogOrExp(f)) return logExp(r, f, depth);
        if (hasTrig(f)) return trig(r, f, depth);
        Optional<SolutionSet> rational = rational(r, f, depth);
        if (rational.isPresent()) return rational.get();
        return numeric(f);
    }

    private interface Test {
        boolean matches(Expr e);
    }

    private static boolean contains(Expr e, Test test) {
        if (test.matches(e)) return true;
        return switch (e) {
            case Expr.Add a -> a.terms().stream().anyMatch(t -> contains(t, test));
            case Expr.Mul m -> m.factors().stream().anyMatch(t -> contains(t, test));
            case Expr.Pow p -> contains(p.base(), test) || contains(p.exponent(), test);
            case Expr.Func fn -> contains(fn.arg(), test);
            default -> false;
        };
    }

    private boolean hasRadical(Expr f) {
        return contains(f, e -> e instanceof Expr.Pow p && !p.base().isFreeOf(v) && p.exponent() instanceof Expr.Num n
                && !n.value().isInteger());
    }

    private boolean hasLogOrExp(Expr f) {
        return contains(f, e -> (e instanceof Expr.Func fn && fn.fn() == Fn.LN && !fn.arg().isFreeOf(v))
                || (e instanceof Expr.Pow p && !p.exponent().isFreeOf(v) && p.base().isFreeOf(v)));
    }

    private boolean hasTrig(Expr f) {
        return contains(f, e -> e instanceof Expr.Func fn && !fn.arg().isFreeOf(v)
                && (fn.fn() == Fn.SIN || fn.fn() == Fn.COS || fn.fn() == Fn.TAN));
    }

    // ---------------------------------------------------------------- Fracciones

    private Optional<SolutionSet> rational(Relation r, Expr f, int depth) {
        Algebra.Fraction fraction = Algebra.fraction(Algebra.together(f));
        Expr numerator = Expand.expand(fraction.numerator());
        Expr denominator = Expand.expand(fraction.denominator());
        if (denominator.isFreeOf(v) || Polynomial.from(numerator, v).isEmpty() || Polynomial.from(denominator, v).isEmpty()) {
            return Optional.empty();
        }

        SolutionSet excluded = new EquationSolver(Rec.scratch(), v, SolveMethod.AUTO).solve(zero(denominator));
        List<Expr> forbidden = excluded instanceof SolutionSet.Finite fin ? fin.values() : List.of();
        if (!forbidden.isEmpty()) {
            String condition = forbidden.stream().map(e -> v + " ≠ " + e).collect(Collectors.joining(", "));
            rec.note(Rule.ALG_DOMAIN, "cond", Expr.sym(condition));
        }
        Relation top = zero(numerator);
        rec.rel(Rule.ALG_NUMERATOR_ZERO, top);

        SolutionSet candidates = solveEquation(top, depth + 1);
        if (!(candidates instanceof SolutionSet.Finite fin)) return Optional.of(candidates);
        List<Expr> kept = new ArrayList<>();
        for (Expr c : fin.values()) {
            boolean bad = forbidden.stream().anyMatch(b -> Math.abs(Eval.value(b) - Eval.value(c)) < 1e-9);
            if (bad) rec.note(Rule.ALG_CHECK_EXTRANEOUS, "x", x, "v", c);
            else kept.add(c);
        }
        return Optional.of(fin.approximate() ? SolutionSet.approximate(kept) : SolutionSet.finite(kept));
    }

    // ---------------------------------------------------------------- Radicales

    private SolutionSet radical(Relation r, Expr f, int depth) {
        Expr.Pow root = firstRadical(f);
        if (root == null) return numeric(f);
        int q = ((Expr.Num) root.exponent()).value().denominator().intValueExact();
        if (!(root.exponent() instanceof Expr.Num n) || !n.value().numerator().equals(BigInteger.ONE)) return numeric(f);

        // f = c·R + resto con c constante
        Expr coefficient = Expr.ZERO;
        List<Expr> rest = new ArrayList<>();
        int count = 0;
        for (Expr term : Algebra.terms(f)) {
            if (!Algebra.contains(term, root)) {
                rest.add(term);
                continue;
            }
            Expr c = Expand.expand(Expr.div(term, root));
            if (!c.isFreeOf(v) || Algebra.contains(c, root)) return numeric(f);
            coefficient = Expr.add(coefficient, c);
            count++;
        }
        if (count == 0 || coefficient.isZero()) return numeric(f);

        Expr isolated = Expand.expand(Expr.div(Expr.neg(Expr.add(rest)), coefficient));
        Relation isolation = new Relation(root, Op.EQ, isolated);
        rec.rel(Rule.ALG_ISOLATE_RADICAL, isolation);

        Expr powered = Expand.expand(Expr.pow(isolated, q));
        Relation raised = new Relation(root.base(), Op.EQ, powered);
        rec.rel(q == 2 ? Rule.ALG_SQUARE_BOTH_SIDES : Rule.ALG_RAISE_BOTH_SIDES, raised, "n", Expr.num(q));

        SolutionSet candidates = solveEquation(raised, depth + 1);
        if (!(candidates instanceof SolutionSet.Finite fin)) return candidates;
        return check(r, fin, q % 2 == 0);
    }

    private Expr.Pow firstRadical(Expr f) {
        Expr.Pow[] found = new Expr.Pow[1];
        contains(f, e -> {
            if (found[0] == null && e instanceof Expr.Pow p && !p.base().isFreeOf(v) && p.exponent() instanceof Expr.Num n
                    && !n.value().isInteger() && n.value().numerator().equals(BigInteger.ONE) && n.value().signum() > 0) {
                found[0] = p;
            }
            return false;
        });
        return found[0];
    }

    /** Sustituye cada candidato en la ecuación y descarta los que no la cumplen. */
    private SolutionSet check(Relation r, SolutionSet.Finite candidates, boolean mayBeExtraneous) {
        if (candidates.values().isEmpty()) return SolutionSet.empty();
        if (!mayBeExtraneous) return candidates;
        List<Expr> kept = new ArrayList<>();
        String list = candidates.values().stream().map(c -> v + " = " + c).collect(Collectors.joining(", "));
        rec.group(Rule.ALG_CHECK_SOLUTIONS, null, () -> {
            for (Expr c : candidates.values()) {
                if (holds(r, c)) {
                    rec.note(Rule.ALG_CHECK_VALID, "x", x, "v", c);
                    kept.add(c);
                } else {
                    rec.note(Rule.ALG_CHECK_EXTRANEOUS, "x", x, "v", c);
                }
            }
            return null;
        }, "list", Expr.sym(list));
        return candidates.approximate() ? SolutionSet.approximate(kept) : SolutionSet.finite(kept);
    }

    /** Si el valor cumple la ecuación, comprobado numéricamente. */
    private boolean holds(Relation r, Expr value) {
        try {
            double left = Eval.value(r.left().substitute(v, value));
            double right = Eval.value(r.right().substitute(v, value));
            if (Double.isNaN(left) || Double.isNaN(right) || Double.isInfinite(left) || Double.isInfinite(right)) return false;
            return Math.abs(left - right) <= 1e-9 * Math.max(1, Math.max(Math.abs(left), Math.abs(right)));
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------- Valor absoluto

    private SolutionSet absolute(Relation r, Expr f, int depth) {
        Expr.Func[] found = new Expr.Func[1];
        contains(f, e -> {
            if (found[0] == null && e instanceof Expr.Func fn && fn.fn() == Fn.ABS && !fn.arg().isFreeOf(v)) found[0] = fn;
            return false;
        });
        Expr.Func abs = found[0];

        // |g1| = |g2|
        List<Expr> terms = Algebra.terms(f);
        List<Expr.Func> absTerms = new ArrayList<>();
        for (Expr t : terms) {
            if (t instanceof Expr.Func fn && fn.fn() == Fn.ABS) absTerms.add(fn);
            else if (t instanceof Expr.Mul m && m.factors().size() == 2 && m.factors().get(0).equals(Expr.MINUS_ONE)
                    && m.factors().get(1) instanceof Expr.Func fn && fn.fn() == Fn.ABS) absTerms.add(fn);
        }
        if (terms.size() == 2 && absTerms.size() == 2) {
            Expr.Func first = (Expr.Func) (terms.get(0) instanceof Expr.Func ? terms.get(0) : ((Expr.Mul) terms.get(0)).factors().get(1));
            Expr.Func second = (Expr.Func) (terms.get(1) instanceof Expr.Func ? terms.get(1) : ((Expr.Mul) terms.get(1)).factors().get(1));
            boolean opposite = (terms.get(0) instanceof Expr.Func) != (terms.get(1) instanceof Expr.Func);
            if (opposite) return absoluteCases(r, first.arg(), second.arg(), depth, true);
        }

        Expr coefficient = Expr.ZERO;
        List<Expr> rest = new ArrayList<>();
        for (Expr term : terms) {
            if (!Algebra.contains(term, abs)) {
                rest.add(term);
                continue;
            }
            Expr c = Expand.expand(Expr.div(term, abs));
            if (!c.isFreeOf(v) || Algebra.contains(c, abs)) return numeric(f);
            coefficient = Expr.add(coefficient, c);
        }
        if (coefficient.isZero()) return numeric(f);
        Expr h = Expand.expand(Expr.div(Expr.neg(Expr.add(rest)), coefficient));
        rec.rel(Rule.ALG_ISOLATE, new Relation(abs, Op.EQ, h), "x", x);
        if (h.isFreeOf(v) && Eval.value(h) < 0) {
            rec.rel(Rule.ALG_ABS_NEGATIVE, new Relation(abs, Op.EQ, h));
            return SolutionSet.empty();
        }
        return absoluteCases(r, abs.arg(), h, depth, false);
    }

    /** {@code |g| = h}: los casos {@code g = h} y {@code g = −h} (con {@code h} el otro valor absoluto, es {@code |g1| = |g2|}). */
    private SolutionSet absoluteCases(Relation r, Expr g, Expr h, int depth, boolean bothAbsolute) {
        Relation positive = new Relation(g, Op.EQ, h);
        Relation negative = new Relation(g, Op.EQ, Expr.neg(h));
        return rec.group(Rule.ALG_ABS_CASES, null, () -> {
            List<SolutionSet> parts = new ArrayList<>();
            for (Relation c : List.of(positive, negative)) {
                parts.add(rec.group(Rule.ALG_ZERO_FACTOR, c, () -> solveEquation(c, depth + 1)));
            }
            SolutionSet all = union(parts);
            if (all instanceof SolutionSet.Finite fin && !bothAbsolute) return check(r, fin, true);
            return all;
        }, "g", g, "h", h);
    }

    // ---------------------------------------------------------------- Exponenciales y logaritmos

    private SolutionSet logExp(Relation r, Expr f, int depth) {
        Optional<SolutionSet> viaLogs = singleLog(r, f, depth);
        if (viaLogs.isPresent()) return viaLogs.get();
        Optional<SolutionSet> viaExp = singleExponential(r, f, depth);
        if (viaExp.isPresent()) return viaExp.get();
        return numeric(f);
    }

    private Optional<SolutionSet> singleLog(Relation r, Expr f, int depth) {
        // Se juntan los logaritmos con coeficientes enteros: c₁ln(a) + c₂ln(b) = ln(a^c₁·b^c₂)
        List<Expr> others = new ArrayList<>();
        List<Expr> logs = new ArrayList<>();
        for (Expr term : Algebra.terms(f)) {
            Expr arg = null;
            Expr c = Expr.ONE;
            if (term instanceof Expr.Func fn && fn.fn() == Fn.LN) {
                arg = fn.arg();
            } else if (term instanceof Expr.Mul m) {
                for (Expr factor : m.factors()) {
                    if (factor instanceof Expr.Func fn && fn.fn() == Fn.LN && !fn.arg().isFreeOf(v) && arg == null) arg = fn.arg();
                }
                if (arg != null) c = Expand.expand(Expr.div(term, Expr.ln(arg)));
            }
            if (arg != null && !arg.isFreeOf(v) && c.isFreeOf(v) && c instanceof Expr.Num cn && cn.value().isInteger()) {
                logs.add(Expr.pow(arg, c));
            } else {
                others.add(term);
            }
        }
        if (logs.isEmpty()) return Optional.empty();
        for (Expr o : others) if (!o.isFreeOf(v)) return Optional.empty();

        Expr combined = Expr.ln(Expr.mul(logs));
        if (logs.size() > 1) rec.rel(Rule.ALG_SIMPLIFY, new Relation(Expr.add(combined, Expr.add(others)), Op.EQ, Expr.ZERO));
        Expr h = Expand.expand(Expr.neg(Expr.add(others)));
        Expr argument = ((Expr.Func) combined).arg();
        rec.rel(Rule.ALG_ISOLATE, new Relation(combined, Op.EQ, h), "x", x);
        rec.note(Rule.ALG_LOG_DOMAIN, "cond", Expr.sym(argument + " > 0"));
        Relation exponential = new Relation(argument, Op.EQ, Expr.exp(h));
        rec.rel(Rule.ALG_TO_EXPONENTIAL, exponential);

        SolutionSet candidates = solveEquation(exponential, depth + 1);
        if (!(candidates instanceof SolutionSet.Finite fin)) return Optional.of(candidates);
        return Optional.of(check(r, fin, true));
    }

    private Optional<SolutionSet> singleExponential(Relation r, Expr f, int depth) {
        // Todas las exponenciales con una misma base b: f es un polinomio en t = b^x
        List<Expr.Pow> exponentials = new ArrayList<>();
        contains(f, e -> {
            if (e instanceof Expr.Pow p && !p.exponent().isFreeOf(v) && p.base().isFreeOf(v)) exponentials.add(p);
            return false;
        });
        if (exponentials.isEmpty()) return Optional.empty();
        Expr base = exponentials.get(0).base();

        // f = c·A + resto, con A = base^g y una sola exponencial distinta
        Expr.Pow first = exponentials.get(0);
        boolean single = exponentials.stream().allMatch(p -> p.equals(first));
        if (single) {
            Expr coefficient = Expr.ZERO;
            List<Expr> rest = new ArrayList<>();
            for (Expr term : Algebra.terms(f)) {
                if (!Algebra.contains(term, first)) {
                    rest.add(term);
                    continue;
                }
                Expr c = Expand.expand(Expr.div(term, first));
                if (!c.isFreeOf(v) || Algebra.contains(c, first)) return Optional.empty();
                coefficient = Expr.add(coefficient, c);
            }
            if (coefficient.isZero()) return Optional.empty();
            Expr h = Expand.expand(Expr.div(Expr.neg(Expr.add(rest)), coefficient));
            // e^sin(x) = x^2: lo que queda a la derecha aun depende de x, no se puede tomar logaritmos de los dos lados
            if (!h.isFreeOf(v)) return Optional.empty();
            rec.rel(Rule.ALG_ISOLATE, new Relation(first, Op.EQ, h), "x", x);
            if (Eval.value(h) <= 0) {
                rec.rel(Rule.ALG_CONTRADICTION, new Relation(first, Op.EQ, h), "x", x);
                return Optional.of(SolutionSet.empty());
            }
            Expr exponent = first.exponent();
            Relation logged = new Relation(Expr.mul(exponent, Expr.ln(base)), Op.EQ, Expr.ln(h));
            rec.rel(Rule.ALG_TAKE_LOG, logged);
            Relation next = new Relation(exponent, Op.EQ, Expr.div(Expr.ln(h), Expr.ln(base)));
            return Optional.of(solveEquation(next, depth + 1));
        }

        // Varias exponenciales: se busca t = b^x con b entero y todas las demás potencias de b
        Optional<Expr> unit = commonBase(exponentials);
        if (unit.isEmpty()) return Optional.empty();
        String t = v.equals("t") ? "s" : "t";
        Expr tSym = Expr.sym(t);
        Expr b = unit.get();
        Expr substituted = f;
        for (Expr.Pow p : exponentials) {
            Optional<Expr> replacement = asPowerOf(p, b, tSym);
            if (replacement.isEmpty()) return Optional.empty();
            substituted = Algebra.replace(substituted, p, replacement.get());
        }
        if (!substituted.isFreeOf(v) || Polynomial.from(substituted, t).isEmpty()) return Optional.empty();

        Expr bx = Expr.pow(b, x);
        Expr shown = Expand.expand(substituted);
        rec.rel(Rule.ALG_SUBSTITUTION, zero(shown), "u", tSym, "e", bx);
        SolutionSet inner = new EquationSolver(rec, t, SolveMethod.AUTO).solve(zero(shown));
        if (!(inner instanceof SolutionSet.Finite fin)) return Optional.of(inner);

        List<SolutionSet> parts = new ArrayList<>();
        for (Expr t0 : fin.values()) {
            Relation back = new Relation(bx, Op.EQ, t0);
            rec.rel(Rule.ALG_BACK_SUBSTITUTION, back, "u", tSym, "e", bx, "x", x);
            if (Eval.value(t0) <= 0) {
                rec.rel(Rule.ALG_CONTRADICTION, back, "x", x);
                continue;
            }
            Relation logged = new Relation(Expr.mul(x, Expr.ln(b)), Op.EQ, Expr.ln(t0));
            rec.rel(Rule.ALG_TAKE_LOG, logged);
            parts.add(solveEquation(new Relation(x, Op.EQ, Expr.div(Expr.ln(t0), Expr.ln(b))), depth + 1));
        }
        return Optional.of(union(parts));
    }

    /** Una base entera pequeña de la que todas las bases son potencias enteras, o vacío. */
    private Optional<Expr> commonBase(List<Expr.Pow> exponentials) {
        for (int b = 2; b <= 12; b++) {
            boolean all = true;
            for (Expr.Pow p : exponentials) {
                if (!(p.base() instanceof Expr.Num n) || !n.value().isInteger() || !isPowerOf(n.value().numerator(), b)) {
                    all = false;
                    break;
                }
            }
            if (all) return Optional.of(Expr.num(b));
        }
        if (exponentials.stream().allMatch(p -> p.base().equals(Expr.E))) return Optional.of(Expr.E);
        return Optional.empty();
    }

    private static boolean isPowerOf(BigInteger value, int b) {
        if (value.signum() <= 0) return false;
        BigInteger bb = BigInteger.valueOf(b);
        while (value.compareTo(BigInteger.ONE) > 0) {
            if (!value.mod(bb).equals(BigInteger.ZERO)) return false;
            value = value.divide(bb);
        }
        return true;
    }

    /** {@code base^(k·x + c)} como {@code b^c' · t^k'} con {@code t = b^x}, o vacío si el exponente no es lineal con k entero. */
    private Optional<Expr> asPowerOf(Expr.Pow p, Expr b, Expr t) {
        Optional<Polynomial> exponent = Polynomial.from(p.exponent(), v);
        if (exponent.isEmpty() || exponent.get().degree() != 1) return Optional.empty();
        Rational k = exponent.get().coefficient(1);
        Rational c = exponent.get().coefficient(0);

        // log_b(base): base = b^m
        Rational m = Rational.ONE;
        if (!p.base().equals(b)) {
            if (!(p.base() instanceof Expr.Num n) || !(b instanceof Expr.Num bn)) return Optional.empty();
            int e = 0;
            BigInteger value = n.value().numerator();
            while (value.compareTo(BigInteger.ONE) > 0) {
                value = value.divide(bn.value().numerator());
                e++;
            }
            m = Rational.of(e);
        }
        Rational power = k.multiply(m);
        if (!power.isInteger() || power.signum() <= 0) return Optional.empty();
        Expr constant = Expr.pow(b, Expr.num(c.multiply(m)));
        return Optional.of(Expr.mul(constant, Expr.pow(t, Expr.num(power))));
    }

    // ---------------------------------------------------------------- Trigonometría

    private SolutionSet trig(Relation r, Expr f, int depth) {
        List<Expr.Func> atoms = new ArrayList<>();
        contains(f, e -> {
            if (e instanceof Expr.Func fn && !fn.arg().isFreeOf(v) && (fn.fn() == Fn.SIN || fn.fn() == Fn.COS || fn.fn() == Fn.TAN)
                    && !atoms.contains(fn)) atoms.add(fn);
            return false;
        });
        if (atoms.size() != 1) return numeric(f);
        Expr.Func atom = atoms.get(0);

        Optional<Polynomial> argument = Polynomial.from(atom.arg(), v);
        if (argument.isEmpty() || argument.get().degree() != 1) return numeric(f);
        Rational alpha = argument.get().coefficient(1);
        Rational beta = argument.get().coefficient(0);

        String t = v.equals("t") ? "s" : "t";
        Expr tSym = Expr.sym(t);
        Expr substituted = Algebra.replace(f, atom, tSym);
        if (!substituted.isFreeOf(v)) return numeric(f);
        Optional<Polynomial> inner = Polynomial.from(substituted, t);
        if (inner.isEmpty() || inner.get().degree() < 1) return numeric(f);

        List<Expr> values;
        if (inner.get().degree() == 1 && atom.equals(f)) {
            values = List.of(Expr.ZERO);
        } else {
            Expr shown = Expand.expand(substituted);
            if (inner.get().degree() >= 2) rec.rel(Rule.ALG_SUBSTITUTION, zero(shown), "u", tSym, "e", atom);
            else rec.rel(Rule.ALG_ISOLATE, new Relation(atom, Op.EQ, Expr.neg(Expr.div(Expr.num(inner.get().coefficient(0)),
                    Expr.num(inner.get().coefficient(1))))), "x", x);
            SolutionSet innerSet = new EquationSolver(rec, t, SolveMethod.AUTO).solve(zero(shown));
            if (!(innerSet instanceof SolutionSet.Finite fin)) return innerSet;
            values = fin.values();
        }
        if (inner.get().degree() == 1 && atom.equals(f)) {
            values = List.of(Expr.ZERO);
        }

        String parameter = v.equals("k") ? "n" : "k";
        Expr k = Expr.sym(parameter);
        List<Expr> families = new ArrayList<>();
        for (Expr value : values) {
            if (inner.get().degree() >= 2) {
                rec.rel(Rule.ALG_BACK_SUBSTITUTION, new Relation(atom, Op.EQ, value), "u", tSym, "e", atom, "x", x);
            }
            List<Expr> angles = new ArrayList<>();
            double c = Eval.value(value);
            switch (atom.fn()) {
                case SIN -> {
                    if (Math.abs(c) > 1 + 1e-12) {
                        rec.rel(Rule.ALG_TRIG_OUT_OF_RANGE, new Relation(atom, Op.EQ, value));
                        continue;
                    }
                    Expr a = Expr.func(Fn.ASIN, value);
                    angles.add(Expr.add(a, Expr.mul(Expr.num(2), Expr.PI, k)));
                    angles.add(Expr.add(Expr.sub(Expr.PI, a), Expr.mul(Expr.num(2), Expr.PI, k)));
                }
                case COS -> {
                    if (Math.abs(c) > 1 + 1e-12) {
                        rec.rel(Rule.ALG_TRIG_OUT_OF_RANGE, new Relation(atom, Op.EQ, value));
                        continue;
                    }
                    Expr a = Expr.func(Fn.ACOS, value);
                    angles.add(Expr.add(a, Expr.mul(Expr.num(2), Expr.PI, k)));
                    angles.add(Expr.add(Expr.neg(a), Expr.mul(Expr.num(2), Expr.PI, k)));
                }
                default -> angles.add(Expr.add(Expr.func(Fn.ATAN, value), Expr.mul(Expr.PI, k)));
            }
            for (Expr angle : angles) {
                Expr solution = Expr.div(Expr.sub(angle, Expr.num(beta)), Expr.num(alpha));
                if (!families.contains(solution)) families.add(solution);
            }
        }
        if (families.isEmpty()) return SolutionSet.empty();
        String list = families.stream().map(e -> v + " = " + e).collect(Collectors.joining(",  "));
        rec.note(Rule.ALG_TRIG_GENERAL, "list", Expr.sym(list));
        return new SolutionSet.Periodic(families, parameter);
    }

    // ---------------------------------------------------------------- Numérico

    /** Cuántas raíces se listan como mucho al buscar numéricamente; con más, la ecuación se deja sin resolver. */
    static final int MAX_NUMERIC_ROOTS = 50;
    private static final int NUMERIC_SAMPLES = 20_000;

    /**
     * Se lanza cuando {@code f} cambia de signo más de {@link #MAX_NUMERIC_ROOTS} veces en la ventana de búsqueda
     * (por ejemplo {@code cos(x^2)}): listar esas soluciones una a una no sirve de nada y colgaría a quien las pinte.
     */
    static final class TooManyRootsException extends ArithmeticException {
        /** Algunas de las raíces más cercanas al origen, de menor a mayor, para dar una idea de cómo son. */
        private final List<Double> samples;

        TooManyRootsException(List<Double> samples) {
            super("Demasiadas soluciones para listarlas");
            this.samples = List.copyOf(samples);
        }

        List<Double> samples() {
            return samples;
        }
    }

    /**
     * Busca raíces reales de {@code f} entre −50 y 50: cambios de signo afinados por bisección. Si hay más de
     * {@link #MAX_NUMERIC_ROOTS}, deja de buscar y lanza {@link TooManyRootsException}.
     */
    private SolutionSet numeric(Expr f) {
        approximate = true;
        List<Double> roots = scan(f, -50, 50, NUMERIC_SAMPLES, MAX_NUMERIC_ROOTS);
        if (roots.size() > MAX_NUMERIC_ROOTS) {
            // Demasiadas para listarlas: se guardan las más cercanas al origen para dar una idea
            List<Double> near = scan(f, -10, 10, NUMERIC_SAMPLES / 2, 12);
            throw new TooManyRootsException(near.subList(0, Math.min(near.size(), 12)));
        }
        List<Expr> values = new ArrayList<>();
        for (double root : roots) {
            double rounded = Math.rint(root * 1e8) / 1e8;
            Expr value = Expr.num(Rational.fromDouble(rounded));
            values.add(value);
            rec.note(Rule.ALG_NUMERIC_ROOT, "x", x, "x0", Expr.sym(SolutionSet.decimal(root)), "v", Expr.sym(SolutionSet.decimal(rounded)));
        }
        return values.isEmpty() ? SolutionSet.empty() : SolutionSet.approximate(values);
    }

    /** Las raíces de {@code f} entre {@code lo} y {@code hi}; si hay más de {@code limit}, devuelve una más y deja de buscar. */
    private List<Double> scan(Expr f, double lo, double hi, int n, int limit) {
        List<Double> roots = new ArrayList<>();
        double step = (hi - lo) / n;
        double previous = value(f, lo);
        for (int i = 1; i <= n; i++) {
            if (i % 1000 == 0) rec.trace().checkBudget();
            double current = lo + i * step;
            double fc = value(f, current);
            double before = current - step;
            if (!Double.isNaN(previous) && !Double.isNaN(fc) && (previous == 0 || previous * fc < 0)) {
                double root = previous == 0 ? before : bisect(f, before, current);
                if (!Double.isNaN(root) && roots.stream().noneMatch(r -> Math.abs(r - root) < 1e-7)) {
                    roots.add(root);
                    if (roots.size() > limit) return roots;
                }
            }
            previous = fc;
        }
        return roots;
    }

    private double value(Expr f, double at) {
        Map<String, Double> env = new HashMap<>();
        env.put(v, at);
        try {
            double value = Eval.value(f, env);
            return Double.isInfinite(value) ? Double.NaN : value;
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private double bisect(Expr f, double a, double b) {
        double fa = value(f, a);
        for (int i = 0; i < 80; i++) {
            double m = (a + b) / 2;
            double fm = value(f, m);
            if (Double.isNaN(fm)) return Double.NaN;
            if (fa * fm <= 0) b = m;
            else {
                a = m;
                fa = fm;
            }
        }
        double root = (a + b) / 2;
        // Un salto de signo en una asíntota no es una raíz
        return Math.abs(value(f, root)) < 1e-6 ? root : Double.NaN;
    }
}
