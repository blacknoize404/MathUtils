package barnesinside.mathutils.cas.syntax;

import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.exceptions.ExpressionParseException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

class SyntaxTest {

    private static Syntax s(String source) {
        return SyntaxParser.parse(source);
    }

    @Test
    void keepsStructureInsteadOfFolding() {
        Syntax tree = s("2 + 3*4");
        assert tree instanceof Syntax.Bin plus && plus.op() == '+'
                && plus.left() instanceof Syntax.Num
                && plus.right() instanceof Syntax.Bin times && times.op() == '*';
    }

    @Test
    void parenthesesBecomeGroups() {
        Syntax tree = s("(2 + 3)*4");
        assert tree instanceof Syntax.Bin times && times.op() == '*' && times.left() instanceof Syntax.Group g
                && g.inner() instanceof Syntax.Bin;

        Syntax nested = s("((x))");
        assert nested instanceof Syntax.Group outer && outer.inner() instanceof Syntax.Group inner
                && inner.inner() instanceof Syntax.Var;
    }

    @Test
    void functionCallParenthesesAreNotGroups() {
        Syntax tree = s("sin(x)");
        assert tree instanceof Syntax.Call call && call.args().get(0) instanceof Syntax.Var;
    }

    @Test
    void atomsAndFunctions() {
        assert s("pi") instanceof Syntax.Const c && c.name().equals("pi");
        assert s("E") instanceof Syntax.Const c && c.name().equals("e");
        assert s("-x") instanceof Syntax.Neg;
        assert s("log(x, 2)") instanceof Syntax.Call c && c.args().size() == 2;
        assert s("ARCTAN(x)") instanceof Syntax.Call c && c.name().equals("atan");
        assert s("2x") instanceof Syntax.Bin b && b.op() == '*';
    }

    @Test
    void extraFunctionsAreCalls() {
        assert SyntaxParser.parse("f(2) + 1", Set.of("f")) instanceof Syntax.Bin b && b.left() instanceof Syntax.Call c
                && c.name().equals("f");
        assert s("f(2)") instanceof Syntax.Bin b && b.op() == '*';
    }

    @Test
    void invalidInputsReportPositions() {
        for (String bad : new String[]{"", "1 +", "(1", "sin(1,2)", "sin"}) {
            try {
                s(bad);
                assert false : bad;
            } catch (ExpressionParseException e) {
                assert true;
            }
        }
    }

    @Test
    void printsInfixText() {
        assert "2 + 3*4".equals(SyntaxPrinter.print(s("2+3*4")));
        assert "(2 + 3)*4".equals(SyntaxPrinter.print(s("(2+3)*4")));
        assert "-x^2".equals(SyntaxPrinter.print(s("-x^2")));
        assert "sin(x) + log(8, 2)".equals(SyntaxPrinter.print(s("sin(x)+log(8,2)")));
        assert "2*x".equals(SyntaxPrinter.print(s("2x")));
        assert "10 - 4 - 3".equals(SyntaxPrinter.print(s("10-4-3")));
    }

    @Test
    void printedTextReparsesToTheSameStructure() {
        for (String source : new String[]{"2+3*4", "(1+2)*(3+4)", "2^3^2", "-(x+1)^2", "sin(x)/(x+1)", "a-(b-c)"}) {
            Syntax tree = s(source);
            assert tree.equals(s(SyntaxPrinter.print(tree))) : source;
        }
    }

    @Test
    void numbersWrapInParenthesesAsOperands() {
        Syntax tree = new Syntax.Bin('*', Syntax.Num.of(barnesinside.mathutils.cas.expr.Rational.of(2)),
                Syntax.Num.of(barnesinside.mathutils.cas.expr.Rational.of(-3)));
        assert "2*(-3)".equals(SyntaxPrinter.print(tree));

        Syntax fraction = new Syntax.Bin('+', Syntax.Num.of(barnesinside.mathutils.cas.expr.Rational.of(1, 3)),
                Syntax.Num.of(barnesinside.mathutils.cas.expr.Rational.of(1)));
        assert "(1/3) + 1".equals(SyntaxPrinter.print(fraction));
    }

