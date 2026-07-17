package barnesinside.mathutils.theme;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;

import java.util.prefs.Preferences;

/**
 * Tema claro/oscuro de la aplicación. Cambia las hojas de estilo de las escenas instaladas y
 * recuerda la elección del usuario.
 */
public final class ThemeManager {

    /** Modo de color. */
    public enum Mode {LIGHT, DARK}

    private static final String PREFERENCE_KEY = "theme";

    private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(Mode.DARK);
    private final Preferences preferences = Preferences.userNodeForPackage(ThemeManager.class);

    public ThemeManager() {
        try {
            mode.set(Mode.valueOf(preferences.get(PREFERENCE_KEY, Mode.DARK.name())));
        } catch (IllegalArgumentException e) {
            mode.set(Mode.DARK);
        }
        mode.addListener((obs, old, now) -> preferences.put(PREFERENCE_KEY, now.name()));
    }

    public ObjectProperty<Mode> modeProperty() {
        return mode;
    }

    public Mode mode() {
        return mode.get();
    }

    public boolean isDark() {
        return mode.get() == Mode.DARK;
    }

    public void toggle() {
        mode.set(isDark() ? Mode.LIGHT : Mode.DARK);
    }

    public Palette palette() {
        return isDark() ? Palette.DARK : Palette.LIGHT;
    }

    /** Aplica las hojas de estilo a la escena y las mantiene al cambiar de tema. */
    public void install(Scene scene) {
        Brand.load();
        apply(scene);
        mode.addListener((obs, old, now) -> apply(scene));
    }

    private void apply(Scene scene) {
        scene.getStylesheets().setAll(
                resource(isDark() ? "theme-dark.css" : "theme-light.css"),
                resource("app.css"));
    }

    private static String resource(String name) {
        return ThemeManager.class.getResource("/mathutils/css/" + name).toExternalForm();
    }
}
