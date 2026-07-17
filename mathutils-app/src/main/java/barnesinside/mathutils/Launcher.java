package barnesinside.mathutils;

import javafx.application.Application;

/**
 * Punto de entrada. Se separa de la clase {@code Application} para poder ejecutar la app desde
 * el classpath sin configurar el módulo de JavaFX.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(MathUtilsApp.class, args);
    }
}
