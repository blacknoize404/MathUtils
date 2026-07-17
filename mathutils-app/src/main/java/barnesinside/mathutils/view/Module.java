package barnesinside.mathutils.view;

import javafx.beans.binding.StringBinding;
import javafx.scene.Node;

/**
 * Un módulo de la aplicación: una herramienta con su entrada en la navegación.
 */
public interface Module {

    /** Identificador estable, usado para navegar entre módulos. */
    String id();

    /** Nombre mostrado en la navegación (se actualiza con el idioma). */
    StringBinding title();

    /** Nombre del icono Lucide de la navegación. */
    String icon();

    /** Contenido del módulo. */
    Node content();

    /** Se llama cada vez que el módulo pasa a ser el visible. */
    default void onShow() {
    }
}
