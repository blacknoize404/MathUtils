package MathUtils.cas.expr;

import MathUtils.exceptions.ExpressionParseException;
import MathUtils.parser.ExpressionParser;

import java.util.Map;
import java.util.Optional;

/**
 * Interpreta texto como una expresión simbólica {@link Expr} en forma canónica.
 * <p>Además de la sintaxis de {@link ExpressionParser}, acepta multiplicación implícita
 * ({@code 2x}, {@code 3(x+1)}, {@code x sin(x)}), las constantes {@code pi}/{@code π},
 * {@code e} y {@code oo}, y las funciones {@code sin cos tan cot sec csc asin acos atan sinh cosh
 * tanh ln log sqrt cbrt exp abs erf root pow} (con los alias {@code arcsin}, {@code arctan}…).
 */
public final class ExprParser {

    private ExprParser() {
    }

    /**
     * @throws ExpressionParseException si la expresión es inválida (con línea y columna)
     */
    public static Expr parse(String source) {
        return ExpressionParser.parse(source, new ExprBuilder(), true);
    }

    /**
     * Interpreta el texto con funciones definidas por el usuario y una unidad de ángulo.
     * @throws ExpressionParseException si la expresión es inválida
     */
    public static Expr parse(String source, Map<String, UserFunction> userFunctions, AngleUnit angleUnit) {
        return ExpressionParser.parse(source, new ExprBuilder(userFunctions, angleUnit), true);
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
