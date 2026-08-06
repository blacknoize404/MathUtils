package barnesinside.mathutils;

import barnesinside.mathutils.theme.Brand;
import barnesinside.mathutils.view.Shell;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * MathUtils Studio: calculadora, árbol de expresión, orden de operaciones y solucionadores
 * paso a paso sobre la librería MathUtils, con la identidad de barnesinside.
 */
public final class MathUtilsApp extends Application {

    @Override
    public void start(Stage stage) {
        Brand.load();

        Shell shell = new Shell(new Workspace());
        Scene scene = new Scene(shell, 1360, 860);
        AppContext.theme().install(scene);

        stage.titleProperty().bind(AppContext.i18n().bind("app.title"));
        AppContext.setHostServices(getHostServices());
        stage.getIcons().add(Brand.appIcon());
        stage.setMinWidth(1040);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }
}
