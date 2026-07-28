package MathUtils.cas.diff;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.Fn;
import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deriva expresiones simbólicas registrando en una {@link Trace} cada regla aplicada.
 * <p>Presenta las reglas como en un curso de cálculo: suma, constante por función, producto,
 * cociente, potencia, cadena (con la función interior {@code u}), exponencial, logaritmo,
 * trigonométricas e inversas, hiperbólicas, valor absoluto y la derivación logarítmica
 * cuando la base y el exponente dependen de la variable.
 */
final class Differentiator {

    private static final List<String> GENERIC_NAMES = List.of("u", "v", "w", "t", "s", "z");

    private final String variable;
    private final Expr x;
    private final Trace trace;
    private final String uName;
    private final Expr u;

    /**
     * @param variable variable de derivación
     * @param taken    nombres de variables ya presentes, para elegir una letra libre como función interior
     */
    Differentiator(String variable, Set<String> taken, Trace trace) {
        this.variable = variable;
        this.x = Expr.sym(variable);
        this.trace = trace;
        String name = GENERIC_NAMES.stream()
                .filter(n -> !n.equals(variable) && !taken.contains(n))
                .findFirst()
                .orElse("u_1");
        this.uName = name;
        this.u = Expr.sym(name);
    }

    Expr diff(Expr e) {
        if (e.isFreeOf(variable)) {
            if (e instanceof Expr.Sym) {
                return trace.leaf(Rule.DIFF_OTHER_VARIABLE, e, Expr.ZERO, Trace.params("x", x, "y", e));
            }
            return trace.leaf(Rule.DIFF_CONSTANT, e, Expr.ZERO, Trace.params("x", x));
        }

        return switch (e) {
            case Expr.Sym s -> trace.leaf(Rule.DIFF_VARIABLE, e, Expr.ONE, Trace.params("x", x));
            case Expr.Add a -> sum(a);
            case Expr.Mul m -> product(m);
            case Expr.Pow p -> power(p);
            case Expr.Func f -> function(f);
            case Expr.Num n -> throw new IllegalStateException("Un número no depende de la variable");
            case Expr.Const c -> throw new IllegalStateException("Una constante no depende de la variable");
        };
    }

    // ---------------------------------------------------------------- Suma

    private Expr sum(Expr.Add a) {
        return trace.step(Rule.DIFF_SUM, a, Trace.params("x", x), () -> {
            List<Expr> derivatives = new ArrayList<>();
            for (Expr term : a.terms()) derivatives.add(diff(term));
            return Expr.add(derivatives);
        });
    }

    // ---------------------------------------------------------------- Producto y cociente

    private Expr product(Expr.Mul m) {
        List<Expr> constants = new ArrayList<>();
        List<Expr> varying = new ArrayList<>();
        for (Expr f : m.factors()) (f.isFreeOf(variable) ? constants : varying).add(f);

        if (!constants.isEmpty()) {
            Expr c = Expr.mul(constants);
            Expr f = Expr.mul(varying);
            return trace.step(Rule.DIFF_CONSTANT_MULTIPLE, m, Trace.params("x", x, "c", c, "f", f),
                    () -> Expr.mul(c, diff(f)));
        }

        // Factores con potencia negativa forman el denominador
        List<Expr> numerator = new ArrayList<>();
        List<Expr> denominator = new ArrayList<>();
        for (Expr f : varying) {
            if (f instanceof Expr.Pow p && p.exponent() instanceof Expr.Num n && n.value().signum() < 0) {
                denominator.add(Expr.pow(p.base(), Expr.num(n.value().negate())));
            } else {
                numerator.add(f);
            }
        }

        if (!numerator.isEmpty() && !denominator.isEmpty()) {
            Expr f = Expr.mul(numerator);
            Expr g = Expr.mul(denominator);
            return trace.step(Rule.DIFF_QUOTIENT, m, Trace.params("x", x, "f", f, "g", g), () -> {
                Expr df = diff(f);
                Expr dg = diff(g);
                return Expr.div(Expr.sub(Expr.mul(df, g), Expr.mul(f, dg)), Expr.pow(g, Expr.num(2)));
            });
        }

        List<Expr> ordered = Expr.displayOrder(varying);
        Expr first = ordered.get(0);
        Expr rest = Expr.mul(ordered.subList(1, ordered.size()));
        return trace.step(Rule.DIFF_PRODUCT, m, Trace.params("x", x, "f", first, "g", rest), () -> {
            Expr df = diff(first);
            Expr dg = diff(rest);
            return Expr.add(Expr.mul(df, rest), Expr.mul(first, dg));
        });
    }

