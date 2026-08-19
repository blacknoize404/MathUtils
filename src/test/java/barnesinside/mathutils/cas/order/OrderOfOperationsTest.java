package barnesinside.mathutils.cas.order;

import barnesinside.mathutils.cas.order.OrderOfOperations.Operation;
import barnesinside.mathutils.cas.order.OrderOfOperations.OrderStep;
import barnesinside.mathutils.cas.order.OrderOfOperations.Reduction;
import barnesinside.mathutils.cas.syntax.Syntax;
import barnesinside.mathutils.cas.syntax.SyntaxPrinter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

class OrderOfOperationsTest {

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final Locale EN = Locale.ENGLISH;

    private static Reduction r(String source) {
        return OrderOfOperations.reduce(source);
    }

    private static String finalText(Reduction r) {
        return SyntaxPrinter.print(r.result());
    }

    private static List<Operation> operations(Reduction r) {
        return r.steps().stream().map(OrderStep::operation).toList();
    }

    private static List<String> expressions(Reduction r) {
        return r.steps().stream().map(s -> SyntaxPrinter.print(s.after())).toList();
    }

    private static void assertResult(String source, String expected) {
        Reduction r = r(source);
        assert r.isFullyEvaluated() : source + " -> " + finalText(r);
        assert expected.equals(finalText(r)) : source + " -> esperado " + expected + " pero fue " + finalText(r);
    }

    // ------------------------------------------------------------ Resultados y orden

    @Test
    void multiplicationBeforeAddition() {
        Reduction r = r("2 + 3*4");
        assert operations(r).equals(List.of(Operation.MULTIPLICATION, Operation.ADDITION));
        assert expressions(r).equals(List.of("2 + 12", "14"));
    }

    @Test
    void parenthesesFirst() {
        Reduction r = r("(2 + 3)*4");
        assert operations(r).equals(List.of(Operation.ADDITION, Operation.MULTIPLICATION));
        assert expressions(r).equals(List.of("5*4", "20"));
        assert r.steps().get(0).insideParentheses();
        assert !r.steps().get(1).insideParentheses();
    }

    @Test
    void innermostGroupsLeftToRight() {
        Reduction r = r("(1 + 2)*(3 + 4)");
        assert expressions(r).equals(List.of("3*(3 + 4)", "3*7", "21"));

        Reduction nested = r("8/(2*(1 + 1))");
        assert expressions(nested).equals(List.of("8/(2*2)", "8/4", "2"));
    }

    @Test
    void powersBeforeProducts() {
        Reduction r = r("2*3 + 4^2");
        assert operations(r).equals(List.of(Operation.POWER, Operation.MULTIPLICATION, Operation.ADDITION));
        assert expressions(r).equals(List.of("2*3 + 16", "6 + 16", "22"));
    }

    @Test
    void powerIsRightAssociative() {
        Reduction r = r("2^3^2");
        assert expressions(r).equals(List.of("2^9", "512"));
    }

    @Test
    void equalLevelOperationsRunLeftToRight() {
        assert expressions(r("10 - 4 - 3")).equals(List.of("6 - 3", "3"));
        assert expressions(r("100/10/5")).equals(List.of("10/5", "2"));
        assert expressions(r("8/2*3")).equals(List.of("4*3", "12"));
        assert expressions(r("1 + 2*3 + 4*5")).equals(List.of("1 + 6 + 4*5", "1 + 6 + 20", "7 + 20", "27"));
    }

    @Test
    void negationAndPowerPrecedence() {
        // -2^2 = -(2^2)
        Reduction r = r("-2^2");
        assert operations(r).equals(List.of(Operation.POWER, Operation.NEGATION));
        assert "-4".equals(finalText(r));

        assertResult("(-2)^2", "4");
        assertResult("2^-1", "0.5");
        assertResult("3 - -2", "5");
        assertResult("-(1 + 2)*2", "-6");
    }

