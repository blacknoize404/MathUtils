package barnesinside.mathutils.math;

import MathUtils.cas.expr.Display;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.Fn;
import MathUtils.cas.expr.Rational;
import barnesinside.mathutils.theme.Brand;
import barnesinside.mathutils.theme.Palette;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.geometry.VPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Composición tipográfica de expresiones: convierte una {@link Expr} en un árbol de cajas
 * (texto, filas, fracciones, potencias, raíces, paréntesis) con sus medidas, y las dibuja en un
 * {@link GraphicsContext}. Cada caja recuerda la subexpresión que representa para resaltarla.
 */
public final class MathLayout {

    private static final double SCRIPT_SCALE = 0.72;

    private MathLayout() {
    }

    // ================================================================ Contexto de dibujo

    /** Datos del dibujo: colores, subexpresión resaltada y registro de zonas para detectar el ratón. */
    public static final class Context {
        final Palette palette;
        final Color color;
        final Expr hovered;
        final List<Hit> hits = new ArrayList<>();

        public Context(Palette palette, Color color, Expr hovered) {
            this.palette = palette;
            this.color = color;
            this.hovered = hovered;
        }

        public List<Hit> hits() {
            return hits;
        }
    }

    /** Zona rectangular que representa una subexpresión. */
    public record Hit(Rectangle2D area, Expr expr) {
    }

    // ================================================================ Cajas

    /** Caja con medidas relativas a su línea base. */
    public abstract static class Box {
        double width;
        double ascent;
        double descent;
        final Expr source;

        Box(Expr source) {
            this.source = source;
        }

        public double width() {
            return width;
        }

        public double ascent() {
            return ascent;
        }

        public double descent() {
            return descent;
        }

        public double height() {
            return ascent + descent;
        }

        abstract void draw(GraphicsContext g, double x, double baseline, Context ctx);

        /** Dibuja la caja, registrando su zona y el resaltado si es la subexpresión señalada. */
        final void paint(GraphicsContext g, double x, double baseline, Context ctx) {
            if (source != null) {
                Rectangle2D area = new Rectangle2D(x - 1, baseline - ascent - 1, width + 2, height() + 2);
                ctx.hits.add(new Hit(area, source));
                if (source.equals(ctx.hovered)) {
                    g.setFill(ctx.palette.accentBg());
                    g.fillRoundRect(area.getMinX() - 2, area.getMinY() - 2, area.getWidth() + 4, area.getHeight() + 4, 6, 6);
                    g.setStroke(ctx.palette.accent());
                    g.setLineWidth(1);
                    g.strokeRoundRect(area.getMinX() - 2, area.getMinY() - 2, area.getWidth() + 4, area.getHeight() + 4, 6, 6);
                }
            }
            draw(g, x, baseline, ctx);
        }
    }

    private static final class TextBox extends Box {
        final String text;
        final Font font;
        final boolean muted;

        TextBox(String text, Font font, boolean muted, Expr source) {
            super(source);
            this.text = text;
            this.font = font;
            this.muted = muted;
            double[] m = measure(text, font);
            this.width = m[0];
            this.ascent = m[1];
            this.descent = m[2];
        }

        @Override
        void draw(GraphicsContext g, double x, double baseline, Context ctx) {
            g.setFont(font);
            g.setFill(muted ? ctx.palette.muted() : ctx.color);
            g.setTextAlign(TextAlignment.LEFT);
            g.setTextBaseline(VPos.BASELINE);
            g.fillText(text, x, baseline);
        }
    }

    private static final class RowBox extends Box {
        final List<Box> items;

        RowBox(List<Box> items, Expr source) {
            super(source);
            this.items = items;
            for (Box b : items) {
                width += b.width;
                ascent = Math.max(ascent, b.ascent);
                descent = Math.max(descent, b.descent);
            }
        }

        @Override
        void draw(GraphicsContext g, double x, double baseline, Context ctx) {
            double cursor = x;
            for (Box b : items) {
                b.paint(g, cursor, baseline, ctx);
                cursor += b.width;
            }
        }
    }

