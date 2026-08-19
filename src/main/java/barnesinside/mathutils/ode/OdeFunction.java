package barnesinside.mathutils.ode;

import java.util.function.DoubleBinaryOperator;

/**
 * Lado derecho de un sistema de EDOs de primer orden {@code y' = f(t, y)},
 * donde {@code y} es un vector de estado.
 */
@FunctionalInterface
public interface OdeFunction {

    /**
     * @param t variable independiente
     * @param y estado actual (no debe modificarse)
     * @return la derivada del estado, del mismo tamaño que {@code y}
     */
    double[] derivative(double t, double[] y);

    /**
     * Adapta una EDO escalar {@code y' = f(t, y)} a un sistema de dimensión 1.
     */
    static OdeFunction scalar(DoubleBinaryOperator f) {
        return (t, y) -> new double[]{f.applyAsDouble(t, y[0])};
    }
}
