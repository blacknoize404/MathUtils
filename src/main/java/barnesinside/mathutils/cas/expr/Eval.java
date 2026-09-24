package barnesinside.mathutils.cas.expr;

import barnesinside.mathutils.special.Special;

import java.util.Map;

/**
 * Evaluación numérica (de punto flotante) de expresiones.
 */
public final class Eval {

    private Eval() {
    }

    /**
     * Evalúa la expresión. Las funciones fuera de su dominio dan {@code NaN}.
     * @param env valor de cada variable
     * @throws IllegalArgumentException si falta el valor de alguna variable
     */
    public static double value(Expr e, Map<String, Double> env) {
        return switch (e) {
            case Expr.Num n -> n.value().doubleValue();
            case Expr.Const c -> switch (c.kind()) {
                case PI -> Math.PI;
                case E -> Math.E;
                case INFINITY -> Double.POSITIVE_INFINITY;
            };
            case Expr.Sym s -> {
                Double v = env.get(s.name());
                if (v == null) throw new IllegalArgumentException("Falta el valor de la variable " + s.name());
                yield v;
            }
            case Expr.Add a -> {
                double sum = 0;
                for (Expr t : a.terms()) sum += value(t, env);
                yield sum;
            }
            case Expr.Mul m -> {
                double product = 1;
                for (Expr f : m.factors()) product *= value(f, env);
                yield product;
            }
            case Expr.Pow p -> power(value(p.base(), env), p.exponent(), env);
            case Expr.Func f -> function(f.fn(), value(f.arg(), env));
        };
    }

    public static double value(Expr e) {
        return value(e, Map.of());
    }

    private static double power(double base, Expr exponent, Map<String, Double> env) {
        double exp = value(exponent, env);
        // Raíz real de un negativo con índice impar: (-8)^(1/3) = -2
        if (base < 0 && exponent instanceof Expr.Num n && !n.value().isInteger() && n.value().denominator().testBit(0)) {
            double magnitude = Math.pow(-base, exp);
            return n.value().numerator().testBit(0) ? -magnitude : magnitude;
        }
        return Math.pow(base, exp);
    }

    private static double function(Fn fn, double x) {
        return switch (fn) {
            case SIN -> Math.sin(x);
            case COS -> Math.cos(x);
            case TAN -> Math.tan(x);
            case COT -> 1 / Math.tan(x);
            case SEC -> 1 / Math.cos(x);
            case CSC -> 1 / Math.sin(x);
            case ASIN -> Math.asin(x);
            case ACOS -> Math.acos(x);
            case ATAN -> Math.atan(x);
            case SINH -> Math.sinh(x);
            case COSH -> Math.cosh(x);
            case TANH -> Math.tanh(x);
            case LN -> Math.log(x);
            case ABS -> Math.abs(x);
            case ERF -> Special.erf(x);
            case SI -> Special.si(x);
            case CI -> Special.ci(x);
            case EI -> Special.ei(x);
            case LI -> Special.li(x);
            case FACTORIAL -> Special.factorial(x);
        };
    }
}
