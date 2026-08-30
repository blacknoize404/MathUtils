package barnesinside.mathutils.cas.poly;

import barnesinside.mathutils.cas.expr.Algebra;
import barnesinside.mathutils.cas.expr.Display;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Rational;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Factorización de expresiones con coeficientes racionales.
 * <p>Aplica, en este orden y de forma recursiva:
 * <ol>
 *   <li>factor común: el máximo común divisor de los coeficientes y las potencias que comparten todos los términos,
 *       sacando el signo menos cuando el término principal es negativo</li>
 *   <li>polinomios de una variable: raíces racionales (teorema de la raíz racional) y polinomios en {@code x^k},
 *       como {@code x^4 + 3x^2 + 2}; cubre los trinomios {@code ax^2 + bx + c}</li>
 *   <li>polinomios homogéneos de dos variables: {@code x^2 + 3xy + 2y^2}</li>
 *   <li>diferencia de cuadrados, suma y diferencia de cubos, trinomio cuadrado perfecto y agrupación</li>
 * </ol>
 * La expresión se expande antes de factorizarla, así que el resultado no depende de cómo estaba escrita.
 * Cada factorización se comprueba expandiéndola de nuevo; si no coincide, se devuelve la expresión original.
 * No factoriza sobre los irracionales: {@code x^2 - 2} se queda como está.
 */
public final class Factor {

    /** Candidatos máximos que se prueban como raíz racional de un polinomio. */
    private static final int MAX_CANDIDATES = 50_000;
    /** Tamaño máximo, en bits, de los coeficientes de los que se buscan divisores. */
    private static final int MAX_BITS = 40;
    private static final int MAX_DEPTH = 24;

    private Factor() {
    }

    /** Resultado de sacar un factor común: {@code factor · quotient}. */
    public record CommonFactor(Expr factor, Expr quotient) {
    }

    /** Un polinomio como constante por polinomios primitivos de coeficientes enteros. */
    private record Factorization(Rational constant, List<Polynomial> factors) {
    }

    /**
     * Factoriza la expresión.
     *
     * @return el producto de factores, o la propia expresión si no se puede factorizar
     */
    public static Expr factor(Expr e) {
        Expr result = factor(e, 0);
        return Expand.expand(e).equals(Expand.expand(result)) ? result : e;
    }

    private static Expr factor(Expr e, int depth) {
        if (depth > MAX_DEPTH) return e;
        return switch (e) {
            case Expr.Num n -> n;
            case Expr.Const c -> c;
            case Expr.Sym s -> s;
            case Expr.Func f -> Expr.func(f.fn(), factor(f.arg(), depth + 1));
            case Expr.Pow p -> Expr.pow(factor(p.base(), depth + 1), p.exponent());
            case Expr.Mul m -> Expr.mul(m.factors().stream().map(x -> factor(x, depth + 1)).toList());
            case Expr.Add a -> factorSum(a, depth);
        };
    }

    private static Expr factorSum(Expr.Add original, int depth) {
        Expr expanded = Expand.expand(original);
        if (!(expanded instanceof Expr.Add sum)) return factor(expanded, depth + 1);

        Optional<CommonFactor> common = commonFactor(sum.terms());
        if (common.isPresent()) {
            return Expr.mul(common.get().factor(), factor(common.get().quotient(), depth + 1));
        }

        List<String> variables = List.copyOf(sum.variables());
        if (variables.size() == 1) {
            Optional<Polynomial> polynomial = Polynomial.from(sum, variables.get(0));
            if (polynomial.isPresent()) {
                Factorization f = factorPolynomial(polynomial.get(), 0);
                if (f.factors().size() > 1 || !f.constant().isOne()) {
                    List<Expr> parts = new ArrayList<>();
                    parts.add(Expr.num(f.constant()));
                    for (Polynomial p : f.factors()) parts.add(p.toExpr(variables.get(0)));
                    Expr product = Expr.mul(parts);
                    if (!product.equals(sum)) return factorPieces(product, sum, depth);
                }
            }
        } else if (variables.size() == 2) {
            List<String> sorted = variables.stream().sorted().toList();
            Optional<Expr> homogeneous = factorHomogeneous(sum, sorted.get(0), sorted.get(1));
            if (homogeneous.isPresent()) return factorPieces(homogeneous.get(), sum, depth);
        }

        Optional<Expr> pattern = factorByPattern(sum.terms());
        if (pattern.isPresent()) return factorPieces(pattern.get(), sum, depth);
        return sum;
    }

