package barnesinside.mathutils.cas.calc;

import barnesinside.mathutils.cas.expr.AngleUnit;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.expr.Fn;
import barnesinside.mathutils.cas.expr.UserFunction;
import barnesinside.mathutils.exceptions.ExpressionParseException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sesión de calculadora: evalúa expresiones con resultado exacto y decimal, recuerda variables,
 * funciones definidas por el usuario, el último resultado ({@code ans}) y un historial.
 * <pre>{@code
 * CalculatorSession calc = new CalculatorSession();
 * calc.evaluate("1/3 + 1/6");        // exacto 1/2, decimal 0.5
 * calc.evaluate("x = 4");            // define x
 * calc.evaluate("f(t) = t^2 + x");   // define una función
 * calc.evaluate("f(3)");             // 13
 * calc.setAngleUnit(AngleUnit.DEGREES);
 * calc.evaluate("sin(30)");          // exacto 1/2
 * }</pre>
 * No es segura para uso concurrente.
 */
public final class CalculatorSession {

    /** Resultado de evaluar una entrada. */
    public enum Status {
        /** Expresión numérica evaluada. */
        OK,
        /** La expresión contiene variables sin valor: el resultado es simbólico. */
        SYMBOLIC,
        /** Se definió una variable o una función. */
        DEFINED,
        /** Error de sintaxis o de cálculo. */
        ERROR
    }

    /**
     * Resultado de una evaluación.
     *
     * @param input         texto evaluado
     * @param status        tipo de resultado
     * @param exact         resultado exacto, o {@code null} si hubo error
     * @param approximate   valor decimal, o {@code null} si el resultado es simbólico o hubo error
     * @param exactText     resultado exacto en texto
     * @param decimalText   valor decimal redondeado a la precisión de la sesión
     * @param latex         resultado exacto en LaTeX
     * @param message       descripción de la definición o del error
     * @param errorPosition posición (desde 0) del error de sintaxis, o {@code null}
     */
    public record Result(String input, Status status, Expr exact, Double approximate, String exactText,
                         String decimalText, String latex, String message, Integer errorPosition) {

        public boolean isError() {
            return status == Status.ERROR;
        }
    }

    /** Resultado de comprobar la sintaxis sin evaluar. */
    public record Validation(boolean valid, String message, Integer position) {
    }

    private static final Pattern FUNCTION_DEFINITION =
            Pattern.compile("^\\s*([\\p{L}_][\\p{L}\\p{N}_]*)\\s*\\(\\s*([\\p{L}_][\\p{L}\\p{N}_]*(?:\\s*,\\s*[\\p{L}_][\\p{L}\\p{N}_]*)*)\\s*\\)\\s*=(?!=)\\s*(.+)$");
    private static final Pattern ASSIGNMENT =
            Pattern.compile("^\\s*([\\p{L}_][\\p{L}\\p{N}_]*)\\s*=(?!=)\\s*(.+)$");
    private static final Set<String> RESERVED = Set.of("pi", "e", "oo", "inf", "infinity", "ans",
            "sqrt", "cbrt", "exp", "log", "root", "pow");
    private static final int MAX_HISTORY = 500;

    private final Map<String, Expr> variables = new LinkedHashMap<>();
    private final Map<String, UserFunction> functions = new LinkedHashMap<>();
    private final List<Result> history = new ArrayList<>();
    private AngleUnit angleUnit = AngleUnit.RADIANS;
    private int precision = 12;
    private double memory = 0;

    // ================================================================ Evaluación

    /**
     * Evalúa una entrada: expresión, asignación ({@code x = 3}) o definición de función
     * ({@code f(x) = x^2}). Nunca lanza excepciones por entradas inválidas: devuelve un resultado
     * con estado {@link Status#ERROR}.
     */
    public Result evaluate(String input) {
        Result result = compute(input == null ? "" : input);
        history.add(result);
        if (history.size() > MAX_HISTORY) history.remove(0);
        return result;
    }

    private Result compute(String input) {
        if (input.isBlank()) return error(input, "Escribe una expresión. Ejemplo: 2 + 3*4", 0);

        Matcher function = FUNCTION_DEFINITION.matcher(input);
        if (function.matches()) return defineFunction(input, function);

        Matcher assignment = ASSIGNMENT.matcher(input);
        if (assignment.matches()) return assign(input, assignment);

        return evaluateExpression(input, input, 0);
    }

