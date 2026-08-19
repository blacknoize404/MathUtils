package barnesinside.mathutils;

import barnesinside.mathutils.algebra.operators.SubOperator;
import barnesinside.mathutils.exceptions.FunctionEvaluationException;
import barnesinside.mathutils.algebra.Const;
import barnesinside.mathutils.algebra.exponentials.Cbrt;
import barnesinside.mathutils.algebra.exponentials.Pow;
import barnesinside.mathutils.algebra.exponentials.Root;
import barnesinside.mathutils.algebra.exponentials.Sqrt;
import barnesinside.mathutils.algebra.trigonometrics.Sin;
import barnesinside.mathutils.algebra.operators.DivOperator;
import barnesinside.mathutils.algebra.operators.MultOperator;
import barnesinside.mathutils.algebra.operators.SumOperator;

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
