package MathUtils.algebra;

import MathUtils.algebra.exponentials.Cbrt;
import MathUtils.algebra.exponentials.Root;
import MathUtils.algebra.exponentials.Sqrt;
import org.junit.jupiter.api.Test;

class FunctionTest {

    Var x = new Var("x");
    Const a = new Const(5);

    @Test
    void equalsToHimself() {

        Function f = new Function(x);

        assert f.equalsTo(f);

    }

    @Test
    void equalsToOtherFunction() {

        Function f = new Function(x);
        Function g = new Function(x);

        assert f.equalsTo(g);

    }

    @Test
    void equalsToOtherFunctionWithDiferentArgumentInstance() {

        Var x2 = new Var("X");

        Function f = new Function(x);
        Function g = new Function(x2);

        assert f.equalsTo(g);

    }

    @Test
    void equalsToOtherFunctionWithSqrtAndRoot2() {

        Sqrt sqrt = new Sqrt(x);
        Root root2 = new Root(x, 2);
        Root root3 = new Root(x, 3);

        Function f = new Function(sqrt);
        Function g = new Function(root2);
        Function h = new Function(root3);

        // Caso en que es igual
        assert f.equalsTo(g);

        // Caso en que es distinto
        assert !f.equalsTo(h);

    }

    @Test
    void equalsToOtherFunctionWithCbrtAndRoot3() {

        Cbrt cbrt = new Cbrt(x);
        Root root3 = new Root(x, 3);
        Root root2 = new Root(x, 2);

        Function f = new Function(cbrt);
        Function g = new Function(root3);
        Function h = new Function(root2);

        // Caso en que es igual
        assert f.equalsTo(g);

        // Caso en que es distinto
        assert !f.equalsTo(h);

    }

    @Test
    void notEqualsToOtherFunctionWithTheSameVariableButDifferentOperators() {

        Function f = new Function(x);
        Function g = new Function(new Var("X"));
        Function h = new Function(x).sin();
        Function i = new Function(new Var("x")).sin();

        // Caso en que es igual
        assert f.equalsTo(g);

        // Caso en que es distinto
        assert !f.equalsTo(h);

        // Caso en que están encadenados pero son iguales
        assert h.equalsTo(i);

    }



}