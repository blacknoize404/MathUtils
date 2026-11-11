package com.barnesinside.mathutils.algebra.trigonometrics;

import com.barnesinside.mathutils.algebra.Const;
import com.barnesinside.mathutils.algebra.Evaluation;
import com.barnesinside.mathutils.algebra.Var;
import com.barnesinside.mathutils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class CosTest {

    @Test
    void evalZero() throws FunctionEvaluationException {
        Cos c = new Cos(new Const(0));
        assert c.eval() == 1.0;
    }

    @Test
    void evalPi() throws FunctionEvaluationException {
        Cos c = new Cos(new Const(Math.PI));
        double result = c.eval();
        assert Math.abs(result + 1.0) < 1e-10 : "Expected ~-1.0 but got " + result;
    }

    @Test
    void evalPiOverTwo() throws FunctionEvaluationException {
        Cos c = new Cos(new Const(Math.PI / 2));
        double result = c.eval();
        assert Math.abs(result) < 1e-10 : "Expected ~0 but got " + result;
    }

    @Test
    void evalWithVariable() throws FunctionEvaluationException {
        Cos c = new Cos(new Var("x"));
        double result = c.eval(new Evaluation("x", 0));
        assert result == 1.0;
    }

    @Test
    void equalsToSame() {
        Cos c1 = new Cos(new Const(0));
        Cos c2 = new Cos(new Const(0));
        assert c1.equalsTo(c2);
    }

    @Test
    void equalsToDifferent() {
        Cos c1 = new Cos(new Const(0));
        Cos c2 = new Cos(new Const(1));
        assert !c1.equalsTo(c2);
    }
}
