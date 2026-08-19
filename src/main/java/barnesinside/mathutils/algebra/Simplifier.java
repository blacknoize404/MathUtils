package barnesinside.mathutils.algebra;

import barnesinside.mathutils.algebra.exponentials.Pow;
import barnesinside.mathutils.algebra.exponentials.Root;
import barnesinside.mathutils.algebra.logarithmics.Log;
import barnesinside.mathutils.algebra.logarithmics.LogN;
import barnesinside.mathutils.algebra.operators.DivOperator;
import barnesinside.mathutils.algebra.operators.MultOperator;
import barnesinside.mathutils.algebra.operators.SubOperator;
import barnesinside.mathutils.algebra.operators.SumOperator;
import barnesinside.mathutils.algebra.primitives.MathElement;
import barnesinside.mathutils.algebra.trigonometrics.Cos;
import barnesinside.mathutils.algebra.trigonometrics.Cot;
import barnesinside.mathutils.algebra.trigonometrics.Sin;
import barnesinside.mathutils.algebra.trigonometrics.Tan;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplificador algebraico básico. Aplica, de abajo hacia arriba y hasta que no haya cambios:
 * <ul>
 *   <li>plegado de constantes ({@code 2 + 3} → {@code 5})</li>
 *   <li>{@code x + 0} → {@code x}, {@code x - 0} → {@code x}, {@code 0 - x} → {@code -1 * x}</li>
 *   <li>{@code x * 1} → {@code x}, {@code x * 0} → {@code 0}, {@code x / 1} → {@code x}, {@code 0 / x} → {@code 0}</li>
 *   <li>{@code x ^ 1} → {@code x}, {@code x ^ 0} → {@code 1}</li>
 *   <li>aplanado de sumas y productos anidados</li>
 * </ul>
 * No modifica el árbol original: devuelve un árbol nuevo.
 */
public final class Simplifier {

    private static final int MAX_PASSES = 10;

    private Simplifier() {
    }

    public static MathElement simplify(MathElement element) {
        MathElement current = element;
        for (int i = 0; i < MAX_PASSES; i++) {
            MathElement next = pass(current);
            if (next.toString().equals(current.toString())) return next;
            current = next;
        }
        return current;
    }

    private static MathElement pass(MathElement e) {
        if (e instanceof Function f) return new Function(pass(f.getContent()));
        if (e instanceof SumOperator op) return sum(op.getContent());
        if (e instanceof SubOperator op) return sub(op.getContent());
        if (e instanceof MultOperator op) return mult(op.getContent());
        if (e instanceof DivOperator op) return div(op.getContent());
        if (e instanceof Pow p) return pow(pass(p.getArgument()), pass(p.getExponent()));
        if (e instanceof Root r) return root(pass(r.getArgument()), pass(r.getRoot()));
        if (e instanceof LogN l) return new LogN(pass(l.getArgument()));
        if (e instanceof Log l) return new Log(pass(l.getArgument()), pass(l.getBase()));
        if (e instanceof Sin s) return new Sin(pass(s.getArgument()));
        if (e instanceof Cos s) return new Cos(pass(s.getArgument()));
        if (e instanceof Tan s) return new Tan(pass(s.getArgument()));
        if (e instanceof Cot s) return new Cot(pass(s.getArgument()));
        return e;
    }

    // ---------------------------------------------------------------- Operadores

    private static MathElement sum(List<MathElement> elements) {
        List<MathElement> terms = new ArrayList<>();
        double constant = 0;
        for (MathElement raw : elements) {
            MathElement e = pass(raw);
            List<MathElement> parts = e instanceof SumOperator s ? s.getContent() : List.of(e);
            for (MathElement part : parts) {
                if (isConst(part)) constant += value(part);
                else terms.add(part);
            }
        }
        if (constant != 0 || terms.isEmpty()) terms.add(new Const(constant));
        return terms.size() == 1 ? terms.get(0) : build(terms, Op.SUM);
    }

