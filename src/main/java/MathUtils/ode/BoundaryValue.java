package MathUtils.ode;

import MathUtils.calculus.RootFinding;
import MathUtils.matrix.Matrix;

import java.util.function.DoubleUnaryOperator;

/**
 * Problemas de valores en la frontera de segundo orden con condiciones
 * {@code y(a) = ya}, {@code y(b) = yb}.
 */
public final class BoundaryValue {

    /**
     * Lado derecho de {@code y'' = f(x, y, y')}.
     */
    @FunctionalInterface
    public interface SecondOrder {
        double apply(double x, double y, double dy);
    }

    private BoundaryValue() {
    }

    /**
     * Método del disparo: busca la pendiente inicial {@code y'(a)} que hace {@code y(b) = yb},
     * integrando con RK4 y refinando con el método de la secante.
     * @param slopeGuess0 primera estimación de y'(a)
     * @param slopeGuess1 segunda estimación de y'(a) (distinta de la primera)
     * @param steps pasos de RK4 por integración
     * @return la solución con {@code steps + 1} puntos; las componentes son {@code [y, y']}
     * @throws ArithmeticException si la secante no converge
     */
    public static OdeSolution shooting(SecondOrder f, double a, double b, double ya, double yb,
                                       double slopeGuess0, double slopeGuess1, int steps) {
        OdeFunction system = (x, y) -> new double[]{y[1], f.apply(x, y[0], y[1])};
        RK4 rk4 = new RK4();

        DoubleUnaryOperator residual = slope ->
                rk4.solve(system, a, new double[]{ya, slope}, b, steps).finalState()[0] - yb;

        double slope = RootFinding.secant(residual, slopeGuess0, slopeGuess1, 1e-12, 100);
        return rk4.solve(system, a, new double[]{ya, slope}, b, steps);
    }

    /**
     * Diferencias finitas centrales para el problema lineal
     * {@code y'' = p(x)·y' + q(x)·y + r(x)}.
     * @param n cantidad de subintervalos (≥ 2)
     * @return la solución con {@code n + 1} puntos; cada estado tiene una sola componente {@code [y]}
     * @throws ArithmeticException si el sistema resultante es singular
     */
    public static OdeSolution finiteDifferences(DoubleUnaryOperator p, DoubleUnaryOperator q, DoubleUnaryOperator r,
                                                double a, double b, double ya, double yb, int n) {
        if (n < 2) throw new IllegalArgumentException("n debe ser al menos 2");

        double h = (b - a) / n;
        int unknowns = n - 1;
        double[][] matrix = new double[unknowns][unknowns];
        double[] rhs = new double[unknowns];

        for (int i = 1; i < n; i++) {
            double x = a + i * h;
            double lower = 1 / (h * h) + p.applyAsDouble(x) / (2 * h);
            double diagonal = -2 / (h * h) - q.applyAsDouble(x);
            double upper = 1 / (h * h) - p.applyAsDouble(x) / (2 * h);

            int row = i - 1;
            matrix[row][row] = diagonal;
            rhs[row] = r.applyAsDouble(x);
            if (i > 1) matrix[row][row - 1] = lower;
            else rhs[row] -= lower * ya;
            if (i < n - 1) matrix[row][row + 1] = upper;
            else rhs[row] -= upper * yb;
        }

        double[] solution = Matrix.of(matrix).solve(rhs);

        double[] t = new double[n + 1];
        double[][] y = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            t[i] = a + i * h;
            double value = i == 0 ? ya : (i == n ? yb : solution[i - 1]);
            y[i] = new double[]{value};
        }
        return new OdeSolution(t, y);
    }
}
