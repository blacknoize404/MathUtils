package MathUtils.ode;

import org.junit.jupiter.api.Test;

class OdeTest {

    /** y' = y, y(0) = 1  ->  y(1) = e */
    private static double errorAtOne(ODESolver solver, int steps) {
        OdeSolution s = solver.solve((t, y) -> y, 0, 1, 1, steps);
        return Math.abs(s.finalState()[0] - Math.E);
    }

    @Test
    void solutionShape() {
        OdeSolution s = new Euler().solve((t, y) -> y, 0, 1, 1, 10);
        assert s.t().length == 11 && s.y().length == 11;
        assert s.t()[0] == 0 && Math.abs(s.finalTime() - 1) < 1e-12;
        assert s.y()[0][0] == 1;
    }

    @Test
    void eulerFirstStep() {
        OdeSolution s = new Euler().solve((t, y) -> y, 0, 1, 1, 1);
        assert s.finalState()[0] == 2;
    }

    @Test
    void methodsConvergeWithExpectedOrder() {
        // Al duplicar los pasos, el error se divide por 2^orden
        assert errorAtOne(new Euler(), 100) / errorAtOne(new Euler(), 200) > 1.9;
        assert errorAtOne(new Heun(), 100) / errorAtOne(new Heun(), 200) > 3.8;
        assert errorAtOne(new Midpoint(), 100) / errorAtOne(new Midpoint(), 200) > 3.8;
        assert errorAtOne(new RK4(), 20) / errorAtOne(new RK4(), 40) > 15;
    }

    @Test
    void rk4IsAccurate() {
        assert errorAtOne(new RK4(), 50) < 1e-8;
    }

    @Test
    void backwardIntegration() {
        OdeSolution s = new RK4().solve((t, y) -> y, 1, Math.E, 0, 50);
        assert Math.abs(s.finalState()[0] - 1) < 1e-8;
    }

    @Test
    void systemHarmonicOscillator() {
        // x'' = -x como sistema [x, v]; x(0)=1, v(0)=0 -> x = cos t
        OdeFunction oscillator = (t, y) -> new double[]{y[1], -y[0]};
        OdeSolution s = new RK4().solve(oscillator, 0, new double[]{1, 0}, Math.PI, 200);
        assert Math.abs(s.finalState()[0] + 1) < 1e-8;
        assert Math.abs(s.finalState()[1]) < 1e-8;
        assert s.component(0).length == 201;
    }

    @Test
    void invalidStepsThrow() {
        try {
            new Euler().solve((t, y) -> y, 0, 1, 1, 0);
            assert false;
        } catch (IllegalArgumentException e) {
            assert true;
        }
    }

    @Test
    void dormandPrinceMeetsTolerance() {
        OdeSolution s = new DormandPrince(1e-10).solve((t, y) -> y, 0, 1, 2);
        assert Math.abs(s.finalState()[0] - Math.exp(2)) < 1e-8;
        assert Math.abs(s.finalTime() - 2) < 1e-12;
    }

    @Test
    void dormandPrinceUsesFewerStepsThanFixedForSmoothProblems() {
        OdeSolution s = new DormandPrince(1e-6).solve((t, y) -> y, 0, 1, 1);
        assert s.t().length < 100;
        assert Math.abs(s.finalState()[0] - Math.E) < 1e-5;
    }

    @Test
    void dormandPrinceHandlesSystemsAndBackward() {
        OdeFunction oscillator = (t, y) -> new double[]{y[1], -y[0]};
        OdeSolution s = new DormandPrince(1e-10).solve(oscillator, 0, new double[]{1, 0}, -Math.PI);
        assert Math.abs(s.finalState()[0] + 1) < 1e-7;
    }

    @Test
    void dormandPrinceAbortsOnTooManySteps() {
        try {
            new DormandPrince(1e-12, 1e-12, 5).solve((t, y) -> y, 0, 1, 10);
            assert false;
        } catch (ArithmeticException e) {
            assert true;
        }
    }

    @Test
    void shootingSolvesBoundaryProblem() {
        // y'' = -y, y(0) = 0, y(pi/2) = 1  ->  y = sin x
        OdeSolution s = BoundaryValue.shooting((x, y, dy) -> -y, 0, Math.PI / 2, 0, 1, 0, 2, 200);
        assert Math.abs(s.finalState()[0] - 1) < 1e-8;
        assert Math.abs(s.y()[100][0] - Math.sin(s.t()[100])) < 1e-7;
    }

    @Test
    void shootingNonLinear() {
        // y'' = 2y^3 con y = 1/(x+1) en [0, 1]: y(0)=1, y(1)=0.5
        OdeSolution s = BoundaryValue.shooting((x, y, dy) -> 2 * y * y * y, 0, 1, 1, 0.5, -0.8, -1.2, 400);
        assert Math.abs(s.y()[200][0] - 1 / (s.t()[200] + 1)) < 1e-6;
    }

    @Test
    void finiteDifferencesLinearProblem() {
        // y'' = -y, y(0) = 0, y(pi/2) = 1
        OdeSolution s = BoundaryValue.finiteDifferences(x -> 0, x -> -1, x -> 0, 0, Math.PI / 2, 0, 1, 200);
        for (int i = 0; i <= 200; i += 20) {
            assert Math.abs(s.y()[i][0] - Math.sin(s.t()[i])) < 1e-4 : i;
        }
    }
}
