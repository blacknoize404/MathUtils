package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.diff.Derivatives;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Fn;
import barnesinside.mathutils.cas.limit.Limits;
import barnesinside.mathutils.cas.poly.Polynomial;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Los pasos de una ecuación que no se puede despejar con álgebra: por qué no se puede, el dominio, la función cuyos ceros se buscan, si
 * es creciente, sus límites en los extremos, cuántas raíces tiene (Bolzano y monotonía), y entre qué valores está cada una antes de
 * afinarla. Es lo que se escribe a mano para justificar una aproximación numérica.
 */
final class NumericAnalysis {

    private final Rec rec;
    private final String v;
    private final Expr x;
    private final Expr f;

    NumericAnalysis(Rec rec, String v, Expr f) {
        this.rec = rec;
        this.v = v;
        this.x = Expr.sym(v);
        this.f = f;
    }

    /** Los pasos que explican por qué hay que aproximar y qué se sabe de las raíces (antes de buscarlas). */
    void explain() {
        try {
            why();
            double[] domain = domain();
            function();
            if (domain == null) return;
            monotonicity(domain);
        } catch (RuntimeException e) {
            // El análisis es un añadido: si algo no se sabe estudiar, queda la búsqueda numérica sola
        }
    }

    /** Entre qué valores cercanos cambia de signo cada raíz, y la comprobación de que {@code f} casi se anula en ella. */
    void bracket(double root, double rounded) {
        try {
            double h = Math.abs(root) < 10 ? 0.02 : 1;
            double a = Math.floor(root / h) * h;
            double b = a + h;
            double fa = value(a);
            double fb = value(b);
            if (Double.isNaN(fa) || Double.isNaN(fb) || fa * fb > 0) return;
            rec.note(Rule.ALG_NUM_BRACKET, "x", x, "a", num(a), "b", num(b), "fa", num(fa), "fb", num(fb));
        } catch (RuntimeException e) {
            // Sin tabla de valores
        }
    }

    void check(double rounded) {
        double value = value(rounded);
        if (!Double.isNaN(value)) rec.note(Rule.ALG_NUM_CHECK, "x", x, "v", num(rounded), "fv", num(value));
    }

    // ================================================================ Pasos

    private void why() {
        Set<String> kinds = new TreeSet<>();
        collect(f, kinds);
        java.util.Optional<Polynomial> polynomial = Polynomial.from(f, v);
        if (polynomial.isPresent() && polynomial.get().degree() >= 5) {
            rec.note(Rule.ALG_NUM_WHY_POLY, "x", x, "n", Expr.num(polynomial.get().degree()));
        } else if (kinds.size() >= 2 || (kinds.size() == 1 && !kinds.contains("polinomio"))) {
            rec.note(Rule.ALG_NUM_WHY_MIXED, "x", x, "kinds", Expr.sym(String.join(", ", kinds)));
        } else {
            rec.note(Rule.ALG_NUM_WHY_GENERIC, "x", x);
        }
    }

    /** Qué clases de términos hay: polinomio, logaritmo, exponencial, trigonométrico, raíz o fracción. */
    private void collect(Expr e, Set<String> kinds) {
        if (e.isFreeOf(v)) return;
        switch (e) {
            case Expr.Func fn -> {
                kinds.add(switch (fn.fn()) {
                    case LN -> "logaritmo";
                    case SIN, COS, TAN, COT, SEC, CSC -> "trigonométrico";
                    case ASIN, ACOS, ATAN -> "trigonométrico inverso";
                    case ABS -> "valor absoluto";
                    default -> "función especial";
                });
                collect(fn.arg(), kinds);
            }
            case Expr.Pow p -> {
                if (!p.exponent().isFreeOf(v)) kinds.add("exponencial");
                else if (p.exponent() instanceof Expr.Num n && !n.value().isInteger()) kinds.add("raíz");
                else if (p.exponent() instanceof Expr.Num n && n.value().signum() < 0) kinds.add("fracción");
                else kinds.add("polinomio");
                collect(p.base(), kinds);
                collect(p.exponent(), kinds);
            }
            case Expr.Add a -> a.terms().forEach(t -> collect(t, kinds));
            case Expr.Mul m -> m.factors().forEach(t -> collect(t, kinds));
            case Expr.Sym s -> kinds.add("polinomio");
            default -> { }
        }
    }

