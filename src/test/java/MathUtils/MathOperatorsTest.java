package MathUtils;

import MathUtils.algebra.operators.SubOperator;
import MathUtils.exceptions.FunctionEvaluationException;
import MathUtils.algebra.Const;
import MathUtils.algebra.exponentials.Cbrt;
import MathUtils.algebra.exponentials.Pow;
import MathUtils.algebra.exponentials.Root;
import MathUtils.algebra.exponentials.Sqrt;
import MathUtils.algebra.trigonometrics.Sin;
import MathUtils.algebra.operators.DivOperator;
import MathUtils.algebra.operators.MultOperator;
import MathUtils.algebra.operators.SumOperator;

public class MathOperatorsTest {

    public static void main(String[] args) throws FunctionEvaluationException {
        System.out.println(
                new Const(5).eval()
        );
        System.out.println(
                new SumOperator(
                        new Pow(
                                new Const(3),
                                new Const(2)
                        ),
                        new Const(6),
                        new Root(
                                new Const(64),
                                new Const(3)
                        )
                ).eval());
        System.out.println(
                new SubOperator(
                        new Pow(
                                new Const(3),
                                new Const(2)
                        ),
                        new Const(6)
                ).eval()
        );
        System.out.println(
                new MultOperator(
                        new Pow(
                                new Const(3),
                                new Const(2)
                        ),
                        new Const(6)
                ).eval()
        );
        System.out.println(
                new DivOperator(
                        new Pow(
                                new Const(3),
                                new Const(2)
                        ),
                        new Const(6)
                ).eval()
        );

        System.out.println(
                new Cbrt(
                        new Const(27)
                ).eval()
        );

        System.out.println(
                new Sqrt(
                        new Const(36)
                ).eval()
        );

        System.out.println(
                new Sin(new Const(Math.PI/2.)).eval()
        );

    }
}
