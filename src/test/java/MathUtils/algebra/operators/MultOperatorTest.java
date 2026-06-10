package MathUtils.algebra.operators;

import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Function;
import MathUtils.algebra.Var;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class MultOperatorTest {

    @Test
    void fractionalBase() throws FunctionEvaluationException {

        // f(t) = 1/8 * t
        Function f = new Function(
                new MultOperator(
                        1/8d,
                        new Var("t")
                )
        );

        // f(8) = 1
        assert f.eval(new Evaluation("t", 8)) == 1;

    }

}