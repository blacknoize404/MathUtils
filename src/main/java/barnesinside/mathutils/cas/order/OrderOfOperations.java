package barnesinside.mathutils.cas.order;

import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprBuilder;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.steps.Messages;
import barnesinside.mathutils.cas.syntax.Syntax;
import barnesinside.mathutils.cas.syntax.SyntaxParser;
import barnesinside.mathutils.cas.syntax.SyntaxPrinter;
import barnesinside.mathutils.cas.syntax.SyntaxTools;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resuelve una expresión numérica en pequeños pasos siguiendo la jerarquía de operaciones:
 * primero lo que hay entre paréntesis (el grupo más interno, de izquierda a derecha); dentro de
 * cada grupo, potencias, funciones y cambios de signo, después multiplicaciones y divisiones y,
 * por último, sumas y restas, siempre de izquierda a derecha.
 * <p>Las operaciones se hacen con aritmética exacta cuando es posible; si no (por ejemplo
 * {@code sin(1)}), el resultado es una aproximación decimal. Los trozos que contienen variables
 * o constantes simbólicas ({@code pi}, {@code e}) no se resuelven: la reducción se detiene
 * cuando no queda ninguna operación entre números.
 */
public final class OrderOfOperations {

    /** Operación que se resuelve en un paso. */
    public enum Operation {
        POWER(3), FUNCTION(3), NEGATION(3), MULTIPLICATION(2), DIVISION(2), ADDITION(1), SUBTRACTION(1);

        private final int level;

        Operation(int level) {
            this.level = level;
        }

        /** Nivel de jerarquía: 3 potencias y funciones, 2 productos y cocientes, 1 sumas y restas. */
        public int level() {
            return level;
        }
    }

    /** Razón por la que la reducción se detuvo antes de terminar. */
    public enum Problem {DIVISION_BY_ZERO, UNDEFINED}

    /**
     * Un paso de la reducción.
     *
     * @param number              número de paso (desde 1)
     * @param operation           operación resuelta
     * @param insideParentheses   si la operación estaba dentro de un paréntesis
     * @param before              expresión antes del paso
     * @param after               expresión después del paso
     * @param redex               subárbol de {@code before} que se resuelve
     * @param result              número de {@code after} que lo sustituye
     * @param nodeId              identificador (en el árbol original) de la operación resuelta
     * @param params              operandos y resultado como texto ({@code a}, {@code b}, {@code r}, {@code f})
     */
    public record OrderStep(int number, Operation operation, boolean insideParentheses, Syntax before, Syntax after,
                            Syntax redex, Syntax result, int nodeId, Map<String, String> params) {

        public OrderStep {
            params = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(params));
        }

        /** Título corto de la operación ("Multiplicación"). */
        public String title(Locale locale) {
            return Messages.get("order." + operation.name() + ".title", locale);
        }

        /** Explicación del paso, incluida la razón por la que se hace en este orden. */
        public String explanation(Locale locale) {
            String text = Messages.interpolateText(Messages.get("order." + operation.name() + ".text", locale), params);
            return insideParentheses ? Messages.get("order.parentheses", locale) + " " + text : text;
        }

        /** Expresión anterior con la operación resuelta resaltada. */
        public List<SyntaxPrinter.Piece> beforePieces() {
            return SyntaxPrinter.pieces(before, redex);
        }

