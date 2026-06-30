package MathUtils.ode;

/**
 * Método clásico de Runge-Kutta de cuarto orden.
 */
public final class RK4 extends FixedStepSolver {

    @Override
    protected double[] step(OdeFunction f, double t, double[] y, double h) {
        double[] k1 = f.derivative(t, y);
        double[] k2 = f.derivative(t + h / 2, axpy(y, h / 2, k1));
        double[] k3 = f.derivative(t + h / 2, axpy(y, h / 2, k2));
        double[] k4 = f.derivative(t + h, axpy(y, h, k3));

        double[] result = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            result[i] = y[i] + h / 6 * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]);
        }
        return result;
    }
}
