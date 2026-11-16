package com.barnesinside.mathutils.cas.expr;

import com.barnesinside.mathutils.parser.TreeBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Construye {@link Expr} desde el análisis sintáctico de {@link com.barnesinside.mathutils.parser.ExpressionParser}.
 * Admite funciones definidas por el usuario y una unidad de ángulo para las funciones
 * trigonométricas (con grados, {@code sin(30)} vale {@code 1/2}).
 */
public final class ExprBuilder implements TreeBuilder<Expr> {

    private static final Set<String> SPECIAL_FUNCTIONS = Set.of("sqrt", "cbrt", "exp", "log", "root", "pow");
    private static final Set<Fn> DIRECT_TRIGONOMETRIC = Set.of(Fn.SIN, Fn.COS, Fn.TAN, Fn.COT, Fn.SEC, Fn.CSC);
    private static final Set<Fn> INVERSE_TRIGONOMETRIC = Set.of(Fn.ASIN, Fn.ACOS, Fn.ATAN);
    /** Siglas de conteo tal y como se escriben en los libros; solo mayúsculas: {@code c}, {@code v} o {@code p} siguen siendo variables. */
    private static final Set<String> COUNTING_SIGLAS = Set.of("C", "P", "V", "M", "VR", "PR", "CR", "PC");
    /** Los mismos cálculos con nombre en palabra (insensibles a mayúsculas, como el resto de funciones). */
    private static final Set<String> COUNTING_NAMES = Set.of(
            "comb", "choose", "binom", "binomial", "perm", "permutation",
            "var", "arr", "varr", "permr", "multinom", "combr", "circ", "circular", "cat", "catalan",
            "vr", "pr", "cr", "pc");

    private final Map<String, UserFunction> userFunctions;
    private final AngleUnit angleUnit;
    /** Nombres de más de una letra que son variables declaradas: esos no se separan en letras. */
    private final java.util.Set<String> declared;

    public ExprBuilder() {
        this(Map.of(), AngleUnit.RADIANS);
    }

    /**
     * @param userFunctions funciones definidas por el usuario, por nombre exacto (sensible a mayúsculas)
     * @param angleUnit     unidad de los ángulos de entrada y de salida de las funciones trigonométricas
     */
    public ExprBuilder(Map<String, UserFunction> userFunctions, AngleUnit angleUnit) {
        this(userFunctions, angleUnit, java.util.Set.of());
    }

    /** @param declared variables ya declaradas, de las que {@code xy} puede ser una sola y no el producto {@code x·y} */
    public ExprBuilder(Map<String, UserFunction> userFunctions, AngleUnit angleUnit, java.util.Set<String> declared) {
        this.userFunctions = Map.copyOf(userFunctions);
        this.angleUnit = angleUnit;
        this.declared = java.util.Set.copyOf(declared);
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
        if (name.equals("γ") || lower.equals("euler_gamma")) return Expr.EULER_GAMMA;
        if (isFunction(name)) throw new IllegalArgumentException("Falta '(' tras la función '" + name + "'");
        // xy, abc: letras seguidas son un producto, como se escribe a mano (salvo una variable declarada con ese nombre)
        if (name.length() >= 2 && name.length() <= 3 && name.chars().allMatch(Character::isLetter) && !name.equals("ans")
                && !declared.contains(name) && !userFunctions.containsKey(name)) {
            Expr product = Expr.ONE;
            for (char c : name.toCharArray()) product = Expr.mul(product, variable(String.valueOf(c)));
            return product;
        }
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
            Expr counted = counting(name, args);
            if (counted != null) return counted;
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
            // El factorial de un entero negativo lo explica; lo demás es una división por cero
            throw new IllegalArgumentException(e.getMessage() != null && e.getMessage().contains("factorial") ? e.getMessage() : "División por cero");
        }
    }

    /**
     * Conteo combinatorio, escrito como en los libros: {@code C(n, k)}, {@code P(n)}, {@code P(n, k)},
     * {@code V(n, k)}, {@code VR(n, k)}, {@code CR(n, k)}, {@code PC(n)}, los multinomiales
     * {@code M(n; r1, …, rk)} y {@code Cat(n)}. Se deshacen en factoriales y potencias, así que
     * sirven igual con números que con variables. {@code null} si el nombre no es de conteo.
     */
    private Expr counting(String name, List<Expr> args) {
        String key = COUNTING_SIGLAS.contains(name) ? name : name.toLowerCase();
        return switch (key) {
            case "C", "comb", "choose", "binom", "binomial" -> {
                expect(name, args, 2);
                Expr n = args.get(0), k = args.get(1);
                yield Expr.div(fact(n), Expr.mul(fact(k), fact(Expr.sub(n, k))));
            }
            case "P", "perm", "permutation" -> {
                if (args.size() == 1) yield fact(args.get(0));
                expect(name, args, 2);
                yield Expr.div(fact(args.get(0)), fact(Expr.sub(args.get(0), args.get(1))));
            }
            case "V", "var", "arr" -> {
                expect(name, args, 2);
                yield Expr.div(fact(args.get(0)), fact(Expr.sub(args.get(0), args.get(1))));
            }
            case "VR", "vr", "varr" -> {
                expect(name, args, 2);
                yield Expr.pow(args.get(0), args.get(1));
            }
            case "CR", "cr", "combr" -> {
                expect(name, args, 2);
                Expr n = args.get(0), k = args.get(1);
                yield Expr.div(fact(Expr.sub(Expr.add(n, k), Expr.ONE)),
                        Expr.mul(fact(k), fact(Expr.sub(n, Expr.ONE))));
            }
            case "PC", "pc", "circ", "circular" -> {
                expect(name, args, 1);
                yield fact(Expr.sub(args.get(0), Expr.ONE));
            }
            case "M", "PR", "pr", "permr", "multinom" -> {
                if (args.size() < 2) throw new IllegalArgumentException(
                        "La función '" + name + "' espera al menos 2 argumentos y recibió " + args.size());
                Expr denominator = Expr.ONE;
                for (int i = 1; i < args.size(); i++) denominator = Expr.mul(denominator, fact(args.get(i)));
                yield Expr.div(fact(args.get(0)), denominator);
            }
            case "cat", "catalan" -> {
                expect(name, args, 1);
                Expr n = args.get(0);
                yield Expr.div(fact(Expr.mul(Expr.num(2), n)),
                        Expr.mul(Expr.mul(fact(n), fact(n)), Expr.add(n, Expr.ONE)));
            }
            default -> null;
        };
    }

    /** {@code n!} sobre el árbol: se pliega si el argumento es un entero no negativo. */
    private static Expr fact(Expr arg) {
        return Expr.func(Fn.FACTORIAL, arg);
    }

    /** {@code true} si el nombre llama a un cálculo de conteo ({@code C}, {@code VR}, {@code comb}…). */
    public static boolean isCountingFunction(String name) {
        return COUNTING_SIGLAS.contains(name) || COUNTING_NAMES.contains(name.toLowerCase());
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
        return userFunctions.containsKey(name) || SPECIAL_FUNCTIONS.contains(name.toLowerCase())
                || Fn.bySymbol(name).isPresent() || isCountingFunction(name);
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
