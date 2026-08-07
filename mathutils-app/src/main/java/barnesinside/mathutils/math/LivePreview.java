package barnesinside.mathutils.math;

import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.ui.Ui;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Supplier;

/**
 * Campo de vista previa: muestra con la notación matemática correcta lo que se está escribiendo,
 * mientras se escribe. Mientras el texto no sea una expresión completa (un paréntesis sin cerrar, un
 * operador al final) muestra el motivo en gris en vez de borrar la fórmula, y con el campo vacío se oculta.
 */
public final class LivePreview extends VBox {

    private final MathView view = MathView.empty(22);
    private final Label message = new Label();
    private final ScrollPane scroller;
    private final javafx.scene.layout.StackPane nodeHolder = new javafx.scene.layout.StackPane();

    public LivePreview() {
        super(6);
        getStyleClass().add("panel");
        setPadding(new Insets(10, 14, 12, 14));

        view.setInteractive(false);
        scroller = new ScrollPane(view);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setFitToHeight(true);
        scroller.getStyleClass().add("animator-scroll");

        message.getStyleClass().addAll("mono", "text-muted");
        message.setStyle("-fx-font-size: 12px;");
        message.setWrapText(true);

        nodeHolder.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        getChildren().addAll(Ui.caption(AppContext.i18n().bind("preview.title")), scroller, nodeHolder, message);
        clear();
    }

    /**
     * Recalcula la vista previa cada vez que cambia alguno de los campos.
     *
     * @param renderer devuelve la fórmula, {@code null} si no hay nada que mostrar o lanza una excepción
     *                 con el motivo cuando el texto todavía no es válido
     */
    public LivePreview follow(List<? extends TextInputControl> fields, Supplier<MathLayout.Box> renderer) {
        Runnable update = () -> refresh(renderer);
        for (TextInputControl field : fields) field.textProperty().addListener((obs, old, now) -> update.run());
        AppContext.i18n().localeProperty().addListener((obs, old, now) -> update.run());
        update.run();
        return this;
    }

    /** Como {@link #follow}, pero el renderizador devuelve un nodo cualquiera (por ejemplo, una matriz). */
    public LivePreview followNodes(List<? extends TextInputControl> fields, Supplier<Node> renderer) {
        Runnable update = () -> refreshNode(renderer);
        for (TextInputControl field : fields) field.textProperty().addListener((obs, old, now) -> update.run());
        AppContext.i18n().localeProperty().addListener((obs, old, now) -> update.run());
        update.run();
        return this;
    }

    public void refreshNode(Supplier<Node> renderer) {
        try {
            Node node = renderer.get();
            if (node == null) {
                clear();
                return;
            }
            view.setBox(null);
            nodeHolder.getChildren().setAll(node);
            nodeHolder.setVisible(true);
            nodeHolder.setManaged(true);
            scroller.setVisible(false);
            scroller.setManaged(false);
            message.setVisible(false);
            message.setManaged(false);
            setVisible(true);
            setManaged(true);
        } catch (RuntimeException e) {
            showMessage(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    /** Vuelve a calcular con el renderizador dado (también al cambiar opciones que no son texto). */
    public void refresh(Supplier<MathLayout.Box> renderer) {
        try {
            MathLayout.Box box = renderer.get();
            if (box == null) {
                clear();
            } else {
                show(box);
            }
        } catch (RuntimeException e) {
            showMessage(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    public void show(MathLayout.Box box) {
        nodeHolder.getChildren().clear();
        nodeHolder.setVisible(false);
        nodeHolder.setManaged(false);
        view.setBox(box);
        // La altura sale de las medidas de la fórmula, no de una pasada de diseño: así no depende de cuándo se muestra
        double height = Math.ceil(box.height()) + 2 * 3 + 14;
        scroller.setMinHeight(height);
        scroller.setPrefHeight(height);
        scroller.setMaxHeight(height);
        scroller.setVisible(true);
        scroller.setManaged(true);
        message.setVisible(false);
        message.setManaged(false);
        setVisible(true);
        setManaged(true);
    }

    /** Muestra por qué todavía no hay fórmula, sin quitar la última que se pudo dibujar. */
    public void showMessage(String text) {
        message.setText(text);
        message.setVisible(true);
        message.setManaged(true);
        boolean hasFormula = view.box() != null;
        scroller.setVisible(hasFormula);
        scroller.setManaged(hasFormula);
        nodeHolder.setVisible(!nodeHolder.getChildren().isEmpty());
        nodeHolder.setManaged(!nodeHolder.getChildren().isEmpty());
        setVisible(true);
        setManaged(true);
    }

    public void clear() {
        view.setBox(null);
        nodeHolder.getChildren().clear();
        nodeHolder.setVisible(false);
        nodeHolder.setManaged(false);
        message.setVisible(false);
        message.setManaged(false);
        setVisible(false);
        setManaged(false);
    }

    /** La fórmula mostrada (para pruebas), o {@code null}. */
    public MathLayout.Box box() {
        return view.box();
    }

    /** El mensaje mostrado (para pruebas), o una cadena vacía. */
    public String messageText() {
        return message.isVisible() ? message.getText() : "";
    }

    /** Nodo de la vista previa para colocarlo en un formulario. */
    public Node node() {
        return this;
    }
}