    // ---------------------------------------------------------------- Potencias y exponenciales

    private Expr power(Expr.Pow p) {
        Expr base = p.base();
        Expr exponent = p.exponent();
        boolean baseDepends = !base.isFreeOf(variable);
        boolean exponentDepends = !exponent.isFreeOf(variable);

        if (!exponentDepends) {
            // (f^n)' = n·f^(n-1)·f'
            if (base.equals(x)) {
                return trace.leaf(Rule.DIFF_POWER, p, powerDerivative(x, exponent), Trace.params("x", x, "n", exponent));
            }
            return chain(p, base, Rule.DIFF_POWER, Expr.pow(u, exponent), powerDerivative(u, exponent),
                    Trace.params("n", exponent));
        }

        if (!baseDepends) {
            if (base.equals(Expr.E)) {
                if (exponent.equals(x)) {
                    return trace.leaf(Rule.DIFF_EXPONENTIAL_E, p, p, Trace.params("x", x));
                }
                return chain(p, exponent, Rule.DIFF_EXPONENTIAL_E, Expr.exp(u), Expr.exp(u), Map.of());
            }
            // (a^u)' = a^u·ln(a)·u'
            Expr outerAfter = Expr.mul(Expr.pow(base, u), Expr.ln(base));
            if (exponent.equals(x)) {
                return trace.leaf(Rule.DIFF_EXPONENTIAL, p, Expr.mul(p, Expr.ln(base)), Trace.params("x", x, "a", base));
            }
            return chain(p, exponent, Rule.DIFF_EXPONENTIAL, Expr.pow(base, u), outerAfter, Trace.params("a", base));
        }

        // Base y exponente dependen de la variable: (f^g)' = f^g·(g'·ln f + g·f'/f)
        return trace.step(Rule.DIFF_POWER_GENERAL, p, Trace.params("x", x, "f", base, "g", exponent), () -> {
            Expr dg = diff(exponent);
            Expr df = diff(base);
            return Expr.mul(p, Expr.add(Expr.mul(dg, Expr.ln(base)), Expr.div(Expr.mul(exponent, df), base)));
        });
    }

    private static Expr powerDerivative(Expr variable, Expr n) {
        return Expr.mul(n, Expr.pow(variable, Expr.sub(n, Expr.ONE)));
    }

    // ---------------------------------------------------------------- Funciones

    private Expr function(Expr.Func f) {
        Rule rule = ruleFor(f.fn());
        if (f.arg().equals(x)) {
            return trace.leaf(rule, f, derivativeOf(f.fn(), x), Trace.params("x", x));
        }
        return chain(f, f.arg(), rule, Expr.func(f.fn(), u), derivativeOf(f.fn(), u), Map.of());
    }

