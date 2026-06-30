package MathUtils.ode;

/**
 * Método de Euler explícito (orden 1): {@code y + h·f(t, y)}.
 */
public final class Euler extends FixedStepSolver {

    @Override
    protected double[] step(OdeFunction f, double t, double[] y, double h) {
        return axpy(y, h, f.derivative(t, y));
    }
}
