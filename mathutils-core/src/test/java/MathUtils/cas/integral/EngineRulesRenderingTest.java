package MathUtils.cas.integral;

import MathUtils.cas.JsonValidator;
import MathUtils.cas.steps.Format;
import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Solution;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import static MathUtils.cas.integral.IntegralChecks.flatten;

/**
 * Los pasos de rational, trigonométricas y radicales se muestran completos en los dos idiomas y en
 * todos los formatos, y el corpus ejercita cada regla nueva al menos una vez.
 */
class EngineRulesRenderingTest {

    private static final String[] CORPUS = {
            "1/(x^2-3x+2)", "(3x+2)/(x^2+x+1)", "1/(x^2+2x+5)", "1/(x^2+1)^2", "1/(x^2-2)", "(x^2+1)/((x-1)*(x^2+x+1))",
            "sin(x)^3", "sin(x)^2*cos(x)^2", "1/(sin(x)^2*cos(x)^2)", "tan(x)^4", "cot(x)^3", "sec(x)^3", "csc(x)^3", "tan(x)^2*sec(x)^2",
            "sin(3x)*cos(2x)", "1/(2+sin(x))",
            "1/(sqrt(x)+x^(1/3))", "x^2*sqrt(1-x^2)", "1/(x^2*sqrt(x^2+1))", "1/(x^2*sqrt(x^2-1))",
            "sqrt(x^2+2x+2)", "sqrt(1-x^2)", "sqrt(x^2+1)"
    };

    @Test
    void textsAreCompleteInEveryLanguageAndFormat() {
        Set<Rule> used = EnumSet.noneOf(Rule.class);
        for (String f : CORPUS) {
            Solution s = Integrals.indefinite(f, "x");
            assert s.isSolved() : f + " -> " + s.status();
            flatten(s.steps()).forEach(step -> used.add(step.rule()));
            for (Locale locale : new Locale[]{Locale.forLanguageTag("es"), Locale.ENGLISH}) {
                for (Format format : Format.values()) {
                    String text = s.render(locale, format);
                    assert !text.contains("${") : f + " (" + locale + ", " + format + "): " + text;
                }
                JsonValidator.assertValid(s.render(locale, Format.JSON));
            }
        }

        for (Rule rule : new Rule[]{Rule.INT_PARTIAL_FRACTIONS, Rule.INT_QUADRATIC_SPLIT, Rule.INT_COMPLETE_SQUARE,
                Rule.INT_REDUCTION, Rule.INT_TRIG_ODD_POWER, Rule.INT_TRIG_HALF_ANGLE,
                Rule.INT_TRIG_TAN_REDUCTION, Rule.INT_TRIG_COT_REDUCTION, Rule.INT_TRIG_SEC_REDUCTION,
                Rule.INT_TRIG_CSC_REDUCTION, Rule.INT_TRIG_TAN_SUBSTITUTION, Rule.INT_TRIG_PRODUCT_TO_SUM,
                Rule.INT_TRIG_WEIERSTRASS, Rule.INT_RADICAL_SUBSTITUTION, Rule.INT_RADICAL_COMPLETE_SQUARE,
                Rule.INT_TRIG_SUBSTITUTION_SIN, Rule.INT_TRIG_SUBSTITUTION_TAN, Rule.INT_TRIG_SUBSTITUTION_SEC,
                Rule.INT_ROOT_DIFFERENCE_FORM, Rule.INT_ROOT_SUM_FORM}) {
            assert used.contains(rule) : "El corpus no usa " + rule;
        }
    }
}