    private static final class SpaceBox extends Box {
        SpaceBox(double width) {
            super(null);
            this.width = width;
        }

        @Override
        void draw(GraphicsContext g, double x, double baseline, Context ctx) {
        }
    }

    private static final class FractionBox extends Box {
        final Box numerator;
        final Box denominator;
        final double axis;
        final double gap;
        final double thickness;

        FractionBox(Box numerator, Box denominator, double size, Expr source) {
            super(source);
            this.numerator = numerator;
            this.denominator = denominator;
            this.axis = size * 0.3;
            this.gap = size * 0.18;
            this.thickness = Math.max(1, size / 14);
            double pad = size * 0.15;
            this.width = Math.max(numerator.width, denominator.width) + 2 * pad;
            this.ascent = axis + thickness / 2 + gap + numerator.height();
            this.descent = denominator.height() + gap + thickness / 2 - axis;
        }

        @Override
        void draw(GraphicsContext g, double x, double baseline, Context ctx) {
            double barY = baseline - axis;
            numerator.paint(g, x + (width - numerator.width) / 2, barY - thickness / 2 - gap - numerator.descent, ctx);
            denominator.paint(g, x + (width - denominator.width) / 2, barY + thickness / 2 + gap + denominator.ascent, ctx);
            g.setStroke(ctx.color);
            g.setLineWidth(thickness);
            g.setLineCap(StrokeLineCap.BUTT);
            g.strokeLine(x, barY, x + width, barY);
        }
    }

    private static final class ScriptBox extends Box {
        final Box base;
        final Box script;
        final double raise;

        ScriptBox(Box base, Box script, double size, Expr source) {
            super(source);
            this.base = base;
            this.script = script;
            this.raise = Math.max(size * 0.38, base.ascent - script.descent * 0.6 - size * 0.12);
            this.width = base.width + script.width + size * 0.04;
            this.ascent = Math.max(base.ascent, raise + script.ascent);
            this.descent = base.descent;
        }

        @Override
        void draw(GraphicsContext g, double x, double baseline, Context ctx) {
            base.paint(g, x, baseline, ctx);
            script.paint(g, x + base.width + 0.5, baseline - raise, ctx);
        }
    }

    private static final class RadicalBox extends Box {
        final Box content;
        final Box index;
        final double signWidth;
        final double top;
        final double indexWidth;

        RadicalBox(Box content, Box index, double size, Expr source) {
            super(source);
            this.content = content;
            this.index = index;
            this.signWidth = size * 0.62;
            double gap = size * 0.14;
            this.indexWidth = index == null ? 0 : Math.max(0, index.width - signWidth * 0.35);
            this.top = content.ascent + gap;
            this.width = indexWidth + signWidth + content.width + size * 0.12;
            this.ascent = top + 1.5;
            this.descent = content.descent + 1;
        }

        @Override
        void draw(GraphicsContext g, double x, double baseline, Context ctx) {
            double left = x + indexWidth;
            double topY = baseline - top;
            double bottomY = baseline + descent;
            double height = bottomY - topY;

            g.setStroke(ctx.color);
            g.setLineWidth(1.2);
            g.setLineCap(StrokeLineCap.ROUND);
            g.setLineJoin(StrokeLineJoin.ROUND);
            g.beginPath();
            g.moveTo(left, topY + height * 0.62);
            g.lineTo(left + signWidth * 0.2, topY + height * 0.54);
            g.lineTo(left + signWidth * 0.45, bottomY);
            g.lineTo(left + signWidth, topY);
            g.lineTo(left + signWidth + content.width + 3, topY);
            g.stroke();

            if (index != null) {
                index.paint(g, x, topY + height * 0.38 - index.descent, ctx);
            }
            content.paint(g, left + signWidth + 1, baseline, ctx);
        }
    }

    /** Delimitadores que se estiran a la altura del contenido. */
    public enum Delimiter {ROUND, SQUARE, BARS}

    private static final class DelimitedBox extends Box {
        final Box content;
        final Delimiter kind;
        final double side;

