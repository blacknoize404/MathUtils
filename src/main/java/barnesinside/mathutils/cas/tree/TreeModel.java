package barnesinside.mathutils.cas.tree;

import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprBuilder;
import barnesinside.mathutils.cas.expr.Fn;
import barnesinside.mathutils.cas.order.OrderOfOperations;
import barnesinside.mathutils.cas.syntax.Syntax;
import barnesinside.mathutils.cas.syntax.SyntaxParser;
import barnesinside.mathutils.cas.syntax.SyntaxPrinter;
import barnesinside.mathutils.cas.syntax.SyntaxTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Árbol de una expresión listo para visualizar: cada nodo con su tipo, precedencia, valor numérico
 * y el paso del orden de operaciones en que se resuelve.
 * <p>{@link #of(String)} conserva la estructura tal como se escribió ({@code 2 + 3*4} es una suma
 * cuyo segundo operando es un producto); {@link #ofExpr(Expr)} muestra la forma canónica simbólica.
 */
public final class TreeModel {

    private static final int LEAF_PRECEDENCE = 6;

    private final TreeNode root;
    private final List<TreeNode> nodes;

    private TreeModel(TreeNode root) {
        this.root = root;
        List<TreeNode> all = new ArrayList<>();
        preorder(root, all);
        this.nodes = List.copyOf(all);
    }

    // ================================================================ Construcción

    /**
     * @throws barnesinside.mathutils.exceptions.ExpressionParseException si el texto no es una expresión válida
     */
    public static TreeModel of(String source) {
        return of(source, Map.of());
    }

    /**
     * @param variables valores de las variables, para calcular el valor de cada subárbol
     * @throws barnesinside.mathutils.exceptions.ExpressionParseException si el texto no es una expresión válida
     */
    public static TreeModel of(String source, Map<String, Double> variables) {
        return of(SyntaxParser.parse(source), variables);
    }

    public static TreeModel of(Syntax syntax, Map<String, Double> variables) {
        Map<Syntax, Integer> ids = SyntaxTools.assignIds(syntax);
        Map<Integer, Integer> order = OrderOfOperations.reduce(syntax).evaluationOrder();
        return new TreeModel(fromSyntax(syntax, 0, ids, order, variables));
    }

    /**
     * Árbol de la forma canónica de una expresión simbólica.
     */
    public static TreeModel ofExpr(Expr expr, Map<String, Double> variables) {
        return new TreeModel(fromExpr(expr, new int[]{0}, variables));
    }

    public static TreeModel ofExpr(Expr expr) {
        return ofExpr(expr, Map.of());
    }

    // ================================================================ Consulta

    public TreeNode root() {
        return root;
    }

    /** Todos los nodos en preorden. */
    public List<TreeNode> nodes() {
        return nodes;
    }

    public int size() {
        return nodes.size();
    }

    public Optional<TreeNode> find(int id) {
        return nodes.stream().filter(n -> n.id() == id).findFirst();
    }

    /** Profundidad del árbol: 1 si solo hay un nodo. */
    public int depth() {
        return depth(root);
    }

    private static int depth(TreeNode n) {
        int max = 0;
        for (TreeNode c : n.children()) max = Math.max(max, depth(c));
        return max + 1;
    }

    private static void preorder(TreeNode n, List<TreeNode> out) {
        out.add(n);
        for (TreeNode c : n.children()) preorder(c, out);
    }

    /** Exportación a JSON para otras interfaces. */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        json(root, sb);
        return sb.toString();
    }

    private static void json(TreeNode n, StringBuilder sb) {
        sb.append("{\"id\":").append(n.id());
        sb.append(",\"kind\":\"").append(n.kind()).append('"');
        sb.append(",\"label\":").append(quote(n.label()));
        sb.append(",\"latex\":").append(quote(n.latex()));
        sb.append(",\"precedence\":").append(n.precedence());
        sb.append(",\"associativity\":").append(n.associativity() == null ? "null" : "\"" + n.associativity() + "\"");
        sb.append(",\"parentheses\":").append(n.parentheses());
        sb.append(",\"value\":").append(n.value() == null ? "null" : Double.toString(n.value()));
        sb.append(",\"text\":").append(quote(n.text()));
        sb.append(",\"evaluationOrder\":").append(n.evaluationOrder() == null ? "null" : n.evaluationOrder().toString());
        sb.append(",\"children\":[");
        for (int i = 0; i < n.children().size(); i++) {
            if (i > 0) sb.append(',');
            json(n.children().get(i), sb);
        }
        sb.append("]}");
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    // ================================================================ Desde Syntax

    private static TreeNode fromSyntax(Syntax node, int parentheses, Map<Syntax, Integer> ids,
                                       Map<Integer, Integer> order, Map<String, Double> variables) {
        if (node instanceof Syntax.Group g) return fromSyntax(g.inner(), parentheses + 1, ids, order, variables);

        int id = ids.get(node);
        List<TreeNode> children = new ArrayList<>();
        for (Syntax child : SyntaxTools.children(node)) children.add(fromSyntax(child, 0, ids, order, variables));

        String text = SyntaxPrinter.print(node);
        Double value = valueOf(node, variables);
        Integer evaluationOrder = order.get(id);

        return switch (node) {
            case Syntax.Num n -> new TreeNode(id, TreeNode.Kind.NUMBER, n.text(), n.text(), LEAF_PRECEDENCE, null,
                    parentheses, value, text, null, children);
            case Syntax.Var v -> new TreeNode(id, TreeNode.Kind.VARIABLE, v.name(), v.name(), LEAF_PRECEDENCE, null,
                    parentheses, value, text, null, children);
            case Syntax.Const c -> new TreeNode(id, TreeNode.Kind.CONSTANT, c.name().equals("pi") ? "π" : "e",
                    c.name().equals("pi") ? "\\pi" : "e", LEAF_PRECEDENCE, null, parentheses, value, text, null, children);
            case Syntax.Neg n -> new TreeNode(id, TreeNode.Kind.NEGATION, "−", "-", SyntaxTools.NEGATION_PRECEDENCE, null,
                    parentheses, value, text, evaluationOrder, children);
            case Syntax.Bin b -> new TreeNode(id, TreeNode.Kind.OPERATOR, operatorLabel(b.op()), operatorLatex(b.op()),
                    SyntaxTools.precedence(b.op()), SyntaxTools.associativity(b.op()), parentheses, value, text,
                    evaluationOrder, children);
            case Syntax.Call c -> new TreeNode(id, TreeNode.Kind.FUNCTION, c.name(), "\\operatorname{" + c.name() + "}",
                    SyntaxTools.FUNCTION_PRECEDENCE, null, parentheses, value, text, evaluationOrder, children);
            case Syntax.Group g -> throw new IllegalStateException("Los grupos se aplanan antes");
        };
    }

    private static String operatorLabel(char op) {
        return switch (op) {
            case '*' -> "×";
            case '/' -> "÷";
            case '-' -> "−";
            default -> String.valueOf(op);
        };
    }

    private static String operatorLatex(char op) {
        return switch (op) {
            case '*' -> "\\times";
            case '/' -> "\\div";
            default -> String.valueOf(op);
        };
    }

    private static Double valueOf(Syntax node, Map<String, Double> variables) {
        try {
            double v = Eval.value(SyntaxTools.toExpr(node, new ExprBuilder()), variables);
            return Double.isFinite(v) ? v : null;
        } catch (ArithmeticException | IllegalArgumentException e) {
            return null;
        }
    }

    // ================================================================ Desde Expr

    private static TreeNode fromExpr(Expr e, int[] counter, Map<String, Double> variables) {
        int id = counter[0]++;
        String text = e.toString();
        Double value = exprValue(e, variables);

        return switch (e) {
            case Expr.Num n -> new TreeNode(id, TreeNode.Kind.NUMBER, n.value().toString(), e.toLatex(), LEAF_PRECEDENCE,
                    null, 0, value, text, null, List.of());
            case Expr.Sym s -> new TreeNode(id, TreeNode.Kind.VARIABLE, s.name(), e.toLatex(), LEAF_PRECEDENCE,
                    null, 0, value, text, null, List.of());
            case Expr.Const c -> new TreeNode(id, TreeNode.Kind.CONSTANT, c.kind() == Expr.Const.Kind.PI ? "π"
                    : c.kind() == Expr.Const.Kind.E ? "e" : "∞", e.toLatex(), LEAF_PRECEDENCE, null, 0, value, text, null, List.of());
            case Expr.Add a -> branch(id, "+", "+", 1, null, a.terms(), counter, variables, value, text);
            case Expr.Mul m -> branch(id, "×", "\\times", 2, SyntaxTools.Associativity.LEFT, m.factors(), counter, variables, value, text);
            case Expr.Pow p -> branch(id, "^", "^", 4, SyntaxTools.Associativity.RIGHT, List.of(p.base(), p.exponent()), counter, variables, value, text);
            case Expr.Func f -> {
                TreeNode child = fromExpr(f.arg(), counter, variables);
                yield new TreeNode(id, TreeNode.Kind.FUNCTION, f.fn().symbol(), functionLatex(f.fn()),
                        SyntaxTools.FUNCTION_PRECEDENCE, null, 0, value, text, null, List.of(child));
            }
        };
    }

    private static TreeNode branch(int id, String label, String latex, int precedence, SyntaxTools.Associativity assoc,
                                   List<Expr> operands, int[] counter, Map<String, Double> variables, Double value, String text) {
        List<TreeNode> children = new ArrayList<>();
        for (Expr operand : operands) children.add(fromExpr(operand, counter, variables));
        return new TreeNode(id, TreeNode.Kind.OPERATOR, label, latex, precedence, assoc, 0, value, text, null, children);
    }

    private static String functionLatex(Fn fn) {
        return switch (fn) {
            case ABS -> "\\left|\\cdot\\right|";
            case ASIN -> "\\arcsin";
            case ACOS -> "\\arccos";
            case ATAN -> "\\arctan";
            case ERF -> "\\operatorname{erf}";
            case SI -> "\\operatorname{Si}";
            case CI -> "\\operatorname{Ci}";
            case EI -> "\\operatorname{Ei}";
            case LI -> "\\operatorname{li}";
            default -> "\\" + fn.symbol();
        };
    }

    private static Double exprValue(Expr e, Map<String, Double> variables) {
        try {
            double v = Eval.value(e, new HashMap<>(variables));
            return Double.isFinite(v) ? v : null;
        } catch (IllegalArgumentException | ArithmeticException ex) {
            return null;
        }
    }
}
