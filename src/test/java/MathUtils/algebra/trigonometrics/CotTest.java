package MathUtils.algebra.trigonometrics;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Var;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class CotTest {

    @Test
    void evalPiOverFour() throws FunctionEvaluationException {
        Cot c = new Cot(new Const(Math.PI / 4));
        double result = c.eval();
        assert Math.abs(result - 1.0) < 1e-10 : "Expected ~1.0 but got " + result;
    }

    @Test
    void evalWithVariable() throws FunctionEvaluationException {
        Cot c = new Cot(new Var("x"));
        double result = c.eval(new Evaluation("x", Math.PI / 4));
        assert Math.abs(result - 1.0) < 1e-10;
    }

    @Test
    void equalsToSame() {
        Cot c1 = new Cot(new Const(Math.PI / 4));
        Cot c2 = new Cot(new Const(Math.PI / 4));
        assert c1.equalsTo(c2);
    }

    @Test
    void equalsToDifferent() {
        Cot c1 = new Cot(new Const(Math.PI / 4));
        Cot c2 = new Cot(new Const(1));
        assert !c1.equalsTo(c2);
    }
}
