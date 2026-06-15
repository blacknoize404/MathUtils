package MathUtils.algebra.exponentials;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Var;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class SqrtTest {

    @Test
    void evalNumber() throws FunctionEvaluationException {
        Sqrt s = new Sqrt(9);
        double result = s.eval();
        assert Math.abs(result - 3.0) < 1e-10 : "Expected ~3.0 but got " + result;
    }

    @Test
    void evalConst() throws FunctionEvaluationException {
        Sqrt s = new Sqrt(new Const(16));
        double result = s.eval();
        assert Math.abs(result - 4.0) < 1e-10;
    }

    @Test
    void evalWithVariable() throws FunctionEvaluationException {
        Sqrt s = new Sqrt(new Var("x"));
        double result = s.eval(new Evaluation("x", 25));
        assert Math.abs(result - 5.0) < 1e-10;
    }

    @Test
    void evalZero() throws FunctionEvaluationException {
        Sqrt s = new Sqrt(0);
        double result = s.eval();
        assert Math.abs(result) < 1e-10;
    }

    @Test
    void evalOne() throws FunctionEvaluationException {
        Sqrt s = new Sqrt(1);
        double result = s.eval();
        assert Math.abs(result - 1.0) < 1e-10;
    }

    @Test
    void equalsToSqrtVsRoot() {
        Sqrt s = new Sqrt(4);
        Root r = new Root(new Const(4), 2);
        assert s.equalsTo(r);
    }

    @Test
    void equalsToDifferent() {
        Sqrt s1 = new Sqrt(4);
        Sqrt s2 = new Sqrt(9);
        assert !s1.equalsTo(s2);
    }

    @Test
    void testToString() {
        Sqrt s = new Sqrt(4);
        String str = s.toString();
        assert str != null;
    }
}
