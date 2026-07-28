package MathUtils.cas.integral;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.expr.Fn;
import MathUtils.cas.poly.Polynomial;
import MathUtils.cas.steps.Solution;
import MathUtils.cas.steps.Trace;

import java.util.Optional;

/**
 * Integrales indefinidas paso a paso.
 * <pre>{@code
 * Solution s = Integrals.indefinite("x*e^x", "x");
 * s.result();   // (x - 1)*e^x   (se muestra más "+ C")
 * }</pre>
 * El motor usa tabla, linealidad, sustitución, expansión e integración por partes. Todo resultado
 * se verifica derivándolo. Si no encuentra la antiderivada, el estado es
 * {@link Solution.Status#UNSOLVED}; si el integrando es de los que no tienen antiderivada
 * elemental (como {@code e^(x²)} o {@code sin(x)/x}), el estado es {@link Solution.Status#NON_ELEMENTAL}.
 */
public final class Integrals {

    private Integrals() {
    }

    /**
     * @throws MathUtils.exceptions.ExpressionParseException si el texto no es una expresión válida
     */
    public static Solution indefinite(String integrand, String variable) {
        return indefinite(ExprParser.parse(integrand), variable);
    }

    /**
     * Integral definida o impropia entre dos extremos, que pueden ser {@code oo} y {@code -oo}.
     * Si diverge, el estado es {@link Solution.Status#DOES_NOT_EXIST}; si el resultado exacto no se
     * puede contrastar con la cuadratura numérica, el estado es {@link Solution.Status#UNSOLVED}. En
     * todo caso el campo {@code approximation} de los extras trae el valor decimal si se pudo calcular.
     */
    public static Solution definite(String integrand, String variable, String lower, String upper) {
        return definite(ExprParser.parse(integrand), variable, ExprParser.parse(lower), ExprParser.parse(upper));
    }

    public static Solution definite(Expr integrand, String variable, Expr lower, Expr upper) {
        return DefiniteIntegrals.solve(integrand, variable, lower, upper);
    }

    /**
     * Integra respecto a {@code variable}. El resultado no incluye la constante de integración.
     */
    public static Solution indefinite(Expr integrand, String variable) {
        Trace trace = new Trace(200, 4000);
        Integrator integrator = new Integrator(trace, 700);

        try {
            Optional<Expr> result = integrator.integrate(integrand, variable, 0);
            if (result.isPresent() && Integrator.verified(integrand, result.get(), variable)) {
                return new Solution(Solution.Kind.INTEGRAL, Solution.Status.SOLVED, integrand, result.get(),
                        trace.steps(), variable, 1);
            }
        } catch (Trace.BudgetExceededException | ArithmeticException e) {
            // Se informa como no resuelta
        }

        Solution.Status status = isKnownNonElementary(integrand, variable)
                ? Solution.Status.NON_ELEMENTAL
                : Solution.Status.UNSOLVED;
        return new Solution(Solution.Kind.INTEGRAL, status, integrand, null, trace.steps(), variable, 1);
    }

    // ================================================================ Integrales sin antiderivada elemental

    /**
     * Reconoce integrandos clásicos sin antiderivada elemental (salvo un factor constante):
     * {@code e^(p(x))} con {@code p} de grado ≥ 2, {@code sin(x)/x}, {@code cos(x)/x},
     * {@code e^x/x}, {@code 1/ln(x)}, {@code x^x} y {@code sin(p(x))}, {@code cos(p(x))} con grado ≥ 2.
     */
    static boolean isKnownNonElementary(Expr f, String v) {
        Expr x = Expr.sym(v);
        Expr core = withoutConstantFactor(f, v);

        if (core instanceof Expr.Pow p) {
            if (p.base().equals(Expr.E) && degreeAtLeastTwo(p.exponent(), v)) return true;
            if (p.base().equals(x) && p.exponent().equals(x)) return true;
            if (p.exponent().equals(Expr.MINUS_ONE) && p.base().equals(Expr.ln(x))) return true;
        }
        if (core instanceof Expr.Func fn && (fn.fn() == Fn.SIN || fn.fn() == Fn.COS) && degreeAtLeastTwo(fn.arg(), v)) {
            return true;
        }
        if (core.equals(Expr.div(Expr.sin(x), x)) || core.equals(Expr.div(Expr.cos(x), x))
                || core.equals(Expr.div(Expr.exp(x), x))) {
            return true;
        }
        return false;
    }

    private static Expr withoutConstantFactor(Expr f, String v) {
        if (!(f instanceof Expr.Mul m)) return f;
        java.util.List<Expr> varying = m.factors().stream().filter(t -> !t.isFreeOf(v)).toList();
        return varying.isEmpty() ? f : Expr.mul(varying);
    }

    private static boolean degreeAtLeastTwo(Expr e, String v) {
        Optional<Polynomial> polynomial = Polynomial.from(e, v);
        return polynomial.isPresent() && polynomial.get().degree() >= 2;
    }
}
