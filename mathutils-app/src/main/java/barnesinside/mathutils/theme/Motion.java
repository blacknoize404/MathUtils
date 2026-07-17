package barnesinside.mathutils.theme;

import javafx.animation.Interpolator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Duration;

/**
 * Movimiento de la marca: dos curvas ({@code in-out} para transiciones de página y
 * {@code spring} con un pequeño rebote para la interacción) y cuatro duraciones.
 * Con "reducir movimiento" todas las duraciones valen cero.
 */
public final class Motion {

    /** Duraciones del sistema de diseño: 150, 300, 500 y 800 ms. */
    public static final double FAST = 150;
    public static final double NORMAL = 300;
    public static final double SLOW = 500;
    public static final double SLOWER = 800;

    private static final BooleanProperty REDUCED = new SimpleBooleanProperty(false);

    /** {@code cubic-bezier(0.16, 1, 0.3, 1)}: transiciones de página, modales y barridos. */
    public static final Interpolator IN_OUT = new Bezier(0.16, 1, 0.3, 1);

    /** {@code cubic-bezier(0.34, 1.56, 0.64, 1)}: interacción, con un leve sobreimpulso. */
    public static final Interpolator SPRING = new Bezier(0.34, 1.56, 0.64, 1);

    private Motion() {
    }

    public static BooleanProperty reducedProperty() {
        return REDUCED;
    }

    public static boolean isReduced() {
        return REDUCED.get();
    }

    /** Duración en milisegundos, o cero si el usuario pidió reducir el movimiento. */
    public static Duration millis(double millis) {
        return REDUCED.get() ? Duration.ZERO : Duration.millis(millis);
    }

    /**
     * Curva bézier cúbica con los puntos de control dados. {@link Interpolator#SPLINE} de JavaFX
     * no admite valores fuera de 0-1, así que el rebote del sistema de diseño necesita esta versión.
     */
    static final class Bezier extends Interpolator {
        private final double x1, y1, x2, y2;

        Bezier(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        protected double curve(double t) {
            if (t <= 0) return 0;
            if (t >= 1) return 1;

            // Se resuelve x(s) = t por bisección y se devuelve y(s)
            double low = 0;
            double high = 1;
            double s = t;
            for (int i = 0; i < 40; i++) {
                s = (low + high) / 2;
                double x = sample(s, x1, x2);
                if (Math.abs(x - t) < 1e-6) break;
                if (x < t) low = s;
                else high = s;
            }
            return sample(s, y1, y2);
        }

        private static double sample(double s, double p1, double p2) {
            double u = 1 - s;
            return 3 * u * u * s * p1 + 3 * u * s * s * p2 + s * s * s;
        }
    }
}