    /** Sigue factorizando los factores de un producto, salvo que no haya cambiado nada. */
    private static Expr factorPieces(Expr product, Expr.Add original, int depth) {
        if (product.equals(original)) return original;
        return factor(product, depth + 1);
    }

    // ---------------------------------------------------------------- factor común

    /**
     * El factor común de los términos de una suma: el máximo común divisor de los coeficientes y las potencias que
     * comparten todos, con el signo menos si el término principal es negativo. Vacío si no hay ninguno.
     */
    public static Optional<CommonFactor> commonFactor(List<Expr> terms) {
        List<Rational> coefficients = new ArrayList<>();
        List<Map<Expr, Integer>> powers = new ArrayList<>();
        for (Expr term : terms) {
            Rational coefficient = Rational.ONE;
            Map<Expr, Integer> map = new LinkedHashMap<>();
            for (Expr f : Algebra.factors(term)) {
                if (f instanceof Expr.Num n) {
                    coefficient = coefficient.multiply(n.value());
                } else if (f instanceof Expr.Pow p && p.exponent() instanceof Expr.Num k
                        && k.value().isInteger() && k.value().fitsInt() && k.value().signum() > 0) {
                    map.merge(p.base(), k.value().intValueExact(), Integer::sum);
                } else {
                    map.merge(f, 1, Integer::sum);
                }
            }
            coefficients.add(coefficient);
            powers.add(map);
        }

        Rational content = content(coefficients);
        if (pullsMinusSign(terms)) content = content.negate();

        List<Expr> common = new ArrayList<>();
        for (Map.Entry<Expr, Integer> entry : powers.get(0).entrySet()) {
            int least = entry.getValue();
            for (Map<Expr, Integer> other : powers) least = Math.min(least, other.getOrDefault(entry.getKey(), 0));
            if (least > 0) common.add(Expr.pow(entry.getKey(), least));
        }
        if (content.equals(Rational.ONE) && common.isEmpty()) return Optional.empty();

        List<Expr> factors = new ArrayList<>(common);
        factors.add(Expr.num(content));
        Expr factor = Expr.mul(factors);
        List<Expr> quotients = new ArrayList<>();
        for (Expr term : terms) quotients.add(Expr.div(term, factor));
        return Optional.of(new CommonFactor(factor, Expr.add(quotients)));
    }

    /**
     * Indica si hay que sacar un signo menos como factor común: cuando todos los términos son negativos o,
     * en una suma con una sola variable, cuando el término de mayor grado es negativo.
     */
    private static boolean pullsMinusSign(List<Expr> terms) {
        if (terms.stream().allMatch(Display::isNegative)) return true;

        Set<String> variables = new HashSet<>();
        for (Expr term : terms) variables.addAll(term.variables());
        if (variables.size() != 1) return false;

        Expr top = null;
        int topDegree = -1;
        boolean unique = false;
        for (Expr term : terms) {
            int degree = degree(term);
            if (degree > topDegree) {
                top = term;
                topDegree = degree;
                unique = true;
            } else if (degree == topDegree) {
                unique = false;
            }
        }
        return unique && Display.isNegative(top);
    }

    private static int degree(Expr term) {
        int total = 0;
        for (Expr f : Algebra.factors(term)) {
            if (f instanceof Expr.Sym) total += 1;
            else if (f instanceof Expr.Pow p && p.base() instanceof Expr.Sym && p.exponent() instanceof Expr.Num k
                    && k.value().isInteger() && k.value().signum() > 0 && k.value().fitsInt()) total += k.value().intValueExact();
        }
        return total;
    }

    /** Máximo común divisor de los numeradores entre el mínimo común múltiplo de los denominadores. */
    private static Rational content(List<Rational> values) {
        BigInteger numerator = BigInteger.ZERO;
        BigInteger denominator = BigInteger.ONE;
        for (Rational v : values) {
            if (v.isZero()) continue;
            numerator = numerator.gcd(v.numerator());
            denominator = denominator.multiply(v.denominator()).divide(denominator.gcd(v.denominator()));
        }
        return numerator.signum() == 0 ? Rational.ONE : Rational.of(numerator, denominator);
    }

    // ---------------------------------------------------------------- polinomios

