package barnesinside.mathutils.view;

import MathUtils.cas.tree.TreeLayout;
import MathUtils.cas.tree.TreeModel;
import MathUtils.cas.tree.TreeNode;
import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.theme.Brand;
import barnesinside.mathutils.theme.Palette;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Dibujo interactivo de un {@link TreeModel}: nodos coloreados por categoría, aristas hairline,
 * número de orden de evaluación en cada operación, zoom con la rueda y desplazamiento arrastrando.
 * <p>Los colores de categoría son los acentos extendidos del sistema de diseño (violeta, cian,
 * magenta, ámbar, esmeralda); el único acento de marca (menta) marca la selección y el orden.
 */
public final class TreeCanvas extends Pane {

    private static final double NODE_HEIGHT = 34;
    private static final double ROW_HEIGHT = 84;
    private static final double MIN_NODE_WIDTH = 40;
    private static final double PADDING = 40;

    private final Canvas canvas = new Canvas();
    private final ObjectProperty<TreeNode> selected = new SimpleObjectProperty<>();
    private final ObjectProperty<TreeNode> hovered = new SimpleObjectProperty<>();

    private TreeModel model;
    private Map<Integer, TreeLayout.Position> layout = Map.of();
    private double cellWidth = 80;

    private double scale = 1;
    private double offsetX;
    private double offsetY;
    private double dragStartX;
    private double dragStartY;
    private double dragOriginX;
    private double dragOriginY;
    private boolean dragged;

    private boolean showOrder = true;
    private Set<Integer> dimmed = Set.of();
    private Integer current;

    public TreeCanvas() {
        getChildren().add(canvas);
        setMinSize(200, 200);

        setOnMousePressed(e -> {
            dragStartX = e.getX();
            dragStartY = e.getY();
            dragOriginX = offsetX;
            dragOriginY = offsetY;
            dragged = false;
        });
        setOnMouseDragged(e -> {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            if (Math.abs(dx) + Math.abs(dy) > 3) dragged = true;
            offsetX = dragOriginX + dx;
            offsetY = dragOriginY + dy;
            redraw();
        });
        setOnMouseReleased(e -> {
            if (!dragged) selected.set(nodeAt(e.getX(), e.getY()));
        });
        setOnMouseMoved(e -> {
            TreeNode node = nodeAt(e.getX(), e.getY());
            if (node != hovered.get()) {
                hovered.set(node);
                setCursor(node == null ? javafx.scene.Cursor.DEFAULT : javafx.scene.Cursor.HAND);
                redraw();
            }
        });
        setOnMouseExited(e -> {
            hovered.set(null);
            redraw();
        });
        setOnScroll(e -> {
            double factor = Math.pow(1.1, e.getDeltaY() / 40.0);
            zoomAt(e.getX(), e.getY(), factor);
        });

        AppContext.theme().modeProperty().addListener((obs, old, now) -> redraw());
    }

    // ---------------------------------------------------------------- Estado

    public void setModel(TreeModel newModel) {
        this.model = newModel;
        selected.set(null);
        hovered.set(null);
        dimmed = Set.of();
        current = null;

        if (newModel == null) {
            layout = Map.of();
            redraw();
            return;
        }
        layout = TreeLayout.compute(newModel);
        double widest = MIN_NODE_WIDTH;
        for (TreeNode n : newModel.nodes()) widest = Math.max(widest, nodeWidth(n));
        cellWidth = widest + 28;
        fit();
    }

    public TreeModel model() {
        return model;
    }

    public ObjectProperty<TreeNode> selectedProperty() {
        return selected;
    }

    public ObjectProperty<TreeNode> hoveredProperty() {
        return hovered;
    }

    /** Muestra u oculta los números de orden de evaluación. */
    public void setShowOrder(boolean show) {
        this.showOrder = show;
        redraw();
    }

    /**
     * Estado de una reproducción paso a paso: los nodos ya resueltos aparecen atenuados y el
     * nodo actual resaltado con el acento.
     */
    public void setStepState(Set<Integer> dimmedIds, Integer currentId) {
        this.dimmed = new HashSet<>(dimmedIds);
        this.current = currentId;
        redraw();
    }

    // ---------------------------------------------------------------- Vista

    /** Ajusta el zoom y el desplazamiento para que todo el árbol quepa centrado. */
    public void fit() {
        if (model == null || getWidth() <= 0 || getHeight() <= 0) {
            redraw();
            return;
        }
        double width = contentWidth();
        double height = contentHeight();
        scale = Math.min(1.4, Math.min((getWidth() - 2 * PADDING) / width, (getHeight() - 2 * PADDING) / height));
        scale = Math.max(0.2, scale);
        offsetX = (getWidth() - width * scale) / 2;
        offsetY = (getHeight() - height * scale) / 2;
        redraw();
    }

    private void zoomAt(double x, double y, double factor) {
        double newScale = Math.max(0.2, Math.min(3, scale * factor));
        double ratio = newScale / scale;
        offsetX = x - (x - offsetX) * ratio;
        offsetY = y - (y - offsetY) * ratio;
        scale = newScale;
        redraw();
    }

    public void zoomIn() {
        zoomAt(getWidth() / 2, getHeight() / 2, 1.25);
    }

    public void zoomOut() {
        zoomAt(getWidth() / 2, getHeight() / 2, 0.8);
    }

    private double contentWidth() {
        double maxX = layout.values().stream().mapToDouble(TreeLayout.Position::x).max().orElse(0);
        return (maxX + 1) * cellWidth;
    }

    private double contentHeight() {
        double maxY = layout.values().stream().mapToDouble(TreeLayout.Position::y).max().orElse(0);
        return maxY * ROW_HEIGHT + NODE_HEIGHT + 24;
    }

