package MathUtils.ode;

import java.util.function.DoubleBinaryOperator;

/**
 * Resuelve el problema de valor inicial {@code y' = f(t, y)}, {@code y(t0) = y0}.
 */
public interface ODESolver {

    /**
     * @param f lado derecho del sistema
     * @param t0 instante inicial
     * @param y0 estado inicial
     * @param tEnd instante final (puede ser menor que {@code t0})
     * @param steps cantidad de pasos de tamaño fijo (≥ 1)
     * @return la trayectoria con {@code steps + 1} puntos
     */
    OdeSolution solve(OdeFunction f, double t0, double[] y0, double tEnd, int steps);

    /**
     * Versión para una EDO escalar.
     */
    default OdeSolution solve(DoubleBinaryOperator f, double t0, double y0, double tEnd, int steps) {
        return solve(OdeFunction.scalar(f), t0, new double[]{y0}, tEnd, steps);
    }
}
