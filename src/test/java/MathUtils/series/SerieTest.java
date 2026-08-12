package MathUtils.series;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Var;
import MathUtils.algebra.operators.MultOperator;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class SerieTest {

    @Test
    void evalConstant() throws FunctionEvaluationException {
        Serie s = new Serie(5, "k", 1, 3);
        double result = s.eval();
        assert result == 15 : "Expected 15 but got " + result;
    }

    @Test
    void evalWithVariable() throws FunctionEvaluationException {
        Serie s = new Serie(new Var("k"), "k", 1, 5);
        double result = s.eval();
        assert result == 15 : "Expected 15 but got " + result;
    }

    @Test
    void evalFromZero() throws FunctionEvaluationException {
        Serie s = new Serie(new Var("k"), "k", 0, 4);
        double result = s.eval();
        assert result == 10 : "Expected 10 but got " + result;
    }

    @Test
    void evalEmpty() throws FunctionEvaluationException {
        Serie s = new Serie(1, "k", 5, 3);
        double result = s.eval();
        assert result == 0 : "Expected 0 but got " + result;
    }

    @Test
    void evalCustomExpression() throws FunctionEvaluationException {
        Serie s = new Serie(new MultOperator(2, new Var("k")), "k", 1, 3);
        double result = s.eval();
        assert result == 12 : "Expected 12 but got " + result;
    }

    @Test
    void evalWithExternalEvaluation() throws FunctionEvaluationException {
        Serie s = new Serie(new Var("x"), "x", 1, 3);
        double result = s.eval();
        assert result == 6 : "Expected 6 but got " + result;
    }

    @Test
    void equalsToNull() {
        Serie s = new Serie(1, "k", 1, 3);
        assert !s.equalsTo(null);
    }

    @Test
    void equalsToNonSerie() {
        Serie s = new Serie(1, "k", 1, 3);
        assert !s.equalsTo(new Const(5));
    }

    @Test
    void equalsToSame() {
        Serie s1 = new Serie(5, "k", 1, 3);
        Serie s2 = new Serie(5, "k", 1, 3);
        assert s1.equalsTo(s2);
    }

    @Test
    void equalsToDifferentContent() {
        Serie s1 = new Serie(5, "k", 1, 3);
        Serie s2 = new Serie(10, "k", 1, 3);
        assert !s1.equalsTo(s2);
    }
}