        DelimitedBox(Box content, Delimiter kind, double size, Expr source) {
            super(source);
            this.content = content;
            this.kind = kind;
            this.side = kind == Delimiter.ROUND ? size * 0.34 : size * 0.26;
            this.width = content.width + 2 * side;
            this.ascent = content.ascent + size * 0.1;
            this.descent = content.descent + size * 0.1;
        }

        @Override
        void draw(GraphicsContext g, double x, double baseline, Context ctx) {
            double topY = baseline - ascent;
            double bottomY = baseline + descent;
            double midY = (topY + bottomY) / 2;
            g.setStroke(ctx.color);
            g.setLineWidth(1.2);
            g.setLineCap(StrokeLineCap.ROUND);
            g.setLineJoin(StrokeLineJoin.ROUND);

            double leftX = x + side * 0.55;
            double rightX = x + width - side * 0.55;
            switch (kind) {
                case ROUND -> {
                    double bulge = side * 0.55;
                    g.beginPath();
                    g.moveTo(x + side * 0.85, topY);
                    g.quadraticCurveTo(x + side * 0.85 - bulge * 2, midY, x + side * 0.85, bottomY);
                    g.stroke();
                    g.beginPath();
                    g.moveTo(x + width - side * 0.85, topY);
                    g.quadraticCurveTo(x + width - side * 0.85 + bulge * 2, midY, x + width - side * 0.85, bottomY);
                    g.stroke();
                }
                case SQUARE -> {
                    g.beginPath();
                    g.moveTo(leftX + side * 0.3, topY);
                    g.lineTo(leftX, topY);
                    g.lineTo(leftX, bottomY);
                    g.lineTo(leftX + side * 0.3, bottomY);
                    g.stroke();
                    g.beginPath();
                    g.moveTo(rightX - side * 0.3, topY);
                    g.lineTo(rightX, topY);
                    g.lineTo(rightX, bottomY);
                    g.lineTo(rightX - side * 0.3, bottomY);
                    g.stroke();
                }
                case BARS -> {
                    g.strokeLine(leftX, topY, leftX, bottomY);
                    g.strokeLine(rightX, topY, rightX, bottomY);
                }
            }
            content.paint(g, x + side, baseline, ctx);
        }
    }

    /** Signo de integral: una S alargada que se estira con el integrando. */
    private static final class IntegralSignBox extends Box {
        IntegralSignBox(double size) {
            super(null);
            this.width = size * 0.62;
            this.ascent = size * 1.15;
            this.descent = size * 0.55;
        }

        @Override
        void draw(GraphicsContext g, double x, double baseline, Context ctx) {
            double top = baseline - ascent;
            double bottom = baseline + descent;
            double h = bottom - top;
            double w = width;

            g.setStroke(ctx.color);
            g.setLineWidth(Math.max(1.3, h / 22));
            g.setLineCap(StrokeLineCap.ROUND);
            g.setLineJoin(StrokeLineJoin.ROUND);
            g.beginPath();
            g.moveTo(x + w * 0.92, top + h * 0.1);
            g.bezierCurveTo(x + w * 0.88, top - h * 0.01, x + w * 0.58, top - h * 0.01, x + w * 0.56, top + h * 0.17);
            g.lineTo(x + w * 0.42, bottom - h * 0.17);
            g.bezierCurveTo(x + w * 0.4, bottom + h * 0.01, x + w * 0.1, bottom + h * 0.01, x + w * 0.08, bottom - h * 0.1);
            g.stroke();
        }
    }

    /** El signo de integral con el límite superior arriba a la derecha y el inferior abajo. */
    private static final class BoundedIntegralBox extends Box {
        final IntegralSignBox sign;
        final Box upper;
        final Box lower;
        final double upperX;
        final double lowerX;
        final double upperRise;
        final double lowerDrop;

