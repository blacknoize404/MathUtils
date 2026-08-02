package MathUtils.cas.limit;

import MathUtils.cas.JsonValidator;
import MathUtils.cas.expr.Eval;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.steps.Format;
import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Solution;
import MathUtils.cas.steps.Step;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class MultiLimitsTest {

    private static final List<String> XY = List.of("x", "y");
    private static final List<String> ORIGIN = List.of("0", "0");

    private static boolean uses(Solution s, Rule rule) {
        List<Step> all = new ArrayList<>();
        collect(s.steps(), all);
        return all.stream().anyMatch(step -> step.rule() == rule);
    }

    private static void collect(List<Step> steps, List<Step> out) {
        for (Step s : steps) {
            out.add(s);
            collect(s.substeps(), out);
        }
    }

    private static Solution solved(String f, List<String> point, double expected) {
        Solution s = MultiLimits.limit(f, XY, point);
        assert s.isSolved() : f + " -> " + s.status() + "\n" + s;
        assert Math.abs(Eval.value(s.result()) - expected) < 1e-12 : f + ": " + s.result();
        return s;
    }

    private static Solution doesNotExist(String f, List<String> point) {
        Solution s = MultiLimits.limit(f, XY, point);
        assert s.status() == Solution.Status.DOES_NOT_EXIST : f + " -> " + s.status() + " " + s.result();
        return s;
    }

    @Test
    void continuousFunctionsAreEvaluatedAtThePoint() {
        solved("x^2 + y^2", List.of("1", "2"), 5);
        solved("sin(x*y)", List.of("pi", "1"), 0);
        assert uses(solved("x*y + 3", List.of("2", "2"), 7), Rule.LIMIT_MULTI_SUBSTITUTION);
    }

    @Test
    void differentPathsProveThatTheLimitDoesNotExist() {
        doesNotExist("(x^2 - y^2)/(x^2 + y^2)", ORIGIN);
        assert uses(doesNotExist("x*y/(x^2 + y^2)", ORIGIN), Rule.LIMIT_MULTI_PATHS);
        doesNotExist("x/(x + y)", ORIGIN);
        doesNotExist("x^2*y/(x^4 + y^2)", ORIGIN);
        doesNotExist("x^3*y/(x^6 + y^2)", ORIGIN);
        doesNotExist("(x + y)/(x - y)", ORIGIN);
    }

    @Test
    void differentIteratedLimitsProveIt() {
        Solution s = doesNotExist("(x - y)/(x + y)", ORIGIN);
        assert uses(s, Rule.LIMIT_MULTI_ITERATED) && uses(s, Rule.LIMIT_MULTI_DIFFERENT);
    }

    @Test
    void polarBoundsProveTheLimit() {
        assert uses(solved("x^2*y/(x^2 + y^2)", ORIGIN, 0), Rule.LIMIT_MULTI_POLAR_ZERO);
        solved("x*y^2/(x^2 + y^2)", ORIGIN, 0);
        solved("(x^3 + y^3)/(x^2 + y^2)", ORIGIN, 0);
        assert uses(solved("(2*x^2 + 2*y^2)/(x^2 + y^2)", ORIGIN, 2), Rule.LIMIT_MULTI_POLAR_CONSTANT);
        solved("(x - 1)^2*(y - 2)/((x - 1)^2 + (y - 2)^2)", List.of("1", "2"), 0);
    }

    @Test
    void agreeingPathsWithoutAProofGiveACandidateNotAResult() {
        Solution s = MultiLimits.limit("sin(x*y)/(x^2 + y^2)*x", XY, ORIGIN);
        assert !s.isSolved() && s.result() == null : s.status() + " " + s.result();
        Solution candidate = MultiLimits.limit("x^2*y^2/(x^2 + y^4)", XY, ORIGIN);
        if (!candidate.isSolved() && candidate.status() == Solution.Status.UNSOLVED) {
            assert candidate.extras().get("candidate") instanceof Expr || candidate.steps().isEmpty() == false;
        }
    }

    @Test
    void threeVariables() {
        Solution s = MultiLimits.limit("x*y*z/(x^2 + y^2 + z^2)", List.of("x", "y", "z"), List.of("0", "0", "0"));
        assert !(s.status() == Solution.Status.DOES_NOT_EXIST) : "este límite existe y vale 0";
        Solution different = MultiLimits.limit("(x*y + y*z + x*z)/(x^2 + y^2 + z^2)",
                List.of("x", "y", "z"), List.of("0", "0", "0"));
        assert different.status() == Solution.Status.DOES_NOT_EXIST : different.status();
    }

    @Test
    void renderingIsCompleteInBothLanguages() {
        for (String f : new String[]{"x^2*y/(x^2 + y^2)", "(x^2 - y^2)/(x^2 + y^2)", "x^2 + y^2"}) {
            Solution s = MultiLimits.limit(f, XY, ORIGIN);
            for (Locale locale : new Locale[]{Locale.forLanguageTag("es"), Locale.ENGLISH}) {
                for (Format format : Format.values()) {
                    String text = s.render(locale, format);
                    assert !text.contains("${") : f + ": " + text;
                }
                JsonValidator.assertValid(s.render(locale, Format.JSON));
            }
        }
        String plain = MultiLimits.limit("x^2*y/(x^2 + y^2)", XY, ORIGIN).render(Locale.forLanguageTag("es"), Format.PLAIN);
        assert plain.contains("cuando (x, y) tiende a (0, 0)") && plain.contains("lim[(x, y) -> (0, 0)]") : plain;
        assert MultiLimits.limit("x^2*y/(x^2 + y^2)", XY, ORIGIN).render(Locale.ENGLISH, Format.LATEX)
                .contains("\\lim_{(x, y) \\to (0, 0)}");
    }

    @Test
    void invalidRequestsAreRejected() {
        try {
            MultiLimits.limit("x + y", XY, List.of("0"));
            assert false;
        } catch (IllegalArgumentException expected) {
            assert !expected.getMessage().isBlank();
        }
    }
}
