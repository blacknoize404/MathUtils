package MathUtils.cas.integral;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.Fn;
import MathUtils.cas.expr.Rational;
import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Trace;

import java.util.Map;
import java.util.Optional;

/**
 * Tabla de integrales básicas con la variable como argumento directo: potencias, exponenciales,
 * trigonométricas e hiperbólicas, y las formas {@code 1/(x²±a²)}, {@code 1/√(a²−x²)} y
 * {@code 1/√(x²+c)}. Los argumentos compuestos se resuelven con sustitución.
 */
final class Table {

    /** Resultado de una búsqueda: la regla aplicada, la antiderivada y los parámetros de la regla. */
    record Hit(Rule rule, Expr antiderivative, Map<String, Expr> params) {
    }

    private Table() {
    }

    static Optional<Hit> lookup(Expr f, String variable) {
        Expr x = Expr.sym(variable);
        Map<String, Expr> base = Trace.params("x", x);

        // x^n
        if (f.equals(x)) return power(x, Expr.ONE, base);
        if (f instanceof Expr.Pow p && p.base().equals(x) && p.exponent() instanceof Expr.Num) {
            return power(x, p.exponent(), base);
        }

        // Exponenciales
        if (f instanceof Expr.Pow p && p.exponent().equals(x)) {
            if (p.base().equals(Expr.E)) return hit(Rule.INT_EXPONENTIAL_E, f, base);
            if (p.base() instanceof Expr.Num a && a.value().signum() > 0 && !a.value().isOne()) {
                return hit(Rule.INT_EXPONENTIAL, Expr.div(f, Expr.ln(p.base())), Trace.params("x", x, "a", p.base()));
            }
        }

        // Funciones elementales de x
        if (f instanceof Expr.Func fn && fn.arg().equals(x)) {
            switch (fn.fn()) {
                case SIN -> {
                    return hit(Rule.INT_SIN, Expr.neg(Expr.cos(x)), base);
                }
                case COS -> {
                    return hit(Rule.INT_COS, Expr.sin(x), base);
                }
                case TAN -> {
                    return hit(Rule.INT_TAN, Expr.neg(Expr.ln(Expr.abs(Expr.cos(x)))), base);
                }
                case COT -> {
                    return hit(Rule.INT_COT, Expr.ln(Expr.abs(Expr.sin(x))), base);
                }
                case SEC -> {
                    return hit(Rule.INT_SEC, Expr.ln(Expr.abs(Expr.add(Expr.func(Fn.SEC, x), Expr.func(Fn.TAN, x)))), base);
                }
                case CSC -> {
                    return hit(Rule.INT_CSC, Expr.neg(Expr.ln(Expr.abs(Expr.add(Expr.func(Fn.CSC, x), Expr.func(Fn.COT, x))))), base);
                }
                case SINH -> {
                    return hit(Rule.INT_SINH, Expr.func(Fn.COSH, x), base);
                }
                case COSH -> {
                    return hit(Rule.INT_COSH, Expr.func(Fn.SINH, x), base);
                }
                case TANH -> {
                    return hit(Rule.INT_TANH, Expr.ln(Expr.func(Fn.COSH, x)), base);
                }
                default -> {
                    // Sin entrada directa en la tabla
                }
            }
        }

        // sec² y csc²
        if (f instanceof Expr.Pow p && p.exponent().equals(Expr.num(2)) && p.base() instanceof Expr.Func fn && fn.arg().equals(x)) {
            if (fn.fn() == Fn.SEC) return hit(Rule.INT_SEC_SQUARED, Expr.func(Fn.TAN, x), base);
            if (fn.fn() == Fn.CSC) return hit(Rule.INT_CSC_SQUARED, Expr.neg(Expr.func(Fn.COT, x)), base);
        }

        // 1/cos, 1/sin, 1/cos² y 1/sin²
        if (f instanceof Expr.Pow p && p.base() instanceof Expr.Func fn && fn.arg().equals(x)
                && p.exponent() instanceof Expr.Num e) {
            int n = e.value().equals(Rational.MINUS_ONE) ? 1 : e.value().equals(Rational.of(-2)) ? 2 : 0;
            if (fn.fn() == Fn.COS && n == 1) return lookup(Expr.func(Fn.SEC, x), variable);
            if (fn.fn() == Fn.SIN && n == 1) return lookup(Expr.func(Fn.CSC, x), variable);
            if (fn.fn() == Fn.COS && n == 2) return hit(Rule.INT_SEC_SQUARED, Expr.func(Fn.TAN, x), base);
            if (fn.fn() == Fn.SIN && n == 2) return hit(Rule.INT_CSC_SQUARED, Expr.neg(Expr.func(Fn.COT, x)), base);
        }

        // sec·tan y csc·cot
        if (f.equals(Expr.mul(Expr.func(Fn.SEC, x), Expr.func(Fn.TAN, x)))) {
            return hit(Rule.INT_SEC_TAN, Expr.func(Fn.SEC, x), base);
        }
        if (f.equals(Expr.mul(Expr.func(Fn.CSC, x), Expr.func(Fn.COT, x)))) {
            return hit(Rule.INT_CSC_COT, Expr.neg(Expr.func(Fn.CSC, x)), base);
        }

        return quadratic(f, x, base);
    }

    private static Optional<Hit> hit(Rule rule, Expr antiderivative, Map<String, Expr> params) {
        return Optional.of(new Hit(rule, antiderivative, params));
    }

