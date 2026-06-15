package MathUtils.algebra.logarithmics;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Var;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class LogTest {

    @Test
    void evalNumberNumber() throws FunctionEvaluationException {
        Log log = new Log(100, 10);
        assert log.eval() == 2.0;
    }

    @Test
    void evalElementNumber() throws FunctionEvaluationException {
        Log log = new Log(new Const(8), 2);
        assert log.eval() == 3.0;
    }

    @Test
    void evalNumberElement() throws FunctionEvaluationException {
        Log log = new Log(new Const(100), new Const(10));
        assert log.eval() == 2.0;
    }

    @Test
    void evalElementElement() throws FunctionEvaluationException {
        Log log = new Log(new Const(81), new Const(3));
        assert log.eval() == 4.0;
    }

    @Test
    void evalWithVariable() throws FunctionEvaluationException {
        Log log = new Log(new Var("x"), 2);
        assert log.eval(new Evaluation("x", 8)) == 3.0;
    }

    @Test
    void evalNaturalLog() throws FunctionEvaluationException {
        Log log = new Log(new Const(Math.E), Math.E);
        assert Math.abs(log.eval() - 1.0) < 1e-10;
    }

    @Test
    void evalLogBase10() throws FunctionEvaluationException {
        Log log = new Log(1000, 10);
        assert Math.abs(log.eval() - 3.0) < 1e-10;
    }

    @Test
    void evalLogBaseChange() throws FunctionEvaluationException {
        Log log = new Log(32, 2);
        assert log.eval() == 5.0;
    }

    @Test
    void evalLogOfOne() throws FunctionEvaluationException {
        Log log = new Log(1, 10);
        assert log.eval() == 0.0;
    }

    @Test
    void evalLogOfBase() throws FunctionEvaluationException {
        Log log = new Log(5, 5);
        assert Math.abs(log.eval() - 1.0) < 1e-10;
    }

    @Test
    void evalArgumentZero() throws FunctionEvaluationException {
        Log log = new Log(0, 10);
        try {
            log.eval();
            assert false : "Should have thrown IllegalArgumentException";
        } catch (IllegalArgumentException e) {
            assert true;
        }
    }

    @Test
    void evalArgumentNegative() throws FunctionEvaluationException {
        Log log = new Log(-5, 10);
        try {
            log.eval();
            assert false : "Should have thrown IllegalArgumentException";
        } catch (IllegalArgumentException e) {
            assert true;
        }
    }

    @Test
    void evalBaseOne() throws FunctionEvaluationException {
        Log log = new Log(10, 1);
        try {
            log.eval();
            assert false : "Should have thrown IllegalArgumentException";
        } catch (IllegalArgumentException e) {
            assert true;
        }
    }

    @Test
    void testToString() {
        Log log = new Log(100, 10);
        String s = log.toString();
        assert s.contains("100") && s.contains("10") && s.contains("Log");
    }

    @Test
    void equalsToSame() {
        Log log1 = new Log(new Const(100), new Const(10));
        Log log2 = new Log(new Const(100), new Const(10));
        assert log1.equalsTo(log2);
    }

    @Test
    void equalsToDifferent() {
        Log log1 = new Log(new Const(100), new Const(10));
        Log log2 = new Log(new Const(64), new Const(8));
        assert !log1.equalsTo(log2);
    }
}
