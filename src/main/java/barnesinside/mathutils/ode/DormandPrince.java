package barnesinside.mathutils.ode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleBinaryOperator;

/**
 * Runge-Kutta 4(5) de Dormand-Prince con paso adaptativo.
 * <p>En cada paso compara las soluciones de orden 4 y 5 para estimar el error local y
 * ajusta el tamaño del paso para mantenerlo bajo
 * {@code absTolerance + relTolerance · |y|} en cada componente.
 */
public final class DormandPrince {

    private static final double[] C = {0, 1.0 / 5, 3.0 / 10, 4.0 / 5, 8.0 / 9, 1, 1};

    private static final double[][] A = {
            {},
            {1.0 / 5},
            {3.0 / 40, 9.0 / 40},
            {44.0 / 45, -56.0 / 15, 32.0 / 9},
            {19372.0 / 6561, -25360.0 / 2187, 64448.0 / 6561, -212.0 / 729},
            {9017.0 / 3168, -355.0 / 33, 46732.0 / 5247, 49.0 / 176, -5103.0 / 18656},
            {35.0 / 384, 0, 500.0 / 1113, 125.0 / 192, -2187.0 / 6784, 11.0 / 84}
    };

    /** Pesos de orden 5: la última fila de A, más un 0 para la etapa 7 (propiedad FSAL). */
    private static final double[] B5 = {
            35.0 / 384, 0, 500.0 / 1113, 125.0 / 192, -2187.0 / 6784, 11.0 / 84, 0
    };

    /** Pesos de orden 4. */
    private static final double[] B4 = {
            5179.0 / 57600, 0, 7571.0 / 16695, 393.0 / 640, -92097.0 / 339200, 187.0 / 2100, 1.0 / 40
    };

    private final double relTolerance;
    private final double absTolerance;
    private final int maxSteps;

    /**
     * @param relTolerance tolerancia relativa (> 0)
     * @param absTolerance tolerancia absoluta (> 0)
     * @param maxSteps máximo de pasos (aceptados o rechazados) antes de abortar
     */
    public DormandPrince(double relTolerance, double absTolerance, int maxSteps) {
        if (relTolerance <= 0 || absTolerance <= 0) throw new IllegalArgumentException("Las tolerancias deben ser positivas");
        if (maxSteps < 1) throw new IllegalArgumentException("maxSteps debe ser al menos 1");
        this.relTolerance = relTolerance;
        this.absTolerance = absTolerance;
        this.maxSteps = maxSteps;
    }

    public DormandPrince(double tolerance) {
        this(tolerance, tolerance, 100_000);
    }

    /**
     * Integra desde {@code t0} hasta {@code tEnd} (que puede ser menor que {@code t0}).
     * @throws ArithmeticException si se supera el máximo de pasos o el paso se vuelve demasiado pequeño
     */
    public OdeSolution solve(OdeFunction f, double t0, double[] y0, double tEnd) {
        List<Double> times = new ArrayList<>();
        List<double[]> states = new ArrayList<>();
        times.add(t0);
        states.add(y0.clone());

        double direction = Math.signum(tEnd - t0);
        double t = t0;
        double[] y = y0.clone();
        double h = direction * Math.abs(tEnd - t0) / 100;

        for (int iteration = 0; direction != 0 && direction * (tEnd - t) > 0; iteration++) {
            if (iteration >= maxSteps) throw new ArithmeticException("Se superó el máximo de " + maxSteps + " pasos");
            if (direction * (t + h - tEnd) > 0) h = tEnd - t;
            if (Math.abs(h) < Math.ulp(t) * 4) throw new ArithmeticException("El paso se volvió demasiado pequeño en t = " + t);

            double[][] k = new double[7][];
            k[0] = f.derivative(t, y);
            for (int s = 1; s < 7; s++) {
                double[] stage = y.clone();
                for (int j = 0; j < s; j++)
                    for (int i = 0; i < y.length; i++) stage[i] += h * A[s][j] * k[j][i];
                k[s] = f.derivative(t + C[s] * h, stage);
            }

            double[] next = y.clone();
            double error = 0;
            for (int i = 0; i < y.length; i++) {
                double fifth = 0;
                double difference = 0;
                for (int s = 0; s < 7; s++) {
                    fifth += B5[s] * k[s][i];
                    difference += (B5[s] - B4[s]) * k[s][i];
                }
                next[i] = y[i] + h * fifth;
                double scale = absTolerance + relTolerance * Math.max(Math.abs(y[i]), Math.abs(next[i]));
                error = Math.max(error, Math.abs(h * difference) / scale);
            }

            if (error <= 1) {
                t = (direction * (t + h - tEnd) >= 0) ? tEnd : t + h;
                y = next;
                times.add(t);
                states.add(y.clone());
            }

            double factor = error == 0 ? 5 : Math.min(5, Math.max(0.2, 0.9 * Math.pow(error, -0.2)));
            h *= factor;
        }

        return new OdeSolution(
                times.stream().mapToDouble(Double::doubleValue).toArray(),
                states.toArray(double[][]::new));
    }

    /**
     * Versión para una EDO escalar.
     */
    public OdeSolution solve(DoubleBinaryOperator f, double t0, double y0, double tEnd) {
        return solve(OdeFunction.scalar(f), t0, new double[]{y0}, tEnd);
    }
}
