package barnesinside.mathutils.calculus;

import barnesinside.mathutils.algebra.primitives.MathElement;

import java.util.function.DoubleUnaryOperator;

/**
 * Búsqueda numérica de raíces: bisección, Newton-Raphson y secante.
 * Los métodos lanzan {@link ArithmeticException} si no convergen.
 */
public final class RootFinding {

    private RootFinding() {
    }

    /**
     * Bisección en un intervalo donde la función cambia de signo.
     * @throws IllegalArgumentException si {@code f(a)} y {@code f(b)} tienen el mismo signo
     */
    public static double bisection(DoubleUnaryOperator f, double a, double b, double tolerance, int maxIterations) {
        double fa = f.applyAsDouble(a);
        double fb = f.applyAsDouble(b);
        if (fa == 0) return a;
        if (fb == 0) return b;
        if (Math.signum(fa) == Math.signum(fb)) {
            throw new IllegalArgumentException("f(a) y f(b) deben tener signos opuestos");
        }

        for (int i = 0; i < maxIterations; i++) {
            double mid = (a + b) / 2;
            double fm = f.applyAsDouble(mid);
            if (fm == 0 || (b - a) / 2 < tolerance) return mid;
            if (Math.signum(fm) == Math.signum(fa)) {
                a = mid;
                fa = fm;
            } else {
                b = mid;
            }
        }
        throw new ArithmeticException("La bisección no convergió en " + maxIterations + " iteraciones");
    }

    /**
     * Newton-Raphson con la derivada dada.
     */
    public static double newton(DoubleUnaryOperator f, DoubleUnaryOperator derivative, double x0,
                                double tolerance, int maxIterations) {
        double x = x0;
        for (int i = 0; i < maxIterations; i++) {
            double fx = f.applyAsDouble(x);
            if (Math.abs(fx) < tolerance) return x;

            double dfx = derivative.applyAsDouble(x);
            if (dfx == 0) throw new ArithmeticException("La derivada se anuló en x = " + x);

            double next = x - fx / dfx;
            if (Math.abs(next - x) < tolerance) return next;
            x = next;
        }
        throw new ArithmeticException("Newton no convergió en " + maxIterations + " iteraciones");
    }

    /**
     * Newton-Raphson sobre una expresión, derivándola simbólicamente.
     */
    public static double newton(MathElement e, String variable, double x0, double tolerance, int maxIterations) {
        return newton(Functions.of(e, variable), Functions.of(Derivative.of(e, variable), variable),
                x0, tolerance, maxIterations);
    }

    /**
     * Método de la secante a partir de dos aproximaciones iniciales.
     */
    public static double secant(DoubleUnaryOperator f, double x0, double x1, double tolerance, int maxIterations) {
        double f0 = f.applyAsDouble(x0);
        double f1 = f.applyAsDouble(x1);

        for (int i = 0; i < maxIterations; i++) {
            if (Math.abs(f1) < tolerance) return x1;
            if (f1 == f0) throw new ArithmeticException("La secante es horizontal en x = " + x1);

            double x2 = x1 - f1 * (x1 - x0) / (f1 - f0);
            if (Math.abs(x2 - x1) < tolerance) return x2;

            x0 = x1;
            f0 = f1;
            x1 = x2;
            f1 = f.applyAsDouble(x1);
        }
        throw new ArithmeticException("La secante no convergió en " + maxIterations + " iteraciones");
    }
}
