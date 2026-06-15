package MathUtils.algebra.operators;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Var;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class DivOperatorTest {

    @Test
    void evalTwoNumbers() throws FunctionEvaluationException {
        DivOperator op = new DivOperator(new Const(10), new Const(2));
        assert op.eval() == 5;
    }

    @Test
    void evalMultipleNumbers() throws FunctionEvaluationException {
        DivOperator op = new DivOperator(new Const(24), new Const(2), new Const(3));
        assert op.eval() == 4;
    }

    @Test
    void evalWithVariables() throws FunctionEvaluationException {
        DivOperator op = new DivOperator(new Var("x"), new Var("y"));
        assert op.eval(new Evaluation("x", 10), new Evaluation("y", 2)) == 5;
    }

    @Test
    void evalMixedConstVar() throws FunctionEvaluationException {
        DivOperator op = new DivOperator(new Const(15), new Var("t"));
        assert op.eval(new Evaluation("t", 3)) == 5;
    }

    @Test
    void evalNumberAndElement() throws FunctionEvaluationException {
        DivOperator op = new DivOperator(10, new Var("x"));
        assert op.eval(new Evaluation("x", 2)) == 5;
    }

    @Test
    void evalElementAndNumber() throws FunctionEvaluationException {
        DivOperator op = new DivOperator(new Var("x"), 2);
        assert op.eval(new Evaluation("x", 10)) == 5;
    }

    @Test
    void evalTwoNumbersViaConstants() throws FunctionEvaluationException {
        DivOperator op = new DivOperator(10, 2);
        assert op.eval() == 5;
    }

    @Test
    void evalDivisionByZero() throws FunctionEvaluationException {
        DivOperator op = new DivOperator(new Const(10), new Const(0));
        try {
            op.eval();
            assert false : "Should have thrown ArithmeticException";
        } catch (ArithmeticException e) {
            assert true;
        }
    }

    @Test
    void evalNegativeDivision() throws FunctionEvaluationException {
        DivOperator op = new DivOperator(new Const(-10), new Const(2));
        assert op.eval() == -5;
    }

    @Test
    void evalDoubleDivision() throws FunctionEvaluationException {
        DivOperator op = new DivOperator(new Const(7), new Const(2));
        assert op.eval() == 3.5;
    }

    @Test
    void testToString() {
        DivOperator op = new DivOperator(new Const(10), new Const(2));
        String s = op.toString();
        assert s.contains("10") && s.contains("2") && s.contains("/");
    }

    @Test
    void equalsToSame() {
        DivOperator op1 = new DivOperator(new Const(10), new Const(2));
        DivOperator op2 = new DivOperator(new Const(10), new Const(2));
        assert op1.equalsTo(op2);
    }

    @Test
    void equalsToDifferent() {
        DivOperator op1 = new DivOperator(new Const(10), new Const(2));
        DivOperator op2 = new DivOperator(new Const(10), new Const(5));
        assert !op1.equalsTo(op2);
    }
}
