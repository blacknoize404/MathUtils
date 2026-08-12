package MathUtils.algebra;

import MathUtils.algebra.exponentials.Pow;
import MathUtils.algebra.exponentials.Root;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Convierte un árbol matemático a código LaTeX.
 * <p>Las divisiones se escriben con {@code \frac}, las raíces con {@code \sqrt}, y se añaden
 * paréntesis solo donde la precedencia lo exige.
 */
public final class LatexPrinter {

    private static final int SUM = 1;
    private static final int PRODUCT = 2;
    private static final int POWER = 3;
    private static final int ATOM = 4;

    private LatexPrinter() {
    }

    public static String toLatex(MathElement element) {
        return print(element);
    }

    private static String print(MathElement e) {
        if (e instanceof Function f) return print(f.getContent());

        if (e instanceof Const c) {
            double value = c.getArgument().doubleValue();
            if (value == Math.PI) return "\\pi";
            if (value == Math.E) return "e";
            return c.toString();
        }
        if (e instanceof Var v) {
            String name = v.getArgument();
            return name.length() == 1 ? name : "\\mathrm{" + name + "}";
        }

        if (e instanceof SumOperator op) return join(op.getContent(), " + ", SUM);
        if (e instanceof SubOperator op) {
            List<MathElement> items = op.getContent();
            return wrap(items.get(0), SUM) + items.subList(1, items.size()).stream()
                    .map(x -> " - " + wrap(x, PRODUCT))
                    .collect(Collectors.joining());
        }
        if (e instanceof MultOperator op) return multiplication(op.getContent());
        if (e instanceof DivOperator op) {
            List<MathElement> items = op.getContent();
            List<MathElement> divisors = items.subList(1, items.size());
            String denominator = divisors.size() == 1
                    ? print(divisors.get(0))
                    : join(divisors, " \\cdot ", PRODUCT);
            return "\\frac{" + print(items.get(0)) + "}{" + denominator + "}";
        }

        if (e instanceof Pow p) return wrap(p.getArgument(), ATOM) + "^{" + print(p.getExponent()) + "}";
        if (e instanceof Root r) {
            boolean square = r.getRoot() instanceof Const c && c.getArgument().doubleValue() == 2;
            return square ? "\\sqrt{" + print(r.getArgument()) + "}"
                    : "\\sqrt[" + print(r.getRoot()) + "]{" + print(r.getArgument()) + "}";
        }

        if (e instanceof LogN l) return "\\ln\\left(" + print(l.getArgument()) + "\\right)";
        if (e instanceof Log l) return "\\log_{" + print(l.getBase()) + "}\\left(" + print(l.getArgument()) + "\\right)";
        if (e instanceof Sin s) return "\\sin\\left(" + print(s.getArgument()) + "\\right)";
        if (e instanceof Cos s) return "\\cos\\left(" + print(s.getArgument()) + "\\right)";
        if (e instanceof Tan s) return "\\tan\\left(" + print(s.getArgument()) + "\\right)";
        if (e instanceof Cot s) return "\\cot\\left(" + print(s.getArgument()) + "\\right)";

        return e.toString();
    }

    private static String multiplication(List<MathElement> factors) {
        // -1 · x se escribe como -x
        if (factors.get(0) instanceof Const c && c.getArgument().doubleValue() == -1) {
            return "-" + join(factors.subList(1, factors.size()), " \\cdot ", PRODUCT);
        }
        return join(factors, " \\cdot ", PRODUCT);
    }

    private static String join(List<MathElement> items, String separator, int minPrecedence) {
        return items.stream().map(x -> wrap(x, minPrecedence)).collect(Collectors.joining(separator));
    }

    /** Envuelve en paréntesis si la precedencia del elemento es menor que la requerida. */
    private static String wrap(MathElement e, int minPrecedence) {
        String text = print(e);
        return precedence(e) < minPrecedence ? "\\left(" + text + "\\right)" : text;
    }

    private static int precedence(MathElement e) {
        if (e instanceof Function f) return precedence(f.getContent());
        if (e instanceof Const c) return c.getArgument().doubleValue() < 0 ? SUM : ATOM;
        if (e instanceof SumOperator || e instanceof SubOperator) return SUM;
        if (e instanceof MultOperator) return PRODUCT;
        if (e instanceof Pow) return POWER;
        return ATOM;
    }
}
