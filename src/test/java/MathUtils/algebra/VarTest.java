package MathUtils.algebra;
import MathUtils.algebra.exponentials.Pow;
import MathUtils.algebra.exponentials.Sqrt;
import MathUtils.algebra.logarithmics.LogN;
import MathUtils.algebra.logarithmics.Log;
import MathUtils.algebra.operators.MultOperator;
import MathUtils.algebra.operators.SubOperator;
import MathUtils.algebra.operators.SumOperator;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;


class VarTest {

    Var x = new Var("x");

    @Test
    void equal() {

        Var x2 = new Var("x");

        assert x.equalsTo(x2);

    }

    @Test
    void equalIgnoreCase() {

        Var x2 = new Var("X");

        assert x.equalsTo(x2);

    }

    @Test
    void notEqual() {

        Var y = new Var("y");
        Var y2 = new Var("Y");

        assert !x.equalsTo(y);
        assert !x.equalsTo(y2);

    }

    // Tarda 20ms para terminar no idea why
    @Test
    void log() {
        assert Math.log(1) / Math.log(2) == 0;
    }

    @Test
    void logEvaluation() throws FunctionEvaluationException {

        // f(t) = log_2(t)
        Function f = new Function(
                new Log(
                        new Var("t"),
                        2
                )
        );

        // t = 1
        Evaluation xVal = new Evaluation("t", 1);

        // f(1) = 0;
        assert f.eval(xVal) == 0;

    }

    @Test
    void powToPowEvaluation() throws FunctionEvaluationException {

        // f(x,y,z) = x^y^z
        Function f = new Function(
                new Pow(
                        new Pow(
                                new Var("x"),
                                new Var("y")
                        ),
                        new Var("z")
                )
        );

        // x = 2, y = 2, z = 3
        Evaluation xVal = new Evaluation("x", 2);
        Evaluation yVal = new Evaluation("y", 2);
        Evaluation zVal = new Evaluation("z", 3);

        // f(2, 2, 3) = 64;
        assert f.eval(xVal, yVal, zVal) == 64;

    }

    @Test
    void varPlusVar() throws FunctionEvaluationException {

        // f(t) = t + t
        Function f = new Function(
                new SumOperator(
                        new Var("t"),
                        new Var("t")
                )
        );

        // t = 2
        Evaluation tVal = new Evaluation("t", 2);

        // f(2) = 4
        assert f.eval(tVal) == 4;

    }

    @Test
    void fullEquationTree() throws FunctionEvaluationException {

        // Variable t
        Var t = new Var("t");

        // f(t) = (Sqrt(2t + 5t) + 45) * Ln(1/8 * t)
        Function f = new Function(
                new MultOperator(
                        new SumOperator(
                                new Sqrt(
                                        new SumOperator(
                                                new MultOperator(2, t),
                                                new MultOperator(5, t)
                                        )
                                ),
                                45
                        ),
                        new LogN(
                                new MultOperator(1/8d, t)
                        )
                )
        );

        // t = 8
        Evaluation tVal = new Evaluation("t", 8);

        // f(8) = 0
        assert f.eval(tVal) == 0;

    }

    @Test
    void fullEquationTree2() throws FunctionEvaluationException {

        // Variable x
        Var x = new Var("x");

        // f(x) = log_10(100) * x^3 * 1/8 - 2
        Function f = new Function(
                new SubOperator(
                        new MultOperator(
                                new Log(100, 10),
                                new Pow(x, 3),
                                new Fraction(1, 8)
                        ),
                        new Const(2)
                )
        );

        // x = 2
        Evaluation tVal = new Evaluation("x", 2);

        // f(2) = 0
        assert f.eval(tVal) == 0;

    }

}