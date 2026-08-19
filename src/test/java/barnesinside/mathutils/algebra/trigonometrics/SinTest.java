package barnesinside.mathutils.algebra.trigonometrics;

import barnesinside.mathutils.algebra.Const;
import barnesinside.mathutils.algebra.Evaluation;
import barnesinside.mathutils.algebra.Var;
import barnesinside.mathutils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class SinTest {

    @Test
    void evalZero() throws FunctionEvaluationException {
        Sin s = new Sin(new Const(0));
        double result = s.eval();
        assert Math.abs(result) < 1e-10 : "Expected ~0 but got " + result;
    }

    @Test
    void evalPiOverTwo() throws FunctionEvaluationException {
        Sin s = new Sin(new Const(Math.PI / 2));
        double result = s.eval();
        assert Math.abs(result - 1.0) < 1e-10 : "Expected ~1.0 but got " + result;
    }

    @Test
    void evalPi() throws FunctionEvaluationException {
        Sin s = new Sin(new Const(Math.PI));
        double result = s.eval();
        assert Math.abs(result) < 1e-10 : "Expected ~0 but got " + result;
    }
}
