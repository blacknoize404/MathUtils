package MathUtils;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Function;
import MathUtils.algebra.Var;
import MathUtils.exceptions.FunctionEvaluationException;

public class FunctionTest {

    public static void main(String[] args) throws FunctionEvaluationException {
        chainProcess();
        chainProcessWithEvaluation();
    }

    public static void chainProcess() throws FunctionEvaluationException {

        Function f = new Function(2);
        f = f.sum(f).sum(f).sum(f);

        System.out.println(f.eval());

    }

    public static void chainProcessWithEvaluation() throws FunctionEvaluationException {
        Function f = new Function(0)
                .mult(new Var("x"))
                .sum(new Const(2))
                .pow(new Const(2));

        System.out.println(f.eval(new Evaluation("x", 3)));
    }

}
