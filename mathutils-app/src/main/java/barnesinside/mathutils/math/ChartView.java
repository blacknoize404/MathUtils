package barnesinside.mathutils.math;

import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.theme.Brand;
import barnesinside.mathutils.theme.Palette;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

/**
 * Gráfico genérico de series de datos: líneas, puntos, barras y vectores desde el origen (para el
 * plano de Argand). Los colores salen de la paleta de visualización de datos de la marca, así que
 * siguen al tema claro u oscuro.
 */
public final class ChartView extends Region {

    /** Forma de dibujar una serie. */
    public enum Kind {LINE, POINTS, BARS, VECTOR}

    /**
     * @param color índice en la paleta de visualización (violeta, cian, magenta, ámbar, esmeralda, rojo);
     *              {@code -1} usa el color del texto
     */
    public record Series(String name, double[] x, double[] y, Kind kind, int color) {
    }

    private static final double MARGIN_LEFT = 50;
    private static final double MARGIN_BOTTOM = 26;
    private static final double MARGIN = 14;

    private final Canvas canvas = new Canvas();
    private final List<Series> series = new ArrayList<>();
    private boolean equalAspect;
    private double xMin;
    private double xMax;
    private double yMin;
    private double yMax;

    public ChartView(double height) {
        setMinHeight(height);
        setPrefHeight(height);
        getChildren().add(canvas);
        AppContext.theme().modeProperty().addListener((obs, old, now) -> redraw());
    }

    /** Mantiene la misma escala en los dos ejes (para ver ángulos reales en el plano complejo). */
    public ChartView equalAspect(boolean value) {
        this.equalAspect = value;
        return this;
    }

    public ChartView add(Series s) {
        series.add(s);
        fit();
        redraw();
        return this;
    }

    public List<Series> series() {
        return List.copyOf(series);
    }

    // ---------------------------------------------------------------- Ventana

    private void fit() {
        double x0 = Double.POSITIVE_INFINITY;
        double x1 = Double.NEGATIVE_INFINITY;
        double y0 = Double.POSITIVE_INFINITY;
        double y1 = Double.NEGATIVE_INFINITY;
        for (Series s : series) {
            for (int i = 0; i < s.x().length; i++) {
                if (!Double.isFinite(s.x()[i]) || !Double.isFinite(s.y()[i])) continue;
                x0 = Math.min(x0, s.x()[i]);
                x1 = Math.max(x1, s.x()[i]);
                y0 = Math.min(y0, s.y()[i]);
                y1 = Math.max(y1, s.y()[i]);
                if (s.kind() == Kind.BARS) y0 = Math.min(y0, 0);
                if (s.kind() == Kind.VECTOR) {
                    x0 = Math.min(x0, 0);
                    x1 = Math.max(x1, 0);
                    y0 = Math.min(y0, 0);
                    y1 = Math.max(y1, 0);
                }
            }
        }
        if (!Double.isFinite(x0)) {
            x0 = -1;
            x1 = 1;
            y0 = -1;
            y1 = 1;
        }
        if (x1 - x0 < 1e-9) {
            x0 -= 1;
            x1 += 1;
        }
        if (y1 - y0 < 1e-9) {
            y0 -= 1;
            y1 += 1;
        }
        boolean bars = series.stream().anyMatch(s -> s.kind() == Kind.BARS);
        double padX = (x1 - x0) * (bars ? 0.08 : 0.1);
        double padY = (y1 - y0) * 0.12;
        xMin = x0 - padX;
        xMax = x1 + padX;
        yMin = y0 - (bars ? 0 : padY);
        yMax = y1 + padY;
    }

    // ---------------------------------------------------------------- Dibujo

    @Override
    protected double computePrefWidth(double height) {
        return 0;
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(Math.max(1, getWidth()));
        canvas.setHeight(Math.max(1, getHeight()));
        redraw();
    }

    private double plotWidth() {
        return canvas.getWidth() - MARGIN_LEFT - MARGIN;
    }

    private double plotHeight() {
        return canvas.getHeight() - MARGIN - MARGIN_BOTTOM;
    }

    private double px(double x) {
        return MARGIN_LEFT + (x - xMin) / (xMax - xMin) * plotWidth();
    }

    private double py(double y) {
        return MARGIN + (yMax - y) / (yMax - yMin) * plotHeight();
    }

    private void applyAspect() {
        if (!equalAspect || plotWidth() <= 0 || plotHeight() <= 0) return;
        double cx = (xMin + xMax) / 2;
        double cy = (yMin + yMax) / 2;
        double unitsX = (xMax - xMin) / plotWidth();
        double unitsY = (yMax - yMin) / plotHeight();
        double units = Math.max(unitsX, unitsY);
        xMin = cx - units * plotWidth() / 2;
        xMax = cx + units * plotWidth() / 2;
        yMin = cy - units * plotHeight() / 2;
        yMax = cy + units * plotHeight() / 2;
    }

    private void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (series.isEmpty() || canvas.getWidth() < 80) return;
        fit();
        applyAspect();
        Palette palette = AppContext.theme().palette();

