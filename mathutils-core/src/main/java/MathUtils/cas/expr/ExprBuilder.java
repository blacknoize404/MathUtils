package MathUtils.cas.expr;

import MathUtils.parser.TreeBuilder;

import java.util.List;
import java.util.Set;

/**
 * Construye {@link Expr} desde el análisis sintáctico de {@link MathUtils.parser.ExpressionParser}.
 */
final class ExprBuilder implements TreeBuilder<Expr> {

    private static final Set<String> SPECIAL_FUNCTIONS = Set.of("sqrt", "cbrt", "exp", "log", "root", "pow");

    @Override
    public Expr number(String text) {
        try {
            return Expr.num(Rational.parse(text));
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Número inválido '" + text + "'");
        }
    }

    @Override
    public Expr variable(String name) {
        String lower = name.toLowerCase();
        if (lower.equals("pi") || name.equals("π")) return Expr.PI;
        if (name.equals("e") || name.equals("E")) return Expr.E;
        if (lower.equals("oo") || lower.equals("inf") || lower.equals("infinity") || name.equals("∞")) return Expr.INFINITY;
        if (isFunction(name)) throw new IllegalArgumentException("Falta '(' tras la función '" + name + "'");
        return Expr.sym(name);
    }

    @Override
    public Expr negate(Expr operand) {
        return Expr.neg(operand);
    }

    @Override
    public Expr binary(char operator, Expr left, Expr right) {
        try {
            return switch (operator) {
                case '+' -> Expr.add(left, right);
                case '-' -> Expr.sub(left, right);
                case '*' -> Expr.mul(left, right);
                case '/' -> Expr.div(left, right);
                default -> Expr.pow(left, right);
            };
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("División por cero");
        }
    }

    @Override
    public Expr function(String name, List<Expr> args) {
        String lower = name.toLowerCase();
        try {
            switch (lower) {
                case "sqrt" -> {
                    return Expr.sqrt(one(name, args));
                }
                case "cbrt" -> {
                    return Expr.pow(one(name, args), Expr.num(1, 3));
                }
                case "exp" -> {
                    return Expr.exp(one(name, args));
                }
                case "log" -> {
                    if (args.size() == 1) return Expr.div(Expr.ln(args.get(0)), Expr.ln(Expr.num(10)));
                    expect(name, args, 2);
                    return Expr.div(Expr.ln(args.get(0)), Expr.ln(args.get(1)));
                }
                case "root" -> {
                    expect(name, args, 2);
                    return Expr.pow(args.get(0), Expr.div(Expr.ONE, args.get(1)));
                }
                case "pow" -> {
                    expect(name, args, 2);
                    return Expr.pow(args.get(0), args.get(1));
                }
                default -> {
                    Fn fn = Fn.bySymbol(name).orElseThrow(() -> new IllegalArgumentException("Función desconocida '" + name + "'"));
                    return Expr.func(fn, one(name, args));
                }
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("División por cero");
        }
    }

    @Override
    public boolean isFunction(String name) {
        return SPECIAL_FUNCTIONS.contains(name.toLowerCase()) || Fn.bySymbol(name).isPresent();
    }

    private static Expr one(String name, List<Expr> args) {
        expect(name, args, 1);
        return args.get(0);
    }

    private static void expect(String name, List<Expr> args, int expected) {
        if (args.size() != expected) {
            throw new IllegalArgumentException(
                    "La función '" + name + "' espera " + expected + " argumento(s) y recibió " + args.size());
        }
    }
}
