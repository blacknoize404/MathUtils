package barnesinside.mathutils.ui;

import barnesinside.mathutils.theme.Brand;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Sección con un título que se pliega y se despliega al pulsarlo, para no apilar todo en la pantalla. */
public final class Collapsible extends VBox {

    private final Node content;
    private final Node chevronDown = Brand.icon("chevron-down", 16);
    private final Node chevronRight = Brand.icon("chevron-right", 16);
    private boolean open;

    public Collapsible(StringBinding title, Node content, boolean open) {
        super(8);
        this.content = content;
        HBox header = new HBox(6, chevronDown, chevronRight, Ui.caption(title));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("toggle-chevron");
        header.setOnMouseClicked(e -> setOpen(!this.open));
        getChildren().addAll(header, content);
        setOpen(open);
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean value) {
        open = value;
        content.setVisible(value);
        content.setManaged(value);
        chevronDown.setVisible(value);
        chevronDown.setManaged(value);
        chevronRight.setVisible(!value);
        chevronRight.setManaged(!value);
    }
}
