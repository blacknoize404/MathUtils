package MathUtils.algebra.primitives;

import MathUtils.algebra.Const;
import MathUtils.algebra.Var;
import MathUtils.algebra.operators.SubOperator;
import MathUtils.algebra.operators.SumOperator;
import MathUtils.algebra.operators.MultOperator;
import MathUtils.algebra.operators.DivOperator;
import org.junit.jupiter.api.Test;

class OperatorTest {

    @Test
    void equalsBetweenConstants() {

        // Cuando son iguales
        SumOperator so = new SumOperator(new Const(5), new Const(5));
        SumOperator so2 = new SumOperator(new Const(5), new Const(5));
        assert so.equalsTo(so2);

        // Cuando el primer valor es distinto
        SumOperator so3 = new SumOperator(new Const(6), new Const(5));
        assert !so.equalsTo(so3);

        // Cuando el segundo valor es distinto
        SumOperator so4 = new SumOperator(new Const(5), new Const(6));
        assert !so.equalsTo(so4);

        // Cuando los dos valores son distintos
        SumOperator so5 = new SumOperator(new Const(6), new Const(6));
        assert !so.equalsTo(so5);

    }

    @Test
    void equalsBetween3OrMoreConstants() {

        // Cuando un operador tiene menos elementos que otro
        SubOperator min = new SubOperator(new Const(5), new Const(5));
        SubOperator so = new SubOperator(new Const(5), new Const(5), new Const(5));
        assert !min.equalsTo(so);

        // Cuando son iguales con 3 elementos
        SubOperator so3 = new SubOperator(new Const(5), new Const(5), new Const(5));
        assert so.equalsTo(so3);

        // Cuando el primer valor es distinto
        SubOperator so4 = new SubOperator(new Const(6), new Const(5), new Const(5));
        assert !so.equalsTo(so4);

        // Cuando el segundo valor es distinto
        SubOperator so5 = new SubOperator(new Const(5), new Const(6), new Const(5));
        assert !so.equalsTo(so5);

        // Cuando el tercer valor es distinto
        SubOperator so6 = new SubOperator(new Const(5), new Const(5), new Const(6));
        assert !so.equalsTo(so6);

        // Cuando el cuarto valor es distinto
        SubOperator so7 = new SubOperator(new Const(5), new Const(5), new Const(5), new Const(5));
        SubOperator so8 = new SubOperator(new Const(5), new Const(5), new Const(5), new Const(6));
        assert !so7.equalsTo(so8);

        // Cuando los dos valores son distintos
        SubOperator so9 = new SubOperator(new Const(6), new Const(6), new Const(6), new Const(6));
        assert !so7.equalsTo(so9);

    }

    @Test
    void equalsBetweenVariables() {

        Var x = new Var("x");

        Var y = new Var("y");

        Var z = new Var("z");

        MultOperator so = new MultOperator(x, x);

        // Cuando son iguales
        MultOperator so2 = new MultOperator(x, x);
        assert so.equalsTo(so2);

        // Cuando el primer valor es distinto
        MultOperator so3 = new MultOperator(y, x);
        assert !so.equalsTo(so3);

        // Cuando el segundo valor es distinto
        MultOperator so4 = new MultOperator(x, y);
        assert !so.equalsTo(so4);

        // Cuando los dos valores son distintos
        MultOperator so5 = new MultOperator(y, z);
        assert !so.equalsTo(so5);

    }

    @Test
    void equalsBetween3OrMoreVariables() {
        Var x = new Var("x");
        Var X = new Var("X");

        Var y = new Var("y");
        Var Y = new Var("Y");

        Var z = new Var("z");
        Var Z = new Var("Z");

        // Cuando un operador tiene menos elementos que otro
        DivOperator min = new DivOperator(x, X);
        DivOperator so = new DivOperator(X, x, X);
        DivOperator so2 = new DivOperator(x, X, x, X);
        assert !min.equalsTo(so);
        assert !min.equalsTo(so2);
        assert !so.equalsTo(so2);

        // Cuando son iguales con 3 elementos
        DivOperator so3 = new DivOperator(X, x, X);
        assert so.equalsTo(so3);

        // Cuando el primer valor es distinto
        DivOperator so4 = new DivOperator(y, x, x);
        assert !so.equalsTo(so4);

        // Cuando el segundo valor es distinto
        DivOperator so5 = new DivOperator(x, z, x);
        assert !so.equalsTo(so5);

        // Cuando el tercer valor es distinto
        DivOperator so6 = new DivOperator(x, x, y);
        assert !so.equalsTo(so6);

        // Cuando el cuarto valor es distinto
        DivOperator so7 = new DivOperator(x, x, x, x);
        DivOperator so8 = new DivOperator(x, x, x, Z);
        assert !so7.equalsTo(so8);

        // Cuando los dos valores son distintos
        DivOperator so9 = new DivOperator(y, Y, z, y);
        assert !so7.equalsTo(so9);

    }

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