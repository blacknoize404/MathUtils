package MathUtils.cas.limit;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.steps.Solution;
import MathUtils.cas.steps.Trace;

import java.util.Map;

/**
 * Cálculo de límites paso a paso.
 * <pre>{@code
 * Solution s = Limits.limit("sin(x)/x", "x", "0");           // 1
 * Solution t = Limits.limit("(1 + 1/x)^x", "x", "oo");       // e
 * Solution u = Limits.limit("1/x", "x", "0", Limits.Direction.RIGHT); // oo
 * }</pre>
 * El punto puede ser un número, una constante ({@code pi/2}) o infinito ({@code oo}, {@code -oo}).
 * Todo resultado se comprueba numéricamente antes de devolverlo: si no concuerda, el estado es
 * {@link Solution.Status#UNSOLVED} en vez de devolver un valor dudoso.
 */
public final class Limits {

    /** Lado por el que se aproxima la variable al punto. */
    public enum Direction {BOTH, LEFT, RIGHT}

    private Limits() {
    }

    /**
     * @throws MathUtils.exceptions.ExpressionParseException si alguno de los textos no es una expresión válida
     */
    public static Solution limit(String function, String variable, String point) {
        return limit(function, variable, point, Direction.BOTH);
    }

    public static Solution limit(String function, String variable, String point, Direction direction) {
        return limit(ExprParser.parse(function), variable, ExprParser.parse(point), direction);
    }

    public static Solution limit(Expr function, String variable, Expr point) {
        return limit(function, variable, point, Direction.BOTH);
    }

    /**
     * Calcula el límite de {@code function} cuando {@code variable} tiende a {@code point}.
     * El resultado puede ser {@code oo} o {@code -oo}; si no existe, el estado es
     * {@link Solution.Status#DOES_NOT_EXIST}.
     */
    public static Solution limit(Expr function, String variable, Expr point, Direction direction) {
        Trace trace = new Trace();
        LimitEngine engine = new LimitEngine(variable, trace);
        LimitEngine.Context context = new LimitEngine.Context(point, direction);
        Map<String, Expr> extras = new java.util.LinkedHashMap<>();
        extras.put("a", point);
        extras.put("direction", Expr.num(direction == Direction.RIGHT ? 1 : direction == Direction.LEFT ? -1 : 0));

        try {
            LimitEngine.Outcome outcome = engine.solve(function, context, 0);
            return switch (outcome) {
                case LimitEngine.Value v -> {
                    boolean verified = engine.verify(function, context, v.value());
                    yield new Solution(Solution.Kind.LIMIT,
                            verified ? Solution.Status.SOLVED : Solution.Status.UNSOLVED,
                            function, verified ? v.value() : null, trace.steps(), variable, 1, extras);
                }
                case LimitEngine.NoLimit n -> new Solution(Solution.Kind.LIMIT, Solution.Status.DOES_NOT_EXIST,
                        function, null, trace.steps(), variable, 1, extras);
                case LimitEngine.Unsolved u -> new Solution(Solution.Kind.LIMIT, Solution.Status.UNSOLVED,
                        function, null, trace.steps(), variable, 1, extras);
            };
        } catch (Trace.BudgetExceededException | ArithmeticException e) {
            return new Solution(Solution.Kind.LIMIT, Solution.Status.UNSOLVED, function, null, trace.steps(),
                    variable, 1, extras);
        }
    }
}
