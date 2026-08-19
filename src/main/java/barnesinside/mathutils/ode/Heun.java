package barnesinside.mathutils.ode;

/**
 * Método de Heun o Euler mejorado (orden 2): promedia la pendiente inicial y la del extremo.
 */
public final class Heun extends FixedStepSolver {

    @Override
    protected double[] step(OdeFunction f, double t, double[] y, double h) {
        double[] k1 = f.derivative(t, y);
        double[] k2 = f.derivative(t + h, axpy(y, h, k1));

        double[] result = new double[y.length];
        for (int i = 0; i < y.length; i++) result[i] = y[i] + h / 2 * (k1[i] + k2[i]);
        return result;
    }
}
