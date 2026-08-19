package barnesinside.mathutils.cas.syntax;

import barnesinside.mathutils.cas.expr.Fn;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.parser.ExpressionParser;
import barnesinside.mathutils.parser.TreeBuilder;

import java.util.List;
import java.util.Set;

/**
 * Interpreta texto como un {@link Syntax} sin normalizar.
 * Acepta la misma sintaxis que {@link barnesinside.mathutils.cas.expr.ExprParser}, incluida la multiplicación implícita.
 */
public final class SyntaxParser {

    private SyntaxParser() {
    }

    /**
     * @throws barnesinside.mathutils.exceptions.ExpressionParseException si la expresión es inválida
     */
    public static Syntax parse(String source) {
        return parse(source, Set.of());
    }

    /**
     * @param extraFunctions nombres de funciones adicionales (por ejemplo, definidas por el usuario)
     *                       que se aceptan sin comprobar su cantidad de argumentos
     * @throws barnesinside.mathutils.exceptions.ExpressionParseException si la expresión es inválida
     */
    public static Syntax parse(String source, Set<String> extraFunctions) {
        return ExpressionParser.parse(source, new Builder(extraFunctions), true);
    }

    private static final class Builder implements TreeBuilder<Syntax> {

        private static final Set<String> SPECIAL = Set.of("sqrt", "cbrt", "exp", "log", "root", "pow");
        private final Set<String> extraFunctions;

        Builder(Set<String> extraFunctions) {
            this.extraFunctions = extraFunctions;
        }

        @Override
        public Syntax number(String text) {
            try {
                Rational exact = Rational.parse(text);
                return new Syntax.Num(text, exact, exact.doubleValue());
            } catch (NumberFormatException | ArithmeticException e) {
                throw new IllegalArgumentException("Número inválido '" + text + "'");
            }
        }

        @Override
        public Syntax variable(String name) {
            String lower = name.toLowerCase();
            if (lower.equals("pi") || name.equals("π")) return new Syntax.Const("pi", Math.PI);
            if (name.equals("e") || name.equals("E")) return new Syntax.Const("e", Math.E);
            if (isFunction(name)) throw new IllegalArgumentException("Falta '(' tras la función '" + name + "'");
            return new Syntax.Var(name);
        }

        @Override
        public Syntax negate(Syntax operand) {
            return new Syntax.Neg(operand);
        }

        @Override
        public Syntax binary(char operator, Syntax left, Syntax right) {
            return new Syntax.Bin(operator, left, right);
        }

        @Override
        public Syntax function(String name, List<Syntax> args) {
            if (extraFunctions.contains(name)) return new Syntax.Call(name, args);

            String lower = name.toLowerCase();
            int arity = switch (lower) {
                case "root", "pow" -> 2;
                case "log" -> args.size() == 2 ? 2 : 1;
                default -> {
                    if (SPECIAL.contains(lower) || Fn.bySymbol(name).isPresent()) yield 1;
                    throw new IllegalArgumentException("Función desconocida '" + name + "'");
                }
            };
            if (args.size() != arity) {
                throw new IllegalArgumentException(
                        "La función '" + name + "' espera " + arity + " argumento(s) y recibió " + args.size());
            }
            return new Syntax.Call(Fn.bySymbol(name).map(Fn::symbol).orElse(lower), args);
        }

        @Override
        public Syntax group(Syntax inner) {
            return new Syntax.Group(inner);
        }

        @Override
        public boolean isFunction(String name) {
            return extraFunctions.contains(name) || SPECIAL.contains(name.toLowerCase()) || Fn.bySymbol(name).isPresent();
        }
    }
}
