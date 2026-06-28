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
 */
public final class ExpressionParser {

    private ExpressionParser() {
    }

    /**
     * Interpreta la expresión como un árbol matemático.
     * @throws ExpressionParseException si la expresión es inválida
     */
    public static MathElement parse(String source) {
        return build(source, toRpn(source));
    }

    /**
     * Interpreta la expresión como una {@link Function}.
     * @throws ExpressionParseException si la expresión es inválida
     */
    public static Function parseFunction(String source) {
        return new Function(parse(source));
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
        return toRpn(source).stream().map(Rpn::describe).toList();
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

    private static List<Rpn> toRpn(String source) {
        List<Token> tokens = tokenize(source);
        if (tokens.isEmpty()) throw new ExpressionParseException("La expresión está vacía", source, 0);

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
                    boolean isCall = idx + 1 < tokens.size() && tokens.get(idx + 1).type() == Token.Type.LEFT_PAREN;
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

    private static MathElement build(String source, List<Rpn> rpn) {
        Deque<MathElement> stack = new ArrayDeque<>();

        for (Rpn item : rpn) {
            switch (item.kind()) {
                case NUMBER -> {
                    try {
                        stack.push(new Const(Double.parseDouble(item.text())));
                    } catch (NumberFormatException e) {
                        throw new ExpressionParseException("Número inválido '" + item.text() + "'", source, item.position());
                    }
                }

                case VARIABLE -> {
                    if (item.text().equalsIgnoreCase("pi")) stack.push(new Const(Math.PI));
                    else if (item.text().equalsIgnoreCase("e")) stack.push(new Const(Math.E));
                    else stack.push(new Var(item.text()));
                }

                case NEGATE -> stack.push(new MultOperator(-1, stack.pop()));

                case BINARY -> {
                    MathElement right = stack.pop();
                    MathElement left = stack.pop();
                    stack.push(switch (item.text()) {
                        case "+" -> new SumOperator(left, right);
                        case "-" -> new SubOperator(left, right);
                        case "*" -> new MultOperator(left, right);
                        case "/" -> new DivOperator(left, right);
                        default -> new Pow(left, right);
                    });
                }

                case FUNCTION -> {
                    MathElement[] args = new MathElement[item.arity()];
                    for (int i = args.length - 1; i >= 0; i--) args[i] = stack.pop();
                    stack.push(buildFunction(source, item, args));
                }
            }
        }
        return stack.pop();
    }

    private static MathElement buildFunction(String source, Rpn item, MathElement[] args) {
        String name = item.text().toLowerCase();

        return switch (name) {
            case "sin" -> new Sin(arg(source, item, args, 1)[0]);
            case "cos" -> new Cos(arg(source, item, args, 1)[0]);
            case "tan" -> new Tan(arg(source, item, args, 1)[0]);
            case "cot" -> new Cot(arg(source, item, args, 1)[0]);
            case "sqrt" -> new Sqrt(arg(source, item, args, 1)[0]);
            case "cbrt" -> new Cbrt(arg(source, item, args, 1)[0]);
            case "ln" -> new LogN(arg(source, item, args, 1)[0]);
            case "exp" -> new Pow(Math.E, arg(source, item, args, 1)[0]);
            case "root" -> {
                MathElement[] a = arg(source, item, args, 2);
                yield new Root(a[0], a[1]);
            }
            case "pow" -> {
                MathElement[] a = arg(source, item, args, 2);
                yield new Pow(a[0], a[1]);
            }
            case "log" -> {
                if (args.length == 1) yield new Log(args[0], 10);
                MathElement[] a = arg(source, item, args, 2);
                yield new Log(a[0], a[1]);
            }
            default -> throw new ExpressionParseException("Función desconocida '" + item.text() + "'", source, item.position());
        };
    }

    private static MathElement[] arg(String source, Rpn item, MathElement[] args, int expected) {
        if (args.length != expected) {
            throw new ExpressionParseException(
                    "La función '" + item.text() + "' espera " + expected + " argumento(s) y recibió " + args.length,
                    source, item.position());
        }
        return args;
    }
}
