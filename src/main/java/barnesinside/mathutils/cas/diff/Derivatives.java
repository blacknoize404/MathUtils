package barnesinside.mathutils.cas.diff;

import barnesinside.mathutils.cas.expr.Algebra;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.poly.Factor;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Trace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Derivación simbólica paso a paso.
 * <pre>{@code
 * Solution s = Derivatives.derivative("x^2*sin(x)", "x");
 * s.result();                                   // 2*x*sin(x) + x^2*cos(x)
 * s.render(Locale.forLanguageTag("es"), Format.LATEX);
 * }</pre>
 * Las derivadas respecto a una variable de una expresión con varias variables son derivadas
 * parciales: las demás variables se tratan como constantes.
 */
public final class Derivatives {

    private Derivatives() {
    }

    /**
     * Deriva registrando las reglas en la traza dada, para que otros motores (límites, integrales)
     * incluyan los pasos de la derivada como subpasos.
     */
    public static Expr differentiate(Expr f, String variable, Trace trace) {
        Set<String> taken = new HashSet<>(f.variables());
        taken.add(variable);
        return new Differentiator(variable, taken, trace).diff(f);
    }

    /**
     * Deriva respecto a {@code variable}.
     * @return la solución con el resultado y los pasos; si se agota el tiempo o la profundidad
     * el estado es {@link Solution.Status#UNSOLVED}
     */
    public static Solution derivative(Expr f, String variable) {
        return nth(f, variable, 1);
    }

    /**
     * Interpreta el texto y lo deriva.
     * @throws barnesinside.mathutils.exceptions.ExpressionParseException si el texto no es una expresión válida
     */
    public static Solution derivative(String source, String variable) {
        ExprParser.Parsed parsed = ExprParser.parseDetailed(source);
        return derivative(parsed.expr(), variable).withPrelude(parsed.steps());
    }

    /**
     * Derivada de orden {@code order} (≥ 1). Cada derivación sucesiva es un paso de nivel superior.
     */
    public static Solution nth(Expr f, String variable, int order) {
        return nth(f, variable, order, DerivativeMethod.AUTO);
    }

    /** Deriva con el planteamiento indicado (en cada derivación sucesiva si el orden es mayor que 1). */
    public static Solution derivative(Expr f, String variable, DerivativeMethod method) {
        return nth(f, variable, 1, method);
    }

    /**
     * Derivada de orden {@code order} con el planteamiento indicado. Si el método no cambia nada para la expresión
     * (por ejemplo, desarrollar algo que ya está desarrollado), se deriva como con {@link DerivativeMethod#AUTO}.
     */
    public static Solution nth(Expr f, String variable, int order, DerivativeMethod method) {
        if (order < 1) throw new IllegalArgumentException("El orden debe ser al menos 1");

        Trace trace = new Trace();
        Set<String> taken = new HashSet<>(f.variables());
        taken.add(variable);
        Differentiator differentiator = new Differentiator(variable, taken, trace);
        Differentiator asProduct = new Differentiator(variable, taken, trace, true);
        Expr variableExpr = Expr.sym(variable);

        try {
            Expr current = f;
            for (int k = 1; k <= order; k++) {
                Expr before = current;
                int degree = k;
                current = order == 1
                        ? withMethod(trace, differentiator, asProduct, method, before, variable)
                        : trace.step(Rule.DIFF_ORDER, before, Trace.params("x", variableExpr, "k", Expr.num(degree)),
                        () -> withMethod(trace, differentiator, asProduct, method, before, variable));
            }
            return new Solution(Solution.Kind.DERIVATIVE, Solution.Status.SOLVED, f, current,
                    trace.steps(), variable, order);
        } catch (Trace.BudgetExceededException | UnsupportedOperationException e) {
            return new Solution(Solution.Kind.DERIVATIVE, Solution.Status.UNSOLVED, f, null,
                    trace.steps(), variable, order);
        }
    }

    // ================================================================ Métodos

    /**
     * Los planteamientos con los que se puede derivar la expresión: el automático y los que de verdad cambian algo.
     * Vacío si la expresión no depende de la variable.
     */
    public static List<DerivativeMethod> applicableMethods(Expr f, String variable) {
        List<DerivativeMethod> methods = new ArrayList<>();
        if (f.isFreeOf(variable)) return methods;
        methods.add(DerivativeMethod.AUTO);
        for (DerivativeMethod m : DerivativeMethod.values()) {
            if (m != DerivativeMethod.AUTO && rewrite(f, variable, m).isPresent()) methods.add(m);
        }
        return methods;
    }

