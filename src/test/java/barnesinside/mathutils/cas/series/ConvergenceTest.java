package barnesinside.mathutils.cas.series;

import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Messages;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Los criterios de convergencia se aplican en su orden, dan el veredicto correcto y lo explican en los dos idiomas. */
class ConvergenceTest {

    private static final Locale ES = Locale.forLanguageTag("es");

    private static Solution test(String term) {
        return Convergence.test(term, "n", "1");
    }

    private static List<Rule> rules(List<Step> steps) {
        List<Rule> all = new ArrayList<>();
        for (Step s : steps) {
            all.add(s.rule());
            all.addAll(rules(s.substeps()));
        }
        return all;
    }

    private static void converges(String term, Rule... used) {
        Solution s = test(term);
        assert Convergence.verdict(s) == Convergence.Verdict.CONVERGES : term + " " + s.status() + " " + rules(s.steps());
        for (Rule r : used) assert rules(s.steps()).contains(r) : term + " sin " + r + " en " + rules(s.steps());
    }

    private static void diverges(String term, Rule... used) {
        Solution s = test(term);
        assert Convergence.verdict(s) == Convergence.Verdict.DIVERGES : term + " " + s.status() + " " + rules(s.steps());
        assert s.status() == Solution.Status.DOES_NOT_EXIST;
        for (Rule r : used) assert rules(s.steps()).contains(r) : term + " sin " + r + " en " + rules(s.steps());
    }

    @Test
    void theRatioTestComesFirstAndDecidesFactorialsAndExponentials() {
        converges("2^n/n!", Rule.SERIES_RATIO_CONVERGES);
        converges("n^2/3^n", Rule.SERIES_RATIO_CONVERGES);
        converges("n!/n^n", Rule.SERIES_RATIO_CONVERGES);
        diverges("n!/2^n", Rule.SERIES_RATIO_DIVERGES);
        diverges("n^n/n!", Rule.SERIES_RATIO_DIVERGES);
        diverges("3^n/n^3", Rule.SERIES_RATIO_DIVERGES);
        // El cociente es siempre el primer criterio
        for (String term : new String[]{"2^n/n!", "1/n^2", "(-1)^n/n"}) {
            List<Rule> used = rules(test(term).steps());
            int ratio = used.indexOf(Rule.SERIES_RATIO);
            assert ratio >= 0 && ratio == 1 + (term.startsWith("(-1)") ? 1 : 0) : used;
        }
    }

    @Test
    void theRootTestFollowsWhenTheRatioDoesNotDecide() {
        Solution s = test("(n/(2*n+1))^n");
        assert Convergence.verdict(s) == Convergence.Verdict.CONVERGES;
        List<Rule> used = rules(s.steps());
        assert used.indexOf(Rule.SERIES_RATIO_INCONCLUSIVE) < used.indexOf(Rule.SERIES_ROOT) : used;
        assert used.contains(Rule.SERIES_ROOT_CONVERGES) : used;
    }

    @Test
    void aGeometricSeriesKnowsItsSum() {
        Solution s = test("1/2^n");
        assert s.isSolved() && Math.abs(Eval.value(s.result()) - 1) < 1e-12 : s.result();
        assert rules(s.steps()).contains(Rule.SERIES_GEOMETRIC_SUM);
        Solution alternating = test("(-1)^n/2^n");
        assert alternating.isSolved() && Math.abs(Eval.value(alternating.result()) + 1.0 / 3) < 1e-12 : alternating.result();
        Solution fromZero = Convergence.test("(1/3)^n", "n", "0");
        assert fromZero.isSolved() && Math.abs(Eval.value(fromZero.result()) - 1.5) < 1e-12 : fromZero.result();
    }

    @Test
    void theNecessaryConditionCatchesTermsThatDoNotVanish() {
        diverges("(1+1/n)^n", Rule.SERIES_NTH_DIVERGES);
        diverges("(n/(n+1))^n", Rule.SERIES_NTH_DIVERGES);
        diverges("(-1)^n*n/(n+1)", Rule.SERIES_NTH_DIVERGES);
    }

    @Test
    void pSeriesAreComparedByTheirExponent() {
        converges("1/n^2", Rule.SERIES_COMPARE_CONVERGES);
        converges("1/(n^2+n)", Rule.SERIES_COMPARE_CONVERGES);
        converges("1/n^(3/2)", Rule.SERIES_COMPARE_CONVERGES);
        diverges("1/n", Rule.SERIES_COMPARE_DIVERGES);
        diverges("1/sqrt(n)", Rule.SERIES_COMPARE_DIVERGES);
        diverges("n/(n^2+1)", Rule.SERIES_COMPARE_DIVERGES);
    }

