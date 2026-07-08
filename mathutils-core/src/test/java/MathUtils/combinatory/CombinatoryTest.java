package MathUtils.combinatory;

import MathUtils.algebra.primitives.MathElement;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class CombinatoryTest {

    @Test
    void permutationsWithoutRepetition() throws FunctionEvaluationException {
        MathElement result = Combinatory.getPermutationsWithoutRepetitionOf(5);
        assert result.eval() == 120;
    }

    @Test
    void variationsWithoutRepetition() throws FunctionEvaluationException {
        MathElement result = Combinatory.getVariationsWithoutRepetitionOf(10, 3);
        double val = result.eval();
        assert val == 720 : "Expected 720 but got " + val;
    }

    @Test
    void variationsWithRepetition() throws FunctionEvaluationException {
        MathElement result = Combinatory.getVariationsWithRepetitionOf(2, 3);
        double val = result.eval();
        assert val == 8 : "Expected 8 but got " + val;
    }

    @Test
    void combinations() throws FunctionEvaluationException {
        MathElement result = Combinatory.getCombinationsOf(10, 3);
        double val = result.eval();
        assert val == 120 : "Expected 120 but got " + val;
    }

    @Test
    void combinationsOfFiveChooseTwo() throws FunctionEvaluationException {
        MathElement result = Combinatory.getCombinationsOf(5, 2);
        double val = result.eval();
        assert val == 10 : "Expected 10 but got " + val;
    }

    @Test
    void variationsWithoutRepetitionSameSize() throws FunctionEvaluationException {
        MathElement result = Combinatory.getVariationsWithoutRepetitionOf(5, 5);
        double val = result.eval();
        assert val == 120 : "Expected 120 but got " + val;
    }

    @Test
    void combinationsSymmetry() throws FunctionEvaluationException {
        MathElement c1 = Combinatory.getCombinationsOf(10, 3);
        MathElement c2 = Combinatory.getCombinationsOf(10, 7);
        assert c1.eval() == c2.eval() : "C(10,3) should equal C(10,7)";
    }
}
