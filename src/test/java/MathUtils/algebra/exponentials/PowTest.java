package MathUtils.algebra.exponentials;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Var;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class PowTest {

    @Test
    void evalNumberNumber() throws FunctionEvaluationException {
        Pow p = new Pow(2, 3);
        assert p.eval() == 8;
    }

    @Test
    void evalElementNumber() throws FunctionEvaluationException {
        Pow p = new Pow(new Const(3), 2);
        assert p.eval() == 9;
    }

    @Test
    void evalNumberElement() throws FunctionEvaluationException {
        Pow p = new Pow(2, new Const(4));
        assert p.eval() == 16;
    }

    @Test
    void evalElementElement() throws FunctionEvaluationException {
        Pow p = new Pow(new Const(2), new Const(3));
        assert p.eval() == 8;
    }

    @Test
    void evalWithVariables() throws FunctionEvaluationException {
        Pow p = new Pow(new Var("x"), 2);
        assert p.eval(new Evaluation("x", 5)) == 25;
    }

    @Test
    void evalNegativeBase() throws FunctionEvaluationException {
        Pow p = new Pow(-2, 3);
        assert p.eval() == -8;
    }

    @Test
    void evalFractionalExponent() throws FunctionEvaluationException {
        Pow p = new Pow(9, 0.5);
        assert Math.abs(p.eval() - 3.0) < 1e-10;
    }

    @Test
    void evalNegativeExponent() throws FunctionEvaluationException {
        Pow p = new Pow(2, -1);
        assert p.eval() == 0.5;
    }

    @Test
    void evalZeroExponent() throws FunctionEvaluationException {
        Pow p = new Pow(5, 0);
        assert p.eval() == 1;
    }

    @Test
    void evalZeroBase() throws FunctionEvaluationException {
        Pow p = new Pow(0, 5);
        assert p.eval() == 0;
    }

    @Test
    void testToString() {
        Pow p = new Pow(new Const(2), new Const(3));
        String s = p.toString();
        assert s.contains("2") && s.contains("3") && s.contains("^");
    }

    @Test
    void toRoot() {
        Pow p = new Pow(8, 3);
        Root r = p.toRoot();
        assert r != null;
    }

    @Test
    void equalsToSame() {
        Pow p1 = new Pow(new Const(2), new Const(3));
        Pow p2 = new Pow(new Const(2), new Const(3));
        assert p1.equalsTo(p2);
    }

    @Test
    void equalsToDifferent() {
        Pow p1 = new Pow(new Const(2), new Const(3));
        Pow p2 = new Pow(new Const(2), new Const(4));
        assert !p1.equalsTo(p2);
    }
}
