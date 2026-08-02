package MathUtils.cas.diff;

import MathUtils.cas.JsonValidator;
import MathUtils.cas.expr.Equivalence;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.steps.Format;
import MathUtils.cas.steps.Solution;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

class MultiDerivativesTest {

    private static Expr p(String s) {
        return ExprParser.parse(s);
    }

    private static void same(Expr actual, String expected) {
        assert Equivalence.areEqual(actual, p(expected)) : actual + " debía ser " + expected;
    }

    @Test
    void mixedPartialsFollowTheGivenOrder() {
        Solution fxy = MultiDerivatives.mixed("x^2*y^3 + sin(x*y)", List.of("x", "y"));
        assert fxy.isSolved() && fxy.order() == 2;
        same(fxy.result(), "6*x*y^2 + cos(x*y) - x*y*sin(x*y)");
        assert fxy.steps().size() == 2 : "un paso principal por derivada";
        Solution fyx = MultiDerivatives.mixed("x^2*y^3 + sin(x*y)", List.of("y", "x"));
        assert Equivalence.areEqual(fxy.result(), fyx.result()) : "el teorema de Schwarz";
    }

    @Test
    void repeatedVariableGivesTheHigherDerivative() {
        same(MultiDerivatives.mixed("x^4*y", List.of("x", "x", "y")).result(), "12*x^2");
    }

    @Test
    void thePartialNotationIsRendered() {
        Solution s = MultiDerivatives.mixed("x^2*y", List.of("x", "y"));
        String plain = s.render(Locale.forLanguageTag("es"), Format.PLAIN);
        assert plain.contains("∂^2/∂y∂x") && plain.contains("parcialmente respecto a x, y") : plain;
        String latex = s.render(Locale.ENGLISH, Format.LATEX);
        assert latex.contains("\\partial") && !latex.contains("${") : latex;
        JsonValidator.assertValid(s.render(Locale.ENGLISH, Format.JSON));
        String single = MultiDerivatives.mixed("x^2*y", List.of("y")).render(Locale.ENGLISH, Format.PLAIN);
        assert single.contains("∂/∂y") : single;
    }

    @Test
    void gradientHessianJacobianDivergenceAndLaplacian() {
        List<String> xy = List.of("x", "y");
        MultiDerivatives.Result gradient = MultiDerivatives.gradient(p("x^2*y + y^3"), xy);
        assert gradient.isSolved() && gradient.rows() == 1 && gradient.columns() == 2;
        same(gradient.at(0, 0).solution().result(), "2*x*y");
        same(gradient.at(0, 1).solution().result(), "x^2 + 3*y^2");

        MultiDerivatives.Result hessian = MultiDerivatives.hessian(p("x^2*y + y^3"), xy);
        assert hessian.rows() == 2 && hessian.columns() == 2;
        same(hessian.at(0, 0).solution().result(), "2*y");
        same(hessian.at(0, 1).solution().result(), "2*x");
        same(hessian.at(1, 0).solution().result(), "2*x");
        same(hessian.at(1, 1).solution().result(), "6*y");

        MultiDerivatives.Result jacobian = MultiDerivatives.jacobian(MultiDerivatives.parseFunctions("x*y; x+y^2"), xy);
        assert jacobian.rows() == 2 && jacobian.columns() == 2;
        same(jacobian.at(0, 0).solution().result(), "y");
        same(jacobian.at(1, 1).solution().result(), "2*y");

        MultiDerivatives.Result divergence = MultiDerivatives.divergence(MultiDerivatives.parseFunctions("x^2; x*y"), xy);
        same(divergence.total(), "2*x + x");

        MultiDerivatives.Result laplacian = MultiDerivatives.laplacian(p("x^2 + y^3 + z"), List.of("x", "y", "z"));
        same(laplacian.total(), "2 + 6*y");
    }

    @Test
    void variablesDefaultToTheFreeOnes() {
        assert MultiDerivatives.variablesOf(p("y*x + z"), p("w")).equals(List.of("w", "x", "y", "z"));
    }

    @Test
    void invalidRequestsAreRejected() {
        try {
            MultiDerivatives.divergence(MultiDerivatives.parseFunctions("x; y; z"), List.of("x", "y"));
            assert false;
        } catch (IllegalArgumentException expected) {
            assert !expected.getMessage().isBlank();
        }
        try {
            MultiDerivatives.mixed("x", List.of());
            assert false;
        } catch (IllegalArgumentException expected) {
            assert !expected.getMessage().isBlank();
        }
    }
}