    @Test
    void numberFormatting() {
        assert "0.5".equals(Syntax.Num.of(barnesinside.mathutils.cas.expr.Rational.of(1, 2)).text());
        assert "0.125".equals(Syntax.Num.of(barnesinside.mathutils.cas.expr.Rational.of(1, 8)).text());
        assert "1/3".equals(Syntax.Num.of(barnesinside.mathutils.cas.expr.Rational.of(1, 3)).text());
        assert "-7".equals(Syntax.Num.of(barnesinside.mathutils.cas.expr.Rational.of(-7)).text());
        assert "1.414213562".equals(Syntax.Num.approximate(Math.sqrt(2)).text());
        assert "0".equals(Syntax.Num.approximate(0).text());
        assert Syntax.Num.approximate(1.5e12).text().contains("E");
    }

    @Test
    void highlightingMarksTheGivenSubtreeByIdentity() {
        Syntax tree = s("2 + 3*4");
        Syntax product = ((Syntax.Bin) tree).right();
        List<SyntaxPrinter.Piece> pieces = SyntaxPrinter.pieces(tree, product);
        assert pieces.size() == 2;
        assert pieces.get(0).text().equals("2 + ") && !pieces.get(0).highlighted();
        assert pieces.get(1).text().equals("3*4") && pieces.get(1).highlighted();

        // Una copia estructuralmente igual pero distinta instancia no se resalta
        Syntax lookalike = new Syntax.Bin('*', s("3"), s("4"));
        assert SyntaxPrinter.pieces(tree, lookalike).size() == 1;
    }

    @Test
    void idsFollowPreorderAndGroupsShareTheirContentsId() {
        Syntax tree = s("(1 + 2)*3");
        Map<Syntax, Integer> ids = SyntaxTools.assignIds(tree);
        Syntax.Bin times = (Syntax.Bin) tree;
        Syntax.Group group = (Syntax.Group) times.left();
        Syntax.Bin plus = (Syntax.Bin) group.inner();

        assert ids.get(times) == 0;
        assert ids.get(plus) == 1 && ids.get(group) == 1;
        assert ids.get(plus.left()) == 2 && ids.get(plus.right()) == 3;
        assert ids.get(times.right()) == 4;
    }

    @Test
    void convertsToSymbolicExpressions() {
        assert SyntaxTools.toExpr(s("2 + 3*4")).equals(Expr.num(14));
        assert SyntaxTools.toExpr(s("x^2 + 1")).equals(ExprParser.parse("x^2 + 1"));
        assert SyntaxTools.toExpr(s("(x+1)(x-1)")).equals(ExprParser.parse("(x+1)(x-1)"));
        assert SyntaxTools.toExpr(s("sin(pi/6)")).equals(Expr.num(1, 2));
    }

    @Test
    void precedenceAndAssociativity() {
        assert SyntaxTools.precedence('+') < SyntaxTools.precedence('*');
        assert SyntaxTools.precedence('*') < SyntaxTools.NEGATION_PRECEDENCE;
        assert SyntaxTools.NEGATION_PRECEDENCE < SyntaxTools.precedence('^');
        assert SyntaxTools.associativity('^') == SyntaxTools.Associativity.RIGHT;
        assert SyntaxTools.associativity('-') == SyntaxTools.Associativity.LEFT;
    }

    @Test
    void legacyParserIsUnaffectedByGroupTracking() {
        assert barnesinside.mathutils.parser.ExpressionParser.parse("(1+2)*3") != null;
        try {
            barnesinside.mathutils.parser.ExpressionParser.parse("2x");
            assert false;
        } catch (ExpressionParseException e) {
            assert true;
        }
    }
}
