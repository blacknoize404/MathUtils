package MathUtils.algebra.operators;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Var;
import MathUtils.algebra.primitives.MathElement;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class SumOperatorTest {

    @Test
    void evalTwoNumbers() throws FunctionEvaluationException {
        SumOperator op = new SumOperator(new Const(3), new Const(4));
        assert op.eval() == 7;
    }

    @Test
    void evalMultipleNumbers() throws FunctionEvaluationException {
        SumOperator op = new SumOperator(new Const(1), new Const(2), new Const(3), new Const(4));
        assert op.eval() == 10;
    }

    @Test
    void evalWithVariables() throws FunctionEvaluationException {
        SumOperator op = new SumOperator(new Var("x"), new Var("y"));
        assert op.eval(new Evaluation("x", 3), new Evaluation("y", 7)) == 10;
    }

    @Test
    void evalMixedConstVar() throws FunctionEvaluationException {
        SumOperator op = new SumOperator(new Const(5), new Var("t"));
        assert op.eval(new Evaluation("t", 3)) == 8;
    }

    @Test
    void evalSingleNumberAndElement() throws FunctionEvaluationException {
        SumOperator op = new SumOperator(5, new Var("x"));
        assert op.eval(new Evaluation("x", 3)) == 8;
    }

    @Test
    void evalElementAndNumber() throws FunctionEvaluationException {
        SumOperator op = new SumOperator(new Var("x"), 5);
        assert op.eval(new Evaluation("x", 3)) == 8;
    }

    @Test
    void evalTwoNumbersViaConstants() throws FunctionEvaluationException {
        SumOperator op = new SumOperator(10, 20);
        assert op.eval() == 30;
    }

    @Test
    void evalMultipleNumbersViaConstants() throws FunctionEvaluationException {
        SumOperator op = new SumOperator(1, 2, 3, 4, 5);
        assert op.eval() == 15;
    }

    @Test
    void evalNegative() throws FunctionEvaluationException {
        SumOperator op = new SumOperator(new Const(-5), new Const(3));
        assert op.eval() == -2;
    }

    @Test
    void evalZero() throws FunctionEvaluationException {
        SumOperator op = new SumOperator(new Const(0), new Const(0));
        assert op.eval() == 0;
    }

    @Test
    void testToString() {
        SumOperator op = new SumOperator(new Const(1), new Const(2), new Const(3));
        String s = op.toString();
        assert s.contains("1") && s.contains("2") && s.contains("3") && s.contains("+");
    }

    @Test
    void equalsToSame() {
        SumOperator op1 = new SumOperator(new Const(1), new Const(2));
        SumOperator op2 = new SumOperator(new Const(1), new Const(2));
        assert op1.equalsTo(op2);
    }

    @Test
    void equalsToDifferentValue() {
        SumOperator op1 = new SumOperator(new Const(1), new Const(2));
        SumOperator op2 = new SumOperator(new Const(1), new Const(3));
        assert !op1.equalsTo(op2);
    }
}
