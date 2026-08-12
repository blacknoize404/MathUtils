package MathUtils.cas.integral;

import MathUtils.cas.expr.Expand;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.Fn;
import MathUtils.cas.expr.Rational;
import MathUtils.cas.poly.Polynomial;
import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Trace;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reconocimiento y reescritura de integrandos trigonométricos: potencias de seno y coseno (con
 * tangente, secante, cotangente y cosecante convertidas a ellos), productos de ángulos distintos
 * y funciones racionales de seno y coseno.
 */
final class Trig {

    /** {@code sin(x)^a · cos(x)^b} con exponentes enteros de cualquier signo. */
    record Form(int sin, int cos) {
    }

    /** Una reescritura equivalente del integrando, que se integra a continuación. */
    record Rewrite(Rule rule, Map<String, Expr> params, Expr expression) {
    }

    private static final int MAX_EXPONENT = 12;

    private Trig() {
    }

    // ================================================================ Reconocimiento

    /** Interpreta {@code f} como producto de potencias de funciones trigonométricas de {@code v}. */
    static Optional<Form> parse(Expr f, String v) {
        Expr x = Expr.sym(v);
        int sin = 0;
        int cos = 0;
        boolean any = false;
        for (Expr factor : f instanceof Expr.Mul m ? m.factors() : List.of(f)) {
            Expr base = factor;
            int exponent = 1;
            if (factor instanceof Expr.Pow p && p.exponent() instanceof Expr.Num n && n.value().isInteger()
                    && n.value().fitsInt() && Math.abs(n.value().intValueExact()) <= MAX_EXPONENT) {
                base = p.base();
                exponent = n.value().intValueExact();
            }
            if (!(base instanceof Expr.Func fn) || !fn.arg().equals(x)) return Optional.empty();
            switch (fn.fn()) {
                case SIN -> sin += exponent;
                case COS -> cos += exponent;
                case TAN -> {
                    sin += exponent;
                    cos -= exponent;
                }
                case COT -> {
                    cos += exponent;
                    sin -= exponent;
                }
                case SEC -> cos -= exponent;
                case CSC -> sin -= exponent;
                default -> {
                    return Optional.empty();
                }
            }
            any = true;
        }
        return any ? Optional.of(new Form(sin, cos)) : Optional.empty();
    }

    // ================================================================ Reescrituras

    private static Expr s(Expr x) {
        return Expr.func(Fn.SIN, x);
    }

    private static Expr c(Expr x) {
        return Expr.func(Fn.COS, x);
    }

    private static Expr oneMinusSquare(Expr e) {
        return Expr.sub(Expr.ONE, Expr.pow(e, Expr.num(2)));
    }

    private static Map<String, Expr> params(String v, Object... extra) {
        Map<String, Expr> map = Trace.params("x", Expr.sym(v));
        for (int i = 0; i < extra.length; i += 2) map.put((String) extra[i], (Expr) extra[i + 1]);
        return map;
    }

    /**
     * Exponente impar: se aparta un factor y el resto se escribe con la otra función usando
     * {@code sin² + cos² = 1}, lo que prepara una sustitución.
     */
    static Optional<Rewrite> odd(Form form, String v) {
        Expr x = Expr.sym(v);
        boolean sinOdd = form.sin() % 2 != 0;
        boolean cosOdd = form.cos() % 2 != 0;
        if (!sinOdd && !cosOdd) return Optional.empty();

        // Con dos impares se elige el exponente positivo más pequeño
        boolean useSin = sinOdd && (!cosOdd || preferred(form.sin(), form.cos()));
        if (useSin) {
            Expr r = Expr.mul(s(x), Expr.pow(oneMinusSquare(c(x)), Expr.num((form.sin() - 1) / 2)),
                    Expr.pow(c(x), Expr.num(form.cos())));
            return Optional.of(new Rewrite(Rule.INT_TRIG_ODD_POWER, params(v, "keep", s(x), "other", c(x), "r", r), r));
        }
        Expr r = Expr.mul(c(x), Expr.pow(oneMinusSquare(s(x)), Expr.num((form.cos() - 1) / 2)),
                Expr.pow(s(x), Expr.num(form.sin())));
        return Optional.of(new Rewrite(Rule.INT_TRIG_ODD_POWER, params(v, "keep", c(x), "other", s(x), "r", r), r));
    }

    private static boolean preferred(int sin, int cos) {
        if (sin > 0 != cos > 0) return sin > 0;
        return Math.abs(sin) <= Math.abs(cos);
    }

    /** Exponentes pares no negativos: fórmulas del ángulo mitad. */
    static Optional<Rewrite> halfAngle(Form form, String v) {
        if (form.sin() < 0 || form.cos() < 0 || form.sin() % 2 != 0 || form.cos() % 2 != 0) return Optional.empty();
        Expr x = Expr.sym(v);
        Expr double2 = c(Expr.mul(Expr.num(2), x));
        Expr sinSquared = Expr.div(Expr.sub(Expr.ONE, double2), Expr.num(2));
        Expr cosSquared = Expr.div(Expr.add(Expr.ONE, double2), Expr.num(2));
        Expr r = Expand.expand(Expr.mul(Expr.pow(sinSquared, Expr.num(form.sin() / 2)),
                Expr.pow(cosSquared, Expr.num(form.cos() / 2))));
        return Optional.of(new Rewrite(Rule.INT_TRIG_HALF_ANGLE, params(v, "r", r), r));
    }

