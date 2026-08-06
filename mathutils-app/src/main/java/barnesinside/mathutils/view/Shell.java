package barnesinside.mathutils.view;

import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.I18n;
import barnesinside.mathutils.Workspace;
import barnesinside.mathutils.theme.Brand;
import barnesinside.mathutils.theme.Motion;
import barnesinside.mathutils.theme.ThemeManager;
import barnesinside.mathutils.ui.Segmented;
import barnesinside.mathutils.ui.Toasts;
import barnesinside.mathutils.ui.Ui;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ventana principal: cabecera con la marca, navegación lateral y el módulo activo.
 */
public final class Shell extends StackPane {

    private final I18n t = AppContext.i18n();
    private final ThemeManager theme = AppContext.theme();
    private final Workspace workspace;

    private final List<Module> modules = new ArrayList<>();
    private final List<HBox> navItems = new ArrayList<>();
    private final StackPane stage = new StackPane();
    private final ImageView logo = new ImageView();
    private Module active;

    public Shell(Workspace workspace) {
        this.workspace = workspace;
        Brand.load();

        modules.add(new CalculatorView(workspace));
        modules.add(new ExpressionTreeView(workspace));
        modules.add(new OrderView(workspace));
        modules.add(new DerivativeView(workspace));
        modules.add(new LimitView(workspace));
        modules.add(new IntegralView(workspace));
        modules.add(new MatrixView(workspace));
        modules.add(new ComplexView(workspace));
        modules.add(new StatisticsView(workspace));
        modules.add(new OdeView(workspace));

        BorderPane layout = new BorderPane();
        layout.setTop(header());
        layout.setLeft(navigation());
        layout.setCenter(stage);

        StackPane toastHost = new StackPane();
        toastHost.setPickOnBounds(false);
        toastHost.setMouseTransparent(true);
        Toasts.install(toastHost);

        getChildren().addAll(layout, toastHost);
        workspace.setNavigator(this::navigate);
        installShortcuts();
        navigate(modules.get(0).id());
    }

    public Module activeModule() {
        return active;
    }

    public List<Module> modules() {
        return modules;
    }

    // ================================================================ Cabecera

    private Node header() {
        logo.setFitHeight(26);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        updateLogo();
        theme.modeProperty().addListener((obs, old, now) -> updateLogo());

        Label wordmark = Ui.label("mathutils", "mono", "semibold");
        wordmark.setStyle("-fx-font-size: 17px;");
        Label studio = Ui.caption("studio");
        HBox brand = new HBox(12, logo, wordmark, studio);
        brand.setAlignment(Pos.CENTER_LEFT);

        Segmented<Locale> language = new Segmented<>(List.of(I18n.SPANISH, I18n.ENGLISH), t.locale(),
                locale -> locale.getLanguage().toUpperCase(Locale.ROOT));
        language.valueProperty().addListener((obs, old, now) -> t.setLocale(now));

        Button themeToggle = new Button();
        themeToggle.getStyleClass().add("btn-icon");
        updateThemeIcon(themeToggle);
        themeToggle.tooltipProperty().set(new javafx.scene.control.Tooltip());
        themeToggle.getTooltip().textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> t.get(theme.isDark() ? "header.theme.toLight" : "header.theme.toDark"),
                t.localeProperty(), theme.modeProperty()));
        themeToggle.setOnAction(e -> {
            RotateTransition spin = new RotateTransition(Motion.millis(450), themeToggle);
            spin.setByAngle(180);
            spin.setInterpolator(Motion.SPRING);
            spin.setOnFinished(done -> themeToggle.setRotate(0));
            spin.play();
            theme.toggle();
            updateThemeIcon(themeToggle);
        });
        Ui.pressEffect(themeToggle);

        Button about = Ui.iconButton("info", t.bind("header.about"), () -> AboutDialog.show(getScene() == null ? null : getScene().getWindow()));
        HBox bar = new HBox(16, brand, Ui.spacer(), language, about, themeToggle);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setFillHeight(false);
        bar.setPrefHeight(64);
        bar.setMinHeight(64);
        bar.getStyleClass().addAll("app-header", "hairline-bottom");
        return bar;
    }

    private void updateLogo() {
        logo.setImage(Brand.mark(theme.isDark()));
    }

    private void updateThemeIcon(Button button) {
        button.setGraphic(Brand.icon(theme.isDark() ? "sun" : "moon", 18));
    }

    // ================================================================ Navegación

    private Node navigation() {
        VBox rail = new VBox(4);
        rail.getStyleClass().addAll("nav-rail", "hairline-right");
        rail.setPrefWidth(236);
        rail.setMinWidth(236);

        for (Module module : modules) {
            HBox item = new HBox(12, Brand.icon(module.icon(), 18), Ui.label(module.title()));
            item.setAlignment(Pos.CENTER_LEFT);
            item.getStyleClass().add("nav-item");
            item.setFocusTraversable(true);
            item.setOnMouseClicked(e -> navigate(module.id()));
            item.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) navigate(module.id());
            });
            Ui.pressEffect(item);
            navItems.add(item);
            rail.getChildren().add(item);
        }

        Label tagline = Ui.label(t.bind("app.tagline"), "text-muted", "body-sm");
        tagline.setWrapText(true);
        rail.getChildren().addAll(Ui.spacer(), tagline);
        return rail;
    }

    /** Abre el módulo con ese identificador con una transición suave. */
    public void navigate(String id) {
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            boolean match = module.id().equals(id);
            navItems.get(i).getStyleClass().remove("active");
            if (match) {
                navItems.get(i).getStyleClass().add("active");
                if (module != active) show(module);
                else module.onShow();
            }
        }
    }

    private void show(Module module) {
        active = module;
        Node node = module.content();
        stage.getChildren().setAll(node);
        VBox.setVgrow(node, Priority.ALWAYS);
        module.onShow();

        node.setOpacity(0);
        node.setTranslateY(16);
        FadeTransition fade = new FadeTransition(Motion.millis(Motion.NORMAL), node);
        fade.setToValue(1);
        fade.setInterpolator(Motion.IN_OUT);
        TranslateTransition slide = new TranslateTransition(Motion.millis(Motion.NORMAL), node);
        slide.setToY(0);
        slide.setInterpolator(Motion.IN_OUT);
        new ParallelTransition(fade, slide).play();
    }

    /** Termina de golpe la transición de entrada del módulo (para capturas y pruebas). */
    public void settle() {
        for (Node node : stage.getChildren()) {
            node.setOpacity(1);
            node.setTranslateY(0);
        }
    }

    private void installShortcuts() {
        sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) return;
            for (int i = 0; i < Math.min(9, modules.size()); i++) {
                String id = modules.get(i).id();
                scene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.valueOf("DIGIT" + (i + 1)), KeyCombination.SHORTCUT_DOWN),
                        () -> navigate(id));
            }
        });
    }

    /** Margen estándar del contenido, usado por los módulos. */
    public static Insets contentPadding() {
        return new Insets(32);
    }
}
