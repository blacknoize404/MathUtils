package MathUtils.algebra.exponentials;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Var;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class CbrtTest {

    @Test
    void evalConst() throws FunctionEvaluationException {
        Cbrt c = new Cbrt(new Const(27));
        double result = c.eval();
        assert Math.abs(result - 3.0) < 1e-10 : "Expected ~3.0 but got " + result;
    }

    @Test
    void evalWithVariable() throws FunctionEvaluationException {
        Cbrt c = new Cbrt(new Var("x"));
        double result = c.eval(new Evaluation("x", 8));
        assert Math.abs(result - 2.0) < 1e-10 : "Expected ~2.0 but got " + result;
    }

    @Test
    void evalNegative() throws FunctionEvaluationException {
        Cbrt c = new Cbrt(new Const(-8));
        double result = c.eval();
        assert Math.abs(result + 2.0) < 1e-10 : "Expected ~-2.0 but got " + result;
    }

    @Test
    void evalZero() throws FunctionEvaluationException {
        Cbrt c = new Cbrt(new Const(0));
        double result = c.eval();
        assert Math.abs(result) < 1e-10;
    }

    @Test
    void evalOne() throws FunctionEvaluationException {
        Cbrt c = new Cbrt(new Const(1));
        double result = c.eval();
        assert Math.abs(result - 1.0) < 1e-10;
    }
}
