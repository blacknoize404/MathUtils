package barnesinside.mathutils.ui;

import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.I18n;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.ArrayList;
import java.util.List;

/**
 * Paleta de símbolos que no están en el teclado: potencias, raíces, constantes, funciones y letras
 * griegas. Se abre desde un botón y escribe en el último campo de texto que tuvo el foco, dejando
 * el cursor donde se sigue escribiendo (dentro del paréntesis en {@code √(}, {@code sin(}…).
 * <p>Todo lo que inserta lo entiende el intérprete de expresiones de la librería.
 */
public final class SymbolPalette {

    /**
     * Un símbolo de la paleta.
     *
     * @param insert texto que se escribe
     * @param label  lo que muestra el botón
     * @param hint   ayuda emergente (cómo se escribiría con el teclado)
     */
    public record Symbol(String insert, String label, String hint) {
        static Symbol of(String text) {
            return new Symbol(text, text, text);
        }

        static Symbol of(String insert, String label, String hint) {
            return new Symbol(insert, label, hint);
        }
    }

    private record Category(String key, List<Symbol> symbols) {
    }

    private final I18n t = AppContext.i18n();
    private final List<TextInputControl> fields = new ArrayList<>();
    private final List<Category> categories;
    private final Popup popup = new Popup();
    private final FlowPane grid = new FlowPane(6, 6);
    private final Segmented<Category> tabs;
    private final VBox panel;
    private TextInputControl target;
    private Button trigger;

    /**
     * @param withComplexUnit añade la unidad imaginaria {@code i} (para el módulo de complejos)
     */
    public SymbolPalette(boolean withComplexUnit) {
        List<Symbol> constants = new ArrayList<>(List.of(
                Symbol.of("π", "π", "π = pi"), Symbol.of("e"), Symbol.of("∞", "∞", "∞ = oo"),
                Symbol.of("−∞", "−∞", "−∞ = -oo"), Symbol.of("°", "°", "grados: 30° = 30*pi/180")));
        if (withComplexUnit) constants.add(0, Symbol.of("i", "i", "unidad imaginaria"));

        categories = List.of(
                new Category("basic", List.of(
                        Symbol.of("·", "·", "· = *"), Symbol.of("×", "×", "× = *"), Symbol.of("÷", "÷", "÷ = /"),
                        Symbol.of("−", "−", "− = -"), Symbol.of("(", "(", "("), Symbol.of(")", ")", ")"),
                        Symbol.of(",", ",", ","), Symbol.of("^(", "^( )", "potencia: x^(n)"))),
                new Category("powers", List.of(
                        Symbol.of("²", "x²", "x² = x^2"), Symbol.of("³", "x³", "x³ = x^3"),
                        Symbol.of("⁴", "x⁴", "x⁴ = x^4"), Symbol.of("⁻¹", "x⁻¹", "x⁻¹ = x^(-1)"),
                        Symbol.of("^(", "xⁿ", "x^(n)"), Symbol.of("√(", "√", "√( = sqrt("),
                        Symbol.of("∛(", "∛", "∛( = cbrt("), Symbol.of("e^(", "eˣ", "e^(x) = exp(x)"))),
                new Category("constants", constants),
                new Category("functions", List.of("sin(", "cos(", "tan(", "asin(", "acos(", "atan(", "sinh(", "cosh(",
                        "tanh(", "ln(", "log(", "exp(", "abs(", "erf(").stream()
                        .map(name -> Symbol.of(name, name.substring(0, name.length() - 1), name)).toList()),
                new Category("greek", "αβγδεζηθκλμνξρσφχψωΓΔΘΛΠΣΦΨΩ".chars()
                        .mapToObj(c -> Symbol.of(String.valueOf((char) c), String.valueOf((char) c),
                                "variable " + (char) c)).toList()));

        tabs = new Segmented<>(categories, categories.get(0), c -> t.get("symbols." + c.key()));
        tabs.valueProperty().addListener((obs, old, now) -> fill(now));

        VBox box = new VBox(12, tabs, grid);
        panel = box;
        box.getStyleClass().add("palette");
        box.setPadding(new Insets(14));
        box.setPrefWidth(540);
        grid.setPrefWrapLength(512);
        popup.getContent().add(new StackPane(box));
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        fill(categories.get(0));
    }

    /** Hace que los campos reciban los símbolos; el primero es el destino mientras no se enfoque otro. */
    public SymbolPalette attach(TextInputControl... inputs) {
        for (TextInputControl field : inputs) {
            if (fields.contains(field)) continue;
            fields.add(field);
            if (target == null) target = field;
            field.focusedProperty().addListener((obs, old, focused) -> {
                if (focused) target = field;
            });
            field.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ESCAPE) popup.hide();
            });
        }
        return this;
    }

    /** El botón que abre y cierra la paleta. */
    public Node trigger() {
        if (trigger == null) {
            trigger = Ui.iconButton("sigma", t.bind("symbols.button"), this::toggle);
            trigger.setFocusTraversable(false);
        }
        return trigger;
    }

    /** El panel de símbolos, por si se quiere mostrar fuera de la ventana emergente (pruebas y capturas). */
    public Node panel() {
        return panel;
    }

    /** Muestra otra categoría ({@code basic}, {@code powers}, {@code constants}, {@code functions}, {@code greek}). */
    public void showCategory(String key) {
        categories.stream().filter(c -> c.key().equals(key)).findFirst().ifPresent(tabs::setValue);
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    /** Escribe un símbolo en el campo de destino (público para pruebas). */
    public void insert(String text) {
        TextInputControl field = target;
        if (field == null) return;
        field.replaceSelection(text);
        // El cursor queda tras lo escrito, es decir, dentro del paréntesis en √( o sin(
        field.requestFocus();
    }

    // ---------------------------------------------------------------- Interno

    private void fill(Category category) {
        grid.getChildren().clear();
        for (Symbol symbol : category.symbols()) {
            Button button = new Button(symbol.label());
            button.getStyleClass().add("symbol-key");
            button.setFocusTraversable(false);
            Tooltip.install(button, new Tooltip(symbol.hint()));
            button.setOnAction(e -> insert(symbol.insert()));
            grid.getChildren().add(button);
        }
    }

    private void toggle() {
        if (popup.isShowing()) {
            popup.hide();
            return;
        }
        Node anchor = trigger;
        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        // La ventana emergente no hereda el tema: se le pasan las hojas de estilo y la raíz que define los colores
        popup.getScene().getStylesheets().setAll(anchor.getScene().getStylesheets());
        if (!popup.getScene().getRoot().getStyleClass().contains("root")) popup.getScene().getRoot().getStyleClass().add("root");
        popup.show(anchor, bounds.getMinX(), bounds.getMaxY() + 6);
    }
}
