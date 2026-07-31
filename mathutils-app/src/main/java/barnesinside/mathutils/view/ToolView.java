package barnesinside.mathutils.view;

import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.I18n;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.ui.SymbolPalette;
import barnesinside.mathutils.ui.Ui;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Base de los módulos de herramientas (matrices, complejos, estadística, EDOs): cabecera, formulario
 * propio, mensaje de error y zona de resultados. Cada módulo aporta su formulario y calcula.
 */
abstract class ToolView implements Module {

    protected final Workspace workspace;
    protected final I18n t = AppContext.i18n();

    private final Label error = new Label();
    private final VBox output = new VBox(20);
    private final Label empty = new Label();
    private final SymbolPalette symbols;
    private Node content;

    protected ToolView(Workspace workspace) {
        this.workspace = workspace;
        this.symbols = new SymbolPalette(imaginaryUnit());
    }

    /** Si la paleta de símbolos ofrece la unidad imaginaria {@code i}. */
    protected boolean imaginaryUnit() {
        return false;
    }

    /** Campos de texto del módulo que reciben los símbolos de la paleta. */
    protected List<javafx.scene.control.TextInputControl> inputs() {
        return List.of();
    }

    /** Prefijo de las claves de texto: {@code matrix}, {@code complex}, {@code stats} u {@code ode}. */
    protected abstract String keyPrefix();

    /** Formulario del módulo. */
    protected abstract Node form();

    /** Ejemplos como pares (texto del chip, acción al pulsarlo). */
    protected List<Example> examples() {
        return List.of();
    }

    /** Un ejemplo que rellena el formulario y calcula. */
    protected record Example(String label, Runnable apply) {
    }

    @Override
    public Node content() {
        if (content == null) content = build();
        return content;
    }

    @Override
    public StringBinding title() {
        return t.bind("nav." + keyPrefix());
    }

    protected StringBinding text(String suffix) {
        return t.bind(keyPrefix() + "." + suffix);
    }

    private Node build() {
        error.getStyleClass().add("message-error");
        error.setWrapText(true);
        error.setManaged(false);
        error.setVisible(false);

        empty.textProperty().bind(text("empty"));
        empty.getStyleClass().add("text-muted");

        Node form = form();
        symbols.attach(inputs().toArray(new javafx.scene.control.TextInputControl[0]));
        VBox main = new VBox(20, Ui.pageHeader(text("title"), text("subtitle"), symbols.trigger()), form, error);

        List<Example> examples = examples();
        if (!examples.isEmpty()) {
            FlowPane chips = new FlowPane(8, 8);
            for (Example example : examples) {
                Label chip = new Label(example.label());
                HBox box = new HBox(chip);
                box.getStyleClass().add("chip");
                box.setOnMouseClicked(e -> example.apply().run());
                chips.getChildren().add(box);
            }
            main.getChildren().add(new VBox(8, Ui.caption(t.bind("solver.examples")), chips));
        }

        output.getChildren().setAll(empty);
        main.getChildren().add(output);
        main.setPadding(new Insets(32));
        main.setMaxWidth(920);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        return scroll;
    }

    // ---------------------------------------------------------------- Resultados

    /** Muestra un error concreto y deja los resultados anteriores. */
    protected void fail(String message) {
        error.setText(message);
        error.setVisible(true);
        error.setManaged(true);
    }

    protected void clearError() {
        error.setVisible(false);
        error.setManaged(false);
    }

    /** Sustituye los resultados mostrados. */
    protected void show(Node... nodes) {
        clearError();
        output.getChildren().setAll(nodes);
    }

    /** Tarjeta con un título en minúsculas pequeñas y su contenido. */
    protected static VBox card(String caption, Node body) {
        VBox card = new VBox(14, Ui.caption(caption), body);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        return card;
    }

    /** Campo de formulario con su rótulo encima. */
    protected VBox field(String key, Node input) {
        return new VBox(4, Ui.caption(t.bind(key)), input);
    }
}
