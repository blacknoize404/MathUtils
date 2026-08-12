package MathUtils.cas.expr;

import MathUtils.algebra.Const;
import MathUtils.algebra.Fraction;
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
import MathUtils.algebra.operators.primitives.Operator;
import MathUtils.algebra.primitives.MathElement;
import MathUtils.algebra.trigonometrics.Cos;
import MathUtils.algebra.trigonometrics.Cot;
import MathUtils.algebra.trigonometrics.Sin;
import MathUtils.algebra.trigonometrics.Tan;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Conversión entre el árbol histórico {@link MathElement} y las expresiones simbólicas {@link Expr}.
 * <p>{@code MathElement → Expr} admite todos los nodos de {@code MathUtils.algebra}.
 * {@code Expr → MathElement} admite todo salvo las funciones sin clase equivalente
 * (secante, cosecante, inversas trigonométricas, hiperbólicas, valor absoluto y erf).
 */
public final class ElementConverter {

    private ElementConverter() {
    }

    /**
     * @throws IllegalArgumentException si el elemento no tiene equivalente simbólico (por ejemplo, series)
     */
    public static Expr toExpr(MathElement element) {
        if (element instanceof Function f) return toExpr(f.getContent());

        if (element instanceof Const c) return constant(c.getArgument());
        if (element instanceof Var v) return Expr.sym(v.getArgument());
        if (element instanceof Fraction f) return Expr.num(Rational.of(f.getNumerator(), f.getDenominator()));

        if (element instanceof SumOperator op) return Expr.add(all(op));
        if (element instanceof SubOperator op) {
            List<Expr> items = all(op);
            List<Expr> terms = new ArrayList<>();
            terms.add(items.get(0));
            for (Expr e : items.subList(1, items.size())) terms.add(Expr.neg(e));
            return Expr.add(terms);
        }
        if (element instanceof MultOperator op) return Expr.mul(all(op));
        if (element instanceof DivOperator op) {
            List<Expr> items = all(op);
            return Expr.div(items.get(0), Expr.mul(items.subList(1, items.size())));
        }

        if (element instanceof Pow p) return Expr.pow(toExpr(p.getArgument()), toExpr(p.getExponent()));
        if (element instanceof Root r) {
            return Expr.pow(toExpr(r.getArgument()), Expr.div(Expr.ONE, toExpr(r.getRoot())));
        }
        if (element instanceof LogN l) return Expr.ln(toExpr(l.getArgument()));
        if (element instanceof Log l) return Expr.div(Expr.ln(toExpr(l.getArgument())), Expr.ln(toExpr(l.getBase())));

        if (element instanceof Sin s) return Expr.func(Fn.SIN, toExpr(s.getArgument()));
        if (element instanceof Cos s) return Expr.func(Fn.COS, toExpr(s.getArgument()));
        if (element instanceof Tan s) return Expr.func(Fn.TAN, toExpr(s.getArgument()));
        if (element instanceof Cot s) return Expr.func(Fn.COT, toExpr(s.getArgument()));

        throw new IllegalArgumentException("No hay equivalente simbólico para " + element.getClass().getSimpleName());
    }

    /**
     * @throws UnsupportedOperationException si la expresión usa una función sin clase equivalente
     */
    public static MathElement toElement(Expr expr) {
        return switch (expr) {
            case Expr.Num n -> number(n.value());
            case Expr.Const c -> switch (c.kind()) {
                case PI -> new Const(Math.PI);
                case E -> new Const(Math.E);
                case INFINITY -> new Const(Double.POSITIVE_INFINITY);
            };
            case Expr.Sym s -> new Var(s.name());
            case Expr.Add a -> {
                List<MathElement> items = convert(a.terms());
                yield new SumOperator(items.get(0), items.get(1), rest(items));
            }
            case Expr.Mul m -> {
                List<MathElement> items = convert(m.factors());
                yield new MultOperator(items.get(0), items.get(1), rest(items));
            }
            case Expr.Pow p -> power(p);
            case Expr.Func f -> function(f);
        };
    }

    // ---------------------------------------------------------------- MathElement -> Expr

    private static List<Expr> all(Operator op) {
        List<Expr> items = new ArrayList<>();
        for (MathElement e : op.getContent()) items.add(toExpr(e));
        return items;
    }

    private static Expr constant(Number value) {
        if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            return Expr.num(value.longValue());
        }
        if (value instanceof BigInteger b) return Expr.num(Rational.of(b));
        double d = value.doubleValue();
        if (d == Math.PI) return Expr.PI;
        if (d == Math.E) return Expr.E;
        if (Double.isInfinite(d) && d > 0) return Expr.INFINITY;
        return Expr.num(Rational.fromDouble(d));
    }

    // ---------------------------------------------------------------- Expr -> MathElement

    private static MathElement number(Rational r) {
        if (r.isInteger()) {
            return r.fitsInt() ? new Const(r.intValueExact()) : new Const(r.doubleValue());
        }
        return new DivOperator(new Const(r.numerator().doubleValue()), new Const(r.denominator().doubleValue()));
    }

    private static List<MathElement> convert(List<Expr> items) {
        List<MathElement> out = new ArrayList<>();
        for (Expr e : items) out.add(toElement(e));
        return out;
    }

    private static MathElement[] rest(List<MathElement> items) {
        return items.subList(2, items.size()).toArray(MathElement[]::new);
    }

    private static MathElement power(Expr.Pow p) {
        MathElement base = toElement(p.base());
        if (p.exponent() instanceof Expr.Num n) {
            if (n.value().equals(Rational.of(1, 2))) return new Sqrt(base);
            if (n.value().equals(Rational.of(1, 3))) return new Cbrt(base);
        }
        return new Pow(base, toElement(p.exponent()));
    }

    private static MathElement function(Expr.Func f) {
        MathElement arg = toElement(f.arg());
        return switch (f.fn()) {
            case SIN -> new Sin(arg);
            case COS -> new Cos(arg);
            case TAN -> new Tan(arg);
            case COT -> new Cot(arg);
            case LN -> new LogN(arg);
            default -> throw new UnsupportedOperationException(
                    "La función " + f.fn().symbol() + " no tiene clase equivalente en MathUtils.algebra");
        };
    }
}