    private static Factorization factorPolynomial(Polynomial p, int depth) {
        Rational constant = Rational.ONE;
        List<Polynomial> factors = new ArrayList<>();

        Polynomial rest = p;
        while (rest.degree() >= 1) {
            Optional<Rational> root = rationalRoot(rest);
            if (root.isEmpty()) break;
            Polynomial linear = Polynomial.of(root.get().numerator().negate().longValue(), root.get().denominator().longValue());
            rest = rest.divide(linear).quotient();
            factors.add(linear);
        }

        // Lo que queda se reduce a un polinomio primitivo con coeficiente principal positivo
        Rational scale = primitiveScale(rest);
        constant = constant.multiply(scale);
        rest = rest.scale(scale.reciprocal());

        if (rest.degree() >= 2) {
            Optional<List<Polynomial>> split = depth < MAX_DEPTH ? splitComposite(rest) : Optional.empty();
            if (split.isPresent()) {
                for (Polynomial part : split.get()) {
                    Factorization sub = factorPolynomial(part, depth + 1);
                    constant = constant.multiply(sub.constant());
                    factors.addAll(sub.factors());
                }
            } else {
                factors.add(rest);
            }
        } else if (rest.degree() == 1) {
            factors.add(rest);
        }
        return new Factorization(constant, factors);
    }

    /** Escala {@code c} tal que {@code p / c} tiene coeficientes enteros coprimos y coeficiente principal positivo. */
    private static Rational primitiveScale(Polynomial p) {
        List<Rational> coefficients = new ArrayList<>();
        for (int k = 0; k <= p.degree(); k++) coefficients.add(p.coefficient(k));
        Rational content = content(coefficients);
        return p.leading().signum() < 0 ? content.negate() : content;
    }

