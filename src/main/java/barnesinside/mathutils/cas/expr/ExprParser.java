package barnesinside.mathutils.cas.expr;

import barnesinside.mathutils.cas.calc.NestedCalculus;
import barnesinside.mathutils.exceptions.ExpressionParseException;
import barnesinside.mathutils.parser.ExpressionParser;

import java.util.Map;
import java.util.Optional;

/**
 * Interpreta texto como una expresión simbólica {@link Expr} en forma canónica.
 * <p>Además de la sintaxis de {@link ExpressionParser}, acepta multiplicación implícita
 * ({@code 2x}, {@code 3(x+1)}, {@code x sin(x)}), las constantes {@code pi}/{@code π},
 * {@code e} y {@code oo}, y las funciones {@code sin cos tan cot sec csc asin acos atan sinh cosh
 * tanh ln log sqrt cbrt exp abs erf root pow} (con los alias {@code arcsin}, {@code arctan}…).
 * También la potencia de una función escrita a mano: {@code sin^2(x)} es {@code (sin(x))^2}.
 */
public final class ExprParser {

    private ExprParser() {
    }

    /**
     * @throws ExpressionParseException si la expresión es inválida (con línea y columna)
     */
    public static Expr parse(String source) {
        NestedCalculus.Expansion nested = NestedCalculus.expand(source, true, Map.of(), Map.of(), AngleUnit.RADIANS);
        return nested.apply(ExpressionParser.parse(FunctionPowers.rewrite(nested.text()), new ExprBuilder(), true));
    }

    /**
     * Interpreta el texto con funciones definidas por el usuario y una unidad de ángulo.
     * @throws ExpressionParseException si la expresión es inválida
     */
    public static Expr parse(String source, Map<String, UserFunction> userFunctions, AngleUnit angleUnit) {
        return parse(source, userFunctions, angleUnit, Map.of());
    }

    /**
     * Como {@link #parse(String, Map, AngleUnit)}, y las integrales, derivadas y límites escritos dentro de la
     * expresión ({@code 1/int(f, x, 0, 1)}, {@code ∫_0^1 f dx}) se resuelven con los valores dados para las variables.
     * @throws ExpressionParseException si la expresión es inválida o si una de esas operaciones no se puede resolver
     */
    public static Expr parse(String source, Map<String, UserFunction> userFunctions, AngleUnit angleUnit,
                             Map<String, Expr> values) {
        return parseDetailed(source, userFunctions, angleUnit, values).expr();
    }

    /** Si el texto lleva integrales, derivadas o límites escritos dentro (que {@link #parse(String)} resuelve). */
    public static boolean hasNestedCalculus(String source) {
        return NestedCalculus.mentions(source);
    }

    /** Como {@link #parse(String)}, con los pasos de lo que se resolvió dentro del texto. */
    public static Parsed parseDetailed(String source) {
        return parseDetailed(source, Map.of(), AngleUnit.RADIANS, Map.of());
    }

    /**
     * La expresión interpretada y si alguna operación anidada se calculó solo de forma numérica.
     *
     * @param approximate {@code true} si la expresión lleva un valor decimal en lugar de uno exacto
     * @param steps       un paso por cada integral, derivada o límite anidado que se resolvió (vacío si no hay ninguno)
     */
    public record Parsed(Expr expr, boolean approximate, java.util.List<barnesinside.mathutils.cas.steps.Step> steps) {
    }

    /** Como {@link #parse(String, Map, AngleUnit, Map)}, indicando además si hubo que aproximar. */
    public static Parsed parseDetailed(String source, Map<String, UserFunction> userFunctions, AngleUnit angleUnit,
                                       Map<String, Expr> values) {
        NestedCalculus.Expansion nested = NestedCalculus.expand(source, true, values, userFunctions, angleUnit);
        Expr expr = ExpressionParser.parse(FunctionPowers.rewrite(nested.text()),
                new ExprBuilder(userFunctions, angleUnit, values.keySet()), true);
        return new Parsed(nested.apply(expr), nested.approximate(), nested.steps());
    }

    /**
     * Comprueba la sintaxis sin resolver las integrales, derivadas ni límites anidados: cada uno cuenta como un valor.
     * @throws ExpressionParseException si la expresión es inválida
     */
    public static void check(String source, Map<String, UserFunction> userFunctions, AngleUnit angleUnit) {
        NestedCalculus.Expansion nested = NestedCalculus.expand(source, false, Map.of(), userFunctions, angleUnit);
        ExpressionParser.parse(FunctionPowers.rewrite(nested.text()), new ExprBuilder(userFunctions, angleUnit), true);
    }

    /**
     * Como {@link #parse(String)} pero devuelve vacío en vez de lanzar la excepción.
     */
    public static Optional<Expr> tryParse(String source) {
        try {
            return Optional.of(parse(source));
        } catch (ExpressionParseException e) {
            return Optional.empty();
        }
    }
}
