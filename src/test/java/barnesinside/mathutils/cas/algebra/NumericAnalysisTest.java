package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Una ecuación que no se puede despejar se aproxima con todos los pasos que la justifican. */
class NumericAnalysisTest {

    private static List<Rule> rules(List<Step> steps) {
        List<Rule> all = new ArrayList<>();
        for (Step s : steps) {
            all.add(s.rule());
            all.addAll(rules(s.substeps()));
        }
        return all;
    }

    @Test
    void theLogarithmicEquationShowsWhyItCannotBeSolvedAndWhereTheRootIs() {
        Solution s = Equations.solve("x^2+5x+ln(2x)=0");
        assert s.isSolved() : s.status();
        List<Rule> rules = rules(s.steps());
        for (Rule rule : List.of(Rule.ALG_NUM_WHY_MIXED, Rule.ALG_NUM_DOMAIN, Rule.ALG_NUM_FUNCTION, Rule.ALG_NUM_MONOTONE_TERMS,
                Rule.ALG_NUM_LIMITS, Rule.ALG_NUM_EXISTENCE_ONE, Rule.ALG_NUM_BRACKET, Rule.ALG_NUMERIC_ROOT, Rule.ALG_NUM_CHECK)) {
            assert rules.contains(rule) : rule + " falta en " + rules;
        }
        // En el orden en que se razona
        assert rules.indexOf(Rule.ALG_NUM_WHY_MIXED) < rules.indexOf(Rule.ALG_NUM_DOMAIN)
                && rules.indexOf(Rule.ALG_NUM_DOMAIN) < rules.indexOf(Rule.ALG_NUM_FUNCTION)
                && rules.indexOf(Rule.ALG_NUM_FUNCTION) < rules.indexOf(Rule.ALG_NUM_MONOTONE_TERMS)
                && rules.indexOf(Rule.ALG_NUM_MONOTONE_TERMS) < rules.indexOf(Rule.ALG_NUM_LIMITS)
                && rules.indexOf(Rule.ALG_NUM_LIMITS) < rules.indexOf(Rule.ALG_NUM_EXISTENCE_ONE)
                && rules.indexOf(Rule.ALG_NUM_EXISTENCE_ONE) < rules.indexOf(Rule.ALG_NUM_BRACKET)
                && rules.indexOf(Rule.ALG_NUM_BRACKET) < rules.indexOf(Rule.ALG_NUMERIC_ROOT) : rules;
        String es = s.render(Locale.of("es"), Format.PLAIN);
        String en = s.render(Locale.ENGLISH, Format.PLAIN);
        assert es.contains("2*x > 0") && es.contains("estrictamente creciente") && es.contains("Bolzano") : es;
        assert es.contains("0.18826") : es;
        assert en.contains("strictly increasing") && !en.contains("${") : en;
        assert !es.contains("${") : es;
    }

    @Test
    void aPolynomialOfDegreeFiveSaysWhy() {
        Solution s = Equations.solve("x^5-x+1=0");
        assert rules(s.steps()).contains(Rule.ALG_NUM_WHY_POLY) : rules(s.steps());
        assert s.render(Locale.of("es"), Format.PLAIN).contains("Abel");
    }

    @Test
    void anEquationWithoutRootsSaysSo() {
        Solution s = Equations.solve("exp(x)+x^2+1=0");
        assert rules(s.steps()).contains(Rule.ALG_NUM_WHY_MIXED) : rules(s.steps());
    }
}
