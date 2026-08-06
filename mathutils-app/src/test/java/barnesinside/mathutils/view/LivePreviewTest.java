package barnesinside.mathutils.view;

import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.AppInfo;
import barnesinside.mathutils.FxTestSupport;
import barnesinside.mathutils.I18n;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.theme.Brand;
import barnesinside.mathutils.theme.ThemeManager;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

class LivePreviewTest {

    private static void context() {
        AppContext.reset(new ThemeManager(), new I18n());
        AppContext.i18n().setLocale(I18n.SPANISH);
    }

    @Test
    void theFormulaFollowsWhatIsTyped() {
        FxTestSupport.onFx(() -> {
            context();
            IntegralView view = new IntegralView(new Workspace());
            view.content();
            assert view.preview().box() == null && !view.preview().isVisible() : "vacío: la vista previa no se ve";

            view.function.setText("x^2*sin(x)");
            assert view.preview().isVisible() && view.preview().box() != null;
            double widthWithoutLimits = view.preview().box().width();

            view.solve("x^2*sin(x)", "0", "pi");
            assert view.preview().box().width() > widthWithoutLimits : "con límites la integral es más ancha";

            view.function.setText("x^2*(");
            assert !view.preview().messageText().isBlank() : "un texto incompleto explica por qué no hay fórmula";
            assert view.preview().box() != null : "se conserva la última fórmula válida";

            view.function.setText("");
            assert view.preview().box() == null && !view.preview().isVisible();
            return null;
        });
    }

    @Test
    void symbolsAreUnderstoodWhileTyping() {
        FxTestSupport.onFx(() -> {
            context();
            LimitView view = new LimitView(new Workspace());
            view.content();
            view.function.setText("sin(2π·x)/√x");
            assert view.preview().box() != null && view.preview().messageText().isEmpty();
            return null;
        });
    }

    @Test
    void multivariablePreviewsFollowTheModeAndTheVariables() {
        FxTestSupport.onFx(() -> {
            context();
            DerivativeView view = new DerivativeView(new Workspace());
            view.content();
            view.function.setText("x^2*y");
            view.variable.setText("x, y");
            double partial = view.preview().box().width();
            view.setMode(DerivativeView.Mode.GRADIENT);
            assert view.preview().box() != null && view.preview().box().width() != partial;
            view.setMode(DerivativeView.Mode.JACOBIAN);
            view.function.setText("x*y; x + y");
            assert view.preview().box() != null && view.preview().messageText().isEmpty();
            return null;
        });
    }

    @Test
    void aboutDialogCreditsTheAuthorAndTheAppHasItsOwnIcon() {
        FxTestSupport.onFx(() -> {
            context();
            Stage stage = AboutDialog.show(null);
            assert stage.isShowing() && stage.getIcons().size() == 1;
            stage.close();

            assert Brand.appIcon().getWidth() == 256 : "el icono de la aplicación es el de MathUtils";
            assert Brand.mark(true).getWidth() > 0 && Brand.mark(false).getWidth() > 0;
            assert AppInfo.AUTHOR_URL.startsWith("https://") && !AppInfo.version().isBlank();
            return null;
        });
    }
}
