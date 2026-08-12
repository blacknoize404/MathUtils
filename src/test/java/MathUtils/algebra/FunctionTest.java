package MathUtils.algebra;

import MathUtils.algebra.exponentials.Cbrt;
import MathUtils.algebra.exponentials.Pow;
import MathUtils.algebra.exponentials.Root;
import MathUtils.algebra.exponentials.Sqrt;
import MathUtils.algebra.operators.DivOperator;
import MathUtils.algebra.operators.MultOperator;
import MathUtils.algebra.operators.SubOperator;
import MathUtils.algebra.operators.SumOperator;
import MathUtils.algebra.primitives.MathElement;
import MathUtils.algebra.trigonometrics.Cos;
import MathUtils.algebra.trigonometrics.Cot;
import MathUtils.algebra.trigonometrics.Sin;
import MathUtils.algebra.trigonometrics.Tan;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class FunctionTest {

    @Test
    void constructorMathElement() throws FunctionEvaluationException {
        Function f = new Function(new Const(42));
        assert f.eval() == 42;
    }

    @Test
    void constructorNumber() throws FunctionEvaluationException {
        Function f = new Function(42);
        assert f.eval() == 42;
    }

    @Test
    void constructorNumberDouble() throws FunctionEvaluationException {
        Function f = new Function(3.14);
        assert f.eval() == 3.14;
    }

    @Test
    void evalWithVariable() throws FunctionEvaluationException {
        Function f = new Function(new Var("x"));
        assert f.eval(new Evaluation("x", 10)) == 10;
    }

    @Test
    void evalWithMissingVariable() {
        Function f = new Function(new Var("x"));
        try {
            f.eval(new Evaluation("y", 10));
            assert false : "Should have thrown FunctionEvaluationException";
        } catch (FunctionEvaluationException e) {
            assert true;
        }
    }

    @Test
    void evalComplexExpression() throws FunctionEvaluationException {
        Function f = new Function(new SumOperator(new Const(3), new Const(4)));
        assert f.eval() == 7;
    }

    @Test
    void equalsToNull() {
        Function f = new Function(new Const(5));
        assert !f.equalsTo(null);
    }

    @Test
    void equalsToNonFunction() {
        Function f = new Function(new Const(5));
        assert !f.equalsTo(new Const(5));
    }

    @Test
    void equalsToSameContent() {
        assert new Function(new Const(5)).equalsTo(new Function(new Const(5)));
    }

    @Test
    void equalsToDifferentContent() {
        assert !new Function(new Const(5)).equalsTo(new Function(new Const(7)));
    }

    @Test
    void getContent() {
        Const c = new Const(5);
        Function f = new Function(c);
        assert f.getContent() == c;
    }

    @Test
    void sum() throws FunctionEvaluationException {
        Function f = new Function(new Var("x")).sum(new Const(3));
        assert f.eval(new Evaluation("x", 2)) == 5;
    }

    @Test
    void sub() throws FunctionEvaluationException {
        Function f = new Function(new Var("x")).sub(new Const(3));
        assert f.eval(new Evaluation("x", 10)) == 7;
    }

    @Test
    void mult() throws FunctionEvaluationException {
        Function f = new Function(new Var("x")).mult(new Const(5));
        assert f.eval(new Evaluation("x", 3)) == 15;
    }

    @Test
    void div() throws FunctionEvaluationException {
        Function f = new Function(new Var("x")).div(new Const(2));
        assert f.eval(new Evaluation("x", 10)) == 5;
    }

    @Test
    void pow() throws FunctionEvaluationException {
        Function f = new Function(new Var("x")).pow(new Const(3));
        assert f.eval(new Evaluation("x", 2)) == 8;
    }

    @Test
    void root() throws FunctionEvaluationException {
        Function f = new Function(new Const(27)).root(new Const(3));
        double result = f.eval();
        assert Math.abs(result - 3.0) < 1e-10 : "Expected ~3.0 but got " + result;
    }

    @Test
    void sin() throws FunctionEvaluationException {
        Function f = new Function(new Const(0)).sin();
        assert f.eval() == 0.0;
    }

    @Test
    void cos() throws FunctionEvaluationException {
        Function f = new Function(new Const(0)).cos();
        assert f.eval() == 1.0;
    }

    @Test
    void tan() throws FunctionEvaluationException {
        Function f = new Function(new Const(0)).tan();
        assert f.eval() == 0.0;
    }

    @Test
    void cot() throws FunctionEvaluationException {
        Function f = new Function(new Const(Math.PI / 4)).cot();
        double result = f.eval();
        assert Math.abs(result - 1.0) < 1e-10 : "Expected ~1.0 but got " + result;
    }

    @Test
    void sumNumberConstant() throws FunctionEvaluationException {
        Function f = new Function(5).sum(new Const(3));
        assert f.eval() == 8;
    }

    @Test
    void chainedOperations() throws FunctionEvaluationException {
        Function f = new Function(new Var("x"))
                .mult(new Const(2))
                .sum(new Const(1));
        assert f.eval(new Evaluation("x", 3)) == 7;
    }

    @Test
    void chainedPowRoot() throws FunctionEvaluationException {
        Function f = new Function(new Var("x"))
                .pow(new Const(2))
                .root(new Const(2));
        double result = f.eval(new Evaluation("x", 4));
        assert Math.abs(result - 4.0) < 1e-10 : "Expected ~4.0 but got " + result;
    }

    @Test
    void testCallChain() {
        Function f = new Function(new Var("t"));
        Function f2 = f.sin().cos().tan();
        assert f2 instanceof Function;
    }
}
