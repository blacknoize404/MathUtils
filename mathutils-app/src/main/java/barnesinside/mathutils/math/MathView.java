package barnesinside.mathutils.math;

import MathUtils.cas.expr.Expr;
import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.theme.Palette;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Componente que dibuja una expresión matemática con composición tipográfica (fracciones,
 * raíces, potencias, paréntesis). Es interactivo: al pasar el ratón resalta la subexpresión bajo
 * el cursor y la publica en {@link #hoveredProperty()}.
 * <p>Se puede intercalar dentro de un {@code TextFlow}: su línea base es la de la fórmula.
 */
public final class MathView extends Region {

    private static final double DEFAULT_PADDING = 3;

    private final Canvas canvas = new Canvas();
    private final ObjectProperty<Expr> hovered = new SimpleObjectProperty<>();
    private MathLayout.Box box;
    private double fontSize;
    private final double padding;
    private boolean muted;
    private boolean interactive = true;
    private java.util.List<MathLayout.Hit> hits = java.util.List.of();

    public MathView(Expr expr, double fontSize) {
        this(expr, fontSize, DEFAULT_PADDING);
    }

    /** Fórmula para intercalar en un texto: casi sin relleno, para que no separe las palabras. */
    public static MathView inline(Expr expr, double fontSize) {
        return new MathView(expr, fontSize, 1);
    }

    private MathView(Expr expr, double fontSize, double padding) {
        this.fontSize = fontSize;
        this.padding = padding;
        getChildren().add(canvas);
        setMouseTransparent(false);

        setOnMouseMoved(e -> {
            if (!interactive) return;
            hovered.set(find(e.getX() - padding, e.getY()));
        });
        setOnMouseExited(e -> hovered.set(null));

        hovered.addListener((obs, old, now) -> redraw());
        AppContext.theme().modeProperty().addListener((obs, old, now) -> redraw());

        setExpr(expr);
    }

    /** Fórmula vacía; se rellena después con {@link #setExpr(Expr)} o {@link #setBox}. */
    public static MathView empty(double fontSize) {
        return new MathView(null, fontSize);
    }

    public void setExpr(Expr expr) {
        setBox(expr == null ? null : MathLayout.of(expr, fontSize));
    }

    /** Dibuja una caja ya construida (por ejemplo, una ecuación de derivada). */
    public void setBox(MathLayout.Box newBox) {
        this.box = newBox;
        hovered.set(null);
        requestLayout();
        redraw();
    }

    public void setFontSize(double size) {
        this.fontSize = size;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        redraw();
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        if (!interactive) hovered.set(null);
    }

    /** Subexpresión bajo el cursor, o {@code null}. */
    public ObjectProperty<Expr> hoveredProperty() {
        return hovered;
    }

    /** Resalta una subexpresión desde fuera (por ejemplo, desde el árbol). */
    public void highlight(Expr expr) {
        hovered.set(expr);
    }

    public MathLayout.Box box() {
        return box;
    }

    // ---------------------------------------------------------------- Medidas y dibujo

    @Override
    protected double computePrefWidth(double height) {
        return box == null ? 0 : Math.ceil(box.width()) + 2 * padding;
    }

    @Override
    protected double computePrefHeight(double width) {
        return box == null ? 0 : Math.ceil(box.height()) + 2 * padding;
    }

    @Override
    protected double computeMinWidth(double height) {
        return computePrefWidth(height);
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected double computeMaxWidth(double height) {
        return computePrefWidth(height);
    }

    @Override
    protected double computeMaxHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    public double getBaselineOffset() {
        return box == null ? 0 : padding + box.ascent();
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(Math.max(1, getWidth()));
        canvas.setHeight(Math.max(1, getHeight()));
        redraw();
    }

    private void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (box == null) {
            hits = java.util.List.of();
            return;
        }
        Palette palette = AppContext.theme().palette();
        Color color = muted ? palette.muted() : palette.fg();
        MathLayout.Context ctx = new MathLayout.Context(palette, color, hovered.get());
        MathLayout.paint(box, g, padding, padding + box.ascent(), ctx);
        hits = ctx.hits();
    }

    /** La zona más pequeña que contiene el punto (la subexpresión más interna). */
    private Expr find(double x, double y) {
        Expr best = null;
        double bestArea = Double.MAX_VALUE;
        for (MathLayout.Hit hit : hits) {
            if (hit.area().contains(x, y)) {
                double area = hit.area().getWidth() * hit.area().getHeight();
                if (area <= bestArea) {
                    best = hit.expr();
                    bestArea = area;
                }
            }
        }
        return best;
    }
}
