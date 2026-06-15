package MathUtils.algebra.operators;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Var;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class MultOperatorTest {

    @Test
    void evalTwoNumbers() throws FunctionEvaluationException {
        MultOperator op = new MultOperator(new Const(3), new Const(4));
        assert op.eval() == 12;
    }

    @Test
    void evalMultipleNumbers() throws FunctionEvaluationException {
        MultOperator op = new MultOperator(new Const(2), new Const(3), new Const(4));
        assert op.eval() == 24;
    }

    @Test
    void evalWithVariables() throws FunctionEvaluationException {
        MultOperator op = new MultOperator(new Var("x"), new Var("y"));
        assert op.eval(new Evaluation("x", 3), new Evaluation("y", 4)) == 12;
    }

    @Test
    void evalMixedConstVar() throws FunctionEvaluationException {
        MultOperator op = new MultOperator(new Const(5), new Var("t"));
        assert op.eval(new Evaluation("t", 3)) == 15;
    }

    @Test
    void evalFractionalBase() throws FunctionEvaluationException {
        MultOperator op = new MultOperator(1 / 8d, new Var("t"));
        assert op.eval(new Evaluation("t", 8)) == 1;
    }

    @Test
    void evalZeroOptimization() throws FunctionEvaluationException {
        MultOperator op = new MultOperator(new Const(0), new Const(100), new Const(200));
        assert op.eval() == 0;
    }

    @Test
    void evalNegative() throws FunctionEvaluationException {
        MultOperator op = new MultOperator(new Const(-3), new Const(4));
        assert op.eval() == -12;
    }

    @Test
    void evalNegativeNegative() throws FunctionEvaluationException {
        MultOperator op = new MultOperator(new Const(-3), new Const(-4));
        assert op.eval() == 12;
    }

    @Test
    void evalNumberAndElement() throws FunctionEvaluationException {
        MultOperator op = new MultOperator(5, new Var("x"));
        assert op.eval(new Evaluation("x", 3)) == 15;
    }

    @Test
    void evalElementAndNumber() throws FunctionEvaluationException {
        MultOperator op = new MultOperator(new Var("x"), 5);
        assert op.eval(new Evaluation("x", 3)) == 15;
    }

    @Test
    void evalTwoNumbersViaConstants() throws FunctionEvaluationException {
        MultOperator op = new MultOperator(3, 4);
        assert op.eval() == 12;
    }

    @Test
    void testToString() {
        MultOperator op = new MultOperator(new Const(2), new Const(3));
        String s = op.toString();
        assert s.contains("2") && s.contains("3") && s.contains("*");
    }

    @Test
    void equalsToSame() {
        MultOperator op1 = new MultOperator(new Const(2), new Const(3));
        MultOperator op2 = new MultOperator(new Const(2), new Const(3));
        assert op1.equalsTo(op2);
    }

    @Test
    void equalsToDifferent() {
        MultOperator op1 = new MultOperator(new Const(2), new Const(3));
        MultOperator op2 = new MultOperator(new Const(2), new Const(4));
        assert !op1.equalsTo(op2);
    }

    @Test
    void ofNumberAndElement() {
        MultOperator op = MultOperator.of(5, new Var("x"));
        assert op.getContent().size() == 2;
    }

    @Test
    void ofElementAndNumber() {
        MultOperator op = MultOperator.of(new Var("x"), 5);
        assert op.getContent().size() == 2;
    }

    @Test
    void ofTwoNumbers() throws FunctionEvaluationException {
        MultOperator op = MultOperator.of(3, 4);
        assert op.eval() == 12;
    }

    @Test
    void ofTwoElements() throws FunctionEvaluationException {
        MultOperator op = MultOperator.of(new Const(3), new Const(4));
        assert op.eval() == 12;
    }
}
