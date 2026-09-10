package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Set;

class RewritingTest {

    private static Expr p(String source) {
        return ExprParser.parse(source);
    }

    @Test
    void replacesWhatMatchesThePattern() {
        Solution s = Rewriting.rewrite(p("sin(3x)^2 + cos(3x)^2 + y"), p("sin(a)^2 + cos(a)^2"), Expr.ONE, Set.of("a"));
        assert s.isSolved() && s.kind() == Solution.Kind.REWRITE && s.result().equals(p("1 + y")) : s.result();
        assert s.steps().size() == 1 && s.steps().get(0).rule() == Rule.POLY_REWRITE;
        assert s.steps().get(0).before().equals(p("sin(3x)^2 + cos(3x)^2 + y")) && s.steps().get(0).after().equals(s.result());
    }

    @Test
    void usesTheWildcardsInTheReplacement() {
        Solution s = Rewriting.rewrite(p("sin(2x)*cos(2x)"), p("sin(a)*cos(a)"), p("sin(2a)/2"), Set.of("a"));
        assert s.isSolved() && s.result().equals(p("sin(4x)/2")) : s.result();
    }

    @Test
    void noMatchIsUnsolved() {
        Solution s = Rewriting.rewrite(p("x^2 + 1"), p("sin(a)"), p("a"), Set.of("a"));
        assert !s.isSolved() && s.result() == null && s.steps().isEmpty();
    }

    @Test
    void rendersInBothLanguages() {
        Solution s = Rewriting.rewrite(p("sin(x)^2 + cos(x)^2"), p("sin(a)^2 + cos(a)^2"), Expr.ONE, Set.of("a"));
        for (Locale locale : List.of(Locale.forLanguageTag("es"), Locale.ENGLISH)) {
            for (Format format : Format.values()) {
                String text = s.render(locale, format);
                assert !text.isBlank() && !text.contains("${") : format + ": " + text;
            }
        }
        assert s.render(Locale.ENGLISH, Format.PLAIN).contains("Rewrite sin(x)^2 + cos(x)^2 replacing");
    }
}
