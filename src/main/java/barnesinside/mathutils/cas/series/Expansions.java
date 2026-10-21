package barnesinside.mathutils.cas.series;

import barnesinside.mathutils.cas.diff.Derivatives;
import barnesinside.mathutils.cas.expr.Algebra;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.integral.Integrals;
import barnesinside.mathutils.cas.limit.Limits;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import barnesinside.mathutils.cas.steps.Trace;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Desarrollos en serie con sus pasos: Taylor y Laurent alrededor de un punto, y Fourier en un intervalo. Los coeficientes salen de
 * derivadas y límites (Taylor y Laurent) o de integrales definidas (Fourier), siempre exactos; si una integral no tiene forma exacta, el
 * coeficiente es un decimal y el resultado lo dice ({@code approximate}).
 * <pre>{@code
 * Expansions.Expansion e = Expansions.taylor("exp(x)", "x", "0", 4);
 * e.solution().result();     // 1 + x + x^2/2 + x^3/6 + x^4/24
 * e.partial(3);              // 1 + x + x^2/2
 * }</pre>
 *
 * @since 0.10
 */
public final class Expansions {

    /** El mayor grado de Taylor o Laurent, y el mayor número de armónicos de Fourier, que se calculan. */
    public static final int MAX_TERMS = 24;
    /** El mayor orden de polo que se busca en Laurent. */
    public static final int MAX_POLE = 6;

    private Expansions() {
    }

    /**
     * Un desarrollo: la solución con sus pasos y los sumandos por separado, para pintar sus sumas parciales.
     *
     * @param solution el desarrollo (el resultado es la suma de los términos)
     * @param terms    los sumandos, de menor a mayor potencia (en Fourier, cada armónico es un sumando)
     */
    public record Expansion(Solution solution, List<Expr> terms) {

        /** La suma de los primeros {@code count} sumandos. */
        public Expr partial(int count) {
            return Expr.add(terms.subList(0, Math.max(0, Math.min(count, terms.size()))));
        }
    }

    // ================================================================ Taylor

    public static Expansion taylor(String f, String variable, String point, int degree) {
        ExprParser.Parsed parsed = ExprParser.parseDetailed(f);
        Expansion e = taylor(parsed.expr(), variable, ExprParser.parse(point), degree);
        return new Expansion(e.solution().withPrelude(parsed.steps()), e.terms());
    }

    /**
     * El desarrollo de Taylor de {@code f} alrededor de {@code point}, hasta el grado {@code degree}. Los coeficientes son
     * {@code f^(k)(a)/k!}; si en el punto la función tiene una singularidad evitable ({@code sin(x)/x} en 0) y las derivadas ya no se
     * resuelven, se desarrollan numerador y denominador y se dividen las series.
     */
    public static Expansion taylor(Expr f, String variable, Expr point, int degree) {
        check(degree);
        checkPoint(point);
        Expr x = Expr.sym(variable);
        List<Step> steps = new ArrayList<>();
        steps.add(info(Rule.TAYLOR_FORMULA, f, null, "f", f, "x", x, "a", point, "N", Expr.num(degree)));
        Coefficients direct = new Coefficients(f, variable, point, steps);
        List<Expr> coefficients = new ArrayList<>();
        for (int k = 0; k <= degree; k++) {
            Expr c = direct.get(k);
            if (c == null) break;
            coefficients.add(c);
        }
        if (coefficients.size() == degree + 1) {
            return finishPower(Solution.Kind.TAYLOR, f, variable, point, steps, coefficients, 0, degree, Rule.TAYLOR_RESULT);
        }
        // Las derivadas sucesivas no se resuelven: se prueba con el cociente de dos series regulares
        List<Step> quotient = new ArrayList<>();
        quotient.add(info(Rule.TAYLOR_FORMULA, f, null, "f", f, "x", x, "a", point, "N", Expr.num(degree)));
        Quotient q = quotient(f, variable, point, degree, quotient);
        if (q == null || q.first < 0) return unsolved(Solution.Kind.TAYLOR, f, variable, point, degree, q == null ? steps : quotient);
        return finishPower(Solution.Kind.TAYLOR, f, variable, point, quotient, q.coefficients, q.first, degree, Rule.TAYLOR_RESULT);
    }

