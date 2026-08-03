package barnesinside.mathutils;

import barnesinside.mathutils.theme.ThemeManager;
import barnesinside.mathutils.view.CalculatorView;
import barnesinside.mathutils.view.DerivativeView;
import barnesinside.mathutils.view.ExpressionTreeView;
import barnesinside.mathutils.view.ComplexView;
import barnesinside.mathutils.view.IntegralView;
import barnesinside.mathutils.view.MatrixView;
import barnesinside.mathutils.view.OdeView;
import barnesinside.mathutils.view.StatisticsView;
import barnesinside.mathutils.view.LimitView;
import barnesinside.mathutils.view.OrderView;
import barnesinside.mathutils.view.Shell;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * Renderiza cada módulo en ambos temas y comprueba que la captura tiene contenido. Las imágenes
 * quedan en {@code target/snapshots} para revisarlas a ojo.
 */
class VisualSnapshotTest {

    @BeforeAll
    static void toolkit() {
        FxTestSupport.start();
    }

    private record Rendered(Shell shell, Scene scene) {
    }

    private static Rendered open(ThemeManager.Mode mode, String moduleId) {
        return open(mode, moduleId, 860);
    }

    private static Rendered open(ThemeManager.Mode mode, String moduleId, int height) {
        return FxTestSupport.onFx(() -> {
            ThemeManager theme = new ThemeManager();
            theme.modeProperty().set(mode);
            AppContext.reset(theme, new I18n());
            AppContext.i18n().setLocale(I18n.SPANISH);

            Workspace workspace = new Workspace();
            Shell shell = new Shell(workspace);
            Scene scene = new Scene(shell, 1360, height);
            theme.install(scene);
            shell.navigate(moduleId);
            shell.settle();
            return new Rendered(shell, scene);
        });
    }

    private static void assertRendered(File file) {
        assert file.exists() && file.length() > 0 : file;
        int colors = FxTestSupport.distinctColors(file);
        assert colors > 40 : file.getName() + " parece en blanco (" + colors + " colores)";
    }

    @Test
    void calculatorInBothThemes() {
        for (ThemeManager.Mode mode : ThemeManager.Mode.values()) {
            Rendered r = open(mode, "calculator");
            File file = FxTestSupport.onFx(() -> {
                var session = r.shell().activeModule();
                return FxTestSupport.snapshot(r.scene(), "calculator-" + mode.name().toLowerCase() + "-vacia");
            });
            assertRendered(file);
        }
    }

    @Test
    void calculatorWithResult() {
        Rendered r = open(ThemeManager.Mode.DARK, "calculator");
        File file = FxTestSupport.onFx(() -> {
            CalculatorView calculator = (CalculatorView) r.shell().activeModule();
            calculator.evaluate("x = 4");
            calculator.evaluate("f(t) = t^2 + x");
            calculator.evaluate("sqrt(12) + 1/3");
            r.shell().settle();
            return FxTestSupport.snapshot(r.scene(), "calculator-dark-historial");
        });
        assertRendered(file);
    }

    @Test
    void treeModule() {
        for (ThemeManager.Mode mode : ThemeManager.Mode.values()) {
            Rendered r = open(mode, "tree");
            File file = FxTestSupport.onFx(() -> {
                ((ExpressionTreeView) r.shell().activeModule()).show("(1 + 2)*3 - sin(x)/2");
                r.shell().settle();
                return FxTestSupport.snapshot(r.scene(), "tree-" + mode.name().toLowerCase());
            });
            assertRendered(file);
        }
    }

    @Test
    void orderModule() {
        for (ThemeManager.Mode mode : ThemeManager.Mode.values()) {
            Rendered r = open(mode, "order");
            File file = FxTestSupport.onFx(() -> {
                ((OrderView) r.shell().activeModule()).solve("2 + 3*4^2 - (6 - 2)/2");
                r.shell().settle();
                return FxTestSupport.snapshot(r.scene(), "order-" + mode.name().toLowerCase());
            });
            assertRendered(file);
        }
    }

    @Test
    void limitModule() {
        for (ThemeManager.Mode mode : ThemeManager.Mode.values()) {
            Rendered r = open(mode, "limits", 1500);
            File file = FxTestSupport.onFx(() -> {
                ((LimitView) r.shell().activeModule()).solve("(1 + 1/x)^x", "oo", MathUtils.cas.limit.Limits.Direction.BOTH);
                r.shell().settle();
                return FxTestSupport.snapshot(r.scene(), "limits-" + mode.name().toLowerCase());
            });
            assertRendered(file);
        }
    }

    @Test
    void integralModule() {
        for (ThemeManager.Mode mode : ThemeManager.Mode.values()) {
            Rendered r = open(mode, "integrals", 1500);
            File file = FxTestSupport.onFx(() -> {
                ((IntegralView) r.shell().activeModule()).solve("e^x*sin(x)");
                r.shell().settle();
                return FxTestSupport.snapshot(r.scene(), "integrals-" + mode.name().toLowerCase());
            });
            assertRendered(file);
        }
    }

    @Test
    void integralWithRadicals() {
        Rendered r = open(ThemeManager.Mode.LIGHT, "integrals", 1700);
        File file = FxTestSupport.onFx(() -> {
            ((IntegralView) r.shell().activeModule()).solve("1/(x^2*sqrt(x^2+1))");
            r.shell().settle();
            return FxTestSupport.snapshot(r.scene(), "integrals-radical-light");
        });
        assertRendered(file);
    }

    @Test
    void definiteIntegralWithArea() {
        for (ThemeManager.Mode mode : ThemeManager.Mode.values()) {
            Rendered r = open(mode, "integrals", 1700);
            File file = FxTestSupport.onFx(() -> {
                ((IntegralView) r.shell().activeModule()).solve("e^(-x^2)", "0", "oo");
                r.shell().settle();
                return FxTestSupport.snapshot(r.scene(), "integrals-definite-" + mode.name().toLowerCase());
            });
            assertRendered(file);
        }
    }

