package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Algebra;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Op;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.expr.Relation;
import barnesinside.mathutils.cas.expr.SolutionSet;
import barnesinside.mathutils.cas.steps.Rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Ecuaciones de primer y segundo grado cuyos coeficientes llevan otras letras ({@code a*x + b = 0}, {@code x + y = 5},
 * {@code x^2 + b*x + c = 0}): se despeja la incógnita dejando las demás letras como números fijos, y el resultado es una
 * expresión en ellas. Cuando hace falta una condición ({@code a ≠ 0}, discriminante no negativo) se dice en un paso.
 */
final class Parametric {

    private Parametric() {
    }

    /** Los coeficientes de la expresión como polinomio en la variable, de grado 1 o 2, o vacío si no lo es. */
    static Optional<Map<Integer, Expr>> coefficients(Expr difference, String v) {
        Optional<Map<Integer, Expr>> found = collect(Expand.expand(difference), v);
        if (found.isEmpty()) return found;
        Map<Integer, Expr> map = new HashMap<>();
        found.get().forEach((degree, coefficient) -> {
            Expr tidy = Expand.expand(coefficient);
            if (!isZero(tidy)) map.put(degree, tidy);
        });
        int degree = map.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        return degree >= 1 && degree <= 2 ? Optional.of(map) : Optional.empty();
    }

    /** Si la ecuación se puede resolver con letras como parámetros. */
    static boolean applies(Relation relation, String v) {
        return relation.op() == Op.EQ && relation.variables().contains(v) && coefficients(relation.difference(), v).isPresent();
    }

    private static boolean isZero(Expr e) {
        return e instanceof Expr.Num n && n.value().isZero();
    }

    private static boolean isNumber(Expr e) {
        return e instanceof Expr.Num;
    }

