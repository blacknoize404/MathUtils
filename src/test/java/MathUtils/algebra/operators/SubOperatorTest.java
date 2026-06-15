package MathUtils.algebra.operators;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Var;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class SubOperatorTest {

    @Test
    void evalTwoNumbers() throws FunctionEvaluationException {
        SubOperator op = new SubOperator(new Const(10), new Const(3));
        assert op.eval() == 7;
    }

    @Test
    void evalMultipleNumbers() throws FunctionEvaluationException {
        SubOperator op = new SubOperator(new Const(20), new Const(5), new Const(3));
        assert op.eval() == 12;
    }

    @Test
    void evalWithVariables() throws FunctionEvaluationException {
        SubOperator op = new SubOperator(new Var("x"), new Var("y"));
        assert op.eval(new Evaluation("x", 10), new Evaluation("y", 3)) == 7;
    }

    @Test
    void evalMixedConstVar() throws FunctionEvaluationException {
        SubOperator op = new SubOperator(new Const(10), new Var("t"));
        assert op.eval(new Evaluation("t", 3)) == 7;
    }

    @Test
    void evalNumberAndElement() throws FunctionEvaluationException {
        SubOperator op = new SubOperator(10, new Var("x"));
        assert op.eval(new Evaluation("x", 3)) == 7;
    }

    @Test
    void evalElementAndNumber() throws FunctionEvaluationException {
        SubOperator op = new SubOperator(new Var("x"), 3);
        assert op.eval(new Evaluation("x", 10)) == 7;
    }

    @Test
    void evalTwoNumbersViaConstants() throws FunctionEvaluationException {
        SubOperator op = new SubOperator(10, 3);
        assert op.eval() == 7;
    }

    @Test
    void evalNegativeResult() throws FunctionEvaluationException {
        SubOperator op = new SubOperator(new Const(3), new Const(10));
        assert op.eval() == -7;
    }

    @Test
    void evalZero() throws FunctionEvaluationException {
        SubOperator op = new SubOperator(new Const(5), new Const(5));
        assert op.eval() == 0;
    }

    @Test
    void testToString() {
        SubOperator op = new SubOperator(new Const(10), new Const(3));
        String s = op.toString();
        assert s.contains("10") && s.contains("3") && s.contains("-");
    }

    @Test
    void equalsToSame() {
        SubOperator op1 = new SubOperator(new Const(10), new Const(3));
        SubOperator op2 = new SubOperator(new Const(10), new Const(3));
        assert op1.equalsTo(op2);
    }

    @Test
    void equalsToDifferent() {
        SubOperator op1 = new SubOperator(new Const(10), new Const(3));
        SubOperator op2 = new SubOperator(new Const(10), new Const(5));
        assert !op1.equalsTo(op2);
    }
}
