package barnesinside.mathutils;

import MathUtils.cas.expr.Equivalence;
import MathUtils.cas.expr.ExprParser;
import barnesinside.mathutils.theme.ThemeManager;
import barnesinside.mathutils.ui.SymbolPalette;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

import java.io.File;

class SymbolPaletteTest {

    private static void freshContext(ThemeManager.Mode mode) {
        ThemeManager theme = new ThemeManager();
        theme.modeProperty().set(mode);
        AppContext.reset(theme, new I18n());
        AppContext.i18n().setLocale(I18n.SPANISH);
    }

    @Test
    void symbolsAreWrittenAtTheCaretAndTheParserUnderstandsThem() {
        FxTestSupport.onFx(() -> {
            freshContext(ThemeManager.Mode.LIGHT);
            TextField field = new TextField("2·x");
            SymbolPalette palette = new SymbolPalette(false).attach(field);
            field.positionCaret(field.getText().length());
            palette.insert("²");
            palette.insert("+√(");
            field.replaceSelection("x)");
            palette.insert("·π");
            assert field.getText().equals("2·x²+√(x)·π") : field.getText();
            assert ExprParser.tryParse(field.getText()).isPresent();
            return null;
        });
    }

    @Test
    void theLastFocusedFieldReceivesTheSymbols() {
        FxTestSupport.onFx(() -> {
            freshContext(ThemeManager.Mode.LIGHT);
            TextField first = new TextField();
            TextField second = new TextField();
            SymbolPalette palette = new SymbolPalette(true).attach(first, second);
            palette.insert("i");
            assert first.getText().equals("i") && second.getText().isEmpty();
            return null;
        });
    }

    @Test
    void everyGreekLetterAndFunctionOfThePaletteParses() {
        for (String letter : "αβγδεζηθκλμνξρσφχψωΓΔΘΛΠΣΦΨΩ".split("")) {
            assert ExprParser.tryParse("2" + letter + "+" + letter).isPresent() : letter;
        }
        for (String f : new String[]{"sin(", "cos(", "tan(", "asin(", "acos(", "atan(", "sinh(", "cosh(", "tanh(", "ln(",
                "log(", "exp(", "abs(", "erf(", "√(", "∛("}) {
            assert ExprParser.tryParse(f + "x)").isPresent() : f;
        }
        assert Equivalence.areEqual(ExprParser.parse("x⁻¹"), ExprParser.parse("1/x"));
    }

    @Test
    void paletteLooksLikeTheBrand() {
        for (ThemeManager.Mode mode : ThemeManager.Mode.values()) {
            File file = FxTestSupport.onFx(() -> {
                ThemeManager theme = new ThemeManager();
                theme.modeProperty().set(mode);
                AppContext.reset(theme, new I18n());
                AppContext.i18n().setLocale(I18n.SPANISH);
                SymbolPalette palette = new SymbolPalette(false);
                palette.showCategory("greek");
                StackPane root = new StackPane(palette.panel());
                root.setPadding(new javafx.geometry.Insets(24));
                Scene scene = new Scene(root, 590, 300);
                theme.install(scene);
                root.applyCss();
                root.layout();
                return FxTestSupport.snapshot(scene, "symbols-" + mode.name().toLowerCase());
            });
            assert file.exists() && FxTestSupport.distinctColors(file) > 20;
        }
    }
}
