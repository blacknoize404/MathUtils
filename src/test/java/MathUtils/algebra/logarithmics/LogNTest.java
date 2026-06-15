package MathUtils.algebra.logarithmics;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Var;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class LogNTest {

    @Test
    void evalE() throws FunctionEvaluationException {
        LogN ln = new LogN(new Const(Math.E));
        double result = ln.eval();
        assert Math.abs(result - 1.0) < 1e-10 : "Expected ~1.0 but got " + result;
    }

    @Test
    void evalOne() throws FunctionEvaluationException {
        LogN ln = new LogN(new Const(1));
        double result = ln.eval();
        assert Math.abs(result) < 1e-10;
    }

    @Test
    void evalWithVariable() throws FunctionEvaluationException {
        LogN ln = new LogN(new Var("x"));
        double result = ln.eval(new Evaluation("x", Math.E));
        assert Math.abs(result - 1.0) < 1e-10;
    }

    @Test
    void evalPowerOfE() throws FunctionEvaluationException {
        LogN ln = new LogN(new Const(Math.E * Math.E));
        double result = ln.eval();
        assert Math.abs(result - 2.0) < 1e-10 : "Expected ~2.0 but got " + result;
    }

    @Test
    void evalZero() throws FunctionEvaluationException {
        LogN ln = new LogN(new Const(0));
        try {
            ln.eval();
            assert false : "Should have thrown exception";
        } catch (IllegalArgumentException e) {
            assert true;
        }
    }

    @Test
    void testToString() {
        LogN ln = new LogN(new Const(Math.E));
        String s = ln.toString();
        assert s != null;
    }
}