    private static Expr withMethod(Trace trace, Differentiator plain, Differentiator asProduct, DerivativeMethod method,
                                   Expr before, String variable) {
        Optional<Expr> rewritten = rewrite(before, variable, method);
        if (rewritten.isEmpty()) return plain.diff(before);

        Expr x = Expr.sym(variable);
        return switch (method) {
            case EXPAND_FIRST -> trace.step(Rule.DIFF_EXPAND_FIRST, before, Trace.params("x", x, "f", before, "g", rewritten.get()),
                    () -> plain.diff(rewritten.get()));
            case SIMPLIFY_FIRST -> trace.step(Rule.DIFF_SIMPLIFY_FIRST, before, Trace.params("x", x, "f", before, "g", rewritten.get()),
                    () -> plain.diff(rewritten.get()));
            case QUOTIENT_AS_PRODUCT -> trace.step(Rule.DIFF_QUOTIENT_AS_PRODUCT, before, Trace.params("x", x, "f", before),
                    () -> asProduct.diff(before));
            case LOGARITHMIC -> trace.step(Rule.DIFF_LOGARITHMIC, before, Trace.params("x", x, "f", before, "g", rewritten.get()),
                    () -> Expr.mul(before, plain.diff(rewritten.get())));
            case AUTO -> plain.diff(before);
        };
    }

    /**
     * La expresión sobre la que se deriva con ese método ({@code ln f} desarrollado en la derivación logarítmica), o
     * vacío si el método no cambia nada.
     */
    private static Optional<Expr> rewrite(Expr f, String v, DerivativeMethod method) {
        switch (method) {
            case EXPAND_FIRST -> {
                Expr expanded = Expand.expand(f);
                return expanded.equals(f) ? Optional.empty() : Optional.of(expanded);
            }
            case SIMPLIFY_FIRST -> {
                Algebra.Fraction fraction = Algebra.fraction(Algebra.together(f));
                if (fraction.denominator().isOne() || fraction.denominator().isFreeOf(v)) return Optional.empty();
                Expr simplified = Expr.div(Factor.factor(fraction.numerator()), Factor.factor(fraction.denominator()));
                return simplified.equals(f) ? Optional.empty() : Optional.of(simplified);
            }
            case QUOTIENT_AS_PRODUCT -> {
                return hasQuotient(f, v) ? Optional.of(f) : Optional.empty();
            }
            case LOGARITHMIC -> {
                boolean product = f instanceof Expr.Mul m && m.factors().stream().filter(t -> !t.isFreeOf(v)).count() >= 2;
                boolean variablePower = f instanceof Expr.Pow p && !p.base().isFreeOf(v) && !p.exponent().isFreeOf(v);
                return product || variablePower ? Optional.of(logExpand(f, v)) : Optional.empty();
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    /** Si hay un cociente con la variable en el denominador en el nivel superior de la expresión. */
    private static boolean hasQuotient(Expr f, String v) {
        if (f instanceof Expr.Mul m) {
            boolean numerator = false;
            boolean denominator = false;
            for (Expr t : m.factors()) {
                if (t.isFreeOf(v)) continue;
                if (t instanceof Expr.Pow p && p.exponent() instanceof Expr.Num n && n.value().signum() < 0) denominator = true;
                else numerator = true;
            }
            return numerator && denominator;
        }
        if (f instanceof Expr.Add a) return a.terms().stream().anyMatch(t -> hasQuotient(t, v));
        return false;
    }

    /** {@code ln f} escrito con las propiedades de los logaritmos: producto en suma y potencia en producto. */
    private static Expr logExpand(Expr f, String v) {
        return switch (f) {
            case Expr.Mul m -> Expr.add(m.factors().stream().filter(t -> !t.isFreeOf(v)).map(t -> logExpand(t, v)).toList());
            case Expr.Pow p -> Expr.mul(p.exponent(), logExpand(p.base(), v));
            default -> Expr.ln(f);
        };
    }
}