    private static Optional<Map<Integer, Expr>> collect(Expr e, String v) {
        if (e.isFreeOf(v)) return Optional.of(Map.of(0, e));
        switch (e) {
            case Expr.Sym s -> {
                return Optional.of(Map.of(1, Expr.ONE));
            }
            case Expr.Add add -> {
                Map<Integer, List<Expr>> parts = new HashMap<>();
                for (Expr term : add.terms()) {
                    Optional<Map<Integer, Expr>> c = collect(term, v);
                    if (c.isEmpty()) return Optional.empty();
                    c.get().forEach((d, coefficient) -> parts.computeIfAbsent(d, k -> new ArrayList<>()).add(coefficient));
                }
                Map<Integer, Expr> sum = new HashMap<>();
                parts.forEach((d, list) -> sum.put(d, Expr.add(list)));
                return Optional.of(sum);
            }
            case Expr.Mul mul -> {
                Map<Integer, Expr> product = Map.of(0, Expr.ONE);
                for (Expr factor : mul.factors()) {
                    Optional<Map<Integer, Expr>> c = collect(factor, v);
                    if (c.isEmpty()) return Optional.empty();
                    product = multiply(product, c.get());
                    if (product.keySet().stream().anyMatch(d -> d > 2)) return Optional.empty();
                }
                return Optional.of(product);
            }
            case Expr.Pow pow -> {
                if (pow.exponent() instanceof Expr.Num n && n.value().isInteger() && n.value().signum() > 0 && n.value().numerator().bitLength() < 4) {
                    Optional<Map<Integer, Expr>> base = collect(pow.base(), v);
                    if (base.isEmpty()) return Optional.empty();
                    Map<Integer, Expr> result = Map.of(0, Expr.ONE);
                    for (int i = 0; i < n.value().numerator().intValue(); i++) {
                        result = multiply(result, base.get());
                        if (result.keySet().stream().anyMatch(d -> d > 2)) return Optional.empty();
                    }
                    return Optional.of(result);
                }
                return Optional.empty();
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    private static Map<Integer, Expr> multiply(Map<Integer, Expr> a, Map<Integer, Expr> b) {
        Map<Integer, List<Expr>> parts = new HashMap<>();
        a.forEach((da, ca) -> b.forEach((db, cb) -> parts.computeIfAbsent(da + db, k -> new ArrayList<>()).add(Expr.mul(ca, cb))));
        Map<Integer, Expr> result = new HashMap<>();
        parts.forEach((d, list) -> result.put(d, Expr.add(list)));
        return result;
    }

    /** Resuelve la ecuación (que debe cumplir {@link #applies}) registrando los pasos. */
    static SolutionSet solve(Rec rec, Relation relation, String v) {
        Expr x = Expr.sym(v);
        Map<Integer, Expr> c = coefficients(relation.difference(), v).orElseThrow();
        Expr a = c.getOrDefault(c.containsKey(2) ? 2 : 1, Expr.ZERO);
        List<String> others = new ArrayList<>(new TreeSet<>(relation.variables()));
        others.remove(v);
        rec.note(Rule.ALG_PARAMETERS, "x", x, "p", Expr.sym(String.join(", ", others)));

        if (!c.containsKey(2)) {
            Expr b = c.getOrDefault(0, Expr.ZERO);
            Expr left = Expr.add(Expr.mul(a, x), b);
            rec.rel(Rule.ALG_SIMPLIFY, new Relation(left, Op.EQ, Expr.ZERO));
            if (!isNumber(a)) rec.note(Rule.ALG_PARAM_NONZERO, "a", a);
            Expr root = Algebra.together(Expand.expand(Expr.div(Expr.neg(b), a)));
            rec.rel(Rule.ALG_ISOLATE, new Relation(x, Op.EQ, root), "x", x);
            return SolutionSet.finite(List.of(root));
        }

        Expr b = c.getOrDefault(1, Expr.ZERO);
        Expr k = c.getOrDefault(0, Expr.ZERO);
        Expr left = Expr.add(Expr.mul(a, Expr.pow(x, 2)), Expr.mul(b, x), k);
        rec.rel(Rule.ALG_SIMPLIFY, new Relation(left, Op.EQ, Expr.ZERO));
        if (!isNumber(a)) rec.note(Rule.ALG_PARAM_NONZERO, "a", a);
        rec.note(Rule.ALG_QUADRATIC_FORM, "x", x, "a", a, "b", b, "c", k);

        if (isZero(b)) {
            // a·x² + c = 0, sin término en x: x = ±√(−c/a)
            Expr inside = Expand.expand(Expr.div(Expr.neg(k), a));
            if (isNumber(inside) && ((Expr.Num) inside).value().signum() < 0) {
                rec.note(Rule.ALG_DISCRIMINANT_NEGATIVE, "d", inside);
                return SolutionSet.empty();
            }
            if (!isNumber(inside)) rec.note(Rule.ALG_PARAM_REAL, "d", inside);
            Expr root = Expr.sqrt(inside);
            if (isZero(inside)) return SolutionSet.finite(List.of(Expr.ZERO));
            rec.rel(Rule.ALG_ISOLATE, new Relation(x, Op.EQ, root), "x", x);
            rec.rel(Rule.ALG_OTHER_SOLUTION, new Relation(x, Op.EQ, Expr.neg(root)), "x", x);
            return SolutionSet.finite(List.of(root, Expr.neg(root)));
        }

        Expr d = Expand.expand(Expr.sub(Expr.pow(b, 2), Expr.mul(Expr.num(4), a, k)));
        rec.note(Rule.ALG_DISCRIMINANT, "d", d);
        if (isNumber(d)) {
            Rational value = ((Expr.Num) d).value();
            if (value.signum() < 0) {
                rec.note(Rule.ALG_DISCRIMINANT_NEGATIVE, "d", d);
                return SolutionSet.empty();
            }
            rec.note(value.isZero() ? Rule.ALG_DISCRIMINANT_ZERO : Rule.ALG_DISCRIMINANT_POSITIVE, "d", d);
        } else {
            rec.note(Rule.ALG_PARAM_REAL, "d", d);
        }
        rec.note(Rule.ALG_QUADRATIC_FORMULA, "a", a, "b", b, "c", k);

        Expr root = Expr.sqrt(d);
        Expr twoA = Expr.mul(Expr.num(2), a);
        Expr r1 = Expr.div(Expr.add(Expr.neg(b), root), twoA);
        Expr r2 = Expr.div(Expr.sub(Expr.neg(b), root), twoA);
        if (isNumber(d) && ((Expr.Num) d).value().isZero()) {
            Expr only = Expand.expand(Expr.div(Expr.neg(b), twoA));
            rec.rel(Rule.ALG_ISOLATE, new Relation(x, Op.EQ, only), "x", x);
            return SolutionSet.finite(List.of(only));
        }
        rec.rel(Rule.ALG_ISOLATE, new Relation(x, Op.EQ, r1), "x", x);
        rec.rel(Rule.ALG_OTHER_SOLUTION, new Relation(x, Op.EQ, r2), "x", x);
        return SolutionSet.finite(List.of(r1, r2));
    }
}
