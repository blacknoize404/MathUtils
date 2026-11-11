package com.barnesinside.mathutils.cas.linalg;

import com.barnesinside.mathutils.cas.expr.Expr;
import com.barnesinside.mathutils.cas.expr.ExprParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class LinalgOrthogonalTest {

    private static List<Expr> vector(String... entries) {
        List<Expr> out = new ArrayList<>();
        for (String e : entries) out.add(ExprParser.parse(e));
        return out;
    }

    @Test
    void gramSchmidtGivesOrthogonalThenOrthonormalVectorsOfTheSameSpace() {
        List<List<Expr>> input = List.of(vector("1", "1", "0"), vector("1", "0", "1"), vector("0", "1", "1"));
        GramSchmidt.Result result = GramSchmidt.of(input);
        List<List<Expr>> u = result.orthogonal();
        for (int i = 0; i < u.size(); i++) {
            for (int j = 0; j < i; j++) assert ExactMatrix.isZero(GramSchmidt.dot(u.get(i), u.get(j))) : "u" + (i + 1) + " y u" + (j + 1) + " no son ortogonales";
        }
        // Al segundo vector se le resta la mitad del primero: el coeficiente es 1/2
        assert result.steps().get(1).coefficients().size() == 1 && result.steps().get(1).coefficients().get(0).equals(ExprParser.parse("1/2")) : result.steps().get(1);
        for (List<Expr> q : result.orthonormal()) assert ExactMatrix.isZero(Expr.sub(GramSchmidt.dot(q, q), Expr.ONE)) : "unitario: " + q;
        // El mismo subespacio
        assert Subspace.spanning(input).sameAs(Subspace.spanning(result.orthonormal()));
        assert result.steps().get(0).describe(Locale.of("es")).contains("q1") && result.steps().get(2).describe(Locale.ENGLISH).contains("projections");
    }

    @Test
    void dependentVectorsAreRejectedWithTheReason() {
        try {
            GramSchmidt.of(List.of(vector("1", "2"), vector("2", "4")));
            assert false : "dos vectores paralelos no se ortogonalizan";
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().contains("dependientes") : expected.getMessage();
        }
    }

    @Test
    void qrFactorsTheMatrixIntoOrthonormalTimesUpperTriangular() {
        ExactMatrix a = ExactMatrix.parse("1 1; 1 0; 0 1");
        QR qr = QR.of(a);
        assert qr.verifies(a) : "Q·R = A: " + qr.q() + " por " + qr.r();
        assert qr.orthonormal() : "QᵀQ = I";
        assert ExactMatrix.isZero(qr.r().get(1, 0)) && qr.r().rows() == 2 && qr.r().cols() == 2 : "R triangular superior 2×2: " + qr.r();
        assert qr.summary(Locale.ENGLISH).contains("upper triangular");
        try {
            QR.of(ExactMatrix.parse("1 2 3; 4 5 6"));
            assert false : "más columnas que filas";
        } catch (IllegalArgumentException expected) {
            // Bien: hace falta una matriz alta
        }
    }

    @Test
    void aDiagonalizableMatrixGivesPDAndTheInverseOfPWithItsSteps() {
        ExactMatrix a = ExactMatrix.parse("4 1; 2 3");
        Diagonalization diag = Diagonalization.of(a);
        assert diag.diagonalizable() : diag;
        assert diag.verifies(a) : "P·D·P⁻¹ = A";
        assert diag.p().multiply(diag.inverse()).sameAs(ExactMatrix.identity(2));
        assert !diag.inversion().steps().isEmpty() : "los pasos de la inversa de P";
        assert diag.conclusion(Locale.ENGLISH).contains("diagonalizable");
        // Un autovalor repetido con tantos autovectores como su multiplicidad
        ExactMatrix scaled = ExactMatrix.parse("2 0; 0 2");
        assert Diagonalization.of(scaled).verifies(scaled);
    }

    @Test
    void theMatricesThatCannotBeDiagonalizedSayWhy() {
        Diagonalization defective = Diagonalization.of(ExactMatrix.parse("1 1; 0 1"));
        assert !defective.diagonalizable() && defective.reason().equals("linalg.diag.defective") : defective.reason();
        assert defective.conclusion(Locale.of("es")).contains("multiplicidad");
        Diagonalization rotation = Diagonalization.of(ExactMatrix.parse("0 -1; 1 0"));
        assert !rotation.diagonalizable() && rotation.reason().equals("linalg.diag.noReal") : rotation.reason();
    }

    @Test
    void theInverseOfAMatrixIsFoundOnlyIfItExists() {
        ExactMatrix a = ExactMatrix.parse("2 1; 5 3");
        assert a.multiply(a.inverse()).sameAs(ExactMatrix.identity(2));
        assert ExactMatrix.parse("1 2; 2 4").inverse() == null : "una matriz singular no tiene inversa";
        assert ExactMatrix.parse("1 2 3; 4 5 6").inverse() == null : "ni una que no es cuadrada";
    }
}