        BoundedIntegralBox(double size, Box upper, Box lower) {
            super(null);
            this.sign = new IntegralSignBox(size);
            this.upper = upper;
            this.lower = lower;
            this.upperX = sign.width * 1.0;
            this.lowerX = sign.width * 0.3;
            this.upperRise = sign.ascent * 0.62 + upper.descent;
            this.lowerDrop = sign.descent * 0.55 + lower.ascent;
            this.width = Math.max(sign.width, Math.max(upperX + upper.width, lowerX + lower.width)) + size * 0.06;
            this.ascent = Math.max(sign.ascent, upperRise + upper.ascent);
            this.descent = Math.max(sign.descent, lowerDrop + lower.descent);
        }

        @Override
        void draw(GraphicsContext g, double x, double baseline, Context ctx) {
            sign.draw(g, x, baseline, ctx);
            upper.paint(g, x + upperX, baseline - upperRise, ctx);
            lower.paint(g, x + lowerX, baseline + lowerDrop, ctx);
        }
    }

    /** Una caja principal con otra pequeña debajo (por ejemplo, {@code lim} con {@code x → a}). */
    private static final class UnderBox extends Box {
        final Box main;
        final Box under;
        final double gap;

        UnderBox(Box main, Box under, double size) {
            super(null);
            this.main = main;
            this.under = under;
            this.gap = size * 0.08;
            this.width = Math.max(main.width, under.width);
            this.ascent = main.ascent;
            this.descent = main.descent + gap + under.height();
        }

        @Override
        void draw(GraphicsContext g, double x, double baseline, Context ctx) {
            main.paint(g, x + (width - main.width) / 2, baseline, ctx);
            under.paint(g, x + (width - under.width) / 2, baseline + main.descent + gap + under.ascent, ctx);
        }
    }

    // ================================================================ Medición

    private static final Text MEASURER = new Text();

    /** Ancho, ascenso y descenso del texto con la fuente dada. */
    private static double[] measure(String text, Font font) {
        MEASURER.setText(text);
        MEASURER.setFont(font);
        MEASURER.setBoundsType(javafx.scene.text.TextBoundsType.LOGICAL);
        double width = MEASURER.getLayoutBounds().getWidth();
        double ascent = MEASURER.getBaselineOffset();
        double height = MEASURER.getLayoutBounds().getHeight();
        return new double[]{width, ascent, Math.max(0, height - ascent)};
    }

    // ================================================================ Construcción pública

    /** Caja de una expresión con el tamaño de fuente dado. */
    public static Box of(Expr expr, double size) {
        return build(expr, size, 0);
    }

    /** Caja de texto sencillo (por ejemplo "d" o "dx"). */
    public static Box text(String text, double size) {
        return new TextBox(text, Brand.sans(size), false, null);
    }

    public static Box textMuted(String text, double size) {
        return new TextBox(text, Brand.sans(size), true, null);
    }

    public static Box row(List<Box> items) {
        return new RowBox(items, null);
    }

    public static Box space(double width) {
        return new SpaceBox(width);
    }

    public static Box fraction(Box numerator, Box denominator, double size) {
        return new FractionBox(numerator, denominator, size, null);
    }

    public static Box delimited(Box content, Delimiter kind, double size) {
        return new DelimitedBox(content, kind, size, null);
    }

    /**
     * Notación de derivada {@code d/dx [ before ] = after}.
     *
     * @param order orden de la derivada (≥ 1)
     */
    public static Box derivativeEquation(Expr variable, Expr before, Expr after, int order, double size) {
        String v = variable.toString();
        Box numerator = order > 1
                ? new RowBox(List.of(text("d", size * 0.9), script(text(String.valueOf(order), size * 0.9), size * 0.9)), null)
                : text("d", size * 0.9);
        Box denominatorVar = order > 1
                ? new RowBox(List.of(text("d" + v, size * 0.9), script(text(String.valueOf(order), size * 0.9), size * 0.9)), null)
                : text("d" + v, size * 0.9);
        List<Box> items = new ArrayList<>();
        items.add(fraction(numerator, denominatorVar, size * 0.9));
        items.add(space(size * 0.2));
        items.add(delimited(of(before, size), Delimiter.SQUARE, size));
        if (after != null) {
            items.add(space(size * 0.35));
            items.add(text("=", size));
            items.add(space(size * 0.35));
            items.add(of(after, size));
        }
        return new RowBox(items, null);
    }

