package barnesinside.mathutils;

import MathUtils.cas.calc.CalculatorSession;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.function.Consumer;

/**
 * Área de trabajo compartida entre los módulos: la sesión de calculadora (variables, funciones,
 * historial) y la última expresión escrita, para enviarla de un módulo a otro.
 */
public final class Workspace {

    private final CalculatorSession session = new CalculatorSession();
    private final StringProperty expression = new SimpleStringProperty("");
    private final ObjectProperty<Consumer<String>> navigator = new SimpleObjectProperty<>();

    public CalculatorSession session() {
        return session;
    }

    /** Última expresión enviada a otro módulo. */
    public StringProperty expressionProperty() {
        return expression;
    }

    /** Lo registra la ventana principal para poder abrir un módulo por su identificador. */
    public void setNavigator(Consumer<String> navigator) {
        this.navigator.set(navigator);
    }

    /**
     * Envía una expresión a otro módulo y lo abre.
     *
     * @param moduleId identificador del módulo de destino
     */
    public void sendTo(String moduleId, String expressionText) {
        expression.set(expressionText);
        Consumer<String> go = navigator.get();
        if (go != null) go.accept(moduleId);
    }
}
