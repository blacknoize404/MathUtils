package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Interval;
import barnesinside.mathutils.cas.expr.Op;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.expr.Relation;
import barnesinside.mathutils.cas.expr.SolutionSet;
import barnesinside.mathutils.cas.poly.Polynomial;
import barnesinside.mathutils.cas.steps.Rule;

import java.util.List;
import java.util.Optional;

/**
 * Ecuaciones e inecuaciones de primer grado: se pasan los términos con la incógnita a la izquierda y los números
 * a la derecha, y se divide entre el coeficiente (cambiando el sentido de la desigualdad si es negativo).
 */
final class Linear {

    private Linear() {
    }

    /** Si los dos lados son polinomios de grado como mucho 1 en la variable y alguno es de grado 1. */
    static boolean applies(Relation r, String v) {
        Optional<Polynomial> left = Polynomial.from(r.left(), v);
        Optional<Polynomial> right = Polynomial.from(r.right(), v);
        return left.isPresent() && right.isPresent() && left.get().degree() <= 1 && right.get().degree() <= 1
                && Math.max(left.get().degree(), right.get().degree()) == 1;
    }

    /** El conjunto de soluciones de {@code x ⋈ c}. */
    static SolutionSet setFor(Op op, Expr c) {
        return switch (op) {
            case EQ -> SolutionSet.finite(List.of(c));
            case NE -> SolutionSet.intervals(List.of(Interval.below(c, false), Interval.above(c, false)));
            case LT -> SolutionSet.intervals(List.of(Interval.below(c, false)));
            case LE -> SolutionSet.intervals(List.of(Interval.below(c, true)));
            case GT -> SolutionSet.intervals(List.of(Interval.above(c, false)));
            case GE -> SolutionSet.intervals(List.of(Interval.above(c, true)));
        };
    }

    /** Resuelve la relación (que debe cumplir {@link #applies}) registrando los pasos. */
    static SolutionSet solve(Rec rec, Relation r, String v) {
        Expr x = Expr.sym(v);
        Polynomial pl = Polynomial.from(r.left(), v).orElseThrow();
        Polynomial pr = Polynomial.from(r.right(), v).orElseThrow();
        Op op = r.op();

        Expr left = pl.toExpr(v);
        Expr right = pr.toExpr(v);
        Relation current = new Relation(left, op, right);
        if (!left.equals(r.left()) || !right.equals(r.right())) rec.rel(Rule.ALG_SIMPLIFY, current);

        Rational a1 = pl.coefficient(1);
        Rational b1 = pl.coefficient(0);
        Rational a2 = pr.coefficient(1);
        Rational b2 = pr.coefficient(0);

        // La incógnita, a la izquierda
        if (!a2.isZero()) {
            Expr t = Expr.mul(Expr.num(a2.negate()), x);
            a1 = a1.subtract(a2);
            current = new Relation(Expr.add(Expr.mul(Expr.num(a1), x), Expr.num(b1)), op, Expr.num(b2));
            rec.rel(Rule.ALG_ADD_TO_BOTH, current, "t", t);
        }
        // Los números, a la derecha
        if (!b1.isZero()) {
            b2 = b2.subtract(b1);
            current = new Relation(Expr.mul(Expr.num(a1), x), op, Expr.num(b2));
            rec.rel(Rule.ALG_ADD_TO_BOTH, current, "t", Expr.num(b1.negate()));
        }

        if (a1.isZero()) {
            // 0 ⋈ b
            boolean holds = op.holdsForSign(-b2.signum());
            Relation constant = new Relation(Expr.ZERO, op, Expr.num(b2));
            if (op == Op.EQ) rec.rel(holds ? Rule.ALG_IDENTITY : Rule.ALG_CONTRADICTION, constant, "x", x);
            else rec.rel(holds ? Rule.INEQ_ALWAYS : Rule.INEQ_NEVER, constant, "x", x);
            return holds ? SolutionSet.allReals() : SolutionSet.empty();
        }

        if (!a1.isOne()) {
            boolean negative = a1.signum() < 0;
            Op divided = negative && op.isInequality() ? op.swapped() : op;
            Rule rule = negative && op.isInequality() ? Rule.ALG_DIVIDE_BOTH_NEGATIVE : Rule.ALG_DIVIDE_BOTH;
            current = new Relation(x, divided, Expr.num(b2.divide(a1)));
            rec.rel(rule, current, "t", Expr.num(a1));
            op = divided;
        }
        return setFor(op, Expr.num(b2.divide(a1)));
    }
}
