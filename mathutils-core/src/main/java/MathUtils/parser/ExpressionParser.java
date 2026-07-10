package MathUtils.parser;

import MathUtils.algebra.Const;
import MathUtils.algebra.Function;
import MathUtils.algebra.Var;
import MathUtils.algebra.exponentials.Cbrt;
import MathUtils.algebra.exponentials.Pow;
import MathUtils.algebra.exponentials.Root;
import MathUtils.algebra.exponentials.Sqrt;
import MathUtils.algebra.logarithmics.Log;
import MathUtils.algebra.logarithmics.LogN;
import MathUtils.algebra.operators.DivOperator;
import MathUtils.algebra.operators.MultOperator;
import MathUtils.algebra.operators.SubOperator;
import MathUtils.algebra.operators.SumOperator;
import MathUtils.algebra.primitives.MathElement;
import MathUtils.algebra.trigonometrics.Cos;
import MathUtils.algebra.trigonometrics.Cot;
import MathUtils.algebra.trigonometrics.Sin;
import MathUtils.algebra.trigonometrics.Tan;
import MathUtils.exceptions.ExpressionParseException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * Convierte texto en un árbol de {@link MathElement}.
 * <p>Proceso: tokenización, conversión de infijo a postfijo (Shunting Yard)
 * y construcción del árbol a partir de la notación postfija.
 * <ul>
 *   <li>Operadores: {@code + - * / ^} (la potencia es asociativa a la derecha y
 *       {@code -x^2} equivale a {@code -(x^2)}), y el signo unario.</li>
 *   <li>Constantes: {@code pi} y {@code e}. Cualquier otro identificador es una variable.</li>
 *   <li>Funciones: {@code sin cos tan cot sqrt cbrt ln exp} (un argumento),
 *       {@code log} (base 10, o {@code log(x, base)}), {@code root(x, n)} y {@code pow(x, y)}.</li>
 * </ul>
 * Los errores se reportan con {@link ExpressionParseException}, con línea y columna.
 * <p>El mismo análisis sintáctico se puede usar para construir otros tipos de árbol con
 * {@link #parse(String, TreeBuilder, boolean)}.
 */
public final class ExpressionParser {

    private ExpressionParser() {
    }

    /**
     * Interpreta la expresión como un árbol matemático.
     * @throws ExpressionParseException si la expresión es inválida
     */
    public static MathElement parse(String source) {
        return parse(source, new ElementBuilder(), false);
    }

    /**
     * Interpreta la expresión como una {@link Function}.
     * @throws ExpressionParseException si la expresión es inválida
     */
    public static Function parseFunction(String source) {
        return new Function(parse(source));
    }

    /**
     * Interpreta la expresión construyendo el árbol con el constructor dado.
     * @param implicitMultiplication si es {@code true}, acepta {@code 2x}, {@code 3(x+1)},
     *                               {@code (x+1)(x-1)} y {@code x sin(x)} como productos
     * @throws ExpressionParseException si la expresión es inválida
     */
    public static <T> T parse(String source, TreeBuilder<T> builder, boolean implicitMultiplication) {
        return build(source, toRpn(source, builder, implicitMultiplication), builder);
    }

    /**
     * Divide la expresión en tokens.
     * @throws ExpressionParseException si hay un carácter o número inválido
     */
    public static List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = source.length();

        while (i < n) {
            char c = source.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
            } else if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(source.charAt(i + 1)))) {
                int start = i;
                while (i < n && Character.isDigit(source.charAt(i))) i++;
                if (i < n && source.charAt(i) == '.') {
                    i++;
                    while (i < n && Character.isDigit(source.charAt(i))) i++;
                }
                // Notación científica: 2e3, 1.5E-2 (solo si tras la e viene un exponente completo)
                if (i < n && (source.charAt(i) == 'e' || source.charAt(i) == 'E')) {
                    int j = i + 1;
                    if (j < n && (source.charAt(j) == '+' || source.charAt(j) == '-')) j++;
                    if (j < n && Character.isDigit(source.charAt(j))) {
                        while (j < n && Character.isDigit(source.charAt(j))) j++;
                        i = j;
                    }
                }
                tokens.add(new Token(Token.Type.NUMBER, source.substring(start, i), start));
            } else if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(source.charAt(i)) || source.charAt(i) == '_')) i++;
                tokens.add(new Token(Token.Type.IDENTIFIER, source.substring(start, i), start));
            } else if ("+-*/^".indexOf(c) >= 0) {
                tokens.add(new Token(Token.Type.OPERATOR, String.valueOf(c), i++));
            } else if (c == '(') {
                tokens.add(new Token(Token.Type.LEFT_PAREN, "(", i++));
            } else if (c == ')') {
                tokens.add(new Token(Token.Type.RIGHT_PAREN, ")", i++));
            } else if (c == ',') {
                tokens.add(new Token(Token.Type.COMMA, ",", i++));
            } else {
                throw new ExpressionParseException("Carácter inesperado '" + c + "'", source, i);
            }
        }
        return tokens;
    }

    /**
     * Convierte la expresión a notación postfija (RPN) legible. Las funciones se indican
     * como {@code nombre/aridad} y el signo unario como {@code neg}.
     * @throws ExpressionParseException si la expresión es inválida
     */
    public static List<String> toPostfix(String source) {
        return toRpn(source, new ElementBuilder(), false).stream().map(Rpn::describe).toList();
    }

    /**
     * Inserta el operador {@code *} donde hay una multiplicación implícita:
     * entre un operando (número, identificador, {@code )}) y otro que empieza
     * (identificador, {@code (}, o número tras identificador o {@code )}).
     */
    private static List<Token> withImplicitMultiplication(List<Token> tokens, TreeBuilder<?> builder) {
        List<Token> result = new ArrayList<>();
        for (Token t : tokens) {
            if (!result.isEmpty()) {
                Token previous = result.get(result.size() - 1);
                if (endsOperand(previous) && startsOperand(t)
                        && !(previous.type() == Token.Type.NUMBER && t.type() == Token.Type.NUMBER)
                        && !(previous.type() == Token.Type.IDENTIFIER && builder.isFunction(previous.text())
                        && t.type() == Token.Type.LEFT_PAREN)) {
                    result.add(new Token(Token.Type.OPERATOR, "*", t.position()));
                }
            }
            result.add(t);
        }
        return result;
    }

    private static boolean endsOperand(Token t) {
        return t.type() == Token.Type.NUMBER || t.type() == Token.Type.IDENTIFIER || t.type() == Token.Type.RIGHT_PAREN;
    }

    private static boolean startsOperand(Token t) {
        return t.type() == Token.Type.NUMBER || t.type() == Token.Type.IDENTIFIER || t.type() == Token.Type.LEFT_PAREN;
    }

    // ---------------------------------------------------------------- Shunting Yard

    private enum Kind {NUMBER, VARIABLE, NEGATE, BINARY, FUNCTION}

    private record Rpn(Kind kind, String text, int position, int arity) {
        String describe() {
            return switch (kind) {
                case NEGATE -> "neg";
                case FUNCTION -> text + "/" + arity;
                default -> text;
            };
        }
    }

    private enum StackKind {BINARY, NEGATE, FUNCTION, LEFT_PAREN}

    private record StackItem(StackKind kind, String text, int position, boolean call) {
    }

    private static int precedence(StackItem item) {
        if (item.kind() == StackKind.NEGATE) return 4;
        return switch (item.text()) {
            case "+", "-" -> 2;
            case "*", "/" -> 3;
            default -> 5; // ^
        };
    }

    private static List<Rpn> toRpn(String source, TreeBuilder<?> builder, boolean implicit) {
        List<Token> tokens = tokenize(source);
        if (tokens.isEmpty()) throw new ExpressionParseException("La expresión está vacía", source, 0);
        if (implicit) tokens = withImplicitMultiplication(tokens, builder);

        List<Rpn> output = new ArrayList<>();
        Deque<StackItem> ops = new ArrayDeque<>();
        Deque<int[]> argCounts = new ArrayDeque<>();
        boolean expectOperand = true;

        for (int idx = 0; idx < tokens.size(); idx++) {
            Token t = tokens.get(idx);

            switch (t.type()) {
                case NUMBER -> {
                    if (!expectOperand) throw unexpected(source, t);
                    output.add(new Rpn(Kind.NUMBER, t.text(), t.position(), 0));
                    expectOperand = false;
                }

                case IDENTIFIER -> {
                    if (!expectOperand) throw unexpected(source, t);
                    boolean isCall = idx + 1 < tokens.size() && tokens.get(idx + 1).type() == Token.Type.LEFT_PAREN
                            && (!implicit || builder.isFunction(t.text()));
                    if (isCall) {
                        ops.push(new StackItem(StackKind.FUNCTION, t.text(), t.position(), true));
                    } else {
                        output.add(new Rpn(Kind.VARIABLE, t.text(), t.position(), 0));
                        expectOperand = false;
                    }
                }

                case OPERATOR -> {
                    if (expectOperand) {
                        if (t.text().equals("-")) {
                            ops.push(new StackItem(StackKind.NEGATE, "neg", t.position(), false));
                        } else if (!t.text().equals("+")) {
                            throw new ExpressionParseException(
                                    "Operador '" + t.text() + "' inesperado, se esperaba un operando", source, t.position());
                        }
                    } else {
                        StackItem current = new StackItem(StackKind.BINARY, t.text(), t.position(), false);
                        boolean rightAssociative = t.text().equals("^");
                        while (!ops.isEmpty()
                                && (ops.peek().kind() == StackKind.BINARY || ops.peek().kind() == StackKind.NEGATE)) {
                            int top = precedence(ops.peek());
                            int cur = precedence(current);
                            if (top > cur || (top == cur && !rightAssociative)) {
                                output.add(toRpnItem(ops.pop()));
                            } else {
                                break;
                            }
                        }
                        ops.push(current);
                        expectOperand = true;
                    }
                }

                case LEFT_PAREN -> {
                    if (!expectOperand) throw unexpected(source, t);
                    boolean call = !ops.isEmpty() && ops.peek().kind() == StackKind.FUNCTION;
                    ops.push(new StackItem(StackKind.LEFT_PAREN, "(", t.position(), call));
                    if (call) {
                        boolean noArgs = idx + 1 < tokens.size()
                                && tokens.get(idx + 1).type() == Token.Type.RIGHT_PAREN;
                        argCounts.push(new int[]{noArgs ? 0 : 1});
                    }
                    expectOperand = true;
                }

                case COMMA -> {
                    if (expectOperand) {
                        throw new ExpressionParseException("Se esperaba un argumento antes de ','", source, t.position());
                    }
                    while (!ops.isEmpty() && ops.peek().kind() != StackKind.LEFT_PAREN) {
                        output.add(toRpnItem(ops.pop()));
                    }
                    if (ops.isEmpty() || !ops.peek().call()) {
                        throw new ExpressionParseException("',' fuera de una llamada a función", source, t.position());
                    }
                    argCounts.peek()[0]++;
                    expectOperand = true;
                }

                case RIGHT_PAREN -> {
                    if (expectOperand) {
                        boolean emptyCall = !ops.isEmpty() && ops.peek().kind() == StackKind.LEFT_PAREN
                                && ops.peek().call() && argCounts.peek()[0] == 0;
                        if (!emptyCall) {
                            throw new ExpressionParseException("Se esperaba una expresión antes de ')'", source, t.position());
                        }
                    }
                    while (!ops.isEmpty() && ops.peek().kind() != StackKind.LEFT_PAREN) {
                        output.add(toRpnItem(ops.pop()));
                    }
                    if (ops.isEmpty()) {
                        throw new ExpressionParseException("')' sin su '(' correspondiente", source, t.position());
                    }
                    StackItem paren = ops.pop();
                    if (paren.call()) {
                        StackItem function = ops.pop();
                        output.add(new Rpn(Kind.FUNCTION, function.text(), function.position(), argCounts.pop()[0]));
                    }
                    expectOperand = false;
                }
            }
        }

        if (expectOperand) {
            throw new ExpressionParseException("La expresión termina de forma inesperada", source, source.length());
        }

        while (!ops.isEmpty()) {
            StackItem item = ops.pop();
            if (item.kind() == StackKind.LEFT_PAREN) {
                throw new ExpressionParseException("'(' sin su ')' correspondiente", source, item.position());
            }
            output.add(toRpnItem(item));
        }
        return output;
    }

    private static Rpn toRpnItem(StackItem item) {
        return new Rpn(item.kind() == StackKind.NEGATE ? Kind.NEGATE : Kind.BINARY, item.text(), item.position(), 0);
    }

    private static ExpressionParseException unexpected(String source, Token t) {
        return new ExpressionParseException("'" + t.text() + "' inesperado, se esperaba un operador", source, t.position());
    }

    // ---------------------------------------------------------------- Árbol

    private static <T> T build(String source, List<Rpn> rpn, TreeBuilder<T> builder) {
        Deque<T> stack = new ArrayDeque<>();

        for (Rpn item : rpn) {
            try {
                switch (item.kind()) {
                    case NUMBER -> stack.push(builder.number(item.text()));
                    case VARIABLE -> stack.push(builder.variable(item.text()));
                    case NEGATE -> stack.push(builder.negate(stack.pop()));
                    case BINARY -> {
                        T right = stack.pop();
                        T left = stack.pop();
                        stack.push(builder.binary(item.text().charAt(0), left, right));
                    }
                    case FUNCTION -> {
                        List<T> args = new ArrayList<>(item.arity());
                        for (int i = 0; i < item.arity(); i++) args.add(0, stack.pop());
                        stack.push(builder.function(item.text(), args));
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new ExpressionParseException(e.getMessage(), source, item.position());
            }
        }
        return stack.pop();
    }

    // ---------------------------------------------------------------- Constructor de MathElement

    /** Construye árboles {@link MathElement} con las clases de {@code MathUtils.algebra}. */
    private static final class ElementBuilder implements TreeBuilder<MathElement> {

        private static final Set<String> FUNCTIONS =
                Set.of("sin", "cos", "tan", "cot", "sqrt", "cbrt", "ln", "exp", "log", "root", "pow");

        @Override
        public MathElement number(String text) {
            try {
                return new Const(Double.parseDouble(text));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Número inválido '" + text + "'");
            }
        }

        @Override
        public MathElement variable(String name) {
            if (name.equalsIgnoreCase("pi")) return new Const(Math.PI);
            if (name.equalsIgnoreCase("e")) return new Const(Math.E);
            return new Var(name);
        }

        @Override
        public MathElement negate(MathElement operand) {
            return new MultOperator(-1, operand);
        }

        @Override
        public MathElement binary(char operator, MathElement left, MathElement right) {
            return switch (operator) {
                case '+' -> new SumOperator(left, right);
                case '-' -> new SubOperator(left, right);
                case '*' -> new MultOperator(left, right);
                case '/' -> new DivOperator(left, right);
                default -> new Pow(left, right);
            };
        }

        @Override
        public MathElement function(String name, List<MathElement> args) {
            return switch (name.toLowerCase()) {
                case "sin" -> new Sin(args(name, args, 1).get(0));
                case "cos" -> new Cos(args(name, args, 1).get(0));
                case "tan" -> new Tan(args(name, args, 1).get(0));
                case "cot" -> new Cot(args(name, args, 1).get(0));
                case "sqrt" -> new Sqrt(args(name, args, 1).get(0));
                case "cbrt" -> new Cbrt(args(name, args, 1).get(0));
                case "ln" -> new LogN(args(name, args, 1).get(0));
                case "exp" -> new Pow(Math.E, args(name, args, 1).get(0));
                case "root" -> {
                    List<MathElement> a = args(name, args, 2);
                    yield new Root(a.get(0), a.get(1));
                }
                case "pow" -> {
                    List<MathElement> a = args(name, args, 2);
                    yield new Pow(a.get(0), a.get(1));
                }
                case "log" -> {
                    if (args.size() == 1) yield new Log(args.get(0), 10);
                    List<MathElement> a = args(name, args, 2);
                    yield new Log(a.get(0), a.get(1));
                }
                default -> throw new IllegalArgumentException("Función desconocida '" + name + "'");
            };
        }

        @Override
        public boolean isFunction(String name) {
            return FUNCTIONS.contains(name.toLowerCase());
        }

        private static List<MathElement> args(String name, List<MathElement> args, int expected) {
            if (args.size() != expected) {
                throw new IllegalArgumentException(
                        "La función '" + name + "' espera " + expected + " argumento(s) y recibió " + args.size());
            }
            return args;
        }
    }
}
