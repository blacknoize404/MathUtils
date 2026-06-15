package MathUtils.statistics;

import org.junit.jupiter.api.Test;

class WeightedMeanTest {

    @Test
    void constructorArrays() {
        double[] weights = {1, 1, 1, 1, 1, 5};
        double[] frequencies = {6, 7, 5, 7, 8, 6};
        WeightedMean wm = new WeightedMean(weights, frequencies);
        double result = wm.eval();
        assert Math.abs(result - 6.3) < 1e-10 : "Expected 6.3 but got " + result;
    }

    @Test
    void constructorArraysDifferentLength() {
        double[] weights = {1, 2, 3};
        double[] frequencies = {1, 2};
        try {
            new WeightedMean(weights, frequencies);
            assert false : "Should have thrown IllegalArgumentException";
        } catch (IllegalArgumentException e) {
            assert true;
        }
    }

    @Test
    void evalEqualWeights() {
        double[] weights = {1, 1, 1};
        double[] frequencies = {10, 20, 30};
        WeightedMean wm = new WeightedMean(weights, frequencies);
        double result = wm.eval();
        assert result == 20.0;
    }

    @Test
    void evalWithZeroWeights() {
        double[] weights = {0, 1, 0};
        double[] frequencies = {10, 20, 30};
        WeightedMean wm = new WeightedMean(weights, frequencies);
        double result = wm.eval();
        assert result == 20.0;
    }

    @Test
    void evalSingleElement() {
        double[] weights = {5};
        double[] frequencies = {10};
        WeightedMean wm = new WeightedMean(weights, frequencies);
        double result = wm.eval();
        assert result == 10.0;
    }

    @Test
    void evalNegativeValues() {
        double[] weights = {1, 1};
        double[] frequencies = {-5, 5};
        WeightedMean wm = new WeightedMean(weights, frequencies);
        double result = wm.eval();
        assert Math.abs(result) < 1e-10;
    }
}