    /**
     * Si el polinomio solo tiene potencias múltiplos de {@code g > 1}, lo factoriza como polinomio en
     * {@code u = x^g} y devuelve los factores de vuelta en {@code x}.
     */
    private static Optional<List<Polynomial>> splitComposite(Polynomial p) {
        int g = 0;
        for (int k = 1; k <= p.degree(); k++) if (!p.coefficient(k).isZero()) g = gcd(g, k);
        if (g < 2) return Optional.empty();

        Rational[] reduced = new Rational[p.degree() / g + 1];
        for (int k = 0; k < reduced.length; k++) reduced[k] = p.coefficient(k * g);
        Factorization inner = factorPolynomial(Polynomial.of(reduced), MAX_DEPTH - 1);
        if (inner.factors().size() < 2) return Optional.empty();

        List<Polynomial> parts = new ArrayList<>();
        for (Polynomial q : inner.factors()) {
            Rational[] spread = new Rational[q.degree() * g + 1];
            java.util.Arrays.fill(spread, Rational.ZERO);
            for (int k = 0; k <= q.degree(); k++) spread[k * g] = q.coefficient(k);
            parts.add(Polynomial.of(spread));
        }
        return Optional.of(parts);
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    /** Una raíz racional del polinomio, buscada con el teorema de la raíz racional. */
    /**
     * Los candidatos a raíz racional {@code ±p/q} de un polinomio (teorema de la raíz racional), de menor a mayor.
     * Vacío si los coeficientes son demasiado grandes o los candidatos demasiados.
     */
    public static List<Rational> rationalRootCandidates(Polynomial p) {
        BigInteger lcm = BigInteger.ONE;
        for (int k = 0; k <= p.degree(); k++) {
            BigInteger d = p.coefficient(k).denominator();
            lcm = lcm.multiply(d).divide(lcm.gcd(d));
        }
        BigInteger constantTerm = p.coefficient(0).multiply(Rational.of(lcm)).numerator();
        BigInteger leading = p.leading().multiply(Rational.of(lcm)).numerator();
        if (constantTerm.signum() == 0 || constantTerm.bitLength() > MAX_BITS || leading.bitLength() > MAX_BITS) return List.of();

        List<BigInteger> numerators = divisors(constantTerm.abs());
        List<BigInteger> denominators = divisors(leading.abs());
        if ((long) numerators.size() * denominators.size() * 2 > MAX_CANDIDATES) return List.of();

        List<Rational> candidates = new ArrayList<>();
        for (BigInteger n : numerators) {
            for (BigInteger d : denominators) {
                if (!n.gcd(d).equals(BigInteger.ONE)) continue;
                candidates.add(Rational.of(n, d));
                candidates.add(Rational.of(n, d).negate());
            }
        }
        candidates.sort(null);
        return candidates;
    }

    /** Una raíz racional del polinomio, o vacío si no tiene. */
    public static Optional<Rational> rationalRoot(Polynomial p) {
        BigInteger lcm = BigInteger.ONE;
        for (int k = 0; k <= p.degree(); k++) {
            BigInteger d = p.coefficient(k).denominator();
            lcm = lcm.multiply(d).divide(lcm.gcd(d));
        }
        BigInteger constantTerm = p.coefficient(0).multiply(Rational.of(lcm)).numerator();
        BigInteger leading = p.leading().multiply(Rational.of(lcm)).numerator();

        if (constantTerm.signum() == 0) return Optional.of(Rational.ZERO);
        if (constantTerm.bitLength() > MAX_BITS || leading.bitLength() > MAX_BITS) return Optional.empty();

        List<BigInteger> numerators = divisors(constantTerm.abs());
        List<BigInteger> denominators = divisors(leading.abs());
        if ((long) numerators.size() * denominators.size() * 2 > MAX_CANDIDATES) return Optional.empty();

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
        List<BigInteger> small = new ArrayList<>();
        List<BigInteger> large = new ArrayList<>();
        long value = n.longValueExact();
        for (long d = 1; d * d <= value; d++) {
            if (value % d != 0) continue;
            small.add(BigInteger.valueOf(d));
            if (d != value / d) large.add(0, BigInteger.valueOf(value / d));
        }
        small.addAll(large);
        return small;
    }

    // ---------------------------------------------------------------- homogéneos de dos variables

    /**
     * {@code S(x, y)} homogéneo de grado {@code d} se factoriza como el polinomio {@code S(t, 1)} y se
     * devuelve cada factor a su forma homogénea.
     */
    private static Optional<Expr> factorHomogeneous(Expr.Add sum, String x, String y) {
        int degree = -1;
        for (Expr term : sum.terms()) {
            int total = 0;
            for (Expr f : Algebra.factors(term)) {
                if (f instanceof Expr.Num) continue;
                if (f instanceof Expr.Sym) total += 1;
                else if (f instanceof Expr.Pow p && p.base() instanceof Expr.Sym && p.exponent() instanceof Expr.Num k
                        && k.value().isInteger() && k.value().signum() > 0 && k.value().fitsInt()) total += k.value().intValueExact();
                else return Optional.empty();
            }
            if (degree >= 0 && total != degree) return Optional.empty();
            degree = total;
        }

        Optional<Polynomial> dehomogenized = Polynomial.from(sum.substitute(y, Expr.ONE), x);
        if (dehomogenized.isEmpty() || dehomogenized.get().degree() != degree) return Optional.empty();

        Factorization f = factorPolynomial(dehomogenized.get(), 0);
        if (f.factors().size() < 2) return Optional.empty();

        List<Expr> parts = new ArrayList<>();
        parts.add(Expr.num(f.constant()));
        for (Polynomial q : f.factors()) {
            List<Expr> terms = new ArrayList<>();
            for (int k = 0; k <= q.degree(); k++) {
                terms.add(Expr.mul(Expr.num(q.coefficient(k)), Expr.pow(Expr.sym(x), k), Expr.pow(Expr.sym(y), q.degree() - k)));
            }
            parts.add(Expr.add(terms));
        }
        return Optional.of(Expr.mul(parts));
    }

    // ---------------------------------------------------------------- identidades notables

    private static Optional<Expr> factorByPattern(List<Expr> terms) {
        return switch (terms.size()) {
            case 2 -> differenceOfSquares(terms).or(() -> sumOfCubes(terms));
            case 3 -> perfectSquareTrinomial(terms);
            case 4 -> grouping(terms);
            default -> Optional.empty();
        };
    }

    /** {@code a^2 - b^2 = (a - b)(a + b)}. */
    private static Optional<Expr> differenceOfSquares(List<Expr> terms) {
        for (int i = 0; i < 2; i++) {
            Expr positive = terms.get(i);
            Expr negative = terms.get(1 - i);
            if (!Display.isNegative(negative) || Display.isNegative(positive)) continue;
            Optional<Expr> a = exactRoot(positive, 2);
            Optional<Expr> b = exactRoot(Expr.neg(negative), 2);
            if (a.isPresent() && b.isPresent()) {
                return Optional.of(Expr.mul(Expr.sub(a.get(), b.get()), Expr.add(a.get(), b.get())));
            }
        }
        return Optional.empty();
    }

    /** {@code a^3 + b^3 = (a + b)(a^2 - ab + b^2)}; con {@code b} negativo es la diferencia de cubos. */
    private static Optional<Expr> sumOfCubes(List<Expr> terms) {
        Optional<Expr> a = exactRoot(terms.get(0), 3);
        Optional<Expr> b = exactRoot(terms.get(1), 3);
        if (a.isEmpty() || b.isEmpty()) return Optional.empty();
        Expr second = Expr.add(Expr.pow(a.get(), 2), Expr.neg(Expr.mul(a.get(), b.get())), Expr.pow(b.get(), 2));
        return Optional.of(Expr.mul(Expr.add(a.get(), b.get()), second));
    }

    /** {@code a^2 ± 2ab + b^2 = (a ± b)^2}. */
    private static Optional<Expr> perfectSquareTrinomial(List<Expr> terms) {
        for (int middle = 0; middle < 3; middle++) {
            Expr first = terms.get((middle + 1) % 3);
            Expr last = terms.get((middle + 2) % 3);
            if (Display.isNegative(first) || Display.isNegative(last)) continue;
            Optional<Expr> a = exactRoot(first, 2);
            Optional<Expr> b = exactRoot(last, 2);
            if (a.isEmpty() || b.isEmpty()) continue;

            Expr cross = Expand.expand(Expr.mul(Expr.num(2), a.get(), b.get()));
            Expr actual = terms.get(middle);
            if (actual.equals(cross)) return Optional.of(Expr.pow(Expr.add(a.get(), b.get()), 2));
            if (actual.equals(Expr.neg(cross))) return Optional.of(Expr.pow(Expr.sub(a.get(), b.get()), 2));
        }
        return Optional.empty();
    }

    /** Agrupa los cuatro términos de dos en dos buscando un binomio común: {@code ax + ay + bx + by = (a + b)(x + y)}. */
    private static Optional<Expr> grouping(List<Expr> terms) {
        int[][] pairings = {{0, 1, 2, 3}, {0, 2, 1, 3}, {0, 3, 1, 2}};
        for (int[] p : pairings) {
            CommonFactor left = groupOf(terms.get(p[0]), terms.get(p[1]));
            CommonFactor right = groupOf(terms.get(p[2]), terms.get(p[3]));
            if (left.factor().isOne() && right.factor().isOne()) continue;

            Expr q1 = left.quotient();
            Expr q2 = right.quotient();
            if (q1.equals(q2)) return Optional.of(Expr.mul(q1, Expr.add(left.factor(), right.factor())));
            if (q1.equals(Expand.expand(Expr.neg(q2)))) {
                return Optional.of(Expr.mul(q1, Expr.sub(left.factor(), right.factor())));
            }
        }
        return Optional.empty();
    }

    /** El factor común de un par de términos, o el par entero (con factor 1) si no comparten nada. */
    private static CommonFactor groupOf(Expr first, Expr second) {
        List<Expr> pair = List.of(first, second);
        return commonFactor(pair).orElseGet(() -> new CommonFactor(Expr.ONE, Expr.add(pair)));
    }

    // ---------------------------------------------------------------- raíces exactas

    /** La raíz {@code n}-ésima exacta de la expresión, si existe como producto de potencias y números. */
    public static Optional<Expr> exactRoot(Expr e, int n) {
        return switch (e) {
            case Expr.Num num -> rationalRoot(num.value(), n).map(Expr::num);
            case Expr.Pow p when p.exponent() instanceof Expr.Num k && k.value().isInteger() && k.value().fitsInt()
                    && k.value().intValueExact() % n == 0 ->
                    Optional.of(Expr.pow(p.base(), k.value().intValueExact() / n));
            case Expr.Mul m -> {
                List<Expr> roots = new ArrayList<>();
                for (Expr f : m.factors()) {
                    Optional<Expr> r = exactRoot(f, n);
                    if (r.isEmpty()) yield Optional.empty();
                    roots.add(r.get());
                }
                yield Optional.of(Expr.mul(roots));
            }
            default -> Optional.empty();
        };
    }

    private static Optional<Rational> rationalRoot(Rational r, int n) {
        if (r.signum() < 0 && n % 2 == 0) return Optional.empty();
        Optional<BigInteger> numerator = integerRoot(r.numerator().abs(), n);
        Optional<BigInteger> denominator = integerRoot(r.denominator(), n);
        if (numerator.isEmpty() || denominator.isEmpty()) return Optional.empty();
        Rational root = Rational.of(numerator.get(), denominator.get());
        return Optional.of(r.signum() < 0 ? root.negate() : root);
    }

    private static Optional<BigInteger> integerRoot(BigInteger value, int n) {
        BigInteger low = BigInteger.ZERO;
        BigInteger high = BigInteger.ONE.shiftLeft(value.bitLength() / n + 1);
        while (low.compareTo(high) <= 0) {
            BigInteger middle = low.add(high).shiftRight(1);
            int comparison = middle.pow(n).compareTo(value);
            if (comparison == 0) return Optional.of(middle);
            if (comparison < 0) low = middle.add(BigInteger.ONE);
            else high = middle.subtract(BigInteger.ONE);
        }
        return Optional.empty();
    }
}
