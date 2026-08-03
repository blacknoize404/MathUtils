package barnesinside.mathutils.view;

import MathUtils.cas.expr.Equivalence;
import MathUtils.cas.expr.Eval;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.steps.Solution;
import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.FxTestSupport;
import barnesinside.mathutils.I18n;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.theme.ThemeManager;
import org.junit.jupiter.api.Test;

import java.util.Map;

class MultiVariableViewTest {

    private static void context() {
        AppContext.reset(new ThemeManager(), new I18n());
        AppContext.i18n().setLocale(I18n.SPANISH);
    }

    private static DerivativeView derivatives() {
        context();
        DerivativeView view = new DerivativeView(new Workspace());
        view.content();
        return view;
    }

    @Test
    void mixedPartialsAreExplainedStepByStep() {
        FxTestSupport.onFx(() -> {
            DerivativeView view = derivatives();
            view.solve("x^2*y^3", "x, y", DerivativeView.Mode.DERIVATIVE);
            Solution s = view.solution();
            assert s.isSolved() && s.order() == 2;
            assert Equivalence.areEqual(s.result(), ExprParser.parse("6*x*y^2"));
            assert view.session().topLevel().size() == 2;
            return null;
        });
    }

    @Test
    void aSingleVariableOfAMultivariableFunctionIsAPartialDerivative() {
        FxTestSupport.onFx(() -> {
            DerivativeView view = derivatives();
            view.solve("x^2*y + sin(y)", "y", DerivativeView.Mode.DERIVATIVE);
            Solution s = view.solution();
            assert s.isSolved() && s.extras().containsKey("var1");
            assert Equivalence.areEqual(s.result(), ExprParser.parse("x^2 + cos(y)"));
            return null;
        });
    }

    @Test
    void gradientAndHessianOfferEveryPart() {
        FxTestSupport.onFx(() -> {
            DerivativeView view = derivatives();
            view.solve("x^2*y + y^3", "", DerivativeView.Mode.GRADIENT);
            assert view.solution().isSolved();
            assert Equivalence.areEqual(view.solution().result(), ExprParser.parse("2*x*y"));

            view.solve("x^2*y + y^3", "x, y", DerivativeView.Mode.HESSIAN);
            assert view.solution() != null && view.solution().order() == 2;
            return null;
        });
    }

    @Test
    void errorsAreShownInsteadOfThrown() {
        FxTestSupport.onFx(() -> {
            DerivativeView view = derivatives();
            view.solve("x^2", "x, y", DerivativeView.Mode.DIVERGENCE);
            Solution before = view.solution();
            view.solve("x*y; y", "x, y, z", DerivativeView.Mode.DIVERGENCE);
            assert view.solution() == before : "un error no debe cambiar el resultado mostrado";
            return null;
        });
    }

    @Test
    void multipleIntegralsAndLimitsWorkFromTheForm() {
        FxTestSupport.onFx(() -> {
            context();
            IntegralView integral = new IntegralView(new Workspace());
            integral.content();
            integral.setVariables("x, y");
            integral.solve("x*y", "0; 0", "y; 1");
            Solution s = integral.solution();
            assert s.isSolved() : s;
            assert Math.abs(Eval.value(s.result(), Map.of()) - 0.125) < 1e-12;

            LimitView limit = new LimitView(new Workspace());
            limit.content();
            limit.setVariables("x, y");
            limit.solve("x^2*y/(x^2 + y^2)", "0; 0", MathUtils.cas.limit.Limits.Direction.BOTH);
            assert limit.solution().isSolved() && limit.solution().result().isZero();
            limit.solve("x*y/(x^2 + y^2)", "0; 0", MathUtils.cas.limit.Limits.Direction.BOTH);
            assert limit.solution().status() == Solution.Status.DOES_NOT_EXIST;
            return null;
        });
    }
}
