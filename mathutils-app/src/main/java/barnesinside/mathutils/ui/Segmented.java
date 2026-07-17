package barnesinside.mathutils.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.function.Function;

/**
 * Control segmentado (una opción activa entre varias), por ejemplo RAD / DEG / GRAD.
 *
 * @param <T> tipo de los valores
 */
public final class Segmented<T> extends HBox {

    private final ObjectProperty<T> value = new SimpleObjectProperty<>();
    private final ToggleGroup group = new ToggleGroup();

    public Segmented(List<T> options, T initial, Function<T, String> label) {
        getStyleClass().add("segmented");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(2);

        for (T option : options) {
            ToggleButton button = new ToggleButton(label.apply(option));
            button.setToggleGroup(group);
            button.setUserData(option);
            button.setSelected(option.equals(initial));
            getChildren().add(button);
        }
        value.set(initial);

        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                // No se permite quedarse sin opción: se restaura la anterior
                if (old != null) old.setSelected(true);
                return;
            }
            @SuppressWarnings("unchecked")
            T selected = (T) now.getUserData();
            value.set(selected);
        });

        value.addListener((obs, old, now) -> {
            for (var toggle : group.getToggles()) {
                if (toggle.getUserData().equals(now)) toggle.setSelected(true);
            }
        });
    }

    public ObjectProperty<T> valueProperty() {
        return value;
    }

    public T getValue() {
        return value.get();
    }

    public void setValue(T newValue) {
        value.set(newValue);
    }
}