    /**
     * Derivada parcial sucesiva: {@code ∂ⁿ/∂y∂x [f]} (la primera variable de la lista se deriva primero).
     */
    public static Box partialEquation(List<Expr> variables, Expr before, Expr after, double size) {
        int order = variables.size();
        StringBuilder denominator = new StringBuilder();
        for (int i = order - 1; i >= 0; i--) denominator.append("∂").append(variables.get(i));
        Box numerator = order > 1
                ? new RowBox(List.of(text("∂", size * 0.9), script(text(String.valueOf(order), size * 0.9), size * 0.9)), null)
                : text("∂", size * 0.9);
        List<Box> items = new ArrayList<>();
        items.add(fraction(numerator, text(denominator.toString(), size * 0.9), size * 0.9));
        items.add(space(size * 0.2));
        items.add(delimited(of(before, size), Delimiter.SQUARE, size));
        if (after != null) {
            items.add(space(size * 0.35));
            items.add(text("=", size));
            items.add(space(size * 0.35));
            items.add(of(after, size));
        }
        return new RowBox(items, null);
    }

    /** Una variable de una integral iterada con sus límites (ambos {@code null} si es indefinida). */
    public record IntegralDimension(Expr variable, Expr lower, Expr upper) {
    }

    /**
     * Integral iterada: los signos del exterior al interior, cada uno con sus límites, el integrando y
     * los diferenciales de dentro hacia fuera.
     *
     * @param dimensions variables de la más interna a la más externa
     */
    public static Box multipleIntegralEquation(List<IntegralDimension> dimensions, Expr integrand, Expr result,
                                               boolean plusConstant, double size) {
        List<Box> items = new ArrayList<>();
        for (int i = dimensions.size() - 1; i >= 0; i--) {
            IntegralDimension d = dimensions.get(i);
            items.add(d.lower() == null ? new IntegralSignBox(size)
                    : new BoundedIntegralBox(size, of(d.upper(), size * 0.72), of(d.lower(), size * 0.72)));
            items.add(space(size * 0.1));
        }
        items.add(of(integrand, size));
        items.add(space(size * 0.2));
        StringBuilder differentials = new StringBuilder();
        for (IntegralDimension d : dimensions) differentials.append("d").append(d.variable()).append(' ');
        items.add(text(differentials.toString().strip(), size));
        if (result != null) {
            items.add(space(size * 0.35));
            items.add(text("=", size));
            items.add(space(size * 0.35));
            items.add(of(result, size));
            if (plusConstant) {
                items.add(space(size * 0.2));
                items.add(text("+ C", size));
            }
        }
        return new RowBox(items, null);
    }

    /**
     * Notación de límite: {@code lim} con {@code x → a} debajo, la función y, si se da, el resultado.
     *
     * @param direction 1 por la derecha, -1 por la izquierda, 0 por ambos lados
     */
    public static Box limitEquation(Expr variable, Expr point, int direction, Expr function, Expr result, double size) {
        List<Box> subscript = new ArrayList<>();
        subscript.add(of(variable, size * 0.75));
        subscript.add(text(" → ", size * 0.75));
        subscript.add(of(point, size * 0.75));
        if (direction != 0) subscript.add(script(text(direction > 0 ? "+" : "−", size * 0.55), size * 0.75));

        List<Box> items = new ArrayList<>();
        items.add(new UnderBox(text("lim", size), new RowBox(subscript, null), size));
        items.add(space(size * 0.25));
        items.add(of(function, size));
        if (result != null) {
            items.add(space(size * 0.35));
            items.add(text("=", size));
            items.add(space(size * 0.35));
            items.add(of(result, size));
        }
        return new RowBox(items, null);
    }

    /**
     * Notación de integral indefinida: {@code ∫ f dx} y, si se da, {@code = F (+ C)}.
     */
    public static Box integralEquation(Expr variable, Expr integrand, Expr result, boolean plusConstant, double size) {
        return integralEquation(variable, integrand, null, null, result, plusConstant, size);
    }

