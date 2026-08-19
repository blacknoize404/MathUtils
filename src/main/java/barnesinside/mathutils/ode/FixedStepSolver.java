package barnesinside.mathutils.ode;

/**
 * Base de los métodos de un paso con tamaño de paso constante.
 */
public abstract class FixedStepSolver implements ODESolver {

    /**
     * Avanza un paso de tamaño {@code h} desde {@code (t, y)}.
     * @return el nuevo estado
     */
    protected abstract double[] step(OdeFunction f, double t, double[] y, double h);

    @Override
    public OdeSolution solve(OdeFunction f, double t0, double[] y0, double tEnd, int steps) {
        if (steps < 1) throw new IllegalArgumentException("steps debe ser al menos 1");

        double h = (tEnd - t0) / steps;
        double[] t = new double[steps + 1];
        double[][] y = new double[steps + 1][];
        t[0] = t0;
        y[0] = y0.clone();

        for (int i = 0; i < steps; i++) {
            y[i + 1] = step(f, t[i], y[i], h);
            t[i + 1] = t0 + (i + 1) * h;
        }
        return new OdeSolution(t, y);
    }

    /** {@code y + scale · k} */
    protected static double[] axpy(double[] y, double scale, double[] k) {
        double[] result = new double[y.length];
        for (int i = 0; i < y.length; i++) result[i] = y[i] + scale * k[i];
        return result;
    }
}
