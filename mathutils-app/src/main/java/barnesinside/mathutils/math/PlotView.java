package barnesinside.mathutils.math;

import MathUtils.cas.expr.Eval;
import MathUtils.cas.expr.Expr;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gráfica de una función de una variable con el área bajo la curva entre dos límites. Los límites
 * pueden ser infinitos (el área se recorta a la ventana) y la ventana se elige sola a partir de
 * ellos; los puntos donde la función no existe parten la curva.
 */
public final class PlotView extends Region {

    private static final int SAMPLES = 720;
    private static final double MARGIN_LEFT = 46;
    private static final double MARGIN_BOTTOM = 26;
    private static final double MARGIN = 14;

    private final Canvas canvas = new Canvas();
    private final Expr f;
    private final String variable;
    private final double lower;
    private final double upper;

    private double xMin;
    private double xMax;
    private double yMin;
    private double yMax;
    private boolean valid;

    /**
     * @param lower extremo inferior del área, o {@code NaN} si no hay área
     * @param upper extremo superior del área, o {@code NaN} si no hay área
     */
    public PlotView(Expr f, String variable, double lower, double upper) {
        this.f = f;
        this.variable = variable;
        this.lower = lower;
        this.upper = upper;
        setMinHeight(300);
        setPrefHeight(300);
        getChildren().add(canvas);
        AppContext.theme().modeProperty().addListener((obs, old, now) -> redraw());
        chooseWindow();
    }

    /** Si la función se pudo evaluar con una sola variable. */
    public boolean isValid() {
        return valid;
    }

    // ---------------------------------------------------------------- Ventana

    private void chooseWindow() {
        boolean hasArea = !Double.isNaN(lower) && !Double.isNaN(upper);
        if (!hasArea) {
            xMin = -6;
            xMax = 6;
        } else if (Double.isFinite(lower) && Double.isFinite(upper)) {
            double span = Math.max(upper - lower, 1e-6);
            xMin = lower - 0.25 * span;
            xMax = upper + 0.25 * span;
        } else if (Double.isFinite(lower)) {
            double span = Math.max(8, 2 * Math.abs(lower));
            xMin = lower - 0.25 * span;
            xMax = lower + span;
        } else if (Double.isFinite(upper)) {
            double span = Math.max(8, 2 * Math.abs(upper));
            xMin = upper - span;
            xMax = upper + 0.25 * span;
        } else {
            xMin = -6;
            xMax = 6;
        }

        double[] ys = new double[SAMPLES + 1];
        List<Double> finite = new ArrayList<>();
        for (int i = 0; i <= SAMPLES; i++) {
            ys[i] = value(xMin + (xMax - xMin) * i / SAMPLES);
            if (Double.isFinite(ys[i])) finite.add(ys[i]);
        }
        valid = finite.size() > 2;
        if (!valid) {
            yMin = -1;
            yMax = 1;
            return;
        }
        double[] sorted = finite.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        double low = sorted[(int) Math.floor((sorted.length - 1) * 0.03)];
        double high = sorted[(int) Math.ceil((sorted.length - 1) * 0.97)];
        yMin = Math.min(0, low);
        yMax = Math.max(0, high);
        if (yMax - yMin < 1e-9) {
            yMin -= 1;
            yMax += 1;
        }
        double pad = (yMax - yMin) * 0.1;
        yMin -= pad;
        yMax += pad;
    }

    private double value(double x) {
        try {
            return Eval.value(f, Map.of(variable, x));
        } catch (RuntimeException e) {
            return Double.NaN;
        }
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

    private double px(double x) {
        return MARGIN_LEFT + (x - xMin) / (xMax - xMin) * (canvas.getWidth() - MARGIN_LEFT - MARGIN);
    }

    private double py(double y) {
        return MARGIN + (yMax - y) / (yMax - yMin) * (canvas.getHeight() - MARGIN - MARGIN_BOTTOM);
    }

    private void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!valid || canvas.getWidth() < 80) return;
        Palette palette = AppContext.theme().palette();

