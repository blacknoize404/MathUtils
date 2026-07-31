package barnesinside.mathutils.view;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.tree.TreeModel;
import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.FxTestSupport;
import barnesinside.mathutils.I18n;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.theme.ThemeManager;
import org.junit.jupiter.api.Test;

import java.util.List;

class StepAnimationTest {

    private static IntegralView open() {
        ThemeManager theme = new ThemeManager();
        AppContext.reset(theme, new I18n());
        AppContext.i18n().setLocale(I18n.SPANISH);
        IntegralView view = new IntegralView(new Workspace());
        view.content();
        return view;
    }

    @Test
    void stepsAreNumberedInReadingOrderWithTheirParents() {
        FxTestSupport.onFx(() -> {
            IntegralView view = open();
            view.solve("x*e^x");
            StepSession session = view.session();
            List<StepSession.Frame> frames = session.frames();
            assert frames.size() >= 3 : frames.size();
            assert frames.get(0).number().equals("1") && frames.get(0).parent() == -1 && frames.get(0).depth() == 0;
            StepSession.Frame nested = frames.stream().filter(f -> f.depth() == 1).findFirst().orElseThrow();
            assert nested.number().startsWith("1.") && nested.parent() == 0;
            for (int i = 0; i < frames.size(); i++) assert frames.get(i).index() == i;
            assert session.topLevel().stream().allMatch(f -> f.depth() == 0);
            return null;
        });
    }

    @Test
    void movingInTheSessionRedrawsTheAnimatorTree() {
        FxTestSupport.onFx(() -> {
            IntegralView view = open();
            view.solve("x^2*sin(x)");
            StepSession session = view.session();
            StepSession.Frame first = session.frames().get(0);
            assert session.current() == first;
            assert view.animator().shownTree() != null;

            StepSession.Frame withExpressions = session.frames().stream().filter(f -> f.after() != null).skip(1).findFirst().orElseThrow();
            session.go(withExpressions.index());
            TreeModel expected = TreeModel.ofExpr(withExpressions.after());
            assert view.animator().shownTree().root().label().equals(expected.root().label());
            assert view.animator().shownTree().nodes().size() == expected.nodes().size();
            return null;
        });
    }

    @Test
    void solvingAgainResetsTheSelection() {
        FxTestSupport.onFx(() -> {
            IntegralView view = open();
            view.solve("x*e^x");
            view.session().go(2);
            view.solve("ln(x)");
            assert view.session().positionProperty().get() == 0;
            Expr problem = view.session().solutionProperty().get().problem();
            assert problem.equals(ExprParser.parse("ln(x)"));
            return null;
        });
    }
}
