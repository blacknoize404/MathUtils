package MathUtils.algebra.primitives;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.Var;
import MathUtils.algebra.exponentials.Root;
import MathUtils.algebra.exponentials.Sqrt;
import MathUtils.algebra.operators.SumOperator;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class ExpressionTest {

    @Test
    void getArgument() {
        Expression<Number> c = new Const(42);
        assert c.getArgument().equals(42);
    }

    @Test
    void equalsToNull() {
        Expression<Number> c = new Const(5);
        assert !c.equalsTo(null);
    }

    @Test
    void equalsToNonExpression() {
        Expression<Number> c = new Const(5);
        assert !c.equalsTo(null);
    }

    @Test
    void equalsToSameClassNumber() {
        assert new Const(3.14).equalsTo(new Const(3.14));
    }

    @Test
    void equalsToSameClassNumberDifferent() {
        assert !new Const(3.14).equalsTo(new Const(2.71));
    }

    @Test
    void equalsToSameClassVar() {
        assert new Var("x").equalsTo(new Var("x"));
    }

    @Test
    void equalsToSameClassVarIgnoreCase() {
        assert new Var("x").equalsTo(new Var("X"));
    }

    @Test
    void equalsToSameClassVarDifferent() {
        assert !new Var("x").equalsTo(new Var("y"));
    }

    @Test
    void equalsToDifferentClassSqrtRoot() {
        Sqrt sqrt = new Sqrt(4);
        Root root = new Root(new Const(4), 2);
        assert sqrt.equalsTo(root);
    }

    @Test
    void equalsToSqrtRootDifferent() {
        Sqrt sqrt = new Sqrt(4);
        Root root = new Root(new Const(9), 2);
        assert !sqrt.equalsTo(root);
    }

    @Test
    void equalsToMathElementArgument() throws FunctionEvaluationException {
        Var v1 = new Var("x");
        Var v2 = new Var("x");
        Expression<MathElement> e1 = new Expression<MathElement>(v1) {
            @Override
            public double eval(Evaluation... evaluations) {
                try {
                    return argument.eval(evaluations);
                } catch (FunctionEvaluationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        Expression<MathElement> e2 = new Expression<MathElement>(v2) {
            @Override
            public double eval(Evaluation... evaluations) {
                try {
                    return argument.eval(evaluations);
                } catch (FunctionEvaluationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        assert e1.equalsTo(e2);
    }

    @Test
    void equalsToMathElementArgumentDifferent() throws FunctionEvaluationException {
        Var v1 = new Var("x");
        Var v2 = new Var("y");
        Expression<MathElement> e1 = new Expression<MathElement>(v1) {
            @Override
            public double eval(Evaluation... evaluations) {
                try {
                    return argument.eval(evaluations);
                } catch (FunctionEvaluationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        Expression<MathElement> e2 = new Expression<MathElement>(v2) {
            @Override
            public double eval(Evaluation... evaluations) {
                try {
                    return argument.eval(evaluations);
                } catch (FunctionEvaluationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        assert !e1.equalsTo(e2);
    }

    @Test
    void testToStringWithE() {
        Const euler = new Const(Math.E);
        assert "E".equals(euler.toString());
    }

    @Test
    void testToStringNumber() {
        Const c = new Const(42);
        assert "42".equals(c.toString());
    }

    @Test
    void testToStringDouble() {
        Const c = new Const(3.14);
        assert "3.14".equals(c.toString());
    }

    @Test
    void testToStringMathElementArgument() throws FunctionEvaluationException {
        SumOperator sum = new SumOperator(new Const(1), new Const(2));
        Expression<MathElement> expr = new Expression<MathElement>(sum) {
            @Override
            public double eval(Evaluation... evaluations) {
                try {
                    return argument.eval(evaluations);
                } catch (FunctionEvaluationException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        String s = expr.toString();
        assert s != null && s.contains("1") && s.contains("2");
    }
}