    @Test
    void aBoundedNumeratorIsComparedWithItsBound() {
        converges("sin(n)/n^2", Rule.SERIES_BOUNDED);
        converges("cos(n)/n^3", Rule.SERIES_BOUNDED);
    }

    @Test
    void theIntegralTestDecidesWhatComparisonCannot() {
        diverges("1/(n*ln(n)^2)", Rule.SERIES_INTEGRAL, Rule.SERIES_INTEGRAL_DIVERGES);
        converges("ln(n)/n^2");
    }

    @Test
    void leibnizGivesConditionalConvergenceAndAbsoluteWinsWhenItCan() {
        Solution conditional = test("(-1)^n/n");
        assert Convergence.verdict(conditional) == Convergence.Verdict.CONVERGES;
        assert rules(conditional.steps()).contains(Rule.SERIES_LEIBNIZ_CONVERGES);
        assert rules(conditional.steps()).contains(Rule.SERIES_CONCLUSION_CONDITIONAL);
        assert Eval.value(conditional.extras().get("absolute")) == 0;
        assert conditional.extras().containsKey("approximation") && Math.abs(Eval.value(conditional.extras().get("approximation")) + Math.log(2)) < 1e-2;

        Solution absolute = test("(-1)^n/n^2");
        assert Convergence.verdict(absolute) == Convergence.Verdict.CONVERGES;
        assert rules(absolute.steps()).contains(Rule.SERIES_CONCLUSION_ABSOLUTE);
        assert !rules(absolute.steps()).contains(Rule.SERIES_LEIBNIZ);
        assert Eval.value(absolute.extras().get("absolute")) == 1;
    }

    @Test
    void whenNoTestDecidesItSaysSo() {
        Solution s = test("1/(n*ln(n+1))");
        assert Convergence.verdict(s) == Convergence.Verdict.UNDECIDED : s.status();
        assert s.status() == Solution.Status.UNSOLVED;
        assert rules(s.steps()).contains(Rule.SERIES_CONCLUSION_UNDECIDED);
    }

    @Test
    void theFirstIndexIsRespected() {
        // 1/(n-3)^2 desde n = 4 es la serie de 1/k^2
        Solution s = Convergence.test("1/(n-3)^2", "n", "4");
        assert Convergence.verdict(s) == Convergence.Verdict.CONVERGES : s.status();
        boolean failed = false;
        try {
            Convergence.test("1/n", "n", "1/2");
        } catch (IllegalArgumentException expected) {
            failed = true;
        }
        assert failed;
    }

    @Test
    void everyStepIsExplainedInSpanishAndEnglish() {
        List<Rule> series = new ArrayList<>();
        for (Rule r : Rule.values()) {
            String n = r.name();
            if (n.startsWith("SERIES_") || n.startsWith("TAYLOR_") || n.startsWith("LAURENT_") || n.startsWith("FOURIER_") || n.startsWith("EXPANSION_")) series.add(r);
        }
        assert series.size() > 40 : series.size();
        for (Rule r : series) {
            for (Locale locale : Messages.supportedLocales()) {
                assert Messages.has(r.titleKey(), locale) : r.titleKey() + " " + locale;
                assert Messages.has(r.textKey(), locale) : r.textKey() + " " + locale;
            }
        }
        for (Solution.Kind kind : new Solution.Kind[]{Solution.Kind.SERIES, Solution.Kind.TAYLOR, Solution.Kind.LAURENT, Solution.Kind.FOURIER}) {
            for (Locale locale : Messages.supportedLocales()) {
                assert Messages.has("solution.kind." + kind.name() + ".problem", locale) : kind + " " + locale;
            }
        }
        for (String key : new String[]{"solution.status.NON_ELEMENTAL.SERIES", "solution.status.DOES_NOT_EXIST.SERIES", "solution.status.UNSOLVED.SERIES"}) {
            for (Locale locale : Messages.supportedLocales()) assert Messages.has(key, locale) : key + " " + locale;
        }
    }

    @Test
    void theSolutionRendersInBothLanguages() {
        Solution s = test("n!/2^n");
        for (Locale locale : Messages.supportedLocales()) {
            for (Format format : Format.values()) {
                String text = s.render(locale, format);
                assert !text.isBlank() && !text.contains("${") : locale + " " + format;
            }
        }
        assert s.render(ES, Format.PLAIN).contains("diverge");
        assert s.render(Locale.ENGLISH, Format.PLAIN).contains("diverges");
    }
}