    /** Los coeficientes {@code f^(k)(a)/k!}, uno a uno y guardados, con un paso por cada derivada y por cada coeficiente. */
    private static final class Coefficients {
        private final Expr variable;
        private final String name;
        private final Expr point;
        private final List<Step> steps;
        private final List<Expr> values = new ArrayList<>();
        private Expr current;
        private BigInteger factorial = BigInteger.ONE;
        private boolean failed;

        Coefficients(Expr f, String name, Expr point, List<Step> steps) {
            this.current = f;
            this.variable = Expr.sym(name);
            this.name = name;
            this.point = point;
            this.steps = steps;
        }

        /** El coeficiente de {@code (x − a)^k}, o {@code null} si el motor no llega a él. */
        Expr get(int k) {
            while (values.size() <= k && !failed) {
                int next = values.size();
                if (next > 0) {
                    Solution d;
                    try {
                        d = Derivatives.derivative(current, name);
                    } catch (RuntimeException e) {
                        failed = true;
                        break;
                    }
                    if (!d.isSolved()) {
                        failed = true;
                        break;
                    }
                    steps.add(new Step(Rule.TAYLOR_DERIVATIVE, current, d.result(), Trace.params("f", current, "x", variable, "k", Expr.num(next)), d.steps()));
                    current = d.result();
                    factorial = factorial.multiply(BigInteger.valueOf(next));
                }
                Expr value = valueAt(current, name, point);
                if (value == null) {
                    failed = true;
                    break;
                }
                Expr coefficient = Expand.expand(Expr.div(value, Expr.num(Rational.of(factorial))));
                steps.add(info(Rule.TAYLOR_COEFFICIENT, current, coefficient, "f", current, "x", variable, "a", point, "k", Expr.num(next), "v", value));
                values.add(coefficient);
            }
            return k < values.size() ? values.get(k) : null;
        }
    }

    /** El valor de {@code g} en el punto: el límite, que cubre también los puntos donde la sustitución directa da {@code 0/0}. */
    private static Expr valueAt(Expr g, String variable, Expr point) {
        try {
            Solution s = Limits.limit(g, variable, point);
            if (s.isSolved() && s.result() != null && finite(s.result())) return s.result();
        } catch (RuntimeException e) {
            // sin valor exacto: el desarrollo se corta aquí
        }
        return null;
    }

    private static Expansion finishPower(Solution.Kind kind, Expr f, String variable, Expr point, List<Step> steps, List<Expr> coefficients,
                                         int firstPower, int degree, Rule closing) {
        Expr x = Expr.sym(variable);
        Expr base = Expr.sub(x, point);
        List<Expr> terms = new ArrayList<>();
        for (int i = 0; i < coefficients.size(); i++) {
            int power = firstPower + i;
            terms.add(power == 0 ? coefficients.get(i) : Expr.mul(coefficients.get(i), Expr.pow(base, power)));
        }
        Map<String, Expr> extras = new LinkedHashMap<>();
        extras.put("a", point);
        extras.put("degree", Expr.num(degree));
        if (kind == Solution.Kind.LAURENT) extras.put("order", Expr.num(Math.max(0, -firstPower)));
        Expr series = Expr.add(terms);
        steps.add(info(closing, f, series, "f", f, "x", x, "a", point, "N", Expr.num(degree)));
        return new Expansion(new Solution(kind, Solution.Status.SOLVED, f, series, steps, variable, 1, extras), terms);
    }

    /**
     * El desarrollo de {@code f = N/D} como cociente de las series de {@code N} y {@code D}: sus primeros coeficientes no nulos dicen el
     * orden del cero de cada uno, y el cociente de las series dice el resto. {@code first} es la menor potencia (negativa si hay polo)
     * y {@code coefficients} van de ahí hasta el grado pedido.
     */
    private record Quotient(int first, List<Expr> coefficients) {
    }

    private static Quotient quotient(Expr f, String variable, Expr point, int degree, List<Step> steps) {
        Expr x = Expr.sym(variable);
        Algebra.Fraction fraction = Algebra.fraction(Algebra.together(f));
        Expr numerator = fraction.numerator();
        Expr denominator = fraction.denominator();
        steps.add(info(Rule.EXPANSION_QUOTIENT, f, null, "f", f, "n", numerator, "d", denominator, "x", x, "a", point));
        List<Step> numeratorSteps = new ArrayList<>();
        List<Step> denominatorSteps = new ArrayList<>();
        Coefficients cn = new Coefficients(numerator, variable, point, numeratorSteps);
        Coefficients cd = new Coefficients(denominator, variable, point, denominatorSteps);

        int zeroN = firstNonZero(cn);
        int zeroD = firstNonZero(cd);
        if (zeroN < 0 || zeroD < 0) {
            steps.add(info(Rule.LAURENT_ESSENTIAL, f, null, "f", f, "x", x, "a", point));
            return null;
        }
        int first = zeroN - zeroD;
        int count = degree - first + 1;
        if (count <= 0) count = 1;
        List<Expr> n = new ArrayList<>();
        List<Expr> d = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Expr a = cn.get(zeroN + i);
            Expr b = cd.get(zeroD + i);
            if (a == null || b == null) {
                steps.add(info(Rule.LAURENT_ESSENTIAL, f, null, "f", f, "x", x, "a", point));
                return null;
            }
            n.add(a);
            d.add(b);
        }
        steps.add(new Step(Rule.TAYLOR_FORMULA, numerator, null, Trace.params("f", numerator, "x", x, "a", point, "N", Expr.num(zeroN + count - 1)), numeratorSteps));
        steps.add(new Step(Rule.TAYLOR_FORMULA, denominator, null, Trace.params("f", denominator, "x", x, "a", point, "N", Expr.num(zeroD + count - 1)), denominatorSteps));
        if (first < 0) {
            steps.add(info(Rule.LAURENT_POLE, f, null, "f", f, "x", x, "a", point, "p", Expr.num(zeroN), "q", Expr.num(zeroD), "m", Expr.num(-first)));
        } else {
            steps.add(info(Rule.LAURENT_REGULAR_POINT, f, null, "f", f, "x", x, "a", point));
        }
        // División de series: q_i = (n_i − Σ_{t=1..i} d_t q_{i−t}) / d_0
        List<Expr> quotientCoefficients = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Expr value = n.get(i);
            for (int t = 1; t <= i; t++) value = Expr.sub(value, Expr.mul(d.get(t), quotientCoefficients.get(i - t)));
            quotientCoefficients.add(Expand.expand(Expr.div(value, d.get(0))));
        }
        List<Expr> all = new ArrayList<>();
        // Una función regular con un cero en el punto: las potencias bajas valen 0
        for (int power = Math.min(first, 0); power < first; power++) all.add(Expr.ZERO);
        all.addAll(quotientCoefficients);
        steps.add(info(Rule.EXPANSION_DIVIDE, f, null, "f", f, "n", numerator, "d", denominator, "x", x, "a", point));
        return new Quotient(Math.min(first, 0), all);
    }

    /** El índice del primer coeficiente que no es 0 (el orden del cero), o −1 si los primeros no lo permiten decidir. */
    private static int firstNonZero(Coefficients c) {
        for (int k = 0; k <= MAX_POLE + 2; k++) {
            Expr v = c.get(k);
            if (v == null) return -1;
            if (!v.isZero()) return k;
        }
        return -1;
    }

    // ================================================================ Laurent

    public static Expansion laurent(String f, String variable, String point, int degree) {
        ExprParser.Parsed parsed = ExprParser.parseDetailed(f);
        Expansion e = laurent(parsed.expr(), variable, ExprParser.parse(point), degree);
        return new Expansion(e.solution().withPrelude(parsed.steps()), e.terms());
    }

    /**
     * El desarrollo de Laurent de {@code f} alrededor de {@code point}, desde la parte singular hasta {@code (x − a)^degree}. La función
     * se escribe como cociente {@code N/D}; los órdenes de los ceros de ambos dan el orden del polo, y la división de sus series, los
     * coeficientes.
     */
    public static Expansion laurent(Expr f, String variable, Expr point, int degree) {
        check(degree);
        checkPoint(point);
        Expr x = Expr.sym(variable);
        List<Step> steps = new ArrayList<>();
        steps.add(info(Rule.LAURENT_FORMULA, f, null, "f", f, "x", x, "a", point, "N", Expr.num(degree)));
        Quotient q = quotient(f, variable, point, degree, steps);
        if (q == null) return unsolved(Solution.Kind.LAURENT, f, variable, point, degree, steps);
        return finishPower(Solution.Kind.LAURENT, f, variable, point, steps, q.coefficients, q.first, degree, Rule.LAURENT_RESULT);
    }

    private static Expansion unsolved(Solution.Kind kind, Expr f, String variable, Expr point, int degree, List<Step> steps) {
        Map<String, Expr> extras = new LinkedHashMap<>();
        extras.put("a", point);
        extras.put("degree", Expr.num(degree));
        return new Expansion(new Solution(kind, Solution.Status.UNSOLVED, f, null, steps, variable, 1, extras), List.of());
    }

    // ================================================================ Fourier

    public static Expansion fourier(String f, String variable, String lower, String upper, int harmonics) {
        ExprParser.Parsed parsed = ExprParser.parseDetailed(f);
        Expansion e = fourier(parsed.expr(), variable, ExprParser.parse(lower), ExprParser.parse(upper), harmonics);
        return new Expansion(e.solution().withPrelude(parsed.steps()), e.terms());
    }

    /**
     * La serie de Fourier de {@code f} en {@code [lower, upper]} (extendida con ese periodo), hasta {@code harmonics} armónicos:
     * {@code a0/2 + Σ a_k cos(kωx) + b_k sin(kωx)} con {@code ω = 2π/(upper − lower)}. En un intervalo simétrico, una función par o impar
     * ahorra la mitad de los coeficientes.
     */
    public static Expansion fourier(Expr f, String variable, Expr lower, Expr upper, int harmonics) {
        check(harmonics);
        double lo = Eval.value(lower);
        double hi = Eval.value(upper);
        if (Double.isNaN(lo) || Double.isNaN(hi) || !(hi > lo) || Double.isInfinite(lo) || Double.isInfinite(hi)) {
            throw new IllegalArgumentException("El intervalo tiene que ser finito y el extremo inferior menor que el superior");
        }
        Expr x = Expr.sym(variable);
        Expr period = Expand.expand(Expr.sub(upper, lower));
        Expr omega = Expr.div(Expr.mul(Expr.num(2), Expr.PI), period);
        Expr scale = Expr.div(Expr.num(2), period);
        List<Step> steps = new ArrayList<>();
        steps.add(info(Rule.FOURIER_PERIOD, f, omega, "f", f, "x", x, "lower", lower, "upper", upper, "T", period, "w", omega, "N", Expr.num(harmonics)));

        boolean symmetric = Math.abs(lo + hi) < 1e-12;
        boolean even = false;
        boolean odd = false;
        if (symmetric) {
            even = parity(f, variable, hi, 1);
            odd = !even && parity(f, variable, hi, -1);
            if (even) steps.add(info(Rule.FOURIER_EVEN, f, null, "f", f, "x", x));
            if (odd) steps.add(info(Rule.FOURIER_ODD, f, null, "f", f, "x", x));
        }
        boolean approximate = false;
        List<Expr> terms = new ArrayList<>();

        Coefficient a0 = odd ? new Coefficient(Expr.ZERO, false) : coefficient(f, variable, lower, upper, scale, Expr.ONE, null, steps,
                Rule.FOURIER_A0, Expr.num(0), x, omega, period);
        if (a0 == null) return unsolved(Solution.Kind.FOURIER, f, variable, lower, upper, harmonics, steps);
        approximate |= a0.approximate;
        if (!a0.value.isZero()) terms.add(Expr.div(a0.value, Expr.num(2)));
        else terms.add(Expr.ZERO);

        for (int k = 1; k <= harmonics; k++) {
            Expr kw = Expr.mul(Expr.num(k), omega, x);
            Coefficient a = odd ? new Coefficient(Expr.ZERO, false)
                    : coefficient(f, variable, lower, upper, scale, Expr.cos(kw), Expr.cos(kw), steps, Rule.FOURIER_AN, Expr.num(k), x, omega, period);
            Coefficient b = even ? new Coefficient(Expr.ZERO, false)
                    : coefficient(f, variable, lower, upper, scale, Expr.sin(kw), Expr.sin(kw), steps, Rule.FOURIER_BN, Expr.num(k), x, omega, period);
            if (a == null || b == null) return unsolved(Solution.Kind.FOURIER, f, variable, lower, upper, harmonics, steps);
            approximate |= a.approximate | b.approximate;
            List<Expr> parts = new ArrayList<>();
            if (!a.value.isZero()) parts.add(Expr.mul(a.value, Expr.cos(kw)));
            if (!b.value.isZero()) parts.add(Expr.mul(b.value, Expr.sin(kw)));
            terms.add(Expr.add(parts));
        }
        Expr series = Expr.add(terms);
        Map<String, Expr> extras = new LinkedHashMap<>();
        extras.put("lower", lower);
        extras.put("upper", upper);
        extras.put("harmonics", Expr.num(harmonics));
        extras.put("period", period);
        if (approximate) extras.put("approximate", Expr.ONE);
        steps.add(info(Rule.FOURIER_RESULT, f, series, "f", f, "x", x, "lower", lower, "upper", upper, "N", Expr.num(harmonics)));
        return new Expansion(new Solution(Solution.Kind.FOURIER, Solution.Status.SOLVED, f, series, steps, variable, 1, extras), terms);
    }

    private record Coefficient(Expr value, boolean approximate) {
    }

    /**
     * {@code (2/T) ∫ f·weight dx}. El primer coeficiente ({@code a0}) lleva {@code weight = 1}; {@code shown} es el factor que se anota en
     * el paso (nulo para {@code a0}).
     */
    private static Coefficient coefficient(Expr f, String variable, Expr lower, Expr upper, Expr scale, Expr weight, Expr shown,
                                           List<Step> steps, Rule rule, Expr k, Expr x, Expr omega, Expr period) {
        Expr integrand = Expr.mul(f, weight);
        Expr exact = null;
        List<Step> inner = List.of();
        try {
            Integral integral = integrate(integrand, variable, lower, upper);
            if (integral != null) {
                Expr value = Expand.expand(Expr.mul(scale, integral.value));
                if (finite(value) && value.toString().length() < 90) {
                    exact = value;
                    inner = integral.steps;
                }
            }
        } catch (RuntimeException e) {
            // se pasa a la integral numérica
        }
        double numeric = simpson(integrand, variable, Eval.value(lower), Eval.value(upper)) * Eval.value(scale);
        boolean approximate = false;
        if (exact == null || Math.abs(Eval.value(exact) - numeric) > 1e-6 * (1 + Math.abs(numeric))) {
            if (Double.isNaN(numeric) || Double.isInfinite(numeric)) return null;
            exact = Math.abs(numeric) < 1e-9 ? Expr.ZERO : ExprParser.parse(String.format(Locale.ROOT, "%.10f", numeric));
            approximate = true;
            inner = List.of();
        } else if (Math.abs(Eval.value(exact)) < 1e-9) {
            exact = Expr.ZERO;
        }
        steps.add(new Step(rule, integrand, exact, Trace.params("f", f, "x", x, "k", k, "lower", lower, "upper", upper, "T", period, "w", omega,
                "g", shown == null ? Expr.ONE : shown), inner));
        return new Coefficient(exact, approximate);
    }

    private record Integral(Expr value, List<Step> steps) {
    }

    /**
     * La integral definida exacta. Si el integrando lleva valores absolutos de expresiones lineales, se parte el intervalo por donde se
     * anulan y en cada trozo el valor absoluto es la expresión o su opuesta (el motor, con el valor absoluto, da números con decimales).
     * {@code null} si no tiene forma exacta.
     */
    private static Integral integrate(Expr g, String variable, Expr lower, Expr upper) {
        List<Expr> args = new ArrayList<>();
        absoluteArguments(g, args);
        if (args.isEmpty()) {
            Solution s = Integrals.definite(g, variable, lower, upper);
            return s.isSolved() && s.result() != null ? new Integral(s.result(), s.steps()) : null;
        }
        double lo = Eval.value(lower);
        double hi = Eval.value(upper);
        List<Expr> cuts = new ArrayList<>(List.of(lower));
        List<Double> cutValues = new ArrayList<>(List.of(lo));
        List<Expr> inside = new ArrayList<>();
        for (Expr arg : args) {
            double c0 = at(arg, variable, 0);
            double c1 = at(arg, variable, 1) - c0;
            if (Math.abs(at(arg, variable, 2) - (c0 + 2 * c1)) > 1e-9 || Math.abs(c1) < 1e-12) return null;
            double root = -c0 / c1;
            if (root > lo + 1e-12 && root < hi - 1e-12) {
                Expr value0 = arg.substitute(variable, Expr.ZERO);
                Expr slope = Expand.expand(Expr.sub(arg.substitute(variable, Expr.ONE), value0));
                inside.add(Expand.expand(Expr.neg(Expr.div(value0, slope))));
            }
        }
        inside.sort(java.util.Comparator.comparingDouble(Eval::value));
        for (Expr cut : inside) {
            if (Math.abs(Eval.value(cut) - cutValues.get(cutValues.size() - 1)) < 1e-12) continue;
            cuts.add(cut);
            cutValues.add(Eval.value(cut));
        }
        cuts.add(upper);
        cutValues.add(hi);
        List<Expr> pieces = new ArrayList<>();
        List<Step> steps = new ArrayList<>();
        for (int i = 0; i + 1 < cuts.size(); i++) {
            double mid = (cutValues.get(i) + cutValues.get(i + 1)) / 2;
            Expr piece = g;
            for (Expr arg : args) piece = Algebra.replace(piece, Expr.abs(arg), at(arg, variable, mid) >= 0 ? arg : Expr.neg(arg));
            Solution s = Integrals.definite(piece, variable, cuts.get(i), cuts.get(i + 1));
            if (!s.isSolved() || s.result() == null) return null;
            pieces.add(s.result());
            if (i == 0) steps.addAll(s.steps());
        }
        return new Integral(Expand.expand(Expr.add(pieces)), steps);
    }

    private static void absoluteArguments(Expr e, List<Expr> out) {
        switch (e) {
            case Expr.Func f -> {
                if (f.fn() == barnesinside.mathutils.cas.expr.Fn.ABS && !out.contains(f.arg())) out.add(f.arg());
                absoluteArguments(f.arg(), out);
            }
            case Expr.Add a -> a.terms().forEach(t -> absoluteArguments(t, out));
            case Expr.Mul m -> m.factors().forEach(t -> absoluteArguments(t, out));
            case Expr.Pow p -> {
                absoluteArguments(p.base(), out);
                absoluteArguments(p.exponent(), out);
            }
            default -> {
            }
        }
    }

    private static Expansion unsolved(Solution.Kind kind, Expr f, String variable, Expr lower, Expr upper, int harmonics, List<Step> steps) {
        Map<String, Expr> extras = new LinkedHashMap<>();
        extras.put("lower", lower);
        extras.put("upper", upper);
        extras.put("harmonics", Expr.num(harmonics));
        return new Expansion(new Solution(kind, Solution.Status.UNSOLVED, f, null, steps, variable, 1, extras), List.of());
    }

    /** Si {@code f(−x) = sign·f(x)} en las muestras de {@code (0, half]}. */
    private static boolean parity(Expr f, String variable, double half, int sign) {
        for (int i = 1; i <= 25; i++) {
            double t = half * i / 25.0 * 0.97;
            double a = at(f, variable, t);
            double b = at(f, variable, -t);
            if (Double.isNaN(a) || Double.isNaN(b)) return false;
            if (Math.abs(b - sign * a) > 1e-9 * (1 + Math.abs(a))) return false;
        }
        return true;
    }

    private static double at(Expr f, String variable, double t) {
        try {
            return Eval.value(f, Map.of(variable, t));
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    /** La integral numérica por Simpson con 4000 subintervalos (el respaldo de lo que el motor no resuelve de forma exacta). */
    private static double simpson(Expr g, String variable, double a, double b) {
        int n = 4000;
        double h = (b - a) / n;
        double sum = at(g, variable, a) + at(g, variable, b);
        for (int i = 1; i < n; i++) sum += at(g, variable, a + i * h) * (i % 2 == 0 ? 2 : 4);
        return sum * h / 3;
    }

    // ================================================================ Común

    private static void checkPoint(Expr point) {
        double v = Eval.value(point);
        if (Double.isNaN(v) || Double.isInfinite(v)) throw new IllegalArgumentException("El punto del desarrollo tiene que ser un número finito");
    }

    private static void check(int terms) {
        if (terms < 0 || terms > MAX_TERMS) throw new IllegalArgumentException("El número de términos tiene que estar entre 0 y " + MAX_TERMS);
    }

    private static boolean finite(Expr e) {
        double v = Eval.value(e);
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private static Step info(Rule rule, Expr before, Expr after, Object... params) {
        return new Step(rule, before, after, Trace.params(params), List.of());
    }
}