    @Test
    void exactArithmeticWithFractions() {
        assertResult("1/3 + 1/6", "0.5");
        assertResult("1/3 + 1/3", "2/3");
        assertResult("(1/2)^2", "0.25");
        assertResult("2/3*3/4", "0.5");
        assertResult("0.1 + 0.2", "0.3");
        assertResult("7/2", "3.5");
        assertResult("2^(-2)", "0.25");
        assertResult("4^(1/2)", "2");
        assertResult("8^(1/3)", "2");
    }

    @Test
    void intermediateFractionsAreWrappedInParentheses() {
        Reduction r = r("1/3 + 2");
        assert expressions(r).equals(List.of("(1/3) + 2", "7/3"));
    }

    @Test
    void functions() {
        assertResult("sqrt(16) + 1", "5");
        assertResult("sin(0) + cos(0)", "1");
        assertResult("abs(-3)", "3");
        assertResult("log(8, 2)", "3");
        assertResult("root(32, 5)", "2");
        assertResult("2*sqrt(9)", "6");

        Reduction r = r("sqrt(16) + 2*3");
        assert operations(r).equals(List.of(Operation.FUNCTION, Operation.MULTIPLICATION, Operation.ADDITION));
    }

    @Test
    void irrationalResultsAreApproximated() {
        Reduction r = r("sqrt(2)");
        assert r.isFullyEvaluated();
        Syntax.Num n = (Syntax.Num) r.result();
        assert !n.isExact();
        assert "1.414213562".equals(n.text());

        Reduction mixed = r("sqrt(2)*2");
        assert Math.abs(((Syntax.Num) mixed.result()).approx() - 2 * Math.sqrt(2)) < 1e-9;
        assert !((Syntax.Num) mixed.result()).isExact();
    }

    @Test
    void variablesAndConstantsBlockReduction() {
        Reduction r = r("2*x + 3*4");
        assert expressions(r).equals(List.of("2*x + 12"));
        assert !r.isFullyEvaluated();

        Reduction constant = r("2*pi + 3*4");
        assert expressions(constant).equals(List.of("2*pi + 12"));

        Reduction blockedFunction = r("sin(pi)");
        assert blockedFunction.steps().isEmpty();
    }

    @Test
    void groupWithBlockedContentIsLeftAlone() {
        Reduction r = r("(x + 1)*(2 + 3)");
        assert expressions(r).equals(List.of("(x + 1)*5"));
    }

    @Test
    void literalInParenthesesIsUnwrappedUpFront() {
        Reduction r = r("(5)*2");
        assert expressions(r).equals(List.of("10"));
    }

    @Test
    void alreadyAnumberHasNoSteps() {
        Reduction r = r("42");
        assert r.steps().isEmpty() && r.isFullyEvaluated();
    }

    // ------------------------------------------------------------ Errores

    @Test
    void divisionByZeroStopsTheReduction() {
        Reduction r = r("1 + 2/0");
        assert r.problem() == OrderOfOperations.Problem.DIVISION_BY_ZERO;
        assert !r.isFullyEvaluated();
        assert r.steps().isEmpty();
        assert "No se puede dividir entre 0.".equals(r.problemMessage(ES));
        assert "You can't divide by 0.".equals(r.problemMessage(EN));

        Reduction late = r("(3 - 3)*2 + 1/(2 - 2)");
        assert late.problem() == OrderOfOperations.Problem.DIVISION_BY_ZERO;
        assert !late.steps().isEmpty();
    }

    @Test
    void undefinedResults() {
        assert r("ln(0)").problem() == OrderOfOperations.Problem.UNDEFINED;
        assert r("sqrt(-1)").problem() == OrderOfOperations.Problem.UNDEFINED;
        assert r("(-8)^(1/2)").problem() == OrderOfOperations.Problem.UNDEFINED;
    }

    // ------------------------------------------------------------ Orden de evaluación y resaltado

