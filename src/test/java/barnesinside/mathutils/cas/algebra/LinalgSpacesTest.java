package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.linalg.Basis;
import barnesinside.mathutils.cas.linalg.ExactMatrix;
import barnesinside.mathutils.cas.linalg.LinearMap;
import barnesinside.mathutils.cas.linalg.Subspace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class LinalgSpacesTest {

    private static List<Expr> v(String... components) {
        List<Expr> out = new ArrayList<>();
        for (String c : components) out.add(ExprParser.parse(c));
        return out;
    }

    @Test
    void theKernelAndTheImageOfAMapObeyTheRankNullityTheorem() {
        // Las filas son dependientes: la tercera es la suma de las dos primeras
        LinearMap map = LinearMap.of(ExactMatrix.parse("1 2 3\n4 5 6\n5 7 9"));
        assert map.rank() == 2 && map.nullity() == 1 && map.rank() + map.nullity() == 3 : map;
        assert !map.injective() && !map.surjective();
        assert map.kernel().size() == 1 && map.kernel().get(0).toString().equals("[1, -2, 1]") : map.kernel();
        // El vector del núcleo cumple A v = 0
        ExactMatrix product = map.matrix().multiply(ExactMatrix.ofColumns(map.kernel()));
        for (int i = 0; i < 3; i++) assert product.get(i, 0).equals(Expr.ZERO) : product;
        // La imagen la generan las columnas con pivote
        assert map.image().size() == 2 && map.image().get(0).toString().equals("[1, 4, 5]") : map.image();
        assert map.summary(Locale.ENGLISH).contains("rank-nullity") && map.summary(Locale.forLanguageTag("es")).contains("teorema de la dimensión");
    }

    @Test
    void anInvertibleMapIsInjectiveAndOnto() {
        LinearMap map = LinearMap.of(ExactMatrix.parse("2 1\n1 1"));
        assert map.injective() && map.surjective() && map.kernel().isEmpty() && map.image().size() == 2 : map;
    }

    @Test
    void aSubspaceHasABasisAndItsCoordinates() {
        // Tres generadores en un plano de ℝ³: el tercero sobra
        Subspace plane = Subspace.spanning(List.of(v("1", "0", "1"), v("0", "1", "1"), v("1", "1", "2")));
        assert plane.dimension() == 2 && plane.basis().size() == 2 : plane;
        assert plane.contains(v("2", "3", "5")) && !plane.contains(v("1", "0", "0")) : "pertenencia";
        assert plane.coordinates(v("2", "3", "5")).toString().equals("[2, 3]") : plane.coordinates(v("2", "3", "5"));
        Subspace same = Subspace.spanning(List.of(v("1", "1", "2"), v("1", "-1", "0")));
        assert plane.sameAs(same) : "los mismos vectores, otra base";
        assert !plane.sameAs(Subspace.spanning(List.of(v("1", "0", "0"))));
    }

    @Test
    void coordinatesAndChangeOfBasis() {
        Basis b = new Basis(List.of(v("1", "1"), v("1", "-1")));
        assert b.coordinates(v("3", "1")).toString().equals("[2, 1]") : b.coordinates(v("3", "1"));
        Basis canonical = new Basis(List.of(v("1", "0"), v("0", "1")));
        // De B a la canónica, la matriz tiene como columnas los vectores de B
        assert b.changeTo(canonical).equals(ExactMatrix.parse("1 1\n1 -1")) : b.changeTo(canonical);
        // Y la de vuelta es su inversa: el producto es la identidad
        ExactMatrix back = canonical.changeTo(b);
        assert b.changeTo(canonical).multiply(back).equals(ExactMatrix.parse("1 0\n0 1")) : back;
    }

    @Test
    void independentVectorsAreCompletedToABasis() {
        Basis basis = Basis.complete(List.of(v("1", "1", "0")));
        assert basis.dimension() == 3 && basis.vectors().get(0).toString().equals("[1, 1, 0]") : basis;
        try {
            Basis.complete(List.of(v("1", "2"), v("2", "4")));
            assert false : "dependientes";
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().contains("dependientes");
        }
        try {
            new Basis(List.of(v("1", "0", "0"), v("0", "1", "0")));
            assert false : "faltan vectores";
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().contains("base");
        }
    }
}
