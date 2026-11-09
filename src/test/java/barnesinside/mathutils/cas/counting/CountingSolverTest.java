package barnesinside.mathutils.cas.counting;

import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import barnesinside.mathutils.discrete.Counting;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;

class CountingSolverTest {

    private static final Locale ES = Locale.forLanguageTag("es");

    /** Comprueba que la solución llega al número esperado y que sus pasos terminan en él, con textos en los dos idiomas. */
    private static void expect(Solution s, long expected, CountingMethod method) {
        expect(s, BigInteger.valueOf(expected), method);
    }

    private static void expect(Solution s, BigInteger expected, CountingMethod method) {
        assert s.isSolved() && s.kind() == Solution.Kind.COUNTING : s;
        assert s.result().equals(new Expr.Num(Rational.of(expected))) : s.result() + " en vez de " + expected;
        assert s.extras().get("method").equals(Expr.sym(method.name())) : s.extras();
        List<Step> steps = s.steps();
        assert steps.size() >= 2 : "debe tener al menos el método y la conclusión";
        assert steps.get(steps.size() - 1).rule() == Rule.COUNT_RESULT && steps.get(steps.size() - 1).params().get("r").equals(s.result());
        // El último cálculo termina en el resultado
        Step last = steps.get(steps.size() - 2);
        assert last.after() == null || last.after().toString().endsWith(expected.toString()) : last.after();
        for (Step step : steps) {
            assert !step.title(ES).isBlank() && !step.title(Locale.ENGLISH).isBlank();
            assert !step.explanation(ES).isEmpty() && !step.explanation(Locale.ENGLISH).isEmpty();
        }
        assert s.render(ES, Format.PLAIN).contains(expected.toString()) && s.render(Locale.ENGLISH, Format.LATEX).contains(expected.toString());
    }

    @Test
    void sumAndProductPrinciples() {
        expect(CountingSolver.sum(3, 5, 7), 15, CountingMethod.SUM);
        expect(CountingSolver.sum(4), 4, CountingMethod.SUM);
        expect(CountingSolver.product(3, 4, 5), 60, CountingMethod.PRODUCT);
        expect(CountingSolver.product(2, 2, 2, 2), 16, CountingMethod.PRODUCT);
    }

    @Test
    void permutationsMatchTheFactorial() {
        for (int n : new int[]{0, 1, 2, 5, 10, 25}) expect(CountingSolver.permutations(n), Counting.factorial(n), CountingMethod.PERMUTATION);
        expect(CountingSolver.permutations(5), 120, CountingMethod.PERMUTATION);
    }

    @Test
    void variationsWithAndWithoutRepetition() {
        expect(CountingSolver.variations(8, 3), 336, CountingMethod.VARIATION);
        expect(CountingSolver.variations(5, 5), 120, CountingMethod.VARIATION);
        expect(CountingSolver.variations(7, 0), 1, CountingMethod.VARIATION);
        for (int n = 0; n <= 12; n++) for (int k = 0; k <= n; k++) expect(CountingSolver.variations(n, k), Counting.permutations(n, k), CountingMethod.VARIATION);
        expect(CountingSolver.variationsWithRepetition(5, 3), 125, CountingMethod.VARIATION_REP);
        expect(CountingSolver.variationsWithRepetition(2, 10), 1024, CountingMethod.VARIATION_REP);
        expect(CountingSolver.variationsWithRepetition(3, 1), 3, CountingMethod.VARIATION_REP);
        expect(CountingSolver.variationsWithRepetition(10, 0), 1, CountingMethod.VARIATION_REP);
    }

    @Test
    void combinationsWithAndWithoutRepetition() {
        expect(CountingSolver.combinations(10, 3), 120, CountingMethod.COMBINATION);
        expect(CountingSolver.combinations(5, 0), 1, CountingMethod.COMBINATION);
        expect(CountingSolver.combinations(6, 6), 1, CountingMethod.COMBINATION);
        expect(CountingSolver.combinations(50, 25), Counting.combinations(50, 25), CountingMethod.COMBINATION);
        for (int n = 0; n <= 12; n++) for (int k = 0; k <= n; k++) expect(CountingSolver.combinations(n, k), Counting.combinations(n, k), CountingMethod.COMBINATION);
        expect(CountingSolver.combinationsWithRepetition(4, 3), 20, CountingMethod.COMBINATION_REP);
        expect(CountingSolver.combinationsWithRepetition(5, 2), 15, CountingMethod.COMBINATION_REP);
        expect(CountingSolver.combinationsWithRepetition(1, 4), 1, CountingMethod.COMBINATION_REP);
        expect(CountingSolver.combinationsWithRepetition(3, 0), 1, CountingMethod.COMBINATION_REP);
    }

    @Test
    void combinationsExplainTheFormulaAndSimplifyTheFactorials() {
        String text = CountingSolver.combinations(10, 3).render(ES, Format.PLAIN);
        assert text.contains("10!/(3!*7!)") && text.contains("720/6") && text.contains("= 120") : text;
    }

    @Test
    void multinomialAndCircularPermutations() {
        expect(CountingSolver.multinomial(11, 4, 4, 2, 1), 34650, CountingMethod.MULTINOMIAL);
        expect(CountingSolver.multinomial(6, 3, 2, 1), 60, CountingMethod.MULTINOMIAL);
        expect(CountingSolver.multinomial(4, 4), 1, CountingMethod.MULTINOMIAL);
        expect(CountingSolver.multinomial(5, 2, 3), Counting.combinations(5, 2), CountingMethod.MULTINOMIAL);
        expect(CountingSolver.circular(6), 120, CountingMethod.CIRCULAR);
        expect(CountingSolver.circular(1), 1, CountingMethod.CIRCULAR);
        expect(CountingSolver.circular(10), Counting.factorial(9), CountingMethod.CIRCULAR);
        expect(CountingSolver.necklaces(6), 60, CountingMethod.CIRCULAR);
        expect(CountingSolver.necklaces(3), 1, CountingMethod.CIRCULAR);
    }

    @Test
    void binomialTheorem() {
        Solution term = CountingSolver.binomialTerm(Expr.sym("x"), Expr.sym("y"), 6, 3);
        assert term.result().equals(ExprParser.parse("15*x^4*y^2")) : term.result();
        assert term.steps().get(term.steps().size() - 1).rule() == Rule.COUNT_RESULT;
        Solution first = CountingSolver.binomialTerm(Expr.sym("x"), Expr.num(2), 5, 1);
        assert first.result().equals(ExprParser.parse("x^5")) : first.result();
        Solution last = CountingSolver.binomialTerm(Expr.sym("x"), Expr.num(2), 5, 6);
        assert last.result().equals(Expr.num(32)) : last.result();
        Solution middle = CountingSolver.binomialTerm(ExprParser.parse("2*x"), ExprParser.parse("-3"), 4, 3);
        assert middle.result().equals(ExprParser.parse("216*x^2")) : middle.result();
        Solution expansion = CountingSolver.binomialExpansion(Expr.sym("x"), Expr.sym("y"), 4);
        assert Expand.expand(Expr.sub(expansion.result(), ExprParser.parse("x^4+4*x^3*y+6*x^2*y^2+4*x*y^3+y^4"))).isZero() : expansion.result();
        Solution minus = CountingSolver.binomialExpansion(Expr.sym("x"), ExprParser.parse("-1"), 3);
        assert Expand.expand(Expr.sub(minus.result(), ExprParser.parse("x^3-3*x^2+3*x-1"))).isZero() : minus.result();
        assert CountingSolver.binomialExpansion(Expr.sym("x"), Expr.sym("y"), 10).isSolved();
    }

    @Test
    void inclusionExclusion() {
        // Tres conjuntos: 10 + 8 + 6 - 3 - 2 - 1 + 1 = 19
        expect(CountingSolver.union(3, 10, 8, 3, 6, 2, 1, 1), 19, CountingMethod.INCLUSION);
        // Dos conjuntos disjuntos: la unión es la suma
        expect(CountingSolver.union(2, 4, 5, 0), 9, CountingMethod.INCLUSION);
        // Los múltiplos de 2, 3 o 5 hasta 30: 15 + 10 + 6 - 5 - 3 - 2 + 1 = 22
        expect(CountingSolver.union(3, 15, 10, 5, 6, 3, 2, 1), 22, CountingMethod.INCLUSION);
    }

    @Test
    void derangementsCatalanStirlingAndPartitions() {
        expect(CountingSolver.derangements(4), 9, CountingMethod.DERANGEMENT);
        expect(CountingSolver.derangements(0), 1, CountingMethod.DERANGEMENT);
        expect(CountingSolver.derangements(1), 0, CountingMethod.DERANGEMENT);
        expect(CountingSolver.derangements(5), 44, CountingMethod.DERANGEMENT);
        expect(CountingSolver.derangements(10), 1334961, CountingMethod.DERANGEMENT);
        for (int n = 0; n <= 14; n++) expect(CountingSolver.catalan(n), Counting.catalan(n), CountingMethod.CATALAN);
        expect(CountingSolver.catalan(5), 42, CountingMethod.CATALAN);
        expect(CountingSolver.stirling(5, 3), 25, CountingMethod.STIRLING);
        expect(CountingSolver.stirling(4, 2), 7, CountingMethod.STIRLING);
        expect(CountingSolver.stirling(0, 0), 1, CountingMethod.STIRLING);
        expect(CountingSolver.stirling(3, 5), 0, CountingMethod.STIRLING);
        for (int n = 0; n <= 9; n++) for (int k = 0; k <= 9; k++) expect(CountingSolver.stirling(n, k), Counting.stirlingSecond(n, k), CountingMethod.STIRLING);
        expect(CountingSolver.partitions(7), 15, CountingMethod.PARTITIONS);
        expect(CountingSolver.partitions(0), 1, CountingMethod.PARTITIONS);
        for (int n = 0; n <= 60; n += 7) expect(CountingSolver.partitions(n), Counting.partitions(n), CountingMethod.PARTITIONS);
    }

    @Test
    void longProductsAreAbbreviated() {
        String text = CountingSolver.permutations(25).render(ES, Format.PLAIN);
        assert text.contains("25·24·23") && text.contains("…") && text.contains("15511210043330985984000000") : text;
    }

    @Test
    void badDataIsExplainedInBothLanguages() {
        try {
            CountingSolver.combinations(3, 5);
            assert false : "no se pueden elegir 5 de 3";
        } catch (CountingException e) {
            assert e.key().equals("choose") && e.message(ES).contains("no puede ser mayor") && e.message(Locale.ENGLISH).contains("cannot be greater") : e.getMessage();
        }
        try {
            CountingSolver.permutations(-2);
            assert false;
        } catch (CountingException e) {
            assert e.message(ES).contains("negativo") && e.message(Locale.ENGLISH).contains("negative");
        }
        try {
            CountingSolver.multinomial(7, 3, 2);
            assert false;
        } catch (CountingException e) {
            assert e.message(ES).contains("deben sumar") && e.message(Locale.ENGLISH).contains("add up");
        }
        try {
            CountingSolver.permutations(CountingSolver.MAX + 1);
            assert false;
        } catch (CountingException e) {
            assert e.message(ES).contains("demasiado grande") && e.message(Locale.ENGLISH).contains("too large");
        }
        try {
            CountingSolver.union(3, 1, 2, 5, 1, 1, 1, 1);
            assert false : "A∩B no puede ser mayor que A";
        } catch (CountingException e) {
            assert e.message(ES).contains("intersección") && e.message(Locale.ENGLISH).contains("intersection");
        }
        try {
            CountingSolver.binomialTerm(Expr.sym("x"), Expr.sym("y"), 4, 7);
            assert false;
        } catch (CountingException e) {
            assert e.key().equals("term");
        }
        try {
            CountingSolver.sum();
            assert false;
        } catch (CountingException e) {
            assert e.key().equals("empty");
        }
        try {
            CountingSolver.combinationsWithRepetition(0, 3);
            assert false;
        } catch (CountingException e) {
            assert e.key().equals("positive");
        }
    }

    @Test
    void theAdvisorPicksTheMethodAndExplainsWhy() {
        // orden, repetición, iguales, todos, círculo
        assert advise(true, false, false, true, false) == CountingMethod.PERMUTATION;
        assert advise(true, false, false, false, false) == CountingMethod.VARIATION;
        assert advise(true, true, false, false, false) == CountingMethod.VARIATION_REP;
        assert advise(false, false, false, false, false) == CountingMethod.COMBINATION;
        assert advise(false, true, false, false, false) == CountingMethod.COMBINATION_REP;
        assert advise(true, false, true, true, false) == CountingMethod.MULTINOMIAL;
        assert advise(true, false, false, true, true) == CountingMethod.CIRCULAR;
        assert advise(false, false, false, true, false) == CountingMethod.COMBINATION;
        boolean[] both = {true, false};
        for (boolean a : both) for (boolean b : both) for (boolean c : both) for (boolean d : both) for (boolean e : both) {
            CountingAdvisor.Advice advice = CountingAdvisor.advise(new CountingAdvisor.Question(a, b, c, d, e));
            assert !advice.reason(ES).isBlank() && !advice.reason(Locale.ENGLISH).isBlank();
            assert !advice.method().title(ES).isBlank() && !advice.method().usage(Locale.ENGLISH).isBlank();
        }
    }

    private static CountingMethod advise(boolean order, boolean repetition, boolean identical, boolean all, boolean circular) {
        return CountingAdvisor.advise(new CountingAdvisor.Question(order, repetition, identical, all, circular)).method();
    }

    @Test
    void everyMethodHasANameInBothLanguages() {
        for (CountingMethod m : CountingMethod.values()) {
            assert !m.title(ES).isBlank() && !m.title(Locale.ENGLISH).isBlank() && !m.usage(ES).isBlank() && !m.usage(Locale.ENGLISH).isBlank() : m;
        }
    }
}