        drawGrid(g, palette);
        for (int i = 0; i < series.size(); i++) drawSeries(g, palette, series.get(i));
        drawLegend(g, palette);
    }

    private static Color color(Palette palette, int index) {
        List<Color> viz = List.of(palette.violet(), palette.cyan(), palette.magenta(), palette.amber(),
                palette.emerald(), palette.red());
        return index < 0 ? palette.fg() : viz.get(index % viz.size());
    }

    private void drawGrid(GraphicsContext g, Palette palette) {
        g.setFont(Font.font(Brand.MONO, 11));
        g.setLineWidth(1);
        double bottom = canvas.getHeight() - MARGIN_BOTTOM;
        double right = canvas.getWidth() - MARGIN;

        for (double x : PlotView.ticks(xMin, xMax)) {
            double sx = Math.round(px(x)) + 0.5;
            g.setStroke(palette.border());
            g.strokeLine(sx, MARGIN, sx, bottom);
            g.setFill(palette.muted());
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(PlotView.label(x), sx, bottom + 17);
        }
        for (double y : PlotView.ticks(yMin, yMax)) {
            double sy = Math.round(py(y)) + 0.5;
            g.setStroke(palette.border());
            g.strokeLine(MARGIN_LEFT, sy, right, sy);
            g.setFill(palette.muted());
            g.setTextAlign(TextAlignment.RIGHT);
            g.fillText(PlotView.label(y), MARGIN_LEFT - 6, sy + 4);
        }

        g.setStroke(palette.muted());
        g.setLineWidth(1.4);
        if (xMin < 0 && xMax > 0) g.strokeLine(Math.round(px(0)) + 0.5, MARGIN, Math.round(px(0)) + 0.5, bottom);
        if (yMin < 0 && yMax > 0) g.strokeLine(MARGIN_LEFT, Math.round(py(0)) + 0.5, right, Math.round(py(0)) + 0.5);
    }

    private void drawSeries(GraphicsContext g, Palette palette, Series s) {
        Color color = color(palette, s.color());
        switch (s.kind()) {
            case LINE -> {
                g.setStroke(color);
                g.setLineWidth(2);
                g.setLineCap(StrokeLineCap.ROUND);
                g.setLineJoin(StrokeLineJoin.ROUND);
                boolean drawing = false;
                for (int i = 0; i < s.x().length; i++) {
                    if (!Double.isFinite(s.x()[i]) || !Double.isFinite(s.y()[i])) {
                        if (drawing) g.stroke();
                        drawing = false;
                        continue;
                    }
                    if (!drawing) {
                        g.beginPath();
                        g.moveTo(px(s.x()[i]), py(s.y()[i]));
                        drawing = true;
                    } else {
                        g.lineTo(px(s.x()[i]), py(s.y()[i]));
                    }
                }
                if (drawing) g.stroke();
            }
            case POINTS -> {
                g.setFill(color);
                for (int i = 0; i < s.x().length; i++) {
                    if (Double.isFinite(s.x()[i]) && Double.isFinite(s.y()[i])) {
                        g.fillOval(px(s.x()[i]) - 4, py(s.y()[i]) - 4, 8, 8);
                    }
                }
            }
            case BARS -> {
                double spacing = Double.MAX_VALUE;
                for (int i = 1; i < s.x().length; i++) spacing = Math.min(spacing, Math.abs(s.x()[i] - s.x()[i - 1]));
                if (spacing == Double.MAX_VALUE) spacing = (xMax - xMin) / 10;
                double width = Math.max(2, spacing / (xMax - xMin) * plotWidth() * 0.86);
                g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.75));
                for (int i = 0; i < s.x().length; i++) {
                    double top = py(Math.max(0, s.y()[i]));
                    double base = py(Math.min(0, s.y()[i]));
                    g.fillRect(px(s.x()[i]) - width / 2, top, width, Math.max(1, base - top));
                }
            }
            case VECTOR -> {
                g.setStroke(color);
                g.setFill(color);
                g.setLineWidth(2);
                for (int i = 0; i < s.x().length; i++) {
                    double x0 = px(0);
                    double y0 = py(0);
                    double x1 = px(s.x()[i]);
                    double y1 = py(s.y()[i]);
                    g.strokeLine(x0, y0, x1, y1);
                    double angle = Math.atan2(y1 - y0, x1 - x0);
                    double head = 9;
                    g.fillPolygon(new double[]{x1, x1 - head * Math.cos(angle - 0.4), x1 - head * Math.cos(angle + 0.4)},
                            new double[]{y1, y1 - head * Math.sin(angle - 0.4), y1 - head * Math.sin(angle + 0.4)}, 3);
                }
            }
        }
    }

    private void drawLegend(GraphicsContext g, Palette palette) {
        List<Series> named = series.stream().filter(s -> s.name() != null && !s.name().isBlank()).toList();
        if (named.size() < 2 && !(named.size() == 1 && series.size() > 1)) return;
        g.setFont(Font.font(Brand.MONO, 11));
        g.setTextAlign(TextAlignment.LEFT);
        double x = MARGIN_LEFT + 10;
        double y = MARGIN + 14;
        for (Series s : named) {
            g.setFill(color(palette, s.color()));
            g.fillRoundRect(x, y - 9, 10, 10, 3, 3);
            g.setFill(palette.fg());
            g.fillText(s.name(), x + 16, y);
            y += 16;
        }
    }
}
