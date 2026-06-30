package MathUtils.ode;

/**
 * Método del punto medio (orden 2): evalúa la pendiente en la mitad del paso.
 */
public final class Midpoint extends FixedStepSolver {

    @Override
    protected double[] step(OdeFunction f, double t, double[] y, double h) {
        double[] k1 = f.derivative(t, y);
        double[] k2 = f.derivative(t + h / 2, axpy(y, h / 2, k1));
        return axpy(y, h, k2);
    }
}