    /** Derivada de la función elemental evaluada en {@code t}: {@code F'(t)}. */
    private static Expr derivativeOf(Fn fn, Expr t) {
        return switch (fn) {
            case SIN -> Expr.cos(t);
            case COS -> Expr.neg(Expr.sin(t));
            case TAN -> Expr.pow(Expr.func(Fn.SEC, t), Expr.num(2));
            case COT -> Expr.neg(Expr.pow(Expr.func(Fn.CSC, t), Expr.num(2)));
            case SEC -> Expr.mul(Expr.func(Fn.SEC, t), Expr.func(Fn.TAN, t));
            case CSC -> Expr.neg(Expr.mul(Expr.func(Fn.CSC, t), Expr.func(Fn.COT, t)));
            case ASIN -> Expr.pow(Expr.sub(Expr.ONE, Expr.pow(t, Expr.num(2))), Expr.num(-1, 2));
            case ACOS -> Expr.neg(Expr.pow(Expr.sub(Expr.ONE, Expr.pow(t, Expr.num(2))), Expr.num(-1, 2)));
            case ATAN -> Expr.div(Expr.ONE, Expr.add(Expr.ONE, Expr.pow(t, Expr.num(2))));
            case SINH -> Expr.func(Fn.COSH, t);
            case COSH -> Expr.func(Fn.SINH, t);
            case TANH -> Expr.sub(Expr.ONE, Expr.pow(Expr.func(Fn.TANH, t), Expr.num(2)));
            case LN -> Expr.div(Expr.ONE, t);
            case ABS -> Expr.div(t, Expr.abs(t));
            case ERF -> Expr.mul(Expr.div(Expr.num(2), Expr.sqrt(Expr.PI)), Expr.exp(Expr.neg(Expr.pow(t, Expr.num(2)))));
            case SI -> Expr.div(Expr.sin(t), t);
            case CI -> Expr.div(Expr.cos(t), t);
            case EI -> Expr.div(Expr.exp(t), t);
            case LI -> Expr.div(Expr.ONE, Expr.ln(t));
        };
    }

    private static Rule ruleFor(Fn fn) {
        return switch (fn) {
            case SIN -> Rule.DIFF_SIN;
            case COS -> Rule.DIFF_COS;
            case TAN -> Rule.DIFF_TAN;
            case COT -> Rule.DIFF_COT;
            case SEC -> Rule.DIFF_SEC;
            case CSC -> Rule.DIFF_CSC;
            case ASIN -> Rule.DIFF_ASIN;
            case ACOS -> Rule.DIFF_ACOS;
            case ATAN -> Rule.DIFF_ATAN;
            case SINH -> Rule.DIFF_SINH;
            case COSH -> Rule.DIFF_COSH;
            case TANH -> Rule.DIFF_TANH;
            case LN -> Rule.DIFF_LN;
            case ABS -> Rule.DIFF_ABS;
            case ERF -> Rule.DIFF_ERF;
            case SI -> Rule.DIFF_SI;
            case CI -> Rule.DIFF_CI;
            case EI -> Rule.DIFF_EI;
            case LI -> Rule.DIFF_LI;
        };
    }

    // ---------------------------------------------------------------- Regla de la cadena

    /**
     * Regla de la cadena: deriva la función exterior {@code F(u)} respecto a {@code u}
     * (con la letra genérica) y multiplica por la derivada de la función interior.
     *
     * @param whole       expresión completa {@code F(inner)}
     * @param inner       función interior
     * @param outerRule   regla que justifica la derivada de la función exterior
     * @param outerBefore {@code F(u)}
     * @param outerAfter  {@code F'(u)}
     * @param extraParams parámetros adicionales del paso de la función exterior
     */
    private Expr chain(Expr whole, Expr inner, Rule outerRule, Expr outerBefore, Expr outerAfter,
                       Map<String, Expr> extraParams) {
        return trace.step(Rule.DIFF_CHAIN, whole, Trace.params("x", x, "u", inner), () -> {
            Map<String, Expr> params = new LinkedHashMap<>();
            params.put("x", u);
            params.putAll(extraParams);
            trace.leaf(outerRule, outerBefore, outerAfter, params);

            Expr innerDerivative = diff(inner);
            return Expr.mul(outerAfter.substitute(uName, inner), innerDerivative);
        });
    }
}