    @Test
    void divergentIntegralShowsThePlot() {
        Rendered r = open(ThemeManager.Mode.LIGHT, "integrals", 1500);
        File file = FxTestSupport.onFx(() -> {
            ((IntegralView) r.shell().activeModule()).solve("1/x", "0", "1");
            r.shell().settle();
            return FxTestSupport.snapshot(r.scene(), "integrals-divergent-light");
        });
        assertRendered(file);
    }

    @Test
    void matrixModule() {
        for (ThemeManager.Mode mode : ThemeManager.Mode.values()) {
            Rendered r = open(mode, "matrices", 1200);
            File file = FxTestSupport.onFx(() -> {
                MatrixView view = (MatrixView) r.shell().activeModule();
                view.setMatrices("2 -1 0; -1 2 -1; 0 -1 2", "1 0 0; 0 1 0; 0 0 1");
                view.run(MatrixView.Op.INVERSE);
                r.shell().settle();
                return FxTestSupport.snapshot(r.scene(), "matrices-" + mode.name().toLowerCase());
            });
            assertRendered(file);
        }
    }

    @Test
    void complexModule() {
        Rendered r = open(ThemeManager.Mode.LIGHT, "complex", 1300);
        File file = FxTestSupport.onFx(() -> {
            ComplexView view = (ComplexView) r.shell().activeModule();
            view.setValues("-8", "1-2i", "3");
            view.run(ComplexView.Op.ROOTS);
            r.shell().settle();
            return FxTestSupport.snapshot(r.scene(), "complex-light");
        });
        assertRendered(file);
    }

    @Test
    void statisticsModule() {
        Rendered r = open(ThemeManager.Mode.LIGHT, "statistics", 1500);
        File file = FxTestSupport.onFx(() -> {
            StatisticsView view = (StatisticsView) r.shell().activeModule();
            view.calculateData("4 8 15 16 23 42 8 15 16 4 12 9 11 14 10 13", "");
            r.shell().settle();
            return FxTestSupport.snapshot(r.scene(), "statistics-light");
        });
        assertRendered(file);
        File distribution = FxTestSupport.onFx(() -> {
            StatisticsView view = (StatisticsView) r.shell().activeModule();
            view.calculateDistribution(StatisticsView.Kind.NORMAL, "100", "15", "115");
            r.shell().settle();
            return FxTestSupport.snapshot(r.scene(), "statistics-normal-light");
        });
        assertRendered(distribution);
    }

    @Test
    void odeModule() {
        for (ThemeManager.Mode mode : ThemeManager.Mode.values()) {
            Rendered r = open(mode, "ode", 1250);
            File file = FxTestSupport.onFx(() -> {
                OdeView view = (OdeView) r.shell().activeModule();
                view.solve("z", "-y - 0.3*z", "0", "1", "0", "20", OdeView.Method.DORMAND_PRINCE);
                r.shell().settle();
                return FxTestSupport.snapshot(r.scene(), "ode-" + mode.name().toLowerCase());
            });
            assertRendered(file);
        }
    }

    @Test
    void organizedSteps() {
        Rendered r = open(ThemeManager.Mode.LIGHT, "integrals", 2300);
        File file = FxTestSupport.onFx(() -> {
            IntegralView view = (IntegralView) r.shell().activeModule();
            view.solve("x^2*sin(x)");
            r.shell().settle();
            return FxTestSupport.snapshot(r.scene(), "integrals-steps-light");
        });
        assertRendered(file);
    }

    @Test
    void hessianWithSeveralVariables() {
        Rendered r = open(ThemeManager.Mode.LIGHT, "derivatives", 1900);
        File file = FxTestSupport.onFx(() -> {
            ((DerivativeView) r.shell().activeModule()).solve("x^2*y^3 + sin(x*y)", "x, y", DerivativeView.Mode.HESSIAN);
            r.shell().settle();
            return FxTestSupport.snapshot(r.scene(), "derivatives-hessian-light");
        });
        assertRendered(file);
    }

    @Test
    void doubleIntegral() {
        Rendered r = open(ThemeManager.Mode.LIGHT, "integrals", 1900);
        File file = FxTestSupport.onFx(() -> {
            IntegralView view = (IntegralView) r.shell().activeModule();
            view.setVariables("x, y");
            view.solve("x*y", "0; 0", "y; 1");
            r.shell().settle();
            return FxTestSupport.snapshot(r.scene(), "integrals-double-light");
        });
        assertRendered(file);
    }

    @Test
    void limitWithSeveralVariables() {
        Rendered r = open(ThemeManager.Mode.LIGHT, "limits", 1700);
        File file = FxTestSupport.onFx(() -> {
            LimitView view = (LimitView) r.shell().activeModule();
            view.setVariables("x, y");
            view.solve("x^2*y/(x^4 + y^2)", "0; 0", MathUtils.cas.limit.Limits.Direction.BOTH);
            r.shell().settle();
            return FxTestSupport.snapshot(r.scene(), "limits-multi-light");
        });
        assertRendered(file);
    }

    @Test
    void derivativeModule() {
        for (ThemeManager.Mode mode : ThemeManager.Mode.values()) {
            Rendered r = open(mode, "derivatives");
            File file = FxTestSupport.onFx(() -> {
                ((DerivativeView) r.shell().activeModule()).solve("x^2 sin(x) + ln(1+3x)/x");
                r.shell().settle();
                return FxTestSupport.snapshot(r.scene(), "derivatives-" + mode.name().toLowerCase());
            });
            assertRendered(file);
        }
    }
}
