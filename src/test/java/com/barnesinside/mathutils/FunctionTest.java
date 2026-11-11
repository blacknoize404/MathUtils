package com.barnesinside.mathutils;

import com.barnesinside.mathutils.algebra.Const;
import com.barnesinside.mathutils.algebra.Evaluation;
import com.barnesinside.mathutils.algebra.Function;
import com.barnesinside.mathutils.algebra.Var;
import com.barnesinside.mathutils.exceptions.FunctionEvaluationException;

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