    @Override
    protected void layoutChildren() {
        boolean first = canvas.getWidth() <= 1;
        canvas.setWidth(Math.max(1, getWidth()));
        canvas.setHeight(Math.max(1, getHeight()));
        if (first && model != null) fit();
        else redraw();
    }

    // ---------------------------------------------------------------- Geometría

    private static final Text MEASURER = new Text();

    private static double textWidth(String text, Font font) {
        MEASURER.setText(text);
        MEASURER.setFont(font);
        return MEASURER.getLayoutBounds().getWidth();
    }

    private double nodeWidth(TreeNode n) {
        return Math.max(MIN_NODE_WIDTH, textWidth(n.label(), Brand.monoMedium(14)) + 24);
    }

    private Point2D center(TreeNode n) {
        TreeLayout.Position p = layout.get(n.id());
        return new Point2D(p.x() * cellWidth + cellWidth / 2, p.y() * ROW_HEIGHT + NODE_HEIGHT / 2 + 12);
    }

    private TreeNode nodeAt(double screenX, double screenY) {
        if (model == null) return null;
        double x = (screenX - offsetX) / scale;
        double y = (screenY - offsetY) / scale;
        for (TreeNode n : model.nodes()) {
            Point2D c = center(n);
            double w = nodeWidth(n);
            if (Math.abs(x - c.getX()) <= w / 2 && Math.abs(y - c.getY()) <= NODE_HEIGHT / 2) return n;
        }
        return null;
    }

    // ---------------------------------------------------------------- Dibujo

    private void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setTransform(1, 0, 0, 1, 0, 0);
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (model == null) return;

        Palette palette = AppContext.theme().palette();
        g.save();
        g.translate(offsetX, offsetY);
        g.scale(scale, scale);
        draw(g, palette, true);
        g.restore();
    }

    private void draw(GraphicsContext g, Palette palette, boolean interactive) {
        for (TreeNode n : model.nodes()) {
            Point2D from = center(n);
            for (TreeNode child : n.children()) {
                Point2D to = center(child);
                boolean faded = dimmed.contains(child.id());
                g.setStroke(palette.fg(faded ? 0.12 : 0.35));
                g.setLineWidth(1.2);
                g.strokeLine(from.getX(), from.getY() + NODE_HEIGHT / 2, to.getX(), to.getY() - NODE_HEIGHT / 2);
            }
        }
        for (TreeNode n : model.nodes()) drawNode(g, palette, n, interactive);
    }

    private Color colorOf(TreeNode n, Palette palette) {
        return switch (n.kind()) {
            case OPERATOR -> palette.violet();
            case FUNCTION -> palette.cyan();
            case NEGATION -> palette.emerald();
            case VARIABLE -> palette.amber();
            case CONSTANT -> palette.magenta();
            case NUMBER -> palette.muted();
        };
    }

    private void drawNode(GraphicsContext g, Palette palette, TreeNode n, boolean interactive) {
        Point2D c = center(n);
        double w = nodeWidth(n);
        double x = c.getX() - w / 2;
        double y = c.getY() - NODE_HEIGHT / 2;

        boolean faded = dimmed.contains(n.id());
        boolean isCurrent = current != null && current == n.id();
        boolean isSelected = interactive && n == selected.get();
        boolean isHovered = interactive && n == hovered.get();
        Color color = colorOf(n, palette);

        double fillAlpha = faded ? 0.04 : isCurrent ? 0.25 : 0.12;
        g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), fillAlpha));
        g.fillRoundRect(x, y, w, NODE_HEIGHT, 8, 8);

        if (n.parentheses() > 0) {
            g.setStroke(palette.muted());
            g.setLineWidth(1);
            g.setLineDashes(4, 3);
            g.strokeRoundRect(x - 5, y - 5, w + 10, NODE_HEIGHT + 10, 12, 12);
            g.setLineDashes((double[]) null);
        }

        if (isSelected || isCurrent) {
            g.setStroke(palette.accent());
            g.setLineWidth(2.5);
        } else if (isHovered) {
            g.setStroke(palette.accent());
            g.setLineWidth(1.5);
        } else {
            g.setStroke(faded ? palette.fg(0.2) : color);
            g.setLineWidth(1);
        }
        g.strokeRoundRect(x, y, w, NODE_HEIGHT, 8, 8);

        g.setFont(Brand.monoMedium(14));
        g.setFill(faded ? palette.muted() : palette.fg());
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(n.label(), c.getX(), c.getY() + 1);

        if (showOrder && n.evaluationOrder() != null) {
            double bx = x + w;
            double by = y;
            g.setFill(faded ? palette.fg(0.25) : palette.accent());
            g.fillOval(bx - 9, by - 9, 18, 18);
            g.setFill(Color.BLACK);
            g.setFont(Brand.monoBold(11));
            g.fillText(String.valueOf(n.evaluationOrder()), bx, by + 0.5);
        }
    }

    // ---------------------------------------------------------------- Exportación

    /**
     * Imagen del árbol completo (sin zoom ni desplazamiento) al doble de resolución, con el
     * color de fondo del tema actual.
     */
    public WritableImage exportImage() {
        if (model == null) return null;
        double width = contentWidth() + 2 * 24;
        double height = contentHeight() + 2 * 24;
        double factor = 2;

        Canvas out = new Canvas(width * factor, height * factor);
        GraphicsContext g = out.getGraphicsContext2D();
        Palette palette = AppContext.theme().palette();
        g.setFill(palette.bg());
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.scale(factor, factor);
        g.translate(24, 24);
        draw(g, palette, false);

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(palette.bg());
        return out.snapshot(parameters, null);
    }
}
