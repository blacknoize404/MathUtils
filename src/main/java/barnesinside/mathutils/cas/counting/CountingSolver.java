package barnesinside.mathutils.cas.counting;

import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Fn;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import barnesinside.mathutils.cas.steps.Trace;
import barnesinside.mathutils.discrete.Counting;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resuelve paso a paso los problemas de conteo de la combinatoria: los principios de la suma y del producto, permutaciones,
 * variaciones y combinaciones (con y sin repetición), permutaciones con elementos repetidos y circulares, el binomio de Newton,
 * inclusión–exclusión, desarreglos y los números de Catalan, de Stirling de segunda especie y de particiones.
 * <p>Cada resolución nombra el método, escribe la fórmula, sustituye los datos, desarrolla y simplifica los factoriales y concluye.
 * El resultado coincide con el de {@link Counting}. Los pasos son los de un {@link Solution} de tipo
 * {@link Solution.Kind#COUNTING}, con textos en español e inglés. Los datos inválidos lanzan {@link CountingException}.
 *
 * @since 0.11
 */
public final class CountingSolver {

    /** El mayor valor de n con el que se detallan los pasos. */
    public static final int MAX = 1000;

    /** Los factores que se escriben antes de abreviar un producto con puntos suspensivos. */
    private static final int SHOWN = 8;

    private CountingSolver() {
    }

    // ================================================================ Principios de la suma y del producto

    /** El principio de la suma: los casos de grupos que no se dan a la vez se suman. */
    public static Solution sum(long... options) {
        List<BigInteger> values = options(options);
        List<Expr> terms = new ArrayList<>();
        BigInteger total = BigInteger.ZERO;
        for (BigInteger v : values) {
            terms.add(num(v));
            total = total.add(v);
        }
        Expr written = terms.size() == 1 ? terms.get(0) : new Expr.Add(terms);
        Work w = new Work();
        w.add(Rule.COUNT_SUM, null, null, "f", written);
        w.add(Rule.COUNT_COMPUTE, written, num(total));
        return w.finish(CountingMethod.SUM, written, total);
    }

    /** El principio del producto: las etapas independientes multiplican sus casos. */
    public static Solution product(long... options) {
        List<BigInteger> values = options(options);
        List<Expr> factors = new ArrayList<>();
        BigInteger total = BigInteger.ONE;
        for (BigInteger v : values) {
            factors.add(num(v));
            total = total.multiply(v);
        }
        Expr written = factors.size() == 1 ? factors.get(0) : new Expr.Mul(factors);
        Work w = new Work();
        w.add(Rule.COUNT_PRODUCT, null, null, "f", written);
        w.add(Rule.COUNT_COMPUTE, written, num(total));
        return w.finish(CountingMethod.PRODUCT, written, total);
    }

    private static List<BigInteger> options(long[] options) {
        if (options.length == 0) throw new CountingException("empty");
        List<BigInteger> out = new ArrayList<>();
        for (long o : options) {
            if (o < 0) throw new CountingException("negative", "a", o);
            out.add(BigInteger.valueOf(o));
        }
        return out;
    }

    // ================================================================ Permutaciones, variaciones y combinaciones

    /** Permutaciones de {@code n} elementos distintos: {@code n!}. */
    public static Solution permutations(int n) {
        check("n", n, 0);
        Work w = new Work();
        w.add(Rule.COUNT_PERMUTATION, null, null, "n", num(n));
        w.add(Rule.COUNT_SUBSTITUTE, fact(sym("n")), fact(n));
        Expr factorial = fact(n);
        BigInteger result = Counting.factorial(n);
        if (n > 1) {
            Expr product = productOf(range(n, 2));
            w.add(Rule.COUNT_EXPAND_PRODUCT, factorial, product);
            w.add(Rule.COUNT_MULTIPLY, product, num(result));
        } else {
            w.add(Rule.COUNT_COMPUTE, factorial, num(result));
        }
        return w.finish(CountingMethod.PERMUTATION, factorial, result);
    }

    /** Variaciones (permutaciones parciales) de {@code k} elementos tomados de {@code n}, sin repetición: {@code n!/(n−k)!}. */
    public static Solution variations(int n, int k) {
        checkChoose(n, k);
        Work w = new Work();
        w.add(Rule.COUNT_VARIATION, null, null, "n", num(n), "k", num(k));
        Expr general = Expr.div(fact(sym("n")), fact(Expr.sub(sym("n"), sym("k"))));
        Expr substituted = quotient(List.of(fact(n)), List.of(fact(n - k)));
        w.add(Rule.COUNT_SUBSTITUTE, general, substituted);
        BigInteger result = ratio(w, substituted, n, n - k);
        return w.finish(CountingMethod.VARIATION, sym("P(" + n + ", " + k + ")"), result);
    }

    /** Variaciones con repetición: {@code nᵏ}. */
    public static Solution variationsWithRepetition(int n, int k) {
        check("n", n, 0);
        check("k", k, 0);
        Work w = new Work();
        w.add(Rule.COUNT_VARIATION_REP, null, null, "n", num(n), "k", num(k));
        Expr general = new Expr.Pow(sym("n"), sym("k"));
        Expr power = new Expr.Pow(num(n), num(k));
        w.add(Rule.COUNT_SUBSTITUTE, general, power);
        BigInteger result = BigInteger.valueOf(n).pow(k);
        if (k > 1) {
            Expr repeated = k <= SHOWN ? new Expr.Mul(java.util.Collections.nCopies(k, num(n))) : sym(n + "·" + n + "·…·" + n);
            w.add(Rule.COUNT_EXPAND_PRODUCT, power, repeated);
            w.add(Rule.COUNT_MULTIPLY, repeated, num(result));
        } else {
            w.add(Rule.COUNT_COMPUTE, power, num(result));
        }
        return w.finish(CountingMethod.VARIATION_REP, sym("PR(" + n + ", " + k + ")"), result);
    }

    /** Combinaciones de {@code k} elementos de {@code n} sin repetición: {@code n!/(k!(n−k)!)}. */
    public static Solution combinations(int n, int k) {
        checkChoose(n, k);
        Work w = new Work();
        w.add(Rule.COUNT_COMBINATION, null, null, "n", num(n), "k", num(k));
        Expr general = binomialFormula(sym("n"), sym("k"));
        Expr substituted = quotient(List.of(fact(n)), List.of(fact(k), fact(n - k)));
        w.add(Rule.COUNT_SUBSTITUTE, general, substituted);
        BigInteger result = ratio(w, substituted, n, k, n - k);
        return w.finish(CountingMethod.COMBINATION, sym("C(" + n + ", " + k + ")"), result);
    }

    /** Combinaciones con repetición de {@code k} elementos de {@code n} tipos: {@code C(n+k−1, k)}. */
    public static Solution combinationsWithRepetition(int n, int k) {
        check("n", n, 1);
        check("k", k, 0);
        int top = n + k - 1;
        Work w = new Work();
        w.add(Rule.COUNT_COMBINATION_REP, null, null, "n", num(n), "k", num(k));
        w.add(Rule.COUNT_REDUCE, sym("CR(" + n + ", " + k + ")"), sym("C(" + top + ", " + k + ")"));
        Expr general = binomialFormula(sym("a"), sym("b"));
        Expr substituted = quotient(List.of(fact(top)), List.of(fact(k), fact(top - k)));
        w.add(Rule.COUNT_SUBSTITUTE, general, substituted);
        BigInteger result = ratio(w, substituted, top, k, top - k);
        return w.finish(CountingMethod.COMBINATION_REP, sym("CR(" + n + ", " + k + ")"), result);
    }

    /** Permutaciones de {@code n} elementos con grupos de iguales de los tamaños dados: {@code n!/(n₁!·n₂!·…)}. */
    public static Solution multinomial(int n, int... groups) {
        check("n", n, 0);
        if (groups.length == 0) throw new CountingException("empty");
        long sum = 0;
        StringBuilder names = new StringBuilder();
        List<Expr> general = new ArrayList<>(List.of(fact(sym("n"))));
        List<Expr> factorials = new ArrayList<>();
        for (int i = 0; i < groups.length; i++) {
            check("n" + (i + 1), groups[i], 0);
            sum += groups[i];
            names.append(i > 0 ? ", " : "").append(groups[i]);
            factorials.add(fact(groups[i]));
        }
        if (sum != n) throw new CountingException("groups", n, names);
        List<Expr> symbolic = new ArrayList<>();
        for (int i = 0; i < groups.length; i++) symbolic.add(fact(sym("n" + (i + 1))));
        Work w = new Work();
        w.add(Rule.COUNT_MULTINOMIAL, null, null, "n", num(n), "g", sym(names.toString()));
        Expr substituted = quotient(List.of(fact(n)), factorials);
        w.add(Rule.COUNT_SUBSTITUTE, quotient(general, symbolic), substituted);
        BigInteger result = ratio(w, substituted, n, groups);
        return w.finish(CountingMethod.MULTINOMIAL, sym("M(" + n + "; " + names + ")"), result);
    }

    /** Permutaciones circulares de {@code n} elementos distintos: {@code (n−1)!}. */
    public static Solution circular(int n) {
        check("n", n, 1);
        Work w = new Work();
        w.add(Rule.COUNT_CIRCULAR, null, null, "n", num(n));
        Expr formula = fact(Expr.sub(sym("n"), Expr.ONE));
        Expr substituted = fact(n - 1);
        w.add(Rule.COUNT_SUBSTITUTE, formula, substituted);
        BigInteger result = Counting.factorial(n - 1);
        if (n - 1 > 1) {
            Expr product = productOf(range(n - 1, 2));
            w.add(Rule.COUNT_EXPAND_PRODUCT, substituted, product);
            w.add(Rule.COUNT_MULTIPLY, product, num(result));
        } else {
            w.add(Rule.COUNT_COMPUTE, substituted, num(result));
        }
        return w.finish(CountingMethod.CIRCULAR, sym("PC(" + n + ")"), result);
    }

    /** Collares de {@code n ≥ 3} elementos distintos (circulares que también se pueden dar la vuelta): {@code (n−1)!/2}. */
    public static Solution necklaces(int n) {
        check("n", n, 3);
        Work w = new Work();
        w.add(Rule.COUNT_NECKLACE, null, null, "n", num(n));
        Expr formula = quotient(List.of(fact(Expr.sub(sym("n"), Expr.ONE))), List.of(num(2)));
        Expr substituted = quotient(List.of(fact(n - 1)), List.of(num(2)));
        w.add(Rule.COUNT_SUBSTITUTE, formula, substituted);
        BigInteger factorial = Counting.factorial(n - 1);
        BigInteger result = factorial.divide(BigInteger.TWO);
        if (n - 1 > 1) {
            Expr product = productOf(range(n - 1, 2));
            Expr over = quotient(List.of(product), List.of(num(2)));
            w.add(Rule.COUNT_EXPAND_PRODUCT, substituted, over);
            w.add(Rule.COUNT_MULTIPLY, over, quotient(List.of(num(factorial)), List.of(num(2))));
        }
        w.add(Rule.COUNT_DIVIDE, quotient(List.of(num(factorial)), List.of(num(2))), num(result));
        return w.finish(CountingMethod.CIRCULAR, sym("N(" + n + ")"), result);
    }

    // ================================================================ Binomio de Newton

    /**
     * El término k-ésimo (contando desde 1) del desarrollo de {@code (a + b)ⁿ}: {@code C(n, k−1)·aⁿ⁻ᵏ⁺¹·bᵏ⁻¹}.
     */
    public static Solution binomialTerm(Expr a, Expr b, int n, int k) {
        check("n", n, 0);
        if (k < 1 || k > n + 1) throw new CountingException("term", k, n, n + 1);
        int j = k - 1;
        Expr base = new Expr.Pow(new Expr.Add(List.of(a, b)), num(n));
        Work w = new Work();
        w.add(Rule.COUNT_BINOMIAL, null, null, "k", num(k), "f", base);
        Expr general = sym("C(n, k − 1)·a^(n − k + 1)·b^(k − 1)");
        Expr substituted = termOf(sym("C(" + n + ", " + j + ")"), a, n - j, b, j);
        w.add(Rule.COUNT_SUBSTITUTE, general, substituted);
        Expr formula = quotient(List.of(fact(n)), List.of(fact(j), fact(n - j)));
        w.add(Rule.COUNT_COEFFICIENT, null, null, "f", formula);
        BigInteger coefficient = ratio(w, formula, n, j, n - j);
        Expr term = Expand.expand(Expr.mul(num(coefficient), Expr.pow(a, n - j), Expr.pow(b, j)));
        Expr withNumber = termOf(num(coefficient), a, n - j, b, j);
        if (!withNumber.toString().equals(term.toString())) w.add(Rule.COUNT_COMPUTE, withNumber, term);
        Expr problem = sym("T" + k + "[(" + a + " + " + b + ")^" + n + "]");
        return w.finishExpr(CountingMethod.BINOMIAL, problem, term, "term");
    }

    /** El desarrollo de {@code (a + b)ⁿ} con los coeficientes binomiales. */
    public static Solution binomialExpansion(Expr a, Expr b, int n) {
        check("n", n, 0);
        Expr base = new Expr.Pow(new Expr.Add(List.of(a, b)), num(n));
        Work w = new Work();
        w.add(Rule.COUNT_BINOMIAL_EXPAND, null, null, "f", base);
        List<Expr> terms = new ArrayList<>();
        List<String> coefficients = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (int j = 0; j <= n; j++) {
            terms.add(termOf(sym("C(" + n + ", " + j + ")"), a, n - j, b, j));
            coefficients.add("C(" + n + ", " + j + ")");
            values.add(Counting.combinations(n, j).toString());
        }
        Expr written = n <= 6 ? new Expr.Add(terms) : sym(String.join(" + ", terms.stream().map(Expr::toString).toList()));
        w.add(Rule.COUNT_TERMS, base, written);
        Expr row = sym(abbreviate(values, ", "));
        w.add(Rule.COUNT_COMPUTE, sym(abbreviate(coefficients, ", ")), row);
        Expr result = Expand.expand(base);
        w.add(Rule.COUNT_COMPUTE, written, result);
        return w.finishExpr(CountingMethod.BINOMIAL, base, result, "expansion");
    }

    /** {@code c·aᵖ·bᑫ} sin normalizar, sin escribir las potencias 0 y 1. */
    private static Expr termOf(Expr c, Expr a, int p, Expr b, int q) {
        List<Expr> factors = new ArrayList<>(List.of(c));
        if (p == 1) factors.add(a);
        else if (p > 1) factors.add(new Expr.Pow(a, num(p)));
        if (q == 1) factors.add(b);
        else if (q > 1) factors.add(new Expr.Pow(b, num(q)));
        return factors.size() == 1 ? c : new Expr.Mul(factors);
    }

    // ================================================================ Inclusión–exclusión

    /**
     * El tamaño de la unión de {@code m} conjuntos (de 2 a 6) a partir de los tamaños de todas las intersecciones. El valor de la
     * posición {@code mascara − 1} es el tamaño de la intersección de los conjuntos cuyos bits están en la máscara: para tres conjuntos,
     * {@code |A|, |B|, |A∩B|, |C|, |A∩C|, |B∩C|, |A∩B∩C|}.
     */
    public static Solution union(int m, long... sizes) {
        if (m < 2 || m > 6 || sizes.length != (1 << m) - 1) throw new CountingException("sets", sizes.length, m);
        for (long s : sizes) if (s < 0) throw new CountingException("negative", "|A|", s);
        for (int mask = 1; mask < sizes.length + 1; mask++) {
            if (Integer.bitCount(mask) < 2) continue;
            for (int bit = 0; bit < m; bit++) {
                if ((mask & (1 << bit)) == 0) continue;
                int part = mask & ~(1 << bit);
                if (sizes[mask - 1] > sizes[part - 1]) throw new CountingException("inconsistent", label(mask), sizes[mask - 1], sizes[part - 1]);
            }
        }
        Work w = new Work();
        w.add(Rule.COUNT_INCLUSION, null, null, "n", num(m));
        BigInteger[] levels = new BigInteger[m + 1];
        for (int j = 1; j <= m; j++) {
            List<String> labels = new ArrayList<>();
            List<String> values = new ArrayList<>();
            BigInteger level = BigInteger.ZERO;
            for (int mask = 1; mask <= sizes.length; mask++) {
                if (Integer.bitCount(mask) != j) continue;
                labels.add(label(mask));
                values.add(Long.toString(sizes[mask - 1]));
                level = level.add(BigInteger.valueOf(sizes[mask - 1]));
            }
            levels[j] = level;
            w.add(Rule.COUNT_LEVEL, sym(abbreviate(labels, " + ")), sym(abbreviate(values, " + ") + (values.size() > 1 ? " = " + level : "")), "j", num(j));
        }
        BigInteger total = BigInteger.ZERO;
        List<String> symbols = new ArrayList<>();
        List<String> signed = new ArrayList<>();
        for (int j = 1; j <= m; j++) {
            boolean plus = j % 2 == 1;
            total = plus ? total.add(levels[j]) : total.subtract(levels[j]);
            symbols.add((j == 1 ? "" : plus ? "+ " : "− ") + "S" + subscript(j));
            signed.add((j == 1 ? "" : plus ? "+ " : "− ") + levels[j]);
        }
        w.add(Rule.COUNT_COMPUTE, sym(String.join(" ", symbols)), sym(String.join(" ", signed) + " = " + total));
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < m; i++) names.append(i > 0 ? " ∪ " : "").append((char) ('A' + i));
        return w.finish(CountingMethod.INCLUSION, sym("|" + names + "|"), total);
    }

    private static String label(int mask) {
        StringBuilder out = new StringBuilder("|");
        boolean first = true;
        for (int bit = 0; bit < 6; bit++) {
            if ((mask & (1 << bit)) == 0) continue;
            out.append(first ? "" : "∩").append((char) ('A' + bit));
            first = false;
        }
        return out.append('|').toString();
    }

    private static String subscript(int j) {
        return String.valueOf("₀₁₂₃₄₅₆₇₈₉".charAt(j));
    }

    /** Desarreglos de {@code n} elementos (permutaciones sin puntos fijos): {@code n!·Σ(−1)ʲ/j!}. */
    public static Solution derangements(int n) {
        check("n", n, 0);
        Work w = new Work();
        w.add(Rule.COUNT_DERANGEMENT, null, null, "n", num(n));
        List<String> written = new ArrayList<>();
        List<String> values = new ArrayList<>();
        BigInteger total = BigInteger.ZERO;
        BigInteger factorial = Counting.factorial(n);
        for (int j = 0; j <= n; j++) {
            BigInteger term = factorial.divide(Counting.factorial(j));
            String sign = j == 0 ? "" : j % 2 == 0 ? "+ " : "− ";
            written.add(sign + n + "!/" + j + "!");
            values.add(sign + term);
            total = j % 2 == 0 ? total.add(term) : total.subtract(term);
        }
        w.add(Rule.COUNT_TERMS, sym("D(" + n + ")"), sym(abbreviate(written, " ")));
        w.add(Rule.COUNT_COMPUTE, sym(abbreviate(values, " ")), sym(total.toString()));
        return w.finish(CountingMethod.DERANGEMENT, sym("D(" + n + ")"), total);
    }

    // ================================================================ Catalan, Stirling y particiones

    /** El número de Catalan {@code Cₙ = C(2n, n)/(n+1)}. */
    public static Solution catalan(int n) {
        check("n", n, 0);
        Work w = new Work();
        w.add(Rule.COUNT_CATALAN, null, null, "n", num(n));
        Expr general = quotient(List.of(sym("C(2n, n)")), List.of(sym("(n + 1)")));
        Expr substituted = quotient(List.of(sym("C(" + 2 * n + ", " + n + ")")), List.of(num(n + 1)));
        w.add(Rule.COUNT_SUBSTITUTE, general, substituted);
        Expr formula = quotient(List.of(fact(2 * n)), List.of(fact(n), fact(n)));
        w.add(Rule.COUNT_COEFFICIENT, null, null, "f", formula);
        BigInteger central = ratio(w, formula, 2 * n, n, n);
        BigInteger result = central.divide(BigInteger.valueOf(n + 1));
        Expr division = quotient(List.of(num(central)), List.of(num(n + 1)));
        w.add(Rule.COUNT_DIVIDE, division, num(result));
        return w.finish(CountingMethod.CATALAN, sym("Cat(" + n + ")"), result);
    }

    /** El número de Stirling de segunda especie {@code S(n, k)}: formas de repartir n elementos distintos en k grupos no vacíos. */
    public static Solution stirling(int n, int k) {
        check("n", n, 0);
        check("k", k, 0);
        Work w = new Work();
        w.add(Rule.COUNT_STIRLING, null, null, "n", num(n), "k", num(k));
        List<String> written = new ArrayList<>();
        List<String> values = new ArrayList<>();
        BigInteger total = BigInteger.ZERO;
        for (int j = 0; j <= k; j++) {
            BigInteger term = Counting.combinations(k, j).multiply(BigInteger.valueOf(j).pow(n));
            boolean negative = (k - j) % 2 == 1;
            String sign = j == 0 ? (negative ? "−" : "") : negative ? "− " : "+ ";
            written.add(sign + "C(" + k + ", " + j + ")·" + j + "^" + n);
            values.add(sign + term);
            total = negative ? total.subtract(term) : total.add(term);
        }
        Expr sum = sym(abbreviate(written, " "));
        w.add(Rule.COUNT_TERMS, sym("Σ (−1)^(k − j)·C(k, j)·j^n"), sum);
        BigInteger kFactorial = Counting.factorial(k);
        w.add(Rule.COUNT_COMPUTE, sum, sym(abbreviate(values, " ") + " = " + total));
        BigInteger result = total.divide(kFactorial);
        w.add(Rule.COUNT_DIVIDE, quotient(List.of(num(total)), List.of(num(kFactorial))), num(result));
        return w.finish(CountingMethod.STIRLING, sym("S(" + n + ", " + k + ")"), result);
    }

    /** El número de particiones {@code p(n)} de un entero, con la recurrencia de los números pentagonales de Euler. */
    public static Solution partitions(int n) {
        check("n", n, 0);
        Work w = new Work();
        w.add(Rule.COUNT_PARTITIONS, null, null, "n", num(n));
        BigInteger result = Counting.partitions(n);
        if (n > 0) {
            List<String> written = new ArrayList<>();
            List<String> values = new ArrayList<>();
            BigInteger total = BigInteger.ZERO;
            for (int k = 1; k * (3 * k - 1) / 2 <= n; k++) {
                boolean plus = k % 2 == 1;
                for (int g : new int[]{k * (3 * k - 1) / 2, k * (3 * k + 1) / 2}) {
                    if (g > n) continue;
                    BigInteger value = Counting.partitions(n - g);
                    boolean first = written.isEmpty();
                    String sign = first ? (plus ? "" : "−") : plus ? "+ " : "− ";
                    written.add(sign + "p(" + (n - g) + ")");
                    values.add(sign + value);
                    total = plus ? total.add(value) : total.subtract(value);
                }
            }
            w.add(Rule.COUNT_TERMS, sym("p(" + n + ")"), sym(abbreviate(written, " ")));
            w.add(Rule.COUNT_COMPUTE, sym(abbreviate(values, " ")), sym(total.toString()));
        }
        return w.finish(CountingMethod.PARTITIONS, sym("p(" + n + ")"), result);
    }

    // ================================================================ Cálculo con factoriales

    /**
     * Calcula {@code n!/(d₁!·d₂!·…)}: desarrolla el factorial de arriba hasta el mayor de abajo, los cancela, multiplica y divide. Va
     * escribiendo los pasos; {@code before} es la fracción de partida.
     */
    private static BigInteger ratio(Work w, Expr before, int n, int... dens) {
        int largest = 0;
        for (int d : dens) largest = Math.max(largest, d);
        boolean cancels = largest > 1;
        // El mayor factorial de abajo se cancela con la cola de n!
        List<Long> numerator = range(n, cancels ? largest + 1 : 2);
        List<Long> denominator = new ArrayList<>();
        boolean removed = false;
        for (int d : dens) {
            if (cancels && !removed && d == largest) {
                removed = true;
                continue;
            }
            denominator.addAll(range(d, 2));
        }
        BigInteger top = BigInteger.ONE;
        for (long f : numerator) top = top.multiply(BigInteger.valueOf(f));
        BigInteger bottom = BigInteger.ONE;
        for (long f : denominator) bottom = bottom.multiply(BigInteger.valueOf(f));
        BigInteger result = top.divide(bottom);

        Expr expanded = fraction(numerator, denominator);
        if (!expanded.equals(before)) w.add(cancels ? Rule.COUNT_CANCEL : Rule.COUNT_EXPAND_PRODUCT, before, expanded);
        Expr multiplied = bottom.equals(BigInteger.ONE) ? num(top) : quotient(List.of(num(top)), List.of(num(bottom)));
        if (!multiplied.equals(expanded)) w.add(Rule.COUNT_MULTIPLY, expanded, multiplied);
        if (!bottom.equals(BigInteger.ONE)) w.add(Rule.COUNT_DIVIDE, multiplied, num(result));
        return result;
    }

    /** Los números de {@code from} a {@code to} en orden descendente (vacío si {@code to > from}). */
    private static List<Long> range(long from, long to) {
        List<Long> out = new ArrayList<>();
        for (long i = from; i >= to; i--) out.add(i);
        return out;
    }

    /** Un producto de enteros escrito como expresión; si es largo, abreviado con puntos suspensivos. */
    private static Expr productOf(List<Long> factors) {
        return fraction(factors, List.of());
    }

    private static Expr fraction(List<Long> numerator, List<Long> denominator) {
        if (numerator.size() + denominator.size() > SHOWN + 2) {
            String top = abbreviateNumbers(numerator);
            if (denominator.isEmpty()) return sym(top);
            return sym(top + " / (" + abbreviateNumbers(denominator) + ")");
        }
        List<Expr> up = new ArrayList<>();
        for (long f : numerator) up.add(num(f));
        List<Expr> down = new ArrayList<>();
        for (long f : denominator) down.add(num(f));
        return quotient(up, down);
    }

    private static String abbreviateNumbers(List<Long> factors) {
        if (factors.isEmpty()) return "1";
        List<String> text = new ArrayList<>();
        for (long f : factors) text.add(Long.toString(f));
        return abbreviate(text, "·");
    }

    /** Une los textos con el separador; si son muchos, deja los primeros y el último con puntos suspensivos entre medias. */
    private static String abbreviate(List<String> items, String separator) {
        if (items.size() <= SHOWN + 2) return String.join(separator, items);
        List<String> shown = new ArrayList<>(items.subList(0, 4));
        shown.add("…");
        shown.add(items.get(items.size() - 1));
        return String.join(separator, shown);
    }

    private static Expr binomialFormula(Expr n, Expr k) {
        return quotient(List.of(fact(n)), List.of(fact(k), fact(Expr.sub(n, k))));
    }

    /** {@code (a·b·…) / (c·d·…)} sin que se normalice: lo que se escribe es lo que se ve. */
    private static Expr quotient(List<Expr> numerator, List<Expr> denominator) {
        List<Expr> all = new ArrayList<>(numerator.isEmpty() ? List.of(Expr.ONE) : numerator);
        for (Expr d : denominator) all.add(new Expr.Pow(d, Expr.MINUS_ONE));
        return all.size() == 1 ? all.get(0) : new Expr.Mul(all);
    }

    private static Expr fact(Expr arg) {
        return new Expr.Func(Fn.FACTORIAL, arg);
    }

    private static Expr fact(long n) {
        return fact(num(n));
    }

    private static Expr num(long v) {
        return new Expr.Num(Rational.of(v));
    }

    private static Expr num(BigInteger v) {
        return new Expr.Num(Rational.of(v));
    }

    private static Expr sym(String name) {
        return Expr.sym(name);
    }

    // ================================================================ Comprobaciones

    private static void check(String name, int value, int minimum) {
        if (value < 0) throw new CountingException("negative", name, value);
        if (value < minimum) throw new CountingException("positive", name, value);
        if (value > MAX) throw new CountingException("tooBig", value, MAX);
    }

    private static void checkChoose(int n, int k) {
        check("n", n, 0);
        check("k", k, 0);
        if (k > n) throw new CountingException("choose", n, k);
    }

    // ================================================================ Acumulador de pasos

    private static final class Work {
        private final List<Step> steps = new ArrayList<>();

        void add(Rule rule, Expr before, Expr after, Object... params) {
            Object[] converted = params;
            steps.add(new Step(rule, before, after, Trace.params(converted), List.of()));
        }

        Solution finish(CountingMethod method, Expr problem, BigInteger result) {
            return finishExpr(method, problem, num(result), "count");
        }

        Solution finishExpr(CountingMethod method, Expr problem, Expr result, String kind) {
            add(Rule.COUNT_RESULT, null, null, "r", result);
            return new Solution(Solution.Kind.COUNTING, Solution.Status.SOLVED, problem, result, steps, "n", 1,
                    Map.of("method", Expr.sym(method.name()), "kind", Expr.sym(kind)));
        }
    }
}
