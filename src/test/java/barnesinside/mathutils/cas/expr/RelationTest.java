package barnesinside.mathutils.cas.expr;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class RelationTest {

    private static Expr p(String source) {
        return ExprParser.parse(source);
    }

    private static Relation rel(String text) {
        return Relation.parse(text).orElseThrow(() -> new AssertionError("no es una relación: " + text));
    }

    @Test
    void parsesEveryOperator() {
        assert rel("x^2 = 4").op() == Op.EQ;
        assert rel("x == 4").op() == Op.EQ;
        assert rel("x < 4").op() == Op.LT;
        assert rel("x <= 4").op() == Op.LE;
        assert rel("x ≤ 4").op() == Op.LE;
        assert rel("x > 4").op() == Op.GT;
        assert rel("x >= 4").op() == Op.GE;
        assert rel("x ≥ 4").op() == Op.GE;
        assert rel("x != 4").op() == Op.NE;
        assert rel("x <> 4").op() == Op.NE;
        assert rel("x ≠ 4").op() == Op.NE;
    }

    @Test
    void keepsBothSides() {
        Relation r = rel("2x + 3 <= x^2 - 1");
        assert r.left().equals(p("2x + 3")) && r.right().equals(p("x^2 - 1")) : r;
        assert r.difference().equals(p("2x + 3 - (x^2 - 1)"));
    }

    @Test
    void doesNotTreatOtherThingsAsRelations() {
        assert Relation.parse("x^2 + 1").isEmpty();
        assert Relation.parse("x < y < z").isEmpty();
        assert Relation.parse("= 3").isEmpty();
        assert Relation.parse("3 =").isEmpty();
        assert Relation.parse("lim_(x->0) sin(x)/x").isEmpty();
        assert Relation.parse("∫ x dx").isEmpty();
        assert Relation.parse(null).isEmpty();
    }

    @Test
    void anOperatorInsideParenthesesIsNotTheRelation() {
        Optional<Relation> r = Relation.parse("f(x) = (x + 1)");
        assert r.isPresent();
    }

    @Test
    void swappingTheSidesTurnsTheOperatorAround() {
        assert rel("x < 3").swapped().op() == Op.GT;
        assert rel("x <= 3").swapped().op() == Op.GE;
        assert rel("x = 3").swapped().op() == Op.EQ;
        assert rel("x < 3").swapped().left().equals(p("3"));
    }

    @Test
    void complementsAndSigns() {
        assert Op.LT.complement() == Op.GE && Op.GE.complement() == Op.LT;
        assert Op.EQ.complement() == Op.NE;
        assert Op.LE.holdsForSign(0) && !Op.LT.holdsForSign(0);
        assert Op.GT.holdsForSign(1) && !Op.GT.holdsForSign(-1);
        assert Op.NE.holdsForSign(-3) && !Op.NE.holdsForSign(0);
    }

    @Test
    void roundTripsThroughStepParameters() {
        Relation r = rel("x^2 - 4 >= 0");
        Map<String, Expr> params = r.toParams();
        assert Relation.fromParams(params).orElseThrow().equals(r);
        assert Relation.fromParams(Map.of()).isEmpty();
    }

    @Test
    void printsPlainTextAndLatex() {
        assert rel("x^2 <= 4").toString().equals("x^2 <= 4") : rel("x^2 <= 4");
        assert rel("x^2 <= 4").toLatex().equals("x^{2} \\leq 4") : rel("x^2 <= 4").toLatex();
        assert rel("x != 1").toLatex().contains("\\neq");
    }

    // ---------------------------------------------------------------- Conjuntos de soluciones

    @Test
    void finiteSetsAreSortedAndWithoutRepeats() {
        SolutionSet set = SolutionSet.finite(List.of(p("3"), p("-2"), p("3"), p("1/2")));
        assert set.toString().equals("{-2, 1/2, 3}") : set;
        assert set.contains(0.5) && set.contains(-2) && !set.contains(1);
        assert SolutionSet.finite(List.of()).isEmpty();
    }

    @Test
    void intervalsAreMergedWhenTheyOverlapOrTouch() {
        SolutionSet set = SolutionSet.intervals(List.of(
                Interval.open(p("3"), p("5")), Interval.below(p("1"), true), Interval.closed(p("1"), p("2")),
                Interval.open(p("4"), p("6"))));
        assert set.toString().equals("(-∞, 2] ∪ (3, 6)") : set;
        assert set.contains(0) && set.contains(2) && !set.contains(2.5) && set.contains(5.9) && !set.contains(6);
    }

    @Test
    void touchingOpenEndsLeaveAHole() {
        SolutionSet set = SolutionSet.intervals(List.of(Interval.below(p("1"), false), Interval.above(p("1"), false)));
        assert set.toString().equals("(-∞, 1) ∪ (1, ∞)") : set;
        assert !set.contains(1);
    }

    @Test
    void anUnboundedUnionCoversTheWholeLine() {
        SolutionSet set = SolutionSet.intervals(List.of(Interval.below(p("1"), true), Interval.above(p("0"), true)));
        assert set instanceof SolutionSet.AllReals : set;
        assert set.toString().equals("ℝ") && set.toLatex().equals("\\mathbb{R}");
    }

    @Test
    void periodicFamiliesDependOnAnIntegerParameter() {
        SolutionSet set = new SolutionSet.Periodic(List.of(p("pi/6 + 2*pi*k"), p("5*pi/6 + 2*pi*k")), "k");
        assert set.contains(Math.PI / 6) && set.contains(Math.PI / 6 + 4 * Math.PI) && !set.contains(Math.PI / 3);
        assert set.toString().endsWith("k ∈ ℤ}") : set;
    }

    @Test
    void latexOfIntervalsAndFiniteSets() {
        assert SolutionSet.intervals(List.of(Interval.below(p("2"), true))).toLatex().equals("(-\\infty, 2]");
        assert SolutionSet.finite(List.of(p("1"), p("2"))).toLatex().equals("\\left\\{1, 2\\right\\}");
        assert SolutionSet.empty().toLatex().equals("\\varnothing");
    }
}