    private static MathElement sub(List<MathElement> elements) {
        MathElement first = pass(elements.get(0));
        List<MathElement> rest = new ArrayList<>();
        for (MathElement raw : elements.subList(1, elements.size())) {
            MathElement e = pass(raw);
            if (!(isConst(e) && value(e) == 0)) rest.add(e);
        }

        if (rest.isEmpty()) return first;
        if (isConst(first) && value(first) == 0 && rest.size() == 1) {
            return pass(new MultOperator(-1, rest.get(0)));
        }
        if (isConst(first) && rest.stream().allMatch(Simplifier::isConst)) {
            double result = value(first);
            for (MathElement e : rest) result -= value(e);
            return new Const(result);
        }

        List<MathElement> all = new ArrayList<>();
        all.add(first);
        all.addAll(rest);
        return build(all, Op.SUB);
    }

    private static MathElement mult(List<MathElement> elements) {
        List<MathElement> factors = new ArrayList<>();
        double coefficient = 1;
        for (MathElement raw : elements) {
            MathElement e = pass(raw);
            List<MathElement> parts = e instanceof MultOperator m ? m.getContent() : List.of(e);
            for (MathElement part : parts) {
                if (isConst(part)) coefficient *= value(part);
                else factors.add(part);
            }
        }

        if (coefficient == 0) return new Const(0);
        if (factors.isEmpty()) return new Const(coefficient);
        if (coefficient != 1) factors.add(0, new Const(coefficient));
        return factors.size() == 1 ? factors.get(0) : build(factors, Op.MULT);
    }

    private static MathElement div(List<MathElement> elements) {
        MathElement numerator = pass(elements.get(0));
        List<MathElement> denominators = new ArrayList<>();
        for (MathElement raw : elements.subList(1, elements.size())) {
            MathElement e = pass(raw);
            if (!(isConst(e) && value(e) == 1)) denominators.add(e);
        }

        if (isConst(numerator) && value(numerator) == 0) return new Const(0);
        if (denominators.isEmpty()) return numerator;

        if (isConst(numerator) && denominators.stream().allMatch(d -> isConst(d) && value(d) != 0)) {
            double result = value(numerator);
            for (MathElement d : denominators) result /= value(d);
            return new Const(result);
        }

        List<MathElement> all = new ArrayList<>();
        all.add(numerator);
        all.addAll(denominators);
        return build(all, Op.DIV);
    }

    // ---------------------------------------------------------------- Potencias y raíces

    private static MathElement pow(MathElement base, MathElement exponent) {
        if (isConst(exponent)) {
            if (value(exponent) == 1) return base;
            if (value(exponent) == 0) return new Const(1);
            if (isConst(base)) {
                double result = Math.pow(value(base), value(exponent));
                if (Double.isFinite(result)) return new Const(result);
            }
        }
        if (isConst(base) && value(base) == 1) return new Const(1);
        return new Pow(base, exponent);
    }

    private static MathElement root(MathElement argument, MathElement index) {
        if (isConst(index) && value(index) == 1) return argument;
        return new Root(argument, index);
    }

    // ---------------------------------------------------------------- Utilidades

    private enum Op {SUM, SUB, MULT, DIV}

    private static MathElement build(List<MathElement> elements, Op op) {
        MathElement first = elements.get(0);
        MathElement second = elements.get(1);
        MathElement[] rest = elements.subList(2, elements.size()).toArray(MathElement[]::new);
        return switch (op) {
            case SUM -> new SumOperator(first, second, rest);
            case SUB -> new SubOperator(first, second, rest);
            case MULT -> new MultOperator(first, second, rest);
            case DIV -> new DivOperator(first, second, rest);
        };
    }

    private static boolean isConst(MathElement e) {
        return e instanceof Const;
    }

    private static double value(MathElement e) {
        return ((Const) e).getArgument().doubleValue();
    }
}