    private static Optional<Hit> power(Expr x, Expr n, Map<String, Expr> base) {
        Rational r = ((Expr.Num) n).value();
        if (r.equals(Rational.MINUS_ONE)) {
            return hit(Rule.INT_RECIPROCAL, Expr.ln(Expr.abs(x)), base);
        }
        Expr next = Expr.add(n, Expr.ONE);
        return hit(Rule.INT_POWER, Expr.div(Expr.pow(x, next), next), Trace.params("x", x, "n", n));
    }

    /** Formas con un polinomio de segundo grado sin término lineal: {@code x² + c}. */
    private static Optional<Hit> quadratic(Expr f, Expr x, Map<String, Expr> base) {
        if (!(f instanceof Expr.Pow p) || !(p.exponent() instanceof Expr.Num e)) return Optional.empty();
        Rational exponent = e.value();
        Rational minusOne = Rational.MINUS_ONE;
        Rational minusHalf = Rational.of(-1, 2);

        // 1/(x² + c)
        if (exponent.equals(minusOne)) {
            Expr c = constantTermOfSquarePlus(p.base(), x);
            if (c instanceof Expr.Num cn && cn.value().signum() != 0) {
                Expr a = Expr.sqrt(Expr.num(cn.value().abs()));
                if (cn.value().signum() > 0) {
                    return hit(Rule.INT_ATAN_FORM, Expr.mul(Expr.div(Expr.ONE, a), Expr.func(Fn.ATAN, Expr.div(x, a))),
                            Trace.params("x", x, "a", a));
                }
                Expr ratio = Expr.div(Expr.sub(x, a), Expr.add(x, a));
                return hit(Rule.INT_LOG_FORM, Expr.mul(Expr.div(Expr.ONE, Expr.mul(Expr.num(2), a)), Expr.ln(Expr.abs(ratio))),
                        Trace.params("x", x, "a", a));
            }
        }

        if (exponent.equals(Rational.of(1, 2))) {
            // sqrt(x² + c)
            Expr c = constantTermOfSquarePlus(p.base(), x);
            if (c instanceof Expr.Num cn && cn.value().signum() != 0) {
                Expr root = Expr.sqrt(Expr.add(Expr.pow(x, Expr.num(2)), c));
                Expr result = Expr.add(Expr.div(Expr.mul(x, root), Expr.num(2)),
                        Expr.mul(Expr.div(c, Expr.num(2)), Expr.ln(Expr.abs(Expr.add(x, root)))));
                return hit(Rule.INT_ROOT_SUM_FORM, result, Trace.params("x", x, "c", c));
            }
            // sqrt(c - x²) con c > 0
            Expr k = constantTermOfSquarePlus(negate(p.base()), x);
            if (k instanceof Expr.Num kn && kn.value().signum() < 0) {
                Expr c2 = Expr.num(kn.value().negate());
                Expr root = Expr.sqrt(p.base());
                Expr result = Expr.add(Expr.div(Expr.mul(x, root), Expr.num(2)),
                        Expr.mul(Expr.div(c2, Expr.num(2)), Expr.func(Fn.ASIN, Expr.div(x, Expr.sqrt(c2)))));
                return hit(Rule.INT_ROOT_DIFFERENCE_FORM, result, Trace.params("x", x, "c", c2));
            }
        }

        if (exponent.equals(minusHalf)) {
            // 1/sqrt(x² + c)
            Expr c = constantTermOfSquarePlus(p.base(), x);
            if (c instanceof Expr.Num cn && cn.value().signum() != 0) {
                Expr root = Expr.sqrt(Expr.add(Expr.pow(x, Expr.num(2)), c));
                return hit(Rule.INT_ASINH_FORM, Expr.ln(Expr.abs(Expr.add(x, root))), Trace.params("x", x, "c", c));
            }
            // 1/sqrt(c - x²)
            Expr negated = negate(p.base());
            Expr k = constantTermOfSquarePlus(negated, x);
            if (k instanceof Expr.Num kn && kn.value().signum() < 0) {
                Expr a = Expr.sqrt(Expr.num(kn.value().abs()));
                return hit(Rule.INT_ASIN_FORM, Expr.func(Fn.ASIN, Expr.div(x, a)), Trace.params("x", x, "a", a));
            }
        }
        return Optional.empty();
    }

    /** {@code -e} distribuyendo el signo sobre las sumas, para que {@code -(4 - x²)} sea {@code x² - 4}. */
    private static Expr negate(Expr e) {
        if (e instanceof Expr.Add sum) return Expr.add(sum.terms().stream().map(Expr::neg).toList());
        return Expr.neg(e);
    }

    /** Si {@code e} es {@code x² + c} con {@code c} libre de x, devuelve {@code c}; si no, {@code null}. */
    static Expr squarePlusConstant(Expr e, Expr x) {
        return constantTermOfSquarePlus(e, x);
    }

    private static Expr constantTermOfSquarePlus(Expr e, Expr x) {
        if (!(e instanceof Expr.Add sum) || sum.terms().size() != 2) return null;
        Expr square = Expr.pow(x, Expr.num(2));
        Expr first = sum.terms().get(0);
        Expr second = sum.terms().get(1);
        if (first.equals(square) && second.isFreeOf(((Expr.Sym) x).name())) return second;
        if (second.equals(square) && first.isFreeOf(((Expr.Sym) x).name())) return first;
        return null;
    }
}