    /** Exponentes pares con suma negativa: se escribe con {@code tan x} y {@code sec² x}. */
    static Optional<Rewrite> tangentSubstitution(Form form, String v) {
        int total = form.sin() + form.cos();
        if (form.sin() % 2 != 0 || total >= 0) return Optional.empty();
        Expr x = Expr.sym(v);
        Expr tan = Expr.func(Fn.TAN, x);
        Expr secSquared = Expr.pow(Expr.func(Fn.SEC, x), Expr.num(2));
        Expr r = Expr.mul(Expr.pow(tan, Expr.num(form.sin())),
                Expr.pow(Expr.add(Expr.ONE, Expr.pow(tan, Expr.num(2))), Expr.num(-total / 2 - 1)), secSquared);
        return Optional.of(new Rewrite(Rule.INT_TRIG_TAN_SUBSTITUTION, params(v, "r", r), r));
    }

    /** {@code sin A cos B}, {@code cos A cos B} o {@code sin A sin B} con ángulos lineales distintos. */
    static Optional<Rewrite> productToSum(Expr f, String v) {
        if (!(f instanceof Expr.Mul m) || m.factors().size() != 2) return Optional.empty();
        if (!(m.factors().get(0) instanceof Expr.Func first) || !(m.factors().get(1) instanceof Expr.Func second)) {
            return Optional.empty();
        }
        if (!isSinOrCos(first.fn()) || !isSinOrCos(second.fn()) || first.arg().equals(second.arg())) {
            return Optional.empty();
        }
        if (!isLinear(first.arg(), v) || !isLinear(second.arg(), v)) return Optional.empty();

        Expr a;
        Expr b;
        Fn fa;
        Fn fb;
        // En la fórmula, sin va antes que cos
        if (first.fn() == Fn.COS && second.fn() == Fn.SIN) {
            a = second.arg();
            b = first.arg();
            fa = Fn.SIN;
            fb = Fn.COS;
        } else {
            a = first.arg();
            b = second.arg();
            fa = first.fn();
            fb = second.fn();
        }
        Expr plus = Expr.add(a, b);
        Expr minus = Expr.sub(a, b);
        Expr half = Expr.num(1, 2);
        Expr r;
        if (fa == Fn.SIN && fb == Fn.COS) {
            r = Expr.mul(half, Expr.add(Expr.func(Fn.SIN, plus), Expr.func(Fn.SIN, minus)));
        } else if (fa == Fn.COS) {
            r = Expr.mul(half, Expr.add(Expr.func(Fn.COS, minus), Expr.func(Fn.COS, plus)));
        } else {
            r = Expr.mul(half, Expr.sub(Expr.func(Fn.COS, minus), Expr.func(Fn.COS, plus)));
        }
        return Optional.of(new Rewrite(Rule.INT_TRIG_PRODUCT_TO_SUM, params(v, "r", r), r));
    }

    private static boolean isSinOrCos(Fn fn) {
        return fn == Fn.SIN || fn == Fn.COS;
    }

    private static boolean isLinear(Expr e, String v) {
        Optional<Polynomial> p = Polynomial.from(e, v);
        return p.isPresent() && p.get().degree() == 1;
    }

    // ================================================================ Weierstrass

    /**
     * La función racional de {@code t = tan(x/2)} que resulta de sustituir seno, coseno y las
     * demás funciones, incluido el factor {@code dx = 2 dt / (1 + t²)}; vacío si {@code f} no es una
     * función racional de funciones trigonométricas de {@code x} con coeficientes racionales.
     */
    static Optional<RationalFunction> weierstrass(Expr f, String v) {
        try {
            RationalFunction converted = convert(f, Expr.sym(v));
            if (converted == null) return Optional.empty();
            RationalFunction dx = RationalFunction.of(Polynomial.of(2), Polynomial.of(1, 0, 1));
            return Optional.of(converted.multiply(dx));
        } catch (ArithmeticException e) {
            return Optional.empty();
        }
    }

    private static RationalFunction convert(Expr e, Expr x) {
        switch (e) {
            case Expr.Num n -> {
                return RationalFunction.constant(n.value());
            }
            case Expr.Add a -> {
                RationalFunction sum = RationalFunction.constant(Rational.ZERO);
                for (Expr term : a.terms()) {
                    RationalFunction t = convert(term, x);
                    if (t == null) return null;
                    sum = sum.add(t);
                }
                return sum;
            }
            case Expr.Mul m -> {
                RationalFunction product = RationalFunction.constant(Rational.ONE);
                for (Expr factor : m.factors()) {
                    RationalFunction t = convert(factor, x);
                    if (t == null) return null;
                    product = product.multiply(t);
                }
                return product;
            }
            case Expr.Pow p -> {
                if (!(p.exponent() instanceof Expr.Num n) || !n.value().isInteger() || !n.value().fitsInt()
                        || Math.abs(n.value().intValueExact()) > MAX_EXPONENT) {
                    return null;
                }
                RationalFunction base = convert(p.base(), x);
                return base == null ? null : base.pow(n.value().intValueExact());
            }
            case Expr.Func fn -> {
                if (!fn.arg().equals(x)) return null;
                RationalFunction sin = RationalFunction.of(Polynomial.of(0, 2), Polynomial.of(1, 0, 1));
                RationalFunction cos = RationalFunction.of(Polynomial.of(1, 0, -1), Polynomial.of(1, 0, 1));
                return switch (fn.fn()) {
                    case SIN -> sin;
                    case COS -> cos;
                    case TAN -> sin.multiply(cos.reciprocal());
                    case COT -> cos.multiply(sin.reciprocal());
                    case SEC -> cos.reciprocal();
                    case CSC -> sin.reciprocal();
                    default -> null;
                };
            }
            default -> {
                return null;
            }
        }
    }
}
