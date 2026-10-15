package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.linalg.ExactMatrix;
import barnesinside.mathutils.cas.linalg.Independence;
import barnesinside.mathutils.cas.linalg.RowReduction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

class LinalgTest {

    private static List<Expr> vector(String... components) {
        List<Expr> v = new ArrayList<>();
        for (String c : components) v.add(ExprParser.parse(c));
        return v;
    }

    @Test
    void aMatrixIsReducedToItsReducedEchelonFormStepByStep() {
        RowReduction.Result r = RowReduction.reduce(ExactMatrix.parse("2 4 -2\n1 3 1\n3 7 -1"));
        assert r.reduced().equals(ExactMatrix.parse("1 0 -5\n0 1 2\n0 0 0")) : r.reduced();
        assert r.rank() == 2 && r.pivots().equals(List.of(0, 1)) : r.pivots();
        assert r.steps().getFirst().op() == RowReduction.Op.SCALE : "se empieza dividiendo la primera fila por su pivote";
        assert r.steps().getLast().after().equals(r.reduced());
        assert r.steps().getFirst().describe(Locale.forLanguageTag("es")).contains("Dividimos la fila 1 entre 2");
    }

    @Test
    void aZeroPivotRowIsSwapped() {
        RowReduction.Result r = RowReduction.reduce(ExactMatrix.parse("0 1\n1 0"));
        assert r.steps().getFirst().op() == RowReduction.Op.SWAP && r.reduced().equals(ExactMatrix.parse("1 0\n0 1")) : r.steps();
        assert r.steps().getFirst().describe(Locale.ENGLISH).equals("Swap rows 1 and 2.");
    }

    @Test
    void fractionsStayExact() {
        RowReduction.Result r = RowReduction.reduce(ExactMatrix.parse("1/2 1/3\n1/4 1/5"));
        assert r.reduced().equals(ExactMatrix.parse("1 0\n0 1")) && r.rank() == 2 : r.reduced();
    }

    @Test
    void independentAndDependentVectors() {
        Independence.Result free = Independence.of(List.of(vector("1", "0", "0"), vector("1", "1", "0"), vector("1", "1", "1")));
        assert free.independent() && free.combination().isEmpty() : free;
        assert free.conclusion(Locale.ENGLISH).contains("linearly independent");

        Independence.Result tied = Independence.of(List.of(vector("1", "2", "3"), vector("4", "5", "6"), vector("7", "8", "9")));
        assert !tied.independent() : tied;
        // La combinación da el vector cero: 1·v1 − 2·v2 + 1·v3 = 0
        List<Expr> c = tied.combination();
        assert c.toString().equals("[1, -2, 1]") : c;
        assert tied.conclusion(Locale.forLanguageTag("es")).contains("linealmente dependientes");
    }

    @Test
    void everyMessageExistsInBothLanguages() {
        ResourceBundle es = ResourceBundle.getBundle("barnesinside/mathutils/cas/linalg/messages", Locale.forLanguageTag("es"));
        ResourceBundle en = ResourceBundle.getBundle("barnesinside/mathutils/cas/linalg/messages", Locale.ENGLISH);
        assert es.keySet().equals(en.keySet()) : es.keySet() + " frente a " + en.keySet();
        for (String key : en.keySet()) assert !en.getString(key).matches(".*[áéíóúñ].*") : key;
    }

    @Test
    void badShapesAreRefused() {
        try {
            ExactMatrix.parse("1 2\n3");
            assert false;
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().contains("columnas");
        }
        try {
            ExactMatrix.parse("1 2\n3 4").multiply(ExactMatrix.parse("1 2 3"));
            assert false;
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().contains("dimensiones");
        }
    }
}
