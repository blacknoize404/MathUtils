package barnesinside.mathutils.cas.limit;

import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.steps.Solution;
import org.junit.jupiter.api.Test;

class TermwiseLimitTest {

    private static Solution limit(String f, String point) {
        return Limits.limit(f, "x", point, Limits.Direction.BOTH);
    }

    @Test
    void sumOfIndeterminateTermsWithFiniteLimits() {
        Solution s = limit("-(x^2*exp(-x) + 2*x*exp(-x) + 2*exp(-x))", "oo");
        assert s.isSolved() && s.result().isZero() : s.status() + " " + s.result();
    }

    @Test
    void constantTimesAFactorWithALimit() {
        Solution s = limit("3*x*exp(-x)", "oo");
        assert s.isSolved() && s.result().isZero();
    }

    @Test
    void infinitiesOfOppositeSignAreNotCombined() {
        Solution s = limit("x*exp(x) - exp(2*x)", "oo");
        assert !s.isSolved() || !s.result().equals(ExprParser.parse("oo"));
    }
}
