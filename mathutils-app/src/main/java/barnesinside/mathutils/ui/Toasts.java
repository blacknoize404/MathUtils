package barnesinside.mathutils.ui;

import barnesinside.mathutils.theme.Motion;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Mensajes breves que aparecen abajo y se desvanecen solos. Confirman acciones como "Copiado."
 */
public final class Toasts {

    private static StackPane host;

    private Toasts() {
    }

    /** Registra el contenedor sobre el que se muestran los mensajes. */
    public static void install(StackPane overlay) {
        host = overlay;
    }

    public static void show(String message) {
        if (host == null) return;

        Label label = new Label(message);
        label.getStyleClass().add("toast");
        label.setMouseTransparent(true);
        StackPane.setAlignment(label, Pos.BOTTOM_CENTER);
        StackPane.setMargin(label, new Insets(0, 0, 32, 0));
        host.getChildren().add(label);

        label.setOpacity(0);
        label.setTranslateY(12);
        FadeTransition in = new FadeTransition(Motion.millis(Motion.NORMAL), label);
        in.setToValue(1);
        in.setInterpolator(Motion.IN_OUT);
        TranslateTransition rise = new TranslateTransition(Motion.millis(Motion.NORMAL), label);
        rise.setToY(0);
        rise.setInterpolator(Motion.IN_OUT);
        FadeTransition out = new FadeTransition(Motion.millis(Motion.NORMAL), label);
        out.setToValue(0);

        SequentialTransition sequence = new SequentialTransition(
                new javafx.animation.ParallelTransition(in, rise),
                new PauseTransition(Duration.millis(1400)),
                out);
        sequence.setOnFinished(e -> host.getChildren().remove(label));
        sequence.play();
    }
}
