package MathUtils.cas.syntax;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprBuilder;
import MathUtils.cas.expr.Rational;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operaciones auxiliares sobre {@link Syntax}: identificadores de nodo, conversión a
 * expresión simbólica y reglas de precedencia.
 */
public final class SyntaxTools {

    /** Asociatividad de un operador. */
    public enum Associativity {LEFT, RIGHT}

    private SyntaxTools() {
    }

    /**
     * Asigna un identificador a cada nodo en preorden. Los grupos (paréntesis) no reciben
     * identificador propio: comparten el de su contenido.
     */
    public static Map<Syntax, Integer> assignIds(Syntax root) {
        Map<Syntax, Integer> ids = new IdentityHashMap<>();
        assign(root, ids, new int[]{0});
        return ids;
    }

    private static void assign(Syntax node, Map<Syntax, Integer> ids, int[] counter) {
        if (node instanceof Syntax.Group g) {
            assign(g.inner(), ids, counter);
            ids.put(g, ids.get(g.inner()));
            return;
        }
        ids.put(node, counter[0]++);
        for (Syntax child : children(node)) assign(child, ids, counter);
    }

    /** Hijos directos del nodo, en el orden en que aparecen escritos. */
    public static List<Syntax> children(Syntax node) {
        return switch (node) {
            case Syntax.Num n -> List.of();
            case Syntax.Var v -> List.of();
            case Syntax.Const c -> List.of();
            case Syntax.Neg n -> List.of(n.operand());
            case Syntax.Bin b -> List.of(b.left(), b.right());
            case Syntax.Call c -> c.args();
            case Syntax.Group g -> List.of(g.inner());
        };
    }

    /** Precedencia de un operador binario: {@code + -} = 1, {@code * /} = 2, {@code ^} = 4. */
    public static int precedence(char op) {
        return switch (op) {
            case '+', '-' -> 1;
            case '*', '/' -> 2;
            default -> 4;
        };
    }

    /** Precedencia del signo menos unario. */
    public static final int NEGATION_PRECEDENCE = 3;

    /** Precedencia de las llamadas a función (se resuelven como las potencias). */
    public static final int FUNCTION_PRECEDENCE = 5;

    public static Associativity associativity(char op) {
        return op == '^' ? Associativity.RIGHT : Associativity.LEFT;
    }

    /**
     * Convierte el árbol sintáctico a una expresión simbólica en forma canónica.
     * @throws ArithmeticException si hay una división por cero
     */
    public static Expr toExpr(Syntax node) {
        return toExpr(node, new ExprBuilder());
    }

    /**
     * Igual que {@link #toExpr(Syntax)} usando el constructor dado (funciones de usuario, unidad de ángulo).
     */
    public static Expr toExpr(Syntax node, ExprBuilder builder) {
        return switch (node) {
            case Syntax.Num n -> n.exact() != null ? Expr.num(n.exact()) : Expr.num(Rational.fromDouble(n.approx()));
            case Syntax.Var v -> Expr.sym(v.name());
            case Syntax.Const c -> c.name().equals("pi") ? Expr.PI : Expr.E;
            case Syntax.Neg n -> Expr.neg(toExpr(n.operand(), builder));
            case Syntax.Bin b -> {
                Expr l = toExpr(b.left(), builder);
                Expr r = toExpr(b.right(), builder);
                yield switch (b.op()) {
                    case '+' -> Expr.add(l, r);
                    case '-' -> Expr.sub(l, r);
                    case '*' -> Expr.mul(l, r);
                    case '/' -> Expr.div(l, r);
                    default -> Expr.pow(l, r);
                };
            }
            case Syntax.Call c -> {
                List<Expr> args = new ArrayList<>();
                for (Syntax a : c.args()) args.add(toExpr(a, builder));
                yield builder.function(c.name(), args);
            }
            case Syntax.Group g -> toExpr(g.inner(), builder);
        };
    }
}