    /**
     * El dominio como (inferior, superior) cuando son condiciones de primer grado de los logaritmos y raíces; {@code null} si no se
     * sabe describir. Se dice en un paso.
     */
    private double[] domain() {
        List<Expr> arguments = new ArrayList<>();
        collectLogArguments(f, arguments);
        double lo = Double.NEGATIVE_INFINITY;
        double hi = Double.POSITIVE_INFINITY;
        List<String> conditions = new ArrayList<>();
        for (Expr argument : arguments) {
            java.util.Optional<Polynomial> p = Polynomial.from(argument, v);
            if (p.isEmpty() || p.get().degree() != 1) return null;
            double c0 = p.get().coefficient(0).doubleValue();
            double c1 = p.get().coefficient(1).doubleValue();
            double bound = -c0 / c1;
            conditions.add(argument + " > 0");
            if (c1 > 0) lo = Math.max(lo, bound);
            else hi = Math.min(hi, bound);
        }
        if (!conditions.isEmpty()) {
            String result = (Double.isInfinite(lo) ? "" : v + " > " + text(lo)) + (!Double.isInfinite(lo) && !Double.isInfinite(hi) ? " y " : "")
                    + (Double.isInfinite(hi) ? "" : v + " < " + text(hi));
            rec.note(Rule.ALG_NUM_DOMAIN, "x", x, "conditions", Expr.sym(String.join(",  ", conditions)), "domain", Expr.sym(result));
        }
        return new double[]{lo, hi};
    }

    private void collectLogArguments(Expr e, List<Expr> out) {
        switch (e) {
            case Expr.Func fn -> {
                if (fn.fn() == Fn.LN && !fn.arg().isFreeOf(v) && !out.contains(fn.arg())) out.add(fn.arg());
                collectLogArguments(fn.arg(), out);
            }
            case Expr.Add a -> a.terms().forEach(t -> collectLogArguments(t, out));
            case Expr.Mul m -> m.factors().forEach(t -> collectLogArguments(t, out));
            case Expr.Pow p -> {
                collectLogArguments(p.base(), out);
                collectLogArguments(p.exponent(), out);
            }
            default -> { }
        }
    }

    private void function() {
        rec.note(Rule.ALG_NUM_FUNCTION, "x", x, "f", f);
    }

    private void monotonicity(double[] domain) {
        Solution derivative = Derivatives.derivative(f, v);
        if (!derivative.isSolved() || derivative.result() == null) return;
        Expr d = derivative.result();
        double[] grid = grid(domain);
        int sign = 0;
        for (double p : grid) {
            double value = valueOf(d, p);
            if (Double.isNaN(value)) continue;
            int s = value > 0 ? 1 : value < 0 ? -1 : 0;
            if (s == 0 || (sign != 0 && s != sign)) {
                sign = 0;
                break;
            }
            sign = s;
        }
        if (sign == 0) return;

        List<String> parts = new ArrayList<>();
        List<Expr> terms = d instanceof Expr.Add a ? a.terms() : List.of(d);
        boolean eachTerm = terms.size() > 1;
        for (Expr term : terms) {
            int ts = 0;
            boolean consistent = true;
            for (double p : grid) {
                double value = valueOf(term, p);
                if (Double.isNaN(value)) continue;
                int s = value > 0 ? 1 : value < 0 ? -1 : 0;
                if (s == 0 || (ts != 0 && s != ts)) consistent = false;
                ts = s;
            }
            if (!consistent || ts != sign) eachTerm = false;
            parts.add(term + (ts > 0 ? " > 0" : ts < 0 ? " < 0" : ""));
        }
        Rule rule = sign > 0 ? (eachTerm ? Rule.ALG_NUM_MONOTONE_TERMS : Rule.ALG_NUM_MONOTONE)
                : (eachTerm ? Rule.ALG_NUM_MONOTONE_TERMS_DOWN : Rule.ALG_NUM_MONOTONE_DOWN);
        rec.note(rule, "x", x, "f", f, "d", d, "parts", Expr.sym(String.join(",  ", parts)));

        limits(domain, sign);
    }

    private void limits(double[] domain, int sign) {
        Expr left;
        Expr right;
        String leftPoint;
        String rightPoint;
        Solution l;
        Solution r;
        if (Double.isInfinite(domain[0])) {
            l = Limits.limit(f, v, Expr.neg(Expr.INFINITY), Limits.Direction.BOTH);
            leftPoint = "-oo";
        } else {
            l = Limits.limit(f, v, Expr.num(barnesinside.mathutils.cas.expr.Rational.fromDouble(domain[0])), Limits.Direction.RIGHT);
            leftPoint = text(domain[0]) + "+";
        }
        if (Double.isInfinite(domain[1])) {
            r = Limits.limit(f, v, Expr.INFINITY, Limits.Direction.BOTH);
            rightPoint = "oo";
        } else {
            r = Limits.limit(f, v, Expr.num(barnesinside.mathutils.cas.expr.Rational.fromDouble(domain[1])), Limits.Direction.LEFT);
            rightPoint = text(domain[1]) + "-";
        }
        if (!l.isSolved() || !r.isSolved()) return;
        left = l.result();
        right = r.result();
        rec.note(Rule.ALG_NUM_LIMITS, "x", x, "f", f, "left", left, "right", right,
                "leftPoint", Expr.sym(leftPoint), "rightPoint", Expr.sym(rightPoint));

        double lv = Eval.value(left);
        double rv = Eval.value(right);
        if (lv * rv < 0 || (lv < 0 && rv > 0) || (lv > 0 && rv < 0)) {
            rec.note(Rule.ALG_NUM_EXISTENCE_ONE, "x", x, "f", f);
        } else {
            rec.note(Rule.ALG_NUM_EXISTENCE_NONE, "x", x, "f", f);
        }
    }

    // ================================================================ Utilidades

    private double[] grid(double[] domain) {
        List<Double> points = new ArrayList<>();
        double[] offsets = {1e-6, 1e-4, 1e-3, 0.01, 0.1, 0.3, 0.7, 1, 2, 5, 10, 100, 1000, 1e5};
        boolean bounded = !Double.isInfinite(domain[0]) && !Double.isInfinite(domain[1]);
        if (bounded) {
            for (double t = 0.001; t < 1; t += 0.003) points.add(domain[0] + t * (domain[1] - domain[0]));
        } else {
            for (double o : offsets) {
                if (!Double.isInfinite(domain[0])) points.add(domain[0] + o);
                if (!Double.isInfinite(domain[1])) points.add(domain[1] - o);
                if (Double.isInfinite(domain[0]) && Double.isInfinite(domain[1])) {
                    points.add(o);
                    points.add(-o);
                }
            }
        }
        return points.stream().filter(p -> p > domain[0] && p < domain[1]).mapToDouble(Double::doubleValue).toArray();
    }

    private double value(double at) {
        return valueOf(f, at);
    }

    private double valueOf(Expr e, double at) {
        Map<String, Double> env = new HashMap<>();
        env.put(v, at);
        try {
            double value = Eval.value(e, env);
            return Double.isInfinite(value) ? Double.NaN : value;
        } catch (RuntimeException ex) {
            return Double.NaN;
        }
    }

    private static Expr num(double value) {
        return Expr.sym(text(value));
    }

    private static String text(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e9) return String.valueOf((long) value);
        String s = String.format(Locale.ROOT, "%.5f", value).replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }
}
