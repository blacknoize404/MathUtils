package MathUtils.algebra.primitives;

import MathUtils.algebra.Const;
import MathUtils.algebra.Var;
import MathUtils.algebra.operators.MultOperator;
import MathUtils.algebra.operators.SubOperator;
import MathUtils.algebra.operators.SumOperator;
import org.junit.jupiter.api.Test;

class ExpressionTest {

    @Test
    void equalsBetweenMathElements() {

        MathElement constant = new Const(2);
        MathElement constant2 = new Const(3);
        MathElement constant3 = new Const(3);

        MathElement variable = new Var("x");
        MathElement variable2 = new Var("X");
        MathElement variable3 = new Var("y");

        MathElement sumOperator = new SumOperator(constant, variable);

        // Cuando son iguales
        MathElement sumOperator2 = new SumOperator(constant, variable2);
        assert sumOperator.equalsTo(sumOperator2);
//
        // Cuando son iguales los componentes pero no el operador
        MathElement subOperator = new SubOperator(constant, variable2);
        assert !sumOperator.equalsTo(subOperator);

        // Cuando el primer valor es distinto
        SumOperator sumOperator3 = new SumOperator(constant2, variable2);
        assert !sumOperator.equalsTo(sumOperator3);

        // Cuando el segundo valor es distinto
        MultOperator sumOperator4 = new MultOperator(constant2, variable3);
        assert !sumOperator.equalsTo(sumOperator4);

        // Cuando los dos valores son distintos
        SumOperator sumOperator5 = new SumOperator(constant3, variable3);
        assert !sumOperator.equalsTo(sumOperator5);

    }
}