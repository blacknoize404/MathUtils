package MathUtils.cas.integral;

import MathUtils.cas.expr.Eval;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.Fn;
import MathUtils.cas.expr.Rational;
import MathUtils.cas.poly.Polynomial;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Puntos donde el integrando puede no estar definido o no estar acotado: ceros de denominadores,
 * argumentos de logaritmos y polos de tangente, secante, cotangente y cosecante. Solo declara
 * "completo" lo que puede garantizar; si no, la integral definida no se resuelve.
 */
final class Singularities {

    /** Un punto exacto con su valor numérico. */
    record Point(Expr exact, double value) {
    }

    /** Puntos dentro del intervalo y si la búsqueda es fiable. */
    record Found(List<Point> points, boolean complete) {
    }

    private static final double TOLERANCE = 1e-12;

    private final String v;
    private final double lo;
    private final double hi;
    private final List<Point> points = new ArrayList<>();
    private boolean complete = true;

    private Singularities(String v, double lo, double hi) {
        this.v = v;
        this.lo = lo;
        this.hi = hi;
    }

    static Found find(Expr f, String v, double lo, double hi) {
        Singularities s = new Singularities(v, lo, hi);
        s.visit(f, new LinkedHashSet<>());
        List<Point> unique = new ArrayList<>();
        s.points.sort(Comparator.comparingDouble(Point::value));
        for (Point p : s.points) {
            if (unique.isEmpty() || Math.abs(unique.get(unique.size() - 1).value() - p.value()) > TOLERANCE) unique.add(p);
        }
        return new Found(unique, s.complete);
    }

    private void visit(Expr e, Set<Expr> seen) {
        if (e.isFreeOf(v) || !seen.add(e)) return;
        switch (e) {
            case Expr.Add a -> a.terms().forEach(t -> visit(t, seen));
            case Expr.Mul m -> m.factors().forEach(t -> visit(t, seen));
            case Expr.Pow p -> {
                if (p.exponent() instanceof Expr.Num n && n.value().signum() < 0) zerosOf(p.base());
                visit(p.base(), seen);
                visit(p.exponent(), seen);
            }
            case Expr.Func fn -> {
                switch (fn.fn()) {
                    case LN -> zerosOf(fn.arg());
                    case TAN, SEC -> trigonometricPoles(fn.arg(), Math.PI / 2, Expr.div(Expr.PI, Expr.num(2)));
                    case COT, CSC -> trigonometricPoles(fn.arg(), 0, Expr.ZERO);
                    default -> { }
                }
                visit(fn.arg(), seen);
            }
            default -> { }
        }
    }

    /** Ceros reales de {@code base} en el intervalo. */
    private void zerosOf(Expr base) {
        if (base.isFreeOf(v)) return;
        if (base instanceof Expr.Func fn && fn.fn() == Fn.LN) {
            // 1/ln(u) no está definido cuando u = 1
            polynomialZeros(Expr.sub(fn.arg(), Expr.ONE), base);
            return;
        }
        if (base instanceof Expr.Func fn && (fn.fn() == Fn.SIN || fn.fn() == Fn.COS)) {
            trigonometricPoles(fn.arg(), fn.fn() == Fn.COS ? Math.PI / 2 : 0,
                    fn.fn() == Fn.COS ? Expr.div(Expr.PI, Expr.num(2)) : Expr.ZERO);
            return;
        }
        polynomialZeros(base, base);
    }

    private void polynomialZeros(Expr expression, Expr original) {
        Optional<Polynomial> p = Polynomial.from(expression, v);
        if (p.isPresent() && p.get().degree() >= 1) {
            Optional<Rationals.Factorization> factored = Rationals.factor(p.get());
            if (factored.isPresent()) {
                for (Rationals.Factor factor : factored.get().factors()) addRoots(factor.base());
                return;
            }
        } else if (p.isPresent()) {
            return;
        }
        if (!hasNoZero(original)) complete = false;
    }

    private void addRoots(Polynomial factor) {
        if (factor.degree() == 1) {
            add(Expr.num(factor.coefficient(0).negate().divide(factor.coefficient(1))));
        } else if (factor.degree() == 2) {
            Rational a = factor.coefficient(2);
            Rational b = factor.coefficient(1);
            Rational disc = b.multiply(b).subtract(Rational.of(4).multiply(a).multiply(factor.coefficient(0)));
            if (disc.signum() > 0) {
                Expr root = Expr.sqrt(Expr.num(disc));
                Expr twoA = Expr.num(Rational.of(2).multiply(a));
                add(Expr.div(Expr.add(Expr.num(b.negate()), root), twoA));
                add(Expr.div(Expr.sub(Expr.num(b.negate()), root), twoA));
            }
        }
    }

    /** Polos {@code (offset + kπ − β)/α} de una función de {@code αx + β}. */
    private void trigonometricPoles(Expr arg, double offsetValue, Expr offset) {
        Optional<Polynomial> p = Polynomial.from(arg, v);
        if (p.isEmpty() || p.get().degree() != 1 || Double.isInfinite(lo) || Double.isInfinite(hi)) {
            complete = false;
            return;
        }
        double alpha = p.get().coefficient(1).doubleValue();
        double beta = p.get().coefficient(0).doubleValue();
        double x1 = (offsetValue - beta) / alpha;
        double x2 = (offsetValue + Math.PI - beta) / alpha;
        double period = Math.abs(x2 - x1);
        int from = (int) Math.floor((Math.min(lo, hi) - x1) / period) - 1;
        int to = (int) Math.ceil((Math.max(lo, hi) - x1) / period) + 1;
        if (to - from > 10_000) {
            complete = false;
            return;
        }
        for (int k = from; k <= to; k++) {
            Expr exact = Expr.div(Expr.sub(Expr.add(offset, Expr.mul(Expr.num(k), Expr.PI)), Expr.num(p.get().coefficient(0))),
                    Expr.num(p.get().coefficient(1)));
            add(exact);
        }
    }

    private void add(Expr exact) {
        double value = Eval.value(exact);
        if (Double.isFinite(value) && value >= lo - TOLERANCE && value <= hi + TOLERANCE) points.add(new Point(exact, value));
    }

    /** Muestrea la expresión en el intervalo: sin cambios de signo ni valores casi nulos, no tiene ceros. */
    private boolean hasNoZero(Expr e) {
        double a = Double.isInfinite(lo) ? -1e3 : lo;
        double b = Double.isInfinite(hi) ? 1e3 : hi;
        int n = 4000;
        int sign = 0;
        for (int i = 0; i <= n; i++) {
            double x = a + (b - a) * i / n;
            double y;
            try {
                y = Eval.value(e, Map.of(v, x));
            } catch (RuntimeException ex) {
                return false;
            }
            if (Double.isNaN(y)) continue;
            if (Math.abs(y) < 1e-6) return false;
            int s = y > 0 ? 1 : -1;
            if (sign != 0 && s != sign) return false;
            sign = s;
        }
        return true;
    }
}