    /**
     * @param offset posición de {@code source} dentro de {@code input}, para que la posición de un
     *               error de sintaxis se refiera siempre al texto completo escrito por el usuario
     */
    private Result evaluateExpression(String input, String source, int offset) {
        Expr value;
        boolean approximated;
        try {
            ExprParser.Parsed parsed = ExprParser.parseDetailed(source, functions, angleUnit, variables);
            value = substituteVariables(parsed.expr());
            approximated = parsed.approximate();
        } catch (ExpressionParseException e) {
            return error(input, cleanMessage(e), e.getPosition() + offset);
        } catch (ArithmeticException e) {
            return error(input, "No se puede dividir entre 0.", null);
        }

        if (value.variables().isEmpty()) {
            double numeric = Eval.value(value);
            if (Double.isNaN(numeric) || Double.isInfinite(numeric)) {
                if (!(value instanceof Expr.Const c && c.kind() == Expr.Const.Kind.INFINITY)) {
                    return error(input, "El resultado no está definido en los números reales.", null);
                }
            }
            variables.put("ans", value);
            String decimal = formatDecimal(value, numeric);
            // Un resultado que viene de una aproximación numérica no tiene forma exacta: se muestra el decimal
            if (approximated) return new Result(input, Status.OK, value, numeric, decimal, decimal, decimal, null, null);
            return new Result(input, Status.OK, value, numeric, value.toString(), decimal, value.toLatex(), null, null);
        }

        variables.put("ans", value);
        return new Result(input, Status.SYMBOLIC, value, null, value.toString(), null, value.toLatex(), null, null);
    }

    private Result assign(String input, Matcher m) {
        String name = m.group(1);
        String reserved = checkName(name);
        if (reserved != null) return error(input, reserved, null);

        Result evaluated = evaluateExpression(input, m.group(2), m.start(2));
        if (evaluated.isError()) return evaluated;

        variables.put(name, evaluated.exact());
        variables.remove("ans");
        variables.put("ans", evaluated.exact());
        return new Result(input, Status.DEFINED, evaluated.exact(), evaluated.approximate(), evaluated.exactText(),
                evaluated.decimalText(), evaluated.latex(), name + " = " + evaluated.exactText(), null);
    }

    private Result defineFunction(String input, Matcher m) {
        String name = m.group(1);
        String reserved = checkName(name);
        if (reserved != null) return error(input, reserved, null);

        List<String> parameters = new ArrayList<>();
        for (String p : m.group(2).split("\\s*,\\s*")) parameters.add(p.trim());
        if (parameters.stream().distinct().count() != parameters.size()) {
            return error(input, "Los parámetros de " + name + " no pueden repetirse.", null);
        }

        Expr body;
        try {
            body = ExprParser.parse(m.group(3), functions, angleUnit);
        } catch (ExpressionParseException e) {
            return error(input, cleanMessage(e), e.getPosition() + m.start(3));
        } catch (ArithmeticException e) {
            return error(input, "No se puede dividir entre 0.", null);
        }

        functions.put(name, new UserFunction(parameters, body));
        String signature = name + "(" + String.join(", ", parameters) + ") = " + body;
        return new Result(input, Status.DEFINED, body, null, body.toString(), null, body.toLatex(), signature, null);
    }