    /** Como {@link #integralEquation(Expr, Expr, Expr, boolean, double)} pero con límites de integración. */
    public static Box integralEquation(Expr variable, Expr integrand, Expr lower, Expr upper, Expr result,
                                       boolean plusConstant, double size) {
        List<Box> items = new ArrayList<>();
        items.add(lower == null || upper == null ? new IntegralSignBox(size)
                : new BoundedIntegralBox(size, of(upper, size * 0.72), of(lower, size * 0.72)));
        items.add(space(size * 0.12));
        items.add(of(integrand, size));
        items.add(space(size * 0.2));
        items.add(text("d", size));
        items.add(of(variable, size));
        if (result != null) {
            items.add(space(size * 0.35));
            items.add(text("=", size));
            items.add(space(size * 0.35));
            items.add(of(result, size));
            if (plusConstant) {
                items.add(space(size * 0.2));
                items.add(text("+ C", size));
            }
        }
        return new RowBox(items, null);
    }

    private static Box script(Box inner, double size) {
        // Exponente suelto: se eleva sobre una base vacía
        return new ScriptBox(new SpaceBox(0), inner, size, null);
    }

    /** Dibuja la caja con su línea base en {@code baseline}. */
    public static void paint(Box box, GraphicsContext g, double x, double baseline, Context ctx) {
        box.paint(g, x, baseline, ctx);
    }

    // ================================================================ Expr -> cajas

    private static final int ADD = 1;
    private static final int MUL = 2;
    private static final int POW = 3;
    private static final int ATOM = 4;

    private static Box build(Expr e, double size, int depth) {
        return switch (e) {
            case Expr.Num n -> number(n, size);
            case Expr.Const c -> new TextBox(switch (c.kind()) {
                case PI -> "π";
                case E -> "e";
                case INFINITY -> "∞";
            }, Brand.sans(size), false, e);
            case Expr.Sym s -> new TextBox(s.name(), s.name().length() == 1 ? Brand.sansItalic(size) : Brand.sans(size), false, e);
            case Expr.Add a -> sum(a, size, depth);
            case Expr.Mul m -> product(m, size, depth);
            case Expr.Pow p -> power(p, size, depth);
            case Expr.Func f -> function(f, size, depth);
        };
    }

    private static int precedence(Expr e) {
        return switch (e) {
            case Expr.Num n -> n.value().signum() < 0 ? ADD : ATOM;
            case Expr.Const c -> ATOM;
            case Expr.Sym s -> ATOM;
            case Expr.Func f -> ATOM;
            case Expr.Add a -> ADD;
            case Expr.Mul m -> MUL;
            case Expr.Pow p -> isRoot(p) || isReciprocal(p) ? ATOM : POW;
        };
    }

    private static boolean isReciprocal(Expr.Pow p) {
        return p.exponent() instanceof Expr.Num n && n.value().signum() < 0;
    }

    private static boolean isRoot(Expr.Pow p) {
        return p.exponent() instanceof Expr.Num n && n.value().numerator().equals(java.math.BigInteger.ONE)
                && !n.value().isInteger() && n.value().denominator().signum() > 0;
    }

    /** Construye {@code e} y le añade paréntesis si su precedencia es menor que la requerida. */
    private static Box wrapped(Expr e, int required, double size, int depth) {
        Box box = build(e, size, depth);
        return precedence(e) < required ? delimited(box, Delimiter.ROUND, size, e) : box;
    }

    private static Box delimited(Box content, Delimiter kind, double size, Expr source) {
        return new DelimitedBox(content, kind, size, source);
    }

    private static Box number(Expr.Num n, double size) {
        Rational r = n.value();
        Font font = Brand.sans(size);
        if (r.isInteger()) return new TextBox(r.toString(), font, false, n);
        List<Box> items = new ArrayList<>();
        if (r.signum() < 0) {
            items.add(new TextBox("−", font, false, null));
            items.add(new SpaceBox(size * 0.1));
        }
        items.add(new FractionBox(new TextBox(r.numerator().abs().toString(), font, false, null),
                new TextBox(r.denominator().toString(), font, false, null), size, null));
        return new RowBox(items, n);
    }

