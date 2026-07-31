package barnesinside.mathutils.ui;

import barnesinside.mathutils.theme.Brand;
import barnesinside.mathutils.theme.Motion;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Locale;

/**
 * Fábricas de componentes con el estilo de la marca: botones primario, secundario, fantasma e
 * icono, etiquetas tipográficas y utilidades de disposición.
 */
public final class Ui {

    private Ui() {
    }

    // ---------------------------------------------------------------- Etiquetas

    public static Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    public static Label label(StringBinding text, String... styleClasses) {
        Label label = new Label();
        label.textProperty().bind(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    /** Etiqueta pequeña en mayúsculas y tipografía mono (la mayúscula es decisión tipográfica). */
    public static Label caption(StringBinding text) {
        Label label = new Label();
        label.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> text.get().toUpperCase(Locale.ROOT), text));
        label.getStyleClass().add("caption");
        return label;
    }

    public static Label caption(String text) {
        Label label = new Label(text.toUpperCase(Locale.ROOT));
        label.getStyleClass().add("caption");
        return label;
    }

    /** Cabecera de página con un elemento a la derecha (por ejemplo, el botón de símbolos). */
    public static HBox pageHeader(StringBinding title, StringBinding subtitle, Node trailing) {
        VBox header = pageHeader(title, subtitle);
        HBox.setHgrow(header, Priority.ALWAYS);
        HBox row = new HBox(12, header, trailing);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    /** Cabecera de página: titular y descripción breve. */
    public static VBox pageHeader(StringBinding title, StringBinding subtitle) {
        Label headline = label(title, "headline");
        Label description = label(subtitle, "text-muted");
        description.setWrapText(true);
        VBox box = new VBox(4, headline, description);
        return box;
    }

    // ---------------------------------------------------------------- Botones

    public static Button primary(StringBinding text, String icon, Runnable action) {
        return button(text, icon, action, "btn-primary");
    }

    public static Button secondary(StringBinding text, String icon, Runnable action) {
        return button(text, icon, action, "btn-secondary");
    }

    public static Button ghost(StringBinding text, String icon, Runnable action) {
        return button(text, icon, action, "btn-ghost");
    }

    private static Button button(StringBinding text, String icon, Runnable action, String style) {
        Button button = new Button();
        button.textProperty().bind(text);
        if (icon != null) {
            button.setGraphic(Brand.icon(icon, 16));
            button.setGraphicTextGap(8);
        }
        button.getStyleClass().add(style);
        button.setOnAction(e -> action.run());
        pressEffect(button);
        return button;
    }

    /** Botón solo con icono; el texto va en el tooltip. */
    public static Button iconButton(String icon, StringBinding tooltip, Runnable action) {
        Button button = new Button();
        button.setGraphic(Brand.icon(icon, 18));
        button.getStyleClass().addAll("btn-icon");
        Tooltip tip = new Tooltip();
        tip.textProperty().bind(tooltip);
        button.setTooltip(tip);
        button.setOnAction(e -> action.run());
        pressEffect(button);
        return button;
    }

    /** Hace que el nodo se encoja a 0,97 al pulsarlo y vuelva con el rebote de la marca. */
    public static void pressEffect(Node node) {
        node.setOnMousePressed(e -> animateScale(node, 0.97));
        node.setOnMouseReleased(e -> animateScale(node, 1));
    }

    private static void animateScale(Node node, double target) {
        ScaleTransition transition = new ScaleTransition(Motion.millis(Motion.FAST), node);
        transition.setInterpolator(Motion.SPRING);
        transition.setToX(target);
        transition.setToY(target);
        transition.play();
    }

    // ---------------------------------------------------------------- Disposición

    public static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    public static VBox vbox(double spacing, Node... children) {
        VBox box = new VBox(spacing, children);
        return box;
    }

    public static HBox hbox(double spacing, Node... children) {
        HBox box = new HBox(spacing, children);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static Insets insets(double all) {
        return new Insets(all);
    }

    /** Copia texto al portapapeles. */
    public static void copy(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
