package com.barnesinside.mathutils.sequences;

import com.barnesinside.mathutils.algebra.Const;
import com.barnesinside.mathutils.algebra.Evaluation;
import com.barnesinside.mathutils.algebra.Var;
import com.barnesinside.mathutils.algebra.operators.SumOperator;
import com.barnesinside.mathutils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class SequenceTest {

    @Test
    void evalWithEvaluation() throws FunctionEvaluationException {
        Sequence s = new Sequence(new Var("n"));
        double result = s.eval(new Evaluation("n", 5));
        assert result == 5;
    }

    @Test
    void evalWithInt() throws FunctionEvaluationException {
        Sequence s = new Sequence(new SumOperator(new Var("n"), new Const(1)));
        double result = s.eval(5);
        assert result == 6;
    }

    @Test
    void evalConstant() throws FunctionEvaluationException {
        Sequence s = new Sequence(new Const(42));
        double result = s.eval();
        assert result == 42;
    }

    @Test
    void equalsToNull() {
        Sequence s = new Sequence(new Const(5));
        assert !s.equalsTo(null);
    }

    @Test
    void equalsToDifferent() {
        Sequence s1 = new Sequence(new Const(5));
        Sequence s2 = new Sequence(new Const(7));
        assert !s1.equalsTo(s2);
    }
}
