package barnesinside.mathutils.cas.calc;

import barnesinside.mathutils.cas.diff.Derivatives;
import barnesinside.mathutils.cas.expr.AngleUnit;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.expr.UserFunction;
import barnesinside.mathutils.cas.integral.Integrals;
import barnesinside.mathutils.cas.limit.Limits;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.exceptions.ExpressionParseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Integrales, derivadas y límites escritos dentro de una expresión mayor, como en
 * {@code 1/int(exp(-x) x^z ln(x), x, 0, oo)} o {@code 2 + ∫_0^1 x^2 dx}.
 * <p>El texto se recorre antes de interpretarlo: cada llamada ({@code int(…)}, {@code diff(…)},
 * {@code limit(…)}) o integral con notación ({@code ∫_a^b f dx}) se resuelve con el motor y se sustituye
 * por un nombre provisional del mismo largo (así las posiciones de los errores siguen valiendo); una vez
 * interpretado el resto, el nombre se cambia por el resultado. Si el resultado exacto no existe pero la
 * integral definida no lleva parámetros, se usa su valor numérico.
 */
public final class NestedCalculus {

    private static final Set<String> INTEGRAL = Set.of("int", "integral", "integrate", "integra", "integrar");
    private static final Set<String> DERIVATIVE = Set.of("diff", "derivative", "derivada", "deriv", "derivar", "differentiate");
    private static final Set<String> LIMIT = Set.of("limit", "limite", "límite");

    private NestedCalculus() {
    }

    /**
     * El texto con las llamadas sustituidas y los resultados que vuelven a su lugar.
     *
     * @param approximate si algún resultado es un valor numérico y no exacto
     */
    public record Expansion(String text, Map<String, Expr> replacements, boolean approximate) {

        /** Sustituye los nombres provisionales por los resultados. */
        public Expr apply(Expr e) {
            return replacements.isEmpty() ? e : e.substitute(replacements);
        }
    }

    /** Indica si el texto puede llevar algo que expandir (comprobación barata para no recorrerlo siempre). */
    static boolean mentions(String source) {
        if (source == null) return false;
        if (source.indexOf('∫') >= 0) return true;
        if (source.indexOf('(') < 0) return false;
        String lower = source.toLowerCase(Locale.ROOT);
        for (Set<String> names : List.of(INTEGRAL, DERIVATIVE, LIMIT)) {
            for (String name : names) if (lower.contains(name)) return true;
        }
        return false;
    }

    /**
     * @param solve  si es {@code false} solo se reconocen las llamadas y se sustituyen por un nombre, sin
     *               resolverlas (para comprobar la sintaxis mientras se escribe)
     * @param values valores de las variables, que se aplican al interior de cada llamada
     */
    public static Expansion expand(String source, boolean solve, Map<String, Expr> values,
                                   Map<String, UserFunction> functions, AngleUnit angleUnit) {
        if (!mentions(source)) return new Expansion(source, Map.of(), false);

        StringBuilder text = new StringBuilder(source);
        Map<String, Expr> replacements = new LinkedHashMap<>();
        boolean approximate = false;
        Context context = new Context(solve, values, functions, angleUnit);

        int i = 0;
        while (i < source.length()) {
            Call call = callAt(source, i);
            if (call == null) {
                i++;
                continue;
            }
            String placeholder = placeholder(source, replacements.size() + 1);
            if (solve) {
                Value value = context.solve(call, source);
                replacements.put(placeholder, value.expr());
                approximate |= value.approximate();
            }
            String padded = placeholder + " ".repeat(Math.max(0, call.end() - i - placeholder.length()));
            text.replace(i, call.end(), padded);
            i = call.end();
        }
        return new Expansion(text.toString(), replacements, approximate);
    }

    private static String placeholder(String source, int n) {
        String name = "_n" + n;
        while (source.contains(name)) name = "_" + name;
        return name;
    }

    // ================================================================ Reconocimiento

    private enum Kind {INTEGRAL, DERIVATIVE, LIMIT}

    /** Una llamada reconocida: dónde termina y sus partes ({@code lower}/{@code upper} {@code null} si no hay). */
    private record Call(int start, int end, Kind kind, List<String> args, String function, String variable,
                        String lower, String upper) {
    }

