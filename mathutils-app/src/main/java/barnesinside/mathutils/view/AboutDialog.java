package barnesinside.mathutils.view;

import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.AppInfo;
import barnesinside.mathutils.I18n;
import barnesinside.mathutils.theme.Brand;
import barnesinside.mathutils.ui.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * «Acerca de»: el nombre y la versión de MathUtils Studio y, aparte, quién la creó, con el logo y la
 * web de barnesinside, el código fuente y las tipografías e iconos de terceros.
 */
public final class AboutDialog {

    private AboutDialog() {
    }

    /** Abre el diálogo sobre la ventana dada (o suelto, si no hay). */
    public static Stage show(Window owner) {
        I18n t = AppContext.i18n();
        boolean dark = AppContext.theme().isDark();

        ImageView mark = new ImageView(Brand.mark(dark));
        mark.setFitHeight(44);
        mark.setPreserveRatio(true);
        mark.setSmooth(true);
        Label name = Ui.label(AppInfo.NAME, "headline");
        Label version = Ui.label(t.get("about.version", AppInfo.version()), "mono", "text-muted");
        HBox title = new HBox(16, mark, new VBox(2, name, version));
        title.setAlignment(Pos.CENTER_LEFT);

        Label description = Ui.label(t.get("about.description"), "text-muted");
        description.setWrapText(true);

        ImageView logo = new ImageView(Brand.logo(Brand.Logo.IMAGOTIPO, dark));
        logo.setFitHeight(26);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);

        VBox content = new VBox(20, title, description,
                section(t.get("about.createdBy"), logo, link(AppInfo.AUTHOR_URL.replace("https://", ""), AppInfo.AUTHOR_URL)),
                section(t.get("about.source"), link(AppInfo.SOURCE_URL.replace("https://", ""), AppInfo.SOURCE_URL)),
                section(t.get("about.credits"), credit(t.get("about.credits.fonts")), credit(t.get("about.credits.icons"))));
        content.setPadding(new Insets(28));
        content.setPrefWidth(460);

        Stage stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        }
        stage.titleProperty().bind(t.bind("about.title"));
        stage.getIcons().add(Brand.appIcon());
        stage.setResizable(false);
        Scene scene = new Scene(content);
        AppContext.theme().install(scene);
        stage.setScene(scene);
        stage.show();
        return stage;
    }

    private static Node section(String caption, Node... items) {
        VBox box = new VBox(8, items);
        box.getChildren().add(0, Ui.caption(caption));
        return box;
    }

    private static Node credit(String text) {
        Label label = Ui.label(text, "body-sm", "text-muted");
        label.setWrapText(true);
        return label;
    }

    private static Hyperlink link(String text, String url) {
        Hyperlink link = new Hyperlink(text);
        link.setPadding(new Insets(0));
        link.setOnAction(e -> AppContext.openLink(url));
        return link;
    }
}
