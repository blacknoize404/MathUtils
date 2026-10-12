package barnesinside.mathutils.cas.expr;

import barnesinside.mathutils.parser.TreeBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Construye la expresión tal como se escribió: los sumandos y los factores quedan en el orden del texto y no se reordenan ni se
 * agrupan ({@code 1 + 1/x} no pasa a {@code 1/x + 1}). Sirve para enseñar el enunciado como lo escribió quien lo pidió; para calcular
 * se usa la expresión normal de {@link ExprBuilder}, equivalente a esta.
 */
public final class TypedExprBuilder implements TreeBuilder<Expr> {

    private final ExprBuilder base = new ExprBuilder();

    @Override
    public Expr number(String text) {
        return base.number(text);
    }

    @Override
    public Expr variable(String name) {
        return base.variable(name);
    }

    @Override
    public Expr negate(Expr operand) {
        // Un número negativo es un número; lo demás, el producto por −1
        if (operand instanceof Expr.Num) return base.negate(operand);
        return new Expr.Mul(List.of(Expr.num(-1), operand));
    }

    @Override
    public Expr binary(char operator, Expr left, Expr right) {
        return switch (operator) {
            case '+' -> new Expr.Add(join(left, right, true));
            case '-' -> new Expr.Add(join(left, negate(right), true));
            case '*' -> new Expr.Mul(join(left, right, false));
            case '/' -> right instanceof Expr.Num ? base.binary('/', left, right)
                    : new Expr.Mul(join(left, new Expr.Pow(right, Expr.num(-1)), false));
            default -> new Expr.Pow(left, right);
        };
    }

    /** Une dos operandos de la misma operación en una sola lista, como {@code a + b + c}. */
    private static List<Expr> join(Expr left, Expr right, boolean sum) {
        List<Expr> items = new ArrayList<>();
        for (Expr side : List.of(left, right)) {
            if (sum && side instanceof Expr.Add a && side == left) items.addAll(a.terms());
            else if (!sum && side instanceof Expr.Mul m && side == left) items.addAll(m.factors());
            else items.add(side);
        }
        return items;
    }

    @Override
    public Expr function(String name, List<Expr> arguments) {
        return base.function(name, arguments);
    }

    @Override
    public boolean isFunction(String name) {
        return base.isFunction(name);
    }
}