    @Test
    void evaluationOrderMapsOriginalNodeIdsToSteps() {
        // ids: 0 '+', 1 '2', 2 '*', 3 '3', 4 '4'
        Reduction r = r("2 + 3*4");
        assert r.evaluationOrder().equals(java.util.Map.of(2, 1, 0, 2));

        // ids: 0 '*', 1 '+', 2 '1', 3 '2', 4 '+', 5 '3', 6 '4'
        Reduction groups = r("(1 + 2)*(3 + 4)");
        assert groups.evaluationOrder().equals(java.util.Map.of(1, 1, 4, 2, 0, 3));
    }

    @Test
    void stepsExposeHighlightedPieces() {
        OrderStep first = r("2 + 3*4").steps().get(0);
        List<SyntaxPrinter.Piece> before = first.beforePieces();
        assert before.size() == 2 && before.get(1).highlighted() && before.get(1).text().equals("3*4");
        List<SyntaxPrinter.Piece> after = first.afterPieces();
        assert after.size() == 2 && after.get(1).highlighted() && after.get(1).text().equals("12");

        OrderStep inParens = r("(2 + 3)*4").steps().get(0);
        assert inParens.beforePieces().get(1).text().equals("2 + 3") || inParens.beforePieces().stream().anyMatch(p -> p.highlighted() && p.text().equals("2 + 3"));
    }

    @Test
    void stepNumbersAreConsecutive() {
        Reduction r = r("1 + 2*3 - 4/2 + 5^2");
        for (int i = 0; i < r.steps().size(); i++) assert r.steps().get(i).number() == i + 1;
        assert "30".equals(finalText(r));
    }

    // ------------------------------------------------------------ Textos

    @Test
    void explanationsInBothLanguages() {
        OrderStep multiplication = r("2 + 3*4").steps().get(0);
        assert multiplication.title(ES).equals("Multiplicación");
        assert multiplication.title(EN).equals("Multiplication");
        assert multiplication.explanation(ES).startsWith("Multiplicamos 3 · 4 = 12.");
        assert multiplication.explanation(EN).startsWith("We multiply 3 · 4 = 12.");

        OrderStep inside = r("(2 + 3)*4").steps().get(0);
        assert inside.explanation(ES).startsWith("Primero lo que está entre paréntesis. Sumamos 2 + 3 = 5.");
        assert inside.explanation(EN).startsWith("First, what is inside the parentheses. We add 2 + 3 = 5.");

        OrderStep power = r("(-2)^2").steps().get(1);
        assert power.explanation(ES).contains("(-2)^2 = 4") : power.explanation(ES);

        OrderStep function = r("sqrt(16)").steps().get(0);
        assert function.explanation(ES).contains("sqrt(16) = 4");
    }

    @Test
    void everyOperationHasTextsInBothLanguages() {
        for (Operation op : Operation.values()) {
            for (Locale locale : new Locale[]{ES, EN}) {
                assert barnesinside.mathutils.cas.steps.Messages.has("order." + op.name() + ".title", locale) : op;
                assert barnesinside.mathutils.cas.steps.Messages.has("order." + op.name() + ".text", locale) : op;
            }
        }
        for (OrderOfOperations.Problem p : OrderOfOperations.Problem.values()) {
            assert barnesinside.mathutils.cas.steps.Messages.has("order.error." + p.name(), ES);
            assert barnesinside.mathutils.cas.steps.Messages.has("order.error." + p.name(), EN);
        }
    }

    @Test
    void noUnresolvedPlaceholders() {
        for (String source : new String[]{"2+3*4", "(1+2)^2", "sqrt(2)+1", "-3", "10/4", "log(8,2)"}) {
            for (OrderStep step : r(source).steps()) {
                for (Locale locale : new Locale[]{ES, EN}) {
                    assert !step.explanation(locale).contains("${") : source;
                }
            }
        }
    }

    @Test
    void reducingAnInvalidExpressionThrows() {
        try {
            r("2 +");
            assert false;
        } catch (barnesinside.mathutils.exceptions.ExpressionParseException e) {
            assert true;
        }
    }
}