    /** Devuelve un mensaje si el nombre no se puede usar, o {@code null} si es válido. */
    private static String checkName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (RESERVED.contains(lower) || Fn.bySymbol(name).isPresent()) {
            return "'" + name + "' es un nombre reservado. Elige otro. Ejemplo: a";
        }
        return null;
    }

    private Expr substituteVariables(Expr e) {
        if (variables.isEmpty()) return e;
        return e.substitute(Map.copyOf(variables));
    }

    // ================================================================ Validación

    /**
     * Comprueba la sintaxis sin evaluar ni guardar nada en el historial, para dar
     * retroalimentación mientras se escribe.
     */
    public Validation validate(String input) {
        if (input == null || input.isBlank()) return new Validation(true, null, null);

        String source = input;
        Matcher function = FUNCTION_DEFINITION.matcher(input);
        Matcher assignment = ASSIGNMENT.matcher(input);
        int offset = 0;
        if (function.matches()) {
            source = function.group(3);
            offset = function.start(3);
        } else if (assignment.matches()) {
            source = assignment.group(2);
            offset = assignment.start(2);
        }

        try {
            ExprParser.check(source, functions, angleUnit);
            return new Validation(true, null, null);
        } catch (ExpressionParseException e) {
            return new Validation(false, cleanMessage(e), e.getPosition() + offset);
        } catch (ArithmeticException e) {
            return new Validation(false, "No se puede dividir entre 0.", null);
        }
    }

    // ================================================================ Estado

    /** Variables definidas (incluye {@code ans} tras la primera evaluación). */
    public Map<String, Expr> variables() {
        return Collections.unmodifiableMap(variables);
    }

    public Map<String, UserFunction> functions() {
        return Collections.unmodifiableMap(functions);
    }

    public List<Result> history() {
        return Collections.unmodifiableList(history);
    }

    public AngleUnit angleUnit() {
        return angleUnit;
    }

    /**
     * Cambia la unidad de los ángulos para las evaluaciones siguientes. Las funciones ya
     * definidas conservan la unidad con la que se escribieron.
     */
    public void setAngleUnit(AngleUnit unit) {
        this.angleUnit = unit;
    }

    public int precision() {
        return precision;
    }

    /**
     * @param digits cifras significativas del valor decimal, entre 1 y 50. Los resultados racionales
     *               usan todas las cifras pedidas; los demás se limitan a 15, que es lo que
     *               permite el cálculo en coma flotante.
     */
    public void setPrecision(int digits) {
        if (digits < 1 || digits > 50) throw new IllegalArgumentException("La precisión debe estar entre 1 y 50 cifras");
        this.precision = digits;
    }

    public void removeVariable(String name) {
        variables.remove(name);
    }

    public void removeFunction(String name) {
        functions.remove(name);
    }

    /** Borra variables, funciones e historial (conserva la unidad de ángulo y la precisión). */
    public void reset() {
        variables.clear();
        functions.clear();
        history.clear();
        memory = 0;
    }

    // ================================================================ Memoria

    public double memory() {
        return memory;
    }

    public void memoryAdd(double value) {
        memory += value;
    }

    public void memorySubtract(double value) {
        memory -= value;
    }

    public void memoryClear() {
        memory = 0;
    }

    // ================================================================ Formato

    /** Máximo de cifras fiables de un valor calculado en coma flotante. */
    private static final int DOUBLE_DIGITS = 15;

    /**
     * Valor decimal: los racionales se dividen con precisión arbitraria; el resto se calcula en
     * coma flotante y se limita a 15 cifras significativas, para no mostrar dígitos falsos.
     */
    private String formatDecimal(Expr exact, double approximate) {
        BigDecimal decimal;
        double magnitude = approximate;
        if (exact instanceof Expr.Num n) {
            if (n.value().isZero()) return "0";
            decimal = new BigDecimal(n.value().numerator())
                    .divide(new BigDecimal(n.value().denominator()), new MathContext(precision));
        } else {
            if (approximate == 0) return "0";
            if (Double.isInfinite(approximate)) return approximate > 0 ? "oo" : "-oo";
            decimal = new BigDecimal(approximate).round(new MathContext(Math.min(precision, DOUBLE_DIGITS)));
        }
        decimal = decimal.stripTrailingZeros();
        double abs = Math.abs(magnitude);
        return abs >= 1e15 || abs < 1e-9 ? decimal.toString().replace("E+", "E") : decimal.toPlainString();
    }

    private static final Pattern POSITION_SUFFIX = Pattern.compile("\\s*\\(línea \\d+, columna \\d+\\)$");

    /**
     * Mensaje de un error de sintaxis sin la coordenada de línea y columna (la posición va en
     * un campo aparte) y con la división por cero redactada como en el resto de la sesión.
     */
    private static String cleanMessage(ExpressionParseException e) {
        String message = POSITION_SUFFIX.matcher(e.getMessage()).replaceAll("");
        return message.equals("División por cero") ? "No se puede dividir entre 0." : message;
    }

    private static Result error(String input, String message, Integer position) {
        return new Result(input, Status.ERROR, null, null, null, null, null, message, position);
    }
}
