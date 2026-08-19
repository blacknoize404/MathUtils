package barnesinside.mathutils.cas.expr;

import barnesinside.mathutils.parser.TreeBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Construye {@link Expr} desde el análisis sintáctico de {@link barnesinside.mathutils.parser.ExpressionParser}.
 * Admite funciones definidas por el usuario y una unidad de ángulo para las funciones
 * trigonométricas (con grados, {@code sin(30)} vale {@code 1/2}).
 */
public final class ExprBuilder implements TreeBuilder<Expr> {

    private static final Set<String> SPECIAL_FUNCTIONS = Set.of("sqrt", "cbrt", "exp", "log", "root", "pow");
    private static final Set<Fn> DIRECT_TRIGONOMETRIC = Set.of(Fn.SIN, Fn.COS, Fn.TAN, Fn.COT, Fn.SEC, Fn.CSC);
    private static final Set<Fn> INVERSE_TRIGONOMETRIC = Set.of(Fn.ASIN, Fn.ACOS, Fn.ATAN);

    private final Map<String, UserFunction> userFunctions;
    private final AngleUnit angleUnit;

    public ExprBuilder() {
        this(Map.of(), AngleUnit.RADIANS);
    }

    /**
     * @param userFunctions funciones definidas por el usuario, por nombre exacto (sensible a mayúsculas)
     * @param angleUnit     unidad de los ángulos de entrada y de salida de las funciones trigonométricas
     */
    public ExprBuilder(Map<String, UserFunction> userFunctions, AngleUnit angleUnit) {
        this.userFunctions = Map.copyOf(userFunctions);
        this.angleUnit = angleUnit;
    }

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
        UserFunction user = userFunctions.get(name);
        if (user != null) {
            try {
                return user.apply(args);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("División por cero");
            }
        }

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
                    return trigonometric(fn, one(name, args));
                }
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("División por cero");
        }
    }

    /** Aplica la unidad de ángulo: las directas convierten la entrada a radianes y las inversas la salida. */
    private Expr trigonometric(Fn fn, Expr argument) {
        if (angleUnit == AngleUnit.RADIANS) return Expr.func(fn, argument);
        Expr factor = angleUnit.toRadiansFactor();
        if (DIRECT_TRIGONOMETRIC.contains(fn)) return Expr.func(fn, Expr.mul(argument, factor));
        if (INVERSE_TRIGONOMETRIC.contains(fn)) return Expr.div(Expr.func(fn, argument), factor);
        return Expr.func(fn, argument);
    }

    @Override
    public boolean isFunction(String name) {
        return userFunctions.containsKey(name) || SPECIAL_FUNCTIONS.contains(name.toLowerCase()) || Fn.bySymbol(name).isPresent();
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