    private static Call callAt(String s, int i) {
        char c = s.charAt(i);
        if (c == '∫') return integralNotation(s, i);
        if (!Character.isLetter(c) || (i > 0 && (Character.isLetterOrDigit(s.charAt(i - 1)) || s.charAt(i - 1) == '_'))) return null;

        int j = i;
        while (j < s.length() && Character.isLetter(s.charAt(j))) j++;
        String name = s.substring(i, j).toLowerCase(Locale.ROOT);
        Kind kind = INTEGRAL.contains(name) ? Kind.INTEGRAL : DERIVATIVE.contains(name) ? Kind.DERIVATIVE
                : LIMIT.contains(name) ? Kind.LIMIT : null;
        if (kind == null) return null;

        int open = j;
        while (open < s.length() && Character.isWhitespace(s.charAt(open))) open++;
        if (open >= s.length() || s.charAt(open) != '(') return null;
        int close = matching(s, open);
        if (close < 0) return null;
        List<String> args = split(s.substring(open + 1, close));
        if (args.size() == 1 && args.get(0).isBlank()) return null;
        return new Call(i, close + 1, kind, args, null, null, null, null);
    }

    /** {@code ∫_a^b f dx}: los límites en cualquier orden y el cuerpo hasta el primer {@code dx} fuera de paréntesis. */
    private static Call integralNotation(String s, int start) {
        int pos = start + 1;
        String lower = null;
        String upper = null;
        for (int k = 0; k < 2; k++) {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
            if (pos >= s.length()) break;
            char mark = s.charAt(pos);
            if ((mark != '_' || lower != null) && (mark != '^' || upper != null)) break;
            int[] end = new int[1];
            String bound = readBound(s, pos + 1, end);
            if (mark == '_') lower = bound;
            else upper = bound;
            pos = end[0];
        }
        int bodyStart = pos;
        int depth = 0;
        for (int k = bodyStart; k < s.length(); k++) {
            char c = s.charAt(k);
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') {
                if (--depth < 0) break;
            } else if (depth == 0 && c == 'd' && k > bodyStart && k + 1 < s.length() && Character.isLetter(s.charAt(k + 1))
                    && isBoundary(s, k - 1) && isBoundary(s, k + 2)) {
                String body = s.substring(bodyStart, k).strip();
                if (body.isEmpty()) break;
                return new Call(start, k + 2, Kind.INTEGRAL, List.of(), body, String.valueOf(s.charAt(k + 1)), lower, upper);
            }
        }
        throw new ExpressionParseException("A la integral le falta el diferencial. Ejemplo: ∫_0^1 x dx", s, start);
    }

    private static boolean isBoundary(String s, int at) {
        if (at < 0 || at >= s.length()) return true;
        char c = s.charAt(at);
        return !Character.isLetterOrDigit(c) && c != '_';
    }

    /** Un límite de integral desde {@code from}: un grupo entre llaves o paréntesis (sin ellos) o una palabra. */
    private static String readBound(String s, int from, int[] end) {
        if (from >= s.length()) {
            end[0] = s.length();
            return "";
        }
        char c = s.charAt(from);
        if (c == '{' || c == '(') {
            int close = matching(s, from);
            if (close > 0) {
                end[0] = close + 1;
                return s.substring(from + 1, close).strip();
            }
        }
        int i = from;
        while (i < s.length() && !Character.isWhitespace(s.charAt(i)) && !(i > from && (s.charAt(i) == '^' || s.charAt(i) == '_'))) i++;
        end[0] = i;
        return s.substring(from, i);
    }

    private static int matching(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static List<String> split(String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(s.substring(start));
        return parts;
    }

    // ================================================================ Resolución

    private record Value(Expr expr, boolean approximate) {
    }

    private record Context(boolean solve, Map<String, Expr> values, Map<String, UserFunction> functions, AngleUnit angleUnit) {

        Value solve(Call call, String source) {
            try {
                return switch (call.kind()) {
                    case INTEGRAL -> integral(call, source);
                    case DERIVATIVE -> derivative(call, source);
                    case LIMIT -> limit(call, source);
                };
            } catch (ExpressionParseException e) {
                throw e;
            } catch (ArithmeticException | IllegalArgumentException e) {
                throw new ExpressionParseException(e.getMessage() == null ? "No se pudo calcular." : e.getMessage(), source, call.start());
            }
        }

        private Expr parse(String text) {
            return ExprParser.parse(text, functions, angleUnit, values);
        }

        /** El texto interpretado y con los valores de las variables (menos la de la operación) ya puestos. */
        private Expr parse(String text, String except) {
            Map<String, Expr> env = new LinkedHashMap<>(values);
            env.remove(except);
            Expr e = parse(text);
            return env.isEmpty() ? e : e.substitute(env);
        }

        private String guess(Expr f) {
            Set<String> names = new TreeSet<>(f.variables());
            if (names.contains("x")) return "x";
            return names.isEmpty() ? "x" : names.iterator().next();
        }

        private Value integral(Call call, String source) {
            String function = call.function();
            String variable = call.variable();
            String lower = call.lower();
            String upper = call.upper();
            if (function == null) {
                List<String> args = call.args();
                function = args.get(0).strip();
                switch (args.size()) {
                    case 1 -> { }
                    case 2 -> variable = args.get(1).strip();
                    case 3 -> {
                        lower = args.get(1).strip();
                        upper = args.get(2).strip();
                    }
                    default -> {
                        variable = args.get(1).strip();
                        lower = args.get(2).strip();
                        upper = args.get(3).strip();
                    }
                }
            }
            if (variable == null) variable = guess(parse(function));
            Expr f = parse(function, variable);

            if (lower == null && upper == null) {
                Solution s = Integrals.indefinite(f, variable);
                if (!s.isSolved()) throw failure("No se pudo calcular la integral de " + f + ".", source, call);
                return new Value(s.result(), false);
            }
            Expr a = parse(lower == null ? "-oo" : lower, variable);
            Expr b = parse(upper == null ? "oo" : upper, variable);
            Solution s = Integrals.definite(f, variable, a, b);
            if (s.isSolved()) return new Value(s.result(), false);
            if (s.status() == Solution.Status.DOES_NOT_EXIST) {
                throw failure("La integral de " + f + " entre " + a + " y " + b + " diverge.", source, call);
            }
            Expr approximation = s.extras().get("approximation");
            if (approximation != null && free(f, variable).isEmpty()) return new Value(approximation, true);
            throw failure(unsolved("la integral de " + f + " entre " + a + " y " + b, free(f, variable)), source, call);
        }

        private Value derivative(Call call, String source) {
            List<String> args = call.args();
            Expr f0 = parse(args.get(0));
            String variable = args.size() > 1 ? args.get(1).strip() : guess(f0);
            Expr f = parse(args.get(0), variable);
            int order = 1;
            if (args.size() > 2) {
                Expr n = parse(args.get(2));
                if (!(n instanceof Expr.Num num) || !num.value().isInteger() || num.value().signum() < 1) {
                    throw failure("El orden de la derivada tiene que ser un entero positivo. Ejemplo: diff(x^3, x, 2)", source, call);
                }
                order = num.value().numerator().intValueExact();
            }
            Solution s = Derivatives.nth(f, variable, order);
            if (!s.isSolved()) throw failure("No se pudo derivar " + f + ".", source, call);
            return new Value(s.result(), false);
        }

        private Value limit(Call call, String source) {
            List<String> args = call.args();
            if (args.size() < 3) throw failure("El límite necesita la función, la variable y el punto. Ejemplo: limit(sin(x)/x, x, 0)", source, call);
            String variable = args.get(1).strip();
            Expr f = parse(args.get(0), variable);
            Expr point = parse(args.get(2), variable);
            Limits.Direction direction = Limits.Direction.BOTH;
            if (args.size() > 3) {
                direction = switch (args.get(3).strip().toLowerCase(Locale.ROOT)) {
                    case "+", "right", "derecha" -> Limits.Direction.RIGHT;
                    case "-", "left", "izquierda" -> Limits.Direction.LEFT;
                    default -> Limits.Direction.BOTH;
                };
            }
            Solution s = Limits.limit(f, variable, point, direction);
            if (s.status() == Solution.Status.DOES_NOT_EXIST) throw failure("El límite de " + f + " en " + point + " no existe.", source, call);
            if (!s.isSolved()) throw failure(unsolved("el límite de " + f + " en " + point, free(f, variable)), source, call);
            return new Value(s.result(), false);
        }

        private static Set<String> free(Expr f, String variable) {
            Set<String> names = new TreeSet<>(f.variables());
            names.remove(variable);
            return names;
        }

        private static String unsolved(String what, Set<String> parameters) {
            String base = "No se pudo calcular " + what + ".";
            if (parameters.isEmpty()) return base;
            return base + " Depende de " + String.join(", ", parameters) + ", que no tiene valor. Dale uno. Ejemplo: "
                    + parameters.iterator().next() + " = 1";
        }

        private static ExpressionParseException failure(String message, String source, Call call) {
            return new ExpressionParseException(message, source, call.start());
        }
    }
}