        drawGrid(g, palette);
        drawArea(g, palette);
        drawCurve(g, palette);
        drawBounds(g, palette);
    }

    private void drawGrid(GraphicsContext g, Palette palette) {
        g.setFont(Font.font(Brand.MONO, 11));
        g.setLineWidth(1);
        double left = MARGIN_LEFT;
        double right = canvas.getWidth() - MARGIN;
        double top = MARGIN;
        double bottom = canvas.getHeight() - MARGIN_BOTTOM;

        for (double x : ticks(xMin, xMax)) {
            double sx = Math.round(px(x)) + 0.5;
            g.setStroke(palette.border());
            g.strokeLine(sx, top, sx, bottom);
            g.setFill(palette.muted());
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(label(x), sx, bottom + 17);
        }
        for (double y : ticks(yMin, yMax)) {
            double sy = Math.round(py(y)) + 0.5;
            g.setStroke(palette.border());
            g.strokeLine(left, sy, right, sy);
            g.setFill(palette.muted());
            g.setTextAlign(TextAlignment.RIGHT);
            g.fillText(label(y), left - 6, sy + 4);
        }

        g.setStroke(palette.muted());
        g.setLineWidth(1.4);
        if (xMin < 0 && xMax > 0) g.strokeLine(Math.round(px(0)) + 0.5, top, Math.round(px(0)) + 0.5, bottom);
        if (yMin < 0 && yMax > 0) g.strokeLine(left, Math.round(py(0)) + 0.5, right, Math.round(py(0)) + 0.5);
    }

    private void drawArea(GraphicsContext g, Palette palette) {
        if (Double.isNaN(lower) || Double.isNaN(upper)) return;
        double from = Math.max(lower, xMin);
        double to = Math.min(upper, xMax);
        if (!(to > from)) return;

        Color fill = Color.color(palette.accent().getRed(), palette.accent().getGreen(), palette.accent().getBlue(), 0.28);
        g.setFill(fill);
        int n = SAMPLES;
        double y0 = py(0);
        List<double[]> run = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            double x = from + (to - from) * i / n;
            double y = value(x);
            if (Double.isFinite(y)) {
                run.add(new double[]{px(x), py(clamp(y))});
            } else {
                fillRun(g, run, y0);
                run.clear();
            }
        }
        fillRun(g, run, y0);
    }

    private void fillRun(GraphicsContext g, List<double[]> run, double y0) {
        if (run.size() < 2) return;
        g.beginPath();
        g.moveTo(run.get(0)[0], y0);
        for (double[] p : run) g.lineTo(p[0], p[1]);
        g.lineTo(run.get(run.size() - 1)[0], y0);
        g.closePath();
        g.fill();
    }

    private void drawCurve(GraphicsContext g, Palette palette) {
        g.setStroke(palette.fg());
        g.setLineWidth(2);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);

        double range = yMax - yMin;
        double previous = Double.NaN;
        boolean drawing = false;
        for (int i = 0; i <= SAMPLES; i++) {
            double x = xMin + (xMax - xMin) * i / SAMPLES;
            double y = value(x);
            boolean finite = Double.isFinite(y);
            boolean jump = finite && !Double.isNaN(previous) && Math.abs(y - previous) > 3 * range;
            if (!finite || jump) {
                if (drawing) g.stroke();
                drawing = false;
            }
            previous = finite ? y : Double.NaN;
            if (!finite) continue;
            double sx = px(x);
            double sy = py(clamp(y));
            if (!drawing) {
                g.beginPath();
                g.moveTo(sx, sy);
                drawing = true;
            } else {
                g.lineTo(sx, sy);
            }
        }
        if (drawing) g.stroke();
    }

    private void drawBounds(GraphicsContext g, Palette palette) {
        if (Double.isNaN(lower) || Double.isNaN(upper)) return;
        g.setStroke(palette.accent());
        g.setLineWidth(1.5);
        g.setLineDashes(5, 4);
        for (double bound : new double[]{lower, upper}) {
            if (Double.isInfinite(bound) || bound < xMin || bound > xMax) continue;
            double sx = Math.round(px(bound)) + 0.5;
            g.strokeLine(sx, MARGIN, sx, canvas.getHeight() - MARGIN_BOTTOM);
        }
        g.setLineDashes((double[]) null);
    }

    private double clamp(double y) {
        double margin = (yMax - yMin) * 2;
        return Math.max(yMin - margin, Math.min(yMax + margin, y));
    }

    // ---------------------------------------------------------------- Marcas de los ejes

    private static double[] ticks(double min, double max) {
        double range = max - min;
        double raw = range / 6;
        double magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
        double step = magnitude;
        for (double m : new double[]{1, 2, 5, 10}) {
            step = m * magnitude;
            if (step >= raw) break;
        }
        double first = Math.ceil(min / step) * step;
        List<Double> out = new ArrayList<>();
        for (double t = first; t <= max + step * 1e-9; t += step) out.add(Math.abs(t) < step * 1e-9 ? 0.0 : t);
        return out.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private static String label(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e6) return String.valueOf((long) v);
        String s = String.format(Locale.ROOT, "%.4g", v);
        return s.contains("e") ? s : Arrays.stream(s.split("\\.")).count() == 2 ? s.replaceAll("0+$", "") : s;
    }
}
