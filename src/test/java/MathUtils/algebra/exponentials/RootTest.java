package MathUtils.algebra.exponentials;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Var;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class RootTest {

    @Test
    void evalElementNumber() throws FunctionEvaluationException {
        Root r = new Root(new Const(8), 3);
        double result = r.eval();
        assert Math.abs(result - 2.0) < 1e-10 : "Expected ~2.0 but got " + result;
    }

    @Test
    void evalNumberElement() throws FunctionEvaluationException {
        Root r = new Root(27, new Const(3));
        double result = r.eval();
        assert Math.abs(result - 3.0) < 1e-10 : "Expected ~3.0 but got " + result;
    }

    @Test
    void evalNumberNumber() throws FunctionEvaluationException {
        Root r = new Root(16, 2);
        double result = r.eval();
        assert Math.abs(result - 4.0) < 1e-10 : "Expected ~4.0 but got " + result;
    }

    @Test
    void evalElementElement() throws FunctionEvaluationException {
        Root r = new Root(new Const(8), new Const(3));
        double result = r.eval();
        assert Math.abs(result - 2.0) < 1e-10 : "Expected ~2.0 but got " + result;
    }

    @Test
    void evalWithVariable() throws FunctionEvaluationException {
        Root r = new Root(new Var("x"), 2);
        double result = r.eval(new Evaluation("x", 9));
        assert Math.abs(result - 3.0) < 1e-10;
    }

    @Test
    void evalSquareRoot() throws FunctionEvaluationException {
        Root r = new Root(25, 2);
        double result = r.eval();
        assert Math.abs(result - 5.0) < 1e-10;
    }

    @Test
    void evalFourthRoot() throws FunctionEvaluationException {
        Root r = new Root(16, 4);
        double result = r.eval();
        assert Math.abs(result - 2.0) < 1e-10 : "Expected ~2.0 but got " + result;
    }

    @Test
    void getRoot() {
        Root r = new Root(new Const(8), 3);
        assert r.getRoot() != null;
    }

    @Test
    void toPow() {
        Root r = new Root(8, 3);
        Pow p = r.toPow();
        assert p != null;
    }

    @Test
    void testToString() {
        Root r = new Root(new Const(8), new Const(3));
        String s = r.toString();
        assert s.contains("8") && s.contains("1/3");
    }

    @Test
    void equalsToSame() {
        Root r1 = new Root(new Const(8), new Const(3));
        Root r2 = new Root(new Const(8), new Const(3));
        assert r1.equalsTo(r2);
    }

    @Test
    void equalsToDifferent() {
        Root r1 = new Root(new Const(8), new Const(3));
        Root r2 = new Root(new Const(9), new Const(3));
        assert !r1.equalsTo(r2);
    }
}
