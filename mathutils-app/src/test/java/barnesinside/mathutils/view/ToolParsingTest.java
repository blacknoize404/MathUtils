package barnesinside.mathutils.view;

import MathUtils.complex.Complex;
import MathUtils.matrix.Matrix;
import barnesinside.mathutils.ui.Numbers;
import org.junit.jupiter.api.Test;

class ToolParsingTest {

    @Test
    void matricesAcceptFractionsCommasAndSemicolons() {
        Matrix m = MatrixView.parse("1/2, 3\n-1  sqrt(4)");
        assert m.rows() == 2 && m.cols() == 2;
        assert m.get(0, 0) == 0.5 && m.get(1, 1) == 2 && m.get(1, 0) == -1;
        assert MatrixView.parse("1 2; 3 4").get(1, 0) == 3;
    }

    @Test
    void invalidMatricesExplainTheProblem() {
        for (String bad : new String[]{"", "1 2\n3", "1 a", "1 2\n3 oo"}) {
            try {
                MatrixView.parse(bad);
                assert false : "debía fallar: " + bad;
            } catch (IllegalArgumentException e) {
                assert !e.getMessage().isBlank();
            }
        }
    }

    @Test
    void complexNumbersAreFormattedReadably() {
        assert ComplexView.format(Complex.of(3, 4)).equals("3 + 4i");
        assert ComplexView.format(Complex.of(3, -1)).equals("3 − i");
        assert ComplexView.format(Complex.of(0, -2)).equals("−2i");
        assert ComplexView.format(Complex.of(5, 1e-15)).equals("5");
        assert ComplexView.format(Complex.of(0.5, 0.25)).equals("1/2 + 1/4i");
    }

    @Test
    void numbersShowExactFractionsWhenTheyExist() {
        assert Numbers.fraction(1.0 / 3).equals("1/3");
        assert Numbers.fraction(-0.75).equals("−3/4");
        assert Numbers.fraction(Math.PI) == null;
        assert Numbers.decimal(2.0).equals("2");
        assert Numbers.decimal(0.1 + 0.2).equals("0.3");
        assert Numbers.both(0.5).equals("1/2 ≈ 0.5");
    }
}
