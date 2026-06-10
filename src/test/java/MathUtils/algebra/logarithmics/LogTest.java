package MathUtils.algebra.logarithmics;

import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Function;
import MathUtils.algebra.Var;
import MathUtils.algebra.operators.MultOperator;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class LogTest {

    @Test
    void fractionalBase() throws FunctionEvaluationException {

        // f(t) = Ln(1/8 * t)
        Function f = new Function(
                new LogN(
                        new MultOperator(
                                1/8d,
                                new Var("t")
                        )
                )
        );

        assert f.eval(new Evaluation("t", 8)) == 0;

    }

}