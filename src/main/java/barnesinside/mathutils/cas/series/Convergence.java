package barnesinside.mathutils.cas.series;

import barnesinside.mathutils.cas.expr.Algebra;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.expr.Fn;
import barnesinside.mathutils.cas.integral.Integrals;
import barnesinside.mathutils.cas.limit.Limits;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import barnesinside.mathutils.cas.steps.Trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convergencia de una serie {@code Σ a_n} con los pasos del razonamiento. Los criterios se aplican en este orden, y el primero que decide
 * termina: D'Alembert (el cociente), Cauchy (la raíz), la condición necesaria ({@code a_n → 0}), la comparación (con una cota
 * acotada o con la serie {@code 1/n^p}), la integral y, si los términos alternan, Leibniz. Los límites son los de {@link Limits}.
 * <pre>{@code
 * Solution s = Convergence.test("n!/2^n", "n", "1");   // diverge
 * s.extras().get("verdict")                              // -1
 * }</pre>
 * El resultado es una {@link Solution} de tipo {@link Solution.Kind#SERIES}: el estado es {@code SOLVED} si converge y se conoce la
 * suma, {@code NON_ELEMENTAL} si converge sin una suma que escribir, {@code DOES_NOT_EXIST} si diverge y {@code UNSOLVED} si ningún
 * criterio decide. Los datos van en {@code extras}: {@code verdict} (1, −1 o 0), {@code absolute} (1 si converge absolutamente),
 * {@code lower} (el primer índice) y, si existen, {@code sum} y {@code approximation}.
 *
 * @since 0.10
 */
public final class Convergence {

    /** Lo que se concluye de una serie. */
    public enum Verdict {CONVERGES, DIVERGES, UNDECIDED}

    private static final double EPS = 1e-9;
    private static final long BUDGET_MILLIS = 25_000;

    private Convergence() {
    }

    /**
     * @throws barnesinside.mathutils.exceptions.ExpressionParseException si alguno de los textos no es una expresión válida
     */
    public static Solution test(String term, String variable, String start) {
        ExprParser.Parsed parsed = ExprParser.parseDetailed(term);
        return test(parsed.expr(), variable, ExprParser.parse(start)).withPrelude(parsed.steps());
    }

    /** Estudia {@code Σ_{variable = start}^{∞} term}. {@code start} tiene que ser un entero. */
    public static Solution test(Expr term, String variable, Expr start) {
        double first = Eval.value(start);
        if (Double.isNaN(first) || first != Math.rint(first)) throw new IllegalArgumentException("El primer índice tiene que ser un entero");
        return new Run(term, variable, start, (int) first).solve();
    }

    /** El veredicto guardado en una solución de tipo {@link Solution.Kind#SERIES}. */
    public static Verdict verdict(Solution s) {
        Expr v = s.extras().get("verdict");
        double value = v == null ? 0 : Eval.value(v);
        return value > 0 ? Verdict.CONVERGES : value < 0 ? Verdict.DIVERGES : Verdict.UNDECIDED;
    }

    /** Un criterio dio una respuesta o no pudo darla. */
    private enum Outcome {CONVERGES, DIVERGES, NOTHING}

    private static final class Run {
        private final Expr term;
        private final String n;
        private final Expr index;
        private final Expr start;
        private final int first;
        private final long deadline = System.currentTimeMillis() + BUDGET_MILLIS;
        private final List<Step> steps = new ArrayList<>();

        private Expr magnitude;
        private boolean alternating;
        private boolean positive;
        private boolean tendsToZero;
        /** Si la serie de valores absolutos diverge (solo importa cuando los términos alternan). */
        private boolean absoluteDiverges;
        private Expr sum;
        private Expr approximation;
        private boolean absolute;

        Run(Expr term, String n, Expr start, int first) {
            this.term = term;
            this.n = n;
            this.index = Expr.sym(n);
            this.start = start;
            this.first = first;
        }

        // ------------------------------------------------------------ Resolución

        Solution solve() {
            steps.add(info(Rule.SERIES_TERM, term, null, "f", term, "x", index, "lower", start));
            magnitude = magnitudeOf(term);
            alternating = !magnitude.equals(term);
            if (alternating) steps.add(info(Rule.SERIES_ALTERNATING, term, magnitude, "f", term, "g", magnitude, "x", index));
            positive = !isNegativeSomewhere(magnitude);

            Outcome outcome = tests();
            if (outcome == Outcome.NOTHING && alternating) outcome = leibniz();
            return finish(outcome);
        }

        private Outcome tests() {
            Outcome ratio = ratioTest();
            if (ratio != Outcome.NOTHING) return ratio;
            Outcome root = rootTest();
            if (root != Outcome.NOTHING) return root;
            Outcome nth = nthTerm();
            if (nth != Outcome.NOTHING) return nth;
            Outcome bounded = boundedComparison();
            if (bounded != Outcome.NOTHING) return bounded;
            Outcome p = pComparison();
            if (p != Outcome.NOTHING) return p;
            return integralTest();
        }

        private boolean expired() {
            return System.currentTimeMillis() > deadline;
        }

        // ------------------------------------------------------------ D'Alembert y Cauchy

        private Outcome ratioTest() {
            Expr quotient = quotient(magnitude);
            Limit limit = limitAtInfinity(positive ? quotient : Expr.abs(quotient));
            if (limit == null) {
                steps.add(info(Rule.SERIES_RATIO_INCONCLUSIVE, quotient, null, "f", magnitude, "r", quotient, "x", index));
                return Outcome.NOTHING;
            }
            steps.add(new Step(Rule.SERIES_RATIO, magnitude, limit.value, Trace.params("f", magnitude, "r", quotient, "x", index), limit.steps));
            Outcome decided = compareToOne(limit.value, Rule.SERIES_RATIO_CONVERGES, Rule.SERIES_RATIO_DIVERGES, Rule.SERIES_RATIO_INCONCLUSIVE);
            if (decided == Outcome.CONVERGES) {
                if (!geometricSum()) approximateSum(8);
            }
            return decided;
        }

        private Outcome rootTest() {
            if (expired()) return Outcome.NOTHING;
            Expr root = rootOf(magnitude);
            Limit limit = limitAtInfinity(root);
            if (limit == null) {
                steps.add(info(Rule.SERIES_ROOT_INCONCLUSIVE, root, null, "f", magnitude, "r", root, "x", index));
                return Outcome.NOTHING;
            }
            steps.add(new Step(Rule.SERIES_ROOT, magnitude, limit.value, Trace.params("f", magnitude, "r", root, "x", index), limit.steps));
            Outcome decided = compareToOne(limit.value, Rule.SERIES_ROOT_CONVERGES, Rule.SERIES_ROOT_DIVERGES, Rule.SERIES_ROOT_INCONCLUSIVE);
            if (decided == Outcome.CONVERGES) approximateSum(8);
            return decided;
        }

        /** L &lt; 1 converge (absolutamente), L &gt; 1 diverge y L = 1 no decide. */
        private Outcome compareToOne(Expr l, Rule converges, Rule diverges, Rule inconclusive) {
            double v = Eval.value(l);
            if (Double.isNaN(v)) {
                steps.add(info(inconclusive, l, null, "L", l, "f", magnitude, "x", index));
                return Outcome.NOTHING;
            }
            if (v < 1 - EPS) {
                steps.add(info(converges, l, null, "L", l, "f", magnitude, "x", index));
                absolute = true;
                return Outcome.CONVERGES;
            }
            if (v > 1 + EPS) {
                steps.add(info(diverges, l, null, "L", l, "f", magnitude, "x", index));
                return Outcome.DIVERGES;
            }
            steps.add(info(inconclusive, l, null, "L", l, "f", magnitude, "x", index));
            return Outcome.NOTHING;
        }

        /**
         * Si el cociente de términos consecutivos es una constante (se comprueba en los primeros), es una serie geométrica y se conoce su
         * suma: el primer término entre {@code 1 − r}.
         */
        private boolean geometricSum() {
            double r0 = valueAt(term, first + 1) / valueAt(term, first);
            if (Double.isNaN(r0) || Math.abs(r0) >= 1) return false;
            for (int k = first + 1; k < first + 6; k++) {
                double r = valueAt(term, k + 1) / valueAt(term, k);
                if (Math.abs(r - r0) > 1e-9 * (1 + Math.abs(r0))) return false;
            }
            Limit magnitudeRatio = limitAtInfinity(positive ? quotient(magnitude) : Expr.abs(quotient(magnitude)));
            if (magnitudeRatio == null) return false;
            Expr r = Expand.expand(r0 < 0 ? Expr.neg(magnitudeRatio.value) : magnitudeRatio.value);
            Expr firstTerm = term.substitute(n, start);
            Expr value = Expand.expand(Expr.div(firstTerm, Expr.sub(Expr.ONE, r)));
            if (!isFinite(value)) return false;
            sum = value;
            steps.add(info(Rule.SERIES_GEOMETRIC_SUM, term, value, "f", term, "r", r, "a", firstTerm, "x", index, "lower", start));
            return true;
        }

        /** Para las que convergen rápido, el valor aproximado sumando términos, redondeado a {@code decimals} decimales. */
        private void approximateSum(int decimals) {
            double total = 0;
            for (int k = first; k < first + 4000; k++) {
                double t = valueAt(term, k);
                if (Double.isNaN(t) || Double.isInfinite(t)) return;
                total += t;
            }
            if (Double.isNaN(total) || Double.isInfinite(total)) return;
            String rounded = new java.math.BigDecimal(total).setScale(decimals, java.math.RoundingMode.HALF_UP).toPlainString();
            approximation = Expr.num(barnesinside.mathutils.cas.expr.Rational.parse(rounded));
        }

        // ------------------------------------------------------------ Condición necesaria

        private Outcome nthTerm() {
            if (expired()) return Outcome.NOTHING;
            Limit limit = limitAtInfinity(positive ? magnitude : Expr.abs(magnitude));
            if (limit == null) return Outcome.NOTHING;
            double v = Eval.value(limit.value);
            if (Double.isNaN(v)) return Outcome.NOTHING;
            steps.add(new Step(Rule.SERIES_NTH_TERM, magnitude, limit.value, Trace.params("f", magnitude, "x", index), limit.steps));
            if (Math.abs(v) > EPS) {
                steps.add(info(Rule.SERIES_NTH_DIVERGES, limit.value, null, "L", limit.value, "f", magnitude, "x", index));
                return Outcome.DIVERGES;
            }
            tendsToZero = true;
            steps.add(info(Rule.SERIES_NTH_ZERO, limit.value, null, "L", limit.value, "f", magnitude, "x", index));
            return Outcome.NOTHING;
        }

        // ------------------------------------------------------------ Comparación

        /** Si el término lleva senos o cosenos de {@code n}, se acota por 1 y se estudia la cota. */
        private Outcome boundedComparison() {
            if (expired()) return Outcome.NOTHING;
            Expr bound = boundTrig(magnitude);
            if (bound.equals(magnitude)) return Outcome.NOTHING;
            Expr abs = Expr.abs(magnitude);
            Outcome inner = comparisonWithP(bound, true, true);
            if (inner == Outcome.NOTHING) {
                steps.add(info(Rule.SERIES_BOUNDED_INCONCLUSIVE, abs, bound, "f", magnitude, "g", bound, "x", index));
                return Outcome.NOTHING;
            }
            // Una cota que diverge no dice nada del término; una que converge, sí
            if (inner == Outcome.DIVERGES) {
                steps.add(info(Rule.SERIES_BOUNDED_INCONCLUSIVE, abs, bound, "f", magnitude, "g", bound, "x", index));
                return Outcome.NOTHING;
            }
            steps.add(info(Rule.SERIES_BOUNDED, abs, bound, "f", magnitude, "g", bound, "x", index));
            absolute = true;
            return Outcome.CONVERGES;
        }

        private Outcome pComparison() {
            if (expired() || !positive) return Outcome.NOTHING;
            return comparisonWithP(magnitude, false, false);
        }

        /**
         * Compara {@code f} con {@code 1/n^p}: el exponente se estima numéricamente (el más cercano de los múltiplos de 1/2) y el
         * cociente de ambas se calcula con un límite exacto. {@code silent} no anota la comparación (la usa la cota).
         */
        private Outcome comparisonWithP(Expr f, boolean silent, boolean upperBoundOnly) {
            double big = 1e6;
            double a = valueAt(f, big);
            double b = valueAt(f, big * 10);
            if (!(a > 0) || !(b > 0)) return Outcome.NOTHING;
            double estimate = -(Math.log(b) - Math.log(a)) / Math.log(10);
            for (double p : exponentCandidates(estimate)) {
                Expr pe = Expr.num(Math.round(p * 2), 2);
                Expr g = Expr.pow(index, Expr.neg(pe));
                Limit limit = limitAtInfinity(Expr.div(f, g));
                if (limit == null) continue;
                double l = Eval.value(limit.value);
                if (Double.isNaN(l)) continue;
                boolean seriesG = p > 1;
                Rule rule;
                Outcome outcome;
                if (l > 0 && !Double.isInfinite(l)) {
                    outcome = seriesG ? Outcome.CONVERGES : Outcome.DIVERGES;
                    rule = seriesG ? Rule.SERIES_COMPARE_CONVERGES : Rule.SERIES_COMPARE_DIVERGES;
                } else if (Math.abs(l) <= EPS && seriesG) {
                    outcome = Outcome.CONVERGES;
                    rule = Rule.SERIES_COMPARE_CONVERGES;
                } else if (Double.isInfinite(l) && l > 0 && !seriesG) {
                    outcome = Outcome.DIVERGES;
                    rule = Rule.SERIES_COMPARE_DIVERGES;
                } else {
                    continue;
                }
                if (silent) return outcome;
                steps.add(new Step(rule, f, limit.value, Trace.params("f", f, "g", g, "p", pe, "L", limit.value, "x", index), limit.steps));
                if (outcome == Outcome.DIVERGES && alternating) {
                    // Que diverja la de valores absolutos no hace diverger la alternada: queda Leibniz
                    absoluteDiverges = true;
                    return Outcome.NOTHING;
                }
                if (outcome == Outcome.CONVERGES) absolute = true;
                return outcome;
            }
            return Outcome.NOTHING;
        }

        /**
         * Los exponentes con los que comparar: el múltiplo de 1/2 más cercano a la estimación si coincide con ella; si no (como con
         * {@code ln(n)/n^2}, que decrece algo más lento que {@code 1/n^2}), uno de los lados que aun así decida: por debajo de la
         * estimación pero mayor que 1, o por encima pero no mayor que 1.
         */
        private static List<Double> exponentCandidates(double estimate) {
            List<Double> out = new ArrayList<>();
            double nearest = Math.rint(estimate * 2) / 2;
            if (Math.abs(estimate - nearest) <= 0.12 && nearest > 0) out.add(nearest);
            double below = Math.floor((estimate - 0.05) * 2) / 2;
            if (below > 1 && !out.contains(below)) out.add(below);
            double above = Math.ceil((estimate + 0.05) * 2) / 2;
            if (above <= 1 && above > 0 && !out.contains(above)) out.add(above);
            return out;
        }

        // ------------------------------------------------------------ Integral

        private Outcome integralTest() {
            if (expired() || !positive || !decreasing(magnitude)) return Outcome.NOTHING;
            Solution integral;
            try {
                integral = Integrals.definite(magnitude, n, start, Expr.INFINITY);
            } catch (RuntimeException e) {
                return Outcome.NOTHING;
            }
            Outcome outcome;
            Expr value = integral.result();
            if (integral.isSolved() && value != null && isFinite(value)) {
                outcome = Outcome.CONVERGES;
            } else if (integral.status() == Solution.Status.DOES_NOT_EXIST) {
                outcome = Outcome.DIVERGES;
            } else {
                return Outcome.NOTHING;
            }
            steps.add(new Step(Rule.SERIES_INTEGRAL, magnitude, value, Trace.params("f", magnitude, "x", index, "lower", start), integral.steps()));
            steps.add(info(outcome == Outcome.CONVERGES ? Rule.SERIES_INTEGRAL_CONVERGES : Rule.SERIES_INTEGRAL_DIVERGES,
                    value, null, "f", magnitude, "I", value == null ? Expr.INFINITY : value, "x", index, "lower", start));
            if (outcome == Outcome.DIVERGES && alternating) {
                absoluteDiverges = true;
                return Outcome.NOTHING;
            }
            if (outcome == Outcome.CONVERGES) {
                absolute = true;
            }
            return outcome;
        }

        // ------------------------------------------------------------ Leibniz

        private Outcome leibniz() {
            if (!tendsToZero) {
                Limit limit = expired() ? null : limitAtInfinity(positive ? magnitude : Expr.abs(magnitude));
                if (limit != null && Math.abs(Eval.value(limit.value)) <= EPS) tendsToZero = true;
            }
            boolean smaller = decreasing(magnitude);
            steps.add(info(Rule.SERIES_LEIBNIZ, magnitude, null, "f", magnitude, "x", index));
            if (tendsToZero && smaller) {
                steps.add(info(Rule.SERIES_LEIBNIZ_CONVERGES, magnitude, null, "f", magnitude, "x", index));
                approximateSum(4);
                return Outcome.CONVERGES;
            }
            steps.add(info(Rule.SERIES_LEIBNIZ_FAILS, magnitude, null, "f", magnitude, "x", index));
            return Outcome.NOTHING;
        }

        // ------------------------------------------------------------ Conclusión

        private Solution finish(Outcome outcome) {
            Map<String, Expr> extras = new LinkedHashMap<>();
            extras.put("lower", start);
            Solution.Status status;
            Expr result = null;
            Rule conclusion;
            switch (outcome) {
                case CONVERGES -> {
                    boolean conditional = alternating && !absolute;
                    conclusion = !alternating ? Rule.SERIES_CONCLUSION_CONVERGES
                            : conditional ? Rule.SERIES_CONCLUSION_CONDITIONAL : Rule.SERIES_CONCLUSION_ABSOLUTE;
                    extras.put("verdict", Expr.ONE);
                    extras.put("absolute", conditional ? Expr.ZERO : Expr.ONE);
                    if (sum != null) {
                        extras.put("sum", sum);
                        result = sum;
                        status = Solution.Status.SOLVED;
                    } else {
                        if (approximation != null) extras.put("approximation", approximation);
                        status = Solution.Status.NON_ELEMENTAL;
                    }
                    steps.add(info(conclusion, term, result, "f", term, "x", index, "lower", start));
                }
                case DIVERGES -> {
                    extras.put("verdict", Expr.MINUS_ONE);
                    extras.put("absolute", Expr.ZERO);
                    status = Solution.Status.DOES_NOT_EXIST;
                    steps.add(info(Rule.SERIES_CONCLUSION_DIVERGES, term, null, "f", term, "x", index, "lower", start));
                }
                default -> {
                    extras.put("verdict", Expr.ZERO);
                    extras.put("absolute", Expr.ZERO);
                    status = Solution.Status.UNSOLVED;
                    steps.add(info(absoluteDiverges ? Rule.SERIES_CONCLUSION_UNDECIDED_ABSOLUTE : Rule.SERIES_CONCLUSION_UNDECIDED,
                            term, null, "f", term, "x", index, "lower", start));
                }
            }
            return new Solution(Solution.Kind.SERIES, status, term, result, steps, n, 1, extras);
        }

        // ------------------------------------------------------------ Términos y cocientes

        private static Step info(Rule rule, Expr before, Expr after, Object... params) {
            return new Step(rule, before, after, Trace.params(params), List.of());
        }

        /** El término sin los factores {@code (-1)^(…n…)}: lo que importa para la convergencia absoluta. */
        private Expr magnitudeOf(Expr e) {
            if (e instanceof Expr.Mul m) {
                List<Expr> kept = new ArrayList<>();
                boolean removed = false;
                for (Expr f : m.factors()) {
                    if (isAlternatingSign(f)) removed = true;
                    else kept.add(f);
                }
                if (removed) return Expr.mul(kept);
                return e;
            }
            return isAlternatingSign(e) ? Expr.ONE : e;
        }

        private boolean isAlternatingSign(Expr f) {
            return f instanceof Expr.Pow p && p.base() instanceof Expr.Num b && b.value().equals(barnesinside.mathutils.cas.expr.Rational.MINUS_ONE)
                    && p.exponent().variables().contains(n);
        }

        /** {@code b(n+1)/b(n)} con los factoriales y las potencias de la misma clase reunidos para que el límite los vea. */
        private Expr quotient(Expr b) {
            Expr next = b.substitute(n, Expr.add(index, Expr.ONE));
            next = unfoldFactorials(b, next);
            Expr q = Expr.div(next, b);
            return fixBases(mergePowers(q));
        }

        /** {@code (a+k)!} en el siguiente término se escribe {@code (a+k)(a+k-1)…(a+1)·a!} para que se vaya con el {@code a!} del anterior. */
        private Expr unfoldFactorials(Expr b, Expr next) {
            Expr out = next;
            for (Expr f : factorialsOf(b)) {
                Expr arg = ((Expr.Func) f).arg();
                Expr moved = arg.substitute(n, Expr.add(index, Expr.ONE));
                Expr diff = Expand.expand(Expr.sub(moved, arg));
                if (!(diff instanceof Expr.Num k) || !k.value().isInteger()) continue;
                int steps = k.value().numerator().intValue();
                if (steps < 1 || steps > 8) continue;
                List<Expr> product = new ArrayList<>();
                for (int i = 1; i <= steps; i++) product.add(Expand.expand(Expr.add(arg, Expr.num(i))));
                product.add(f);
                out = Algebra.replace(out, Expr.func(Fn.FACTORIAL, moved), Expr.mul(product));
            }
            return out;
        }

        private static List<Expr> factorialsOf(Expr e) {
            List<Expr> found = new ArrayList<>();
            collect(e, Fn.FACTORIAL, found);
            return found;
        }

        private static void collect(Expr e, Fn fn, List<Expr> out) {
            switch (e) {
                case Expr.Func f -> {
                    if (f.fn() == fn && !out.contains(f)) out.add(f);
                    collect(f.arg(), fn, out);
                }
                case Expr.Add a -> a.terms().forEach(t -> collect(t, fn, out));
                case Expr.Mul m -> m.factors().forEach(t -> collect(t, fn, out));
                case Expr.Pow p -> {
                    collect(p.base(), fn, out);
                    collect(p.exponent(), fn, out);
                }
                default -> {
                }
            }
        }

        /** {@code p^e · q^(−e)} pasa a {@code (p/q)^e}: así el límite reconoce las formas {@code (1 + c/n)^n}. */
        private Expr mergePowers(Expr e) {
            return switch (e) {
                case Expr.Mul m -> {
                    List<Expr> factors = new ArrayList<>(m.factors().stream().map(this::mergePowers).toList());
                    boolean changed = true;
                    while (changed) {
                        changed = false;
                        search:
                        for (int i = 0; i < factors.size(); i++) {
                            for (int j = i + 1; j < factors.size(); j++) {
                                if (factors.get(i) instanceof Expr.Pow a && factors.get(j) instanceof Expr.Pow b
                                        && Expand.expand(Expr.add(a.exponent(), b.exponent())).isZero() && a.exponent().variables().contains(n)) {
                                    Expr merged = Expr.pow(Expr.div(a.base(), b.base()), a.exponent());
                                    factors.remove(j);
                                    factors.set(i, merged);
                                    changed = true;
                                    break search;
                                }
                            }
                        }
                    }
                    yield Expr.mul(factors);
                }
                case Expr.Add a -> Expr.add(a.terms().stream().map(this::mergePowers).toList());
                case Expr.Pow p -> Expr.pow(mergePowers(p.base()), mergePowers(p.exponent()));
                case Expr.Func f -> Expr.func(f.fn(), mergePowers(f.arg()));
                default -> e;
            };
        }

        /** Una base que tiende a 1 con exponente que crece se escribe {@code 1 + (N−D)/D}: la forma que sabe resolver el límite. */
        private Expr fixBases(Expr e) {
            return switch (e) {
                case Expr.Mul m -> Expr.mul(m.factors().stream().map(this::fixBases).toList());
                case Expr.Add a -> Expr.add(a.terms().stream().map(this::fixBases).toList());
                case Expr.Func f -> Expr.func(f.fn(), fixBases(f.arg()));
                case Expr.Pow p -> {
                    Expr base = fixBases(p.base());
                    if (p.exponent().variables().contains(n) && base.variables().contains(n) && !(base instanceof Expr.Sym)
                            && Math.abs(valueAt(base, 1e6) - 1) < 1e-3) {
                        Algebra.Fraction fraction = Algebra.fraction(base);
                        if (!(fraction.denominator() instanceof Expr.Num)) {
                            Expr rest = Expand.expand(Expr.sub(fraction.numerator(), fraction.denominator()));
                            base = Expr.add(Expr.ONE, Expr.div(rest, fraction.denominator()));
                        }
                    }
                    yield Expr.pow(base, p.exponent());
                }
                default -> e;
            };
        }

        /** {@code b(n)^(1/n)} sin pasar por el logaritmo cuando el término ya es una potencia de {@code n}. */
        private Expr rootOf(Expr e) {
            return switch (e) {
                case Expr.Mul m -> Expr.mul(m.factors().stream().map(this::rootOf).toList());
                case Expr.Pow p -> {
                    if (p.exponent().equals(index)) yield p.base();
                    Expr scaled = Expr.div(p.exponent(), index);
                    yield Expr.pow(p.base(), scaled);
                }
                case Expr.Num num -> Expr.pow(num, Expr.div(Expr.ONE, index));
                default -> Expr.pow(e, Expr.div(Expr.ONE, index));
            };
        }

        /** Cambia los senos y cosenos que dependen de {@code n} por 1 (su cota en valor absoluto). */
        private Expr boundTrig(Expr e) {
            return switch (e) {
                case Expr.Func f when (f.fn() == Fn.SIN || f.fn() == Fn.COS) && f.arg().variables().contains(n) -> Expr.ONE;
                case Expr.Mul m -> Expr.mul(m.factors().stream().map(this::boundTrig).toList());
                case Expr.Add a -> Expr.add(a.terms().stream().map(this::boundTrig).toList());
                case Expr.Pow p -> Expr.pow(boundTrig(p.base()), p.exponent());
                case Expr.Func f -> Expr.func(f.fn(), boundTrig(f.arg()));
                default -> e;
            };
        }

        // ------------------------------------------------------------ Numérico

        private double valueAt(Expr e, double at) {
            try {
                return Eval.value(e, Map.of(n, at));
            } catch (RuntimeException ex) {
                return Double.NaN;
            }
        }

        /** Si algún término de los primeros no es positivo (a partir de ahí, ni la integral ni la comparación valen). */
        private boolean isNegativeSomewhere(Expr b) {
            for (int k = first; k < first + 60; k++) {
                double v = valueAt(b, k);
                if (Double.isNaN(v) || v < 0) return true;
            }
            for (double k : new double[]{500, 5_000, 100_000}) {
                double v = valueAt(b, Math.max(k, first));
                if (!Double.isNaN(v) && v < 0) return true;
            }
            return false;
        }

        /** Si los términos no crecen (de manera comprobada en muestras: no es una demostración, pero el criterio lo exige). */
        private boolean decreasing(Expr b) {
            double previous = valueAt(b, first);
            int lastRise = -1;
            for (int k = first + 1; k < first + 400; k++) {
                double v = valueAt(b, k);
                if (Double.isNaN(v)) return false;
                if (v > previous * (1 + 1e-9) + 1e-300) lastRise = k;
                previous = v;
            }
            // Basta que dejen de subir pronto: los criterios valen «a partir de un índice»
            return lastRise < first + 20;
        }

        private static boolean isFinite(Expr e) {
            double v = Eval.value(e);
            return !Double.isNaN(v) && !Double.isInfinite(v);
        }

        private boolean isConstant(Expr e) {
            return !e.variables().contains(n);
        }

        // ------------------------------------------------------------ Límites

        private record Limit(Expr value, List<Step> steps) {
        }

        /** El límite cuando {@code n → ∞}, o {@code null} si el motor no lo resuelve. */
        private Limit limitAtInfinity(Expr f) {
            if (expired()) return null;
            try {
                Solution s = Limits.limit(fixBases(f), n, Expr.INFINITY);
                if (s.isSolved() && s.result() != null) return new Limit(s.result(), s.steps());
            } catch (RuntimeException e) {
                // el motor no pudo con este límite: el criterio no decide
            }
            return null;
        }
    }
}
