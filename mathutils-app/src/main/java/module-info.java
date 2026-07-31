/**
 * MathUtils Studio: aplicación de escritorio JavaFX que explota la librería MathUtils.
 * <p>El módulo es abierto porque sus recursos (fuentes, iconos, CSS y textos) se cargan por nombre
 * y JavaFX necesita acceso reflectivo a la clase de la aplicación.
 */
open module barnesinside.mathutils {
    requires MathUtils;
    requires javafx.controls;
    requires javafx.swing;
    requires java.prefs;
}
