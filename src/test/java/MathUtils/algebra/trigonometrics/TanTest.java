package MathUtils.algebra.trigonometrics;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Var;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class TanTest {

    @Test
    void evalZero() throws FunctionEvaluationException {
        Tan t = new Tan(new Const(0));
        assert t.eval() == 0.0;
    }

    @Test
    void evalPiOverFour() throws FunctionEvaluationException {
        Tan t = new Tan(new Const(Math.PI / 4));
        double result = t.eval();
        assert Math.abs(result - 1.0) < 1e-10 : "Expected ~1.0 but got " + result;
    }

    @Test
    void evalWithVariable() throws FunctionEvaluationException {
        Tan t = new Tan(new Var("x"));
        double result = t.eval(new Evaluation("x", 0));
        assert result == 0.0;
    }

    @Test
    void equalsToSame() {
        Tan t1 = new Tan(new Const(0));
        Tan t2 = new Tan(new Const(0));
        assert t1.equalsTo(t2);
    }

    @Test
    void equalsToDifferent() {
        Tan t1 = new Tan(new Const(0));
        Tan t2 = new Tan(new Const(1));
        assert !t1.equalsTo(t2);
    }
}
