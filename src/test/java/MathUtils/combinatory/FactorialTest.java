package MathUtils.combinatory;

import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class FactorialTest {

    @Test
    void factorialOfZero() throws FunctionEvaluationException {
        Factorial f = new Factorial(0);
        assert f.eval() == 1;
    }

    @Test
    void factorialOfOne() throws FunctionEvaluationException {
        Factorial f = new Factorial(1);
        assert f.eval() == 1;
    }

    @Test
    void factorialOfFive() throws FunctionEvaluationException {
        Factorial f = new Factorial(5);
        assert f.eval() == 120;
    }

    @Test
    void factorialOfTen() throws FunctionEvaluationException {
        Factorial f = new Factorial(10);
        assert f.eval() == 3628800;
    }

    @Test
    void ofStaticMethod() throws FunctionEvaluationException {
        Factorial f = Factorial.of(5);
        assert f.eval() == 120;
    }

    @Test
    void factorialOfThree() throws FunctionEvaluationException {
        Factorial f = new Factorial(3);
        assert f.eval() == 6;
    }

    @Test
    void equalsToSame() {
        Factorial f1 = new Factorial(5);
        Factorial f2 = new Factorial(5);
        assert f1.equalsTo(f2);
    }

    @Test
    void equalsToDifferent() {
        Factorial f1 = new Factorial(5);
        Factorial f2 = new Factorial(6);
        assert !f1.equalsTo(f2);
    }
}