        /** Expresión posterior con el resultado resaltado. */
        public List<SyntaxPrinter.Piece> afterPieces() {
            return SyntaxPrinter.pieces(after, result);
        }
    }

    /**
     * Resultado completo de la reducción.
     *
     * @param original        árbol original
     * @param steps           pasos en orden
     * @param result          expresión final (un número si se pudo resolver por completo)
     * @param problem         razón por la que se detuvo, o {@code null}
     * @param evaluationOrder para cada operación del árbol original (por su identificador), en qué
     *                        paso se resuelve; las operaciones no resueltas no aparecen
     */
    public record Reduction(Syntax original, List<OrderStep> steps, Syntax result, Problem problem,
                            Map<Integer, Integer> evaluationOrder) {

        public Reduction {
            steps = List.copyOf(steps);
            evaluationOrder = Map.copyOf(evaluationOrder);
        }

        /** Verdadero si la expresión quedó reducida a un único número. */
        public boolean isFullyEvaluated() {
            return problem == null && result instanceof Syntax.Num;
        }

        public String problemMessage(Locale locale) {
            return problem == null ? null : Messages.get("order.error." + problem.name(), locale);
        }
    }

    private static final int MAX_STEPS = 10_000;

    private OrderOfOperations() {
    }

    /**
     * @throws barnesinside.mathutils.exceptions.ExpressionParseException si el texto no es una expresión válida
     */
    public static Reduction reduce(String source) {
        return reduce(SyntaxParser.parse(source));
    }

    public static Reduction reduce(Syntax original) {
        Map<Syntax, Integer> ids = new IdentityHashMap<>(SyntaxTools.assignIds(original));
        Syntax current = unwrap(original, ids);

        List<OrderStep> steps = new ArrayList<>();
        Map<Integer, Integer> order = new LinkedHashMap<>();
        Problem problem = null;

        while (steps.size() < MAX_STEPS) {
            Ready next = chooseNext(current);
            if (next == null) break;

            Syntax.Num value;
            try {
                value = evaluate(next.node());
            } catch (ArithmeticException e) {
                problem = e.getMessage() != null && e.getMessage().contains("cero") ? Problem.DIVISION_BY_ZERO : Problem.UNDEFINED;
                break;
            }

            Syntax after = replace(current, next.node(), value, ids);
            int number = steps.size() + 1;
            int nodeId = ids.get(next.node());
            order.put(nodeId, number);
            steps.add(new OrderStep(number, operationOf(next.node()), next.group() != null, current, after,
                    next.node(), value, nodeId, paramsOf(next.node(), value)));
            current = after;
        }
        return new Reduction(original, steps, current, problem, order);
    }

    // ---------------------------------------------------------------- Selección de la siguiente operación

    private record Ready(Syntax node, Syntax.Group group) {
    }

    private static Ready chooseNext(Syntax tree) {
        List<Ready> ready = new ArrayList<>();
        collect(tree, null, ready);
        if (ready.isEmpty()) return null;

        // Paréntesis primero: el grupo más interno (sin otro grupo con operaciones dentro), el de más a la izquierda
        List<Syntax.Group> groupsWithReady = new ArrayList<>();
        for (Ready r : ready) if (r.group() != null && !containsIdentity(groupsWithReady, r.group())) groupsWithReady.add(r.group());

        Syntax.Group scope = null;
        for (Syntax.Group candidate : groupsWithReady) {
            boolean innermost = groupsWithReady.stream().noneMatch(other -> other != candidate && contains(candidate, other));
            if (innermost) {
                scope = candidate;
                break;
            }
        }

        Ready best = null;
        for (Ready r : ready) {
            if (r.group() != scope) continue;
            if (best == null || operationOf(r.node()).level() > operationOf(best.node()).level()) best = r;
        }
        return best;
    }

    private static void collect(Syntax node, Syntax.Group group, List<Ready> out) {
        switch (node) {
            case Syntax.Group g -> collect(g.inner(), g, out);
            case Syntax.Neg n -> {
                collect(n.operand(), group, out);
                if (n.operand() instanceof Syntax.Num) out.add(new Ready(n, group));
            }
            case Syntax.Bin b -> {
                collect(b.left(), group, out);
                if (b.left() instanceof Syntax.Num && b.right() instanceof Syntax.Num) out.add(new Ready(b, group));
                collect(b.right(), group, out);
            }
            case Syntax.Call c -> {
                for (Syntax arg : c.args()) collect(arg, group, out);
                if (c.args().stream().allMatch(a -> a instanceof Syntax.Num)) out.add(new Ready(c, group));
            }
            default -> { }
        }
    }

    private static boolean containsIdentity(List<Syntax.Group> list, Syntax.Group g) {
        for (Syntax.Group x : list) if (x == g) return true;
        return false;
    }

    /** Indica si {@code target} aparece (por identidad) dentro de {@code outer}. */
    private static boolean contains(Syntax outer, Syntax target) {
        if (outer == target) return true;
        for (Syntax child : SyntaxTools.children(outer)) if (contains(child, target)) return true;
        return false;
    }

    private static Operation operationOf(Syntax node) {
        return switch (node) {
            case Syntax.Neg n -> Operation.NEGATION;
            case Syntax.Call c -> Operation.FUNCTION;
            case Syntax.Bin b -> switch (b.op()) {
                case '^' -> Operation.POWER;
                case '*' -> Operation.MULTIPLICATION;
                case '/' -> Operation.DIVISION;
                case '+' -> Operation.ADDITION;
                default -> Operation.SUBTRACTION;
            };
            default -> throw new IllegalStateException("No es una operación: " + node);
        };
    }

    // ---------------------------------------------------------------- Evaluación

    private static Syntax.Num evaluate(Syntax node) {
        return switch (node) {
            case Syntax.Neg n -> {
                Syntax.Num a = (Syntax.Num) n.operand();
                yield a.isExact() ? Syntax.Num.of(a.exact().negate()) : Syntax.Num.approximate(-a.approx());
            }
            case Syntax.Bin b -> binary(b.op(), (Syntax.Num) b.left(), (Syntax.Num) b.right());
            case Syntax.Call c -> call(c);
            default -> throw new IllegalStateException("No se puede evaluar " + node);
        };
    }

    private static Syntax.Num binary(char op, Syntax.Num a, Syntax.Num b) {
        if (a.isExact() && b.isExact()) {
            Expr x = Expr.num(a.exact());
            Expr y = Expr.num(b.exact());
            Expr result = switch (op) {
                case '+' -> Expr.add(x, y);
                case '-' -> Expr.sub(x, y);
                case '*' -> Expr.mul(x, y);
                case '/' -> Expr.div(x, y);
                default -> Expr.pow(x, y);
            };
            return fromExpr(result);
        }

        double x = a.approx();
        double y = b.approx();
        double result = switch (op) {
            case '+' -> x + y;
            case '-' -> x - y;
            case '*' -> x * y;
            case '/' -> {
                if (y == 0) throw new ArithmeticException("División por cero");
                yield x / y;
            }
            default -> Math.pow(x, y);
        };
        return approximate(result);
    }

    private static Syntax.Num call(Syntax.Call c) {
        boolean exact = c.args().stream().allMatch(a -> ((Syntax.Num) a).isExact());
        List<Expr> args = new ArrayList<>();
        for (Syntax a : c.args()) {
            Syntax.Num n = (Syntax.Num) a;
            args.add(Expr.num(n.isExact() ? n.exact() : Rational.fromDouble(n.approx())));
        }

        Expr result;
        try {
            result = new ExprBuilder().function(c.name(), args);
        } catch (IllegalArgumentException e) {
            throw new ArithmeticException("División por cero");
        }
        if (exact) return fromExpr(result);
        return approximate(Eval.value(result));
    }

    /** Número exacto si la expresión se redujo a un racional; si no, su aproximación decimal. */
    private static Syntax.Num fromExpr(Expr result) {
        if (result instanceof Expr.Num n) return Syntax.Num.of(n.value());
        return approximate(Eval.value(result));
    }

    private static Syntax.Num approximate(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) throw new ArithmeticException("Resultado no definido");
        return Syntax.Num.approximate(value);
    }

    // ---------------------------------------------------------------- Reconstrucción del árbol

    /** Sustituye {@code target} por {@code replacement}, quitando los paréntesis que quedan alrededor de un número. */
    private static Syntax replace(Syntax root, Syntax target, Syntax replacement, Map<Syntax, Integer> ids) {
        if (root == target) return replacement;

        Syntax rebuilt = switch (root) {
            case Syntax.Neg n -> {
                Syntax o = replace(n.operand(), target, replacement, ids);
                yield o == n.operand() ? n : new Syntax.Neg(o);
            }
            case Syntax.Bin b -> {
                Syntax l = replace(b.left(), target, replacement, ids);
                Syntax r = replace(b.right(), target, replacement, ids);
                yield l == b.left() && r == b.right() ? b : new Syntax.Bin(b.op(), l, r);
            }
            case Syntax.Call c -> {
                List<Syntax> args = new ArrayList<>();
                boolean changed = false;
                for (Syntax a : c.args()) {
                    Syntax replaced = replace(a, target, replacement, ids);
                    changed |= replaced != a;
                    args.add(replaced);
                }
                yield changed ? new Syntax.Call(c.name(), args) : c;
            }
            case Syntax.Group g -> {
                Syntax inner = replace(g.inner(), target, replacement, ids);
                if (inner == g.inner()) yield g;
                yield inner instanceof Syntax.Num ? inner : new Syntax.Group(inner);
            }
            default -> root;
        };
        if (rebuilt != root) ids.put(rebuilt, ids.get(root));
        return rebuilt;
    }

    /** Quita los paréntesis que rodean directamente a un número. */
    private static Syntax unwrap(Syntax node, Map<Syntax, Integer> ids) {
        Syntax rebuilt = switch (node) {
            case Syntax.Neg n -> {
                Syntax o = unwrap(n.operand(), ids);
                yield o == n.operand() ? n : new Syntax.Neg(o);
            }
            case Syntax.Bin b -> {
                Syntax l = unwrap(b.left(), ids);
                Syntax r = unwrap(b.right(), ids);
                yield l == b.left() && r == b.right() ? b : new Syntax.Bin(b.op(), l, r);
            }
            case Syntax.Call c -> {
                List<Syntax> args = new ArrayList<>();
                boolean changed = false;
                for (Syntax a : c.args()) {
                    Syntax u = unwrap(a, ids);
                    changed |= u != a;
                    args.add(u);
                }
                yield changed ? new Syntax.Call(c.name(), args) : c;
            }
            case Syntax.Group g -> {
                Syntax inner = unwrap(g.inner(), ids);
                if (inner instanceof Syntax.Num) yield inner;
                yield inner == g.inner() ? g : new Syntax.Group(inner);
            }
            default -> node;
        };
        if (rebuilt != node && !(node instanceof Syntax.Group)) ids.put(rebuilt, ids.get(node));
        return rebuilt;
    }

    // ---------------------------------------------------------------- Textos de los operandos

    private static Map<String, String> paramsOf(Syntax node, Syntax.Num result) {
        Map<String, String> params = new LinkedHashMap<>();
        switch (node) {
            case Syntax.Neg n -> params.put("a", operand((Syntax.Num) n.operand()));
            case Syntax.Bin b -> {
                params.put("a", operand((Syntax.Num) b.left()));
                params.put("b", operand((Syntax.Num) b.right()));
            }
            case Syntax.Call c -> {
                params.put("f", c.name());
                StringBuilder args = new StringBuilder();
                for (int i = 0; i < c.args().size(); i++) {
                    if (i > 0) args.append(", ");
                    args.append(((Syntax.Num) c.args().get(i)).text());
                }
                params.put("a", args.toString());
            }
            default -> { }
        }
        params.put("r", result.text());
        return params;
    }

    private static String operand(Syntax.Num n) {
        return n.needsParentheses() ? "(" + n.text() + ")" : n.text();
    }
}