    private static Box sum(Expr.Add sum, double size, int depth) {
        List<Box> items = new ArrayList<>();
        Font font = Brand.sans(size);
        boolean first = true;
        for (Expr term : Display.sortedTerms(sum.terms())) {
            boolean negative = Display.isNegative(term);
            Expr shown = negative ? Expr.neg(term) : term;
            if (first) {
                if (negative) items.add(new TextBox("−", font, false, null));
            } else {
                items.add(new SpaceBox(size * 0.28));
                items.add(new TextBox(negative ? "−" : "+", font, false, null));
                items.add(new SpaceBox(size * 0.28));
            }
            items.add(wrapped(shown, negative ? MUL : ADD, size, depth));
            first = false;
        }
        return new RowBox(items, sum);
    }

    private static Box product(Expr.Mul mul, double size, int depth) {
        return fractionOrProduct(Display.split(mul), mul, size, depth);
    }

    private static Box fractionOrProduct(Display.Fraction f, Expr source, double size, int depth) {
        Box numerator = f.numerator().isEmpty() ? new TextBox("1", Brand.sans(size), false, null) : factors(f.numerator(), size, depth);
        Box body = numerator;
        if (f.hasDenominator()) {
            body = new FractionBox(numerator, factors(f.denominator(), size, depth), size, null);
        }
        if (!f.negative()) return rewrap(body, source);

        List<Box> items = new ArrayList<>();
        items.add(new TextBox("−", Brand.sans(size), false, null));
        items.add(new SpaceBox(size * 0.08));
        items.add(body);
        return new RowBox(items, source);
    }

    private static Box rewrap(Box body, Expr source) {
        return new RowBox(List.of(body), source);
    }

    private static Box factors(List<Expr> items, double size, int depth) {
        List<Box> boxes = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Expr factor = items.get(i);
            Box box = wrapped(factor, MUL + 1, size, depth);
            if (i > 0) {
                boolean startsWithDigit = factor instanceof Expr.Num;
                boxes.add(startsWithDigit ? new TextBox("·", Brand.sans(size), false, null) : new SpaceBox(size * 0.14));
            }
            boxes.add(box);
        }
        return boxes.size() == 1 ? boxes.get(0) : new RowBox(boxes, null);
    }

    private static Box power(Expr.Pow p, double size, int depth) {
        if (isReciprocal(p)) return fractionOrProduct(Display.split(p), p, size, depth);

        if (p.exponent() instanceof Expr.Num n && isRoot(p)) {
            Box content = build(p.base(), size, depth);
            Box index = n.value().denominator().equals(java.math.BigInteger.TWO)
                    ? null
                    : new TextBox(n.value().denominator().toString(), Brand.sans(size * 0.6), false, null);
            return new RadicalBox(content, index, size, p);
        }

        Box base = p.base().equals(Expr.E)
                ? new TextBox("e", Brand.sans(size), false, p.base())
                : wrapped(p.base(), ATOM, size, depth);
        Box exponent = build(p.exponent(), size * SCRIPT_SCALE, depth + 1);
        return new ScriptBox(base, exponent, size, p);
    }

    private static Box function(Expr.Func f, double size, int depth) {
        Box argument = build(f.arg(), size, depth);
        if (f.fn() == Fn.ABS) return delimited(argument, Delimiter.BARS, size, f);

        String name = switch (f.fn()) {
            case ASIN -> "arcsin";
            case ACOS -> "arccos";
            case ATAN -> "arctan";
            default -> f.fn().symbol();
        };
        List<Box> items = new ArrayList<>();
        items.add(new TextBox(name, Brand.sans(size), false, null));
        items.add(new SpaceBox(size * 0.06));
        items.add(delimited(argument, Delimiter.ROUND, size, null));
        return new RowBox(items, f);
    }
}
