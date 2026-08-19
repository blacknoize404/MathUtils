package barnesinside.mathutils.calculus;

import barnesinside.mathutils.algebra.Const;
import barnesinside.mathutils.algebra.Function;
import barnesinside.mathutils.algebra.Simplifier;
import barnesinside.mathutils.algebra.Var;
import barnesinside.mathutils.algebra.exponentials.Pow;
import barnesinside.mathutils.algebra.exponentials.Root;
import barnesinside.mathutils.algebra.logarithmics.Log;
import barnesinside.mathutils.algebra.logarithmics.LogN;
import barnesinside.mathutils.algebra.operators.DivOperator;
import barnesinside.mathutils.algebra.operators.MultOperator;
import barnesinside.mathutils.algebra.operators.SubOperator;
import barnesinside.mathutils.algebra.operators.SumOperator;
import barnesinside.mathutils.algebra.operators.primitives.Operator;
import barnesinside.mathutils.algebra.primitives.Expression;
import barnesinside.mathutils.algebra.primitives.MathElement;
import barnesinside.mathutils.algebra.trigonometrics.Cos;
import barnesinside.mathutils.algebra.trigonometrics.Cot;
import barnesinside.mathutils.algebra.trigonometrics.Sin;
import barnesinside.mathutils.algebra.trigonometrics.Tan;

import java.util.ArrayList;
import java.util.List;

/**
 * Diferenciación simbólica: reglas de la suma, el producto, el cociente, la potencia
 * y la cadena, con derivadas de las funciones trigonométricas y logarítmicas.
 * <p>Los resultados se devuelven ya simplificados con {@link Simplifier}.
 * También ofrece derivadas parciales, gradiente, jacobiano y hessiana.
 */
public final class Derivative {

    private Derivative() {
    }

    /**
     * Deriva la expresión respecto a una variable (derivada parcial si hay más variables).
     * @throws UnsupportedOperationException si la expresión contiene un elemento sin regla de derivación
     */
    public static MathElement of(MathElement element, String variable) {
        return Simplifier.simplify(differentiate(element, variable));
    }

    /**
     * Derivada de orden {@code order} (≥ 0).
     */
    public static MathElement of(MathElement element, String variable, int order) {
        if (order < 0) throw new IllegalArgumentException("El orden debe ser mayor o igual que 0");
        MathElement result = element;
        for (int i = 0; i < order; i++) result = of(result, variable);
        return result;
    }

    /**
     * Gradiente: arreglo de derivadas parciales en el orden de las variables recibidas.
     */
    public static MathElement[] gradient(MathElement element, String... variables) {
        MathElement[] gradient = new MathElement[variables.length];
        for (int i = 0; i < variables.length; i++) gradient[i] = of(element, variables[i]);
        return gradient;
    }

    /**
     * Matriz jacobiana: {@code J[i][j] = ∂f_i/∂x_j}.
     */
    public static MathElement[][] jacobian(MathElement[] functions, String... variables) {
        MathElement[][] jacobian = new MathElement[functions.length][];
        for (int i = 0; i < functions.length; i++) jacobian[i] = gradient(functions[i], variables);
        return jacobian;
    }

    /**
     * Matriz hessiana: {@code H[i][j] = ∂²f/∂x_i∂x_j}.
     */
    public static MathElement[][] hessian(MathElement element, String... variables) {
        MathElement[][] hessian = new MathElement[variables.length][variables.length];
        for (int i = 0; i < variables.length; i++) {
            MathElement first = of(element, variables[i]);
            for (int j = 0; j < variables.length; j++) hessian[i][j] = of(first, variables[j]);
        }
        return hessian;
    }

    // ---------------------------------------------------------------- Reglas

    private static MathElement differentiate(MathElement e, String v) {
        if (e instanceof Function f) return differentiate(f.getContent(), v);
        if (e instanceof Const) return new Const(0);
        if (e instanceof Var var) return new Const(var.getArgument().equalsIgnoreCase(v) ? 1 : 0);

        if (e instanceof SumOperator op) return new SumOperator(first(derivatives(op, v)), second(derivatives(op, v)), rest(derivatives(op, v)));
        if (e instanceof SubOperator op) return new SubOperator(first(derivatives(op, v)), second(derivatives(op, v)), rest(derivatives(op, v)));
        if (e instanceof MultOperator op) return productRule(op.getContent(), v);
        if (e instanceof DivOperator op) return quotientRule(op.getContent(), v);

        if (e instanceof Pow p) return powerRule(p, v);
        if (e instanceof Root r) return differentiate(r.toPow(), v);

        if (e instanceof LogN l) return new DivOperator(differentiate(l.getArgument(), v), l.getArgument());
        if (e instanceof Log l) return logRule(l, v);

        if (e instanceof Sin s) {
            return new MultOperator(new Cos(s.getArgument()), differentiate(s.getArgument(), v));
        }
        if (e instanceof Cos c) {
            return new MultOperator(-1, new Sin(c.getArgument()), differentiate(c.getArgument(), v));
        }
        if (e instanceof Tan t) {
            return new DivOperator(differentiate(t.getArgument(), v), new Pow(new Cos(t.getArgument()), 2));
        }
        if (e instanceof Cot c) {
            return new DivOperator(
                    new MultOperator(-1, differentiate(c.getArgument(), v)),
                    new Pow(new Sin(c.getArgument()), 2));
        }

        throw new UnsupportedOperationException("No hay regla de derivación para " + e.getClass().getSimpleName());
    }

    private static List<MathElement> derivatives(Operator op, String v) {
        List<MathElement> result = new ArrayList<>();
        for (MathElement element : op.getContent()) result.add(differentiate(element, v));
        return result;
    }

    private static MathElement first(List<MathElement> l) {
        return l.get(0);
    }

    private static MathElement second(List<MathElement> l) {
        return l.get(1);
    }

    private static MathElement[] rest(List<MathElement> l) {
        return l.subList(2, l.size()).toArray(MathElement[]::new);
    }

    /** (f1·f2·…·fn)' = Σ fi' · Π(j≠i) fj */
    private static MathElement productRule(List<MathElement> factors, String v) {
        List<MathElement> terms = new ArrayList<>();
        for (int i = 0; i < factors.size(); i++) {
            List<MathElement> product = new ArrayList<>(factors);
            product.set(i, differentiate(factors.get(i), v));
            terms.add(multiply(product));
        }
        return new SumOperator(first(terms), second(terms), rest(terms));
    }

    /** (u/v)' = (u'v − uv') / v², aplicado de izquierda a derecha para divisiones encadenadas. */
    private static MathElement quotientRule(List<MathElement> elements, String v) {
        MathElement u = elements.get(0);
        MathElement du = differentiate(u, v);

        for (MathElement divisor : elements.subList(1, elements.size())) {
            MathElement dv = differentiate(divisor, v);
            du = new DivOperator(
                    new SubOperator(new MultOperator(du, divisor), new MultOperator(u, dv)),
                    new Pow(divisor, 2));
            u = new DivOperator(u, divisor);
        }
        return du;
    }

    private static MathElement powerRule(Pow p, String v) {
        MathElement base = p.getArgument();
        MathElement exponent = p.getExponent();
        boolean baseDepends = dependsOn(base, v);
        boolean exponentDepends = dependsOn(exponent, v);

        if (!exponentDepends) {
            // (f^n)' = n · f^(n-1) · f'
            return new MultOperator(exponent, new Pow(base, new SubOperator(exponent, new Const(1))), differentiate(base, v));
        }
        if (!baseDepends) {
            // (a^g)' = a^g · ln(a) · g'
            return new MultOperator(p, new LogN(base), differentiate(exponent, v));
        }
        // (f^g)' = f^g · (g' ln f + g f' / f)
        return new MultOperator(p, new SumOperator(
                new MultOperator(differentiate(exponent, v), new LogN(base)),
                new DivOperator(new MultOperator(exponent, differentiate(base, v)), base)));
    }

    private static MathElement logRule(Log l, String v) {
        if (!dependsOn(l.getBase(), v)) {
            // (log_b f)' = f' / (f · ln b)
            return new DivOperator(differentiate(l.getArgument(), v), new MultOperator(l.getArgument(), new LogN(l.getBase())));
        }
        // log_b f = ln f / ln b
        return quotientRule(List.of(new LogN(l.getArgument()), new LogN(l.getBase())), v);
    }

    private static MathElement multiply(List<MathElement> factors) {
        if (factors.size() == 1) return factors.get(0);
        return new MultOperator(first(factors), second(factors), rest(factors));
    }

    // ---------------------------------------------------------------- Dependencia

    /**
     * Indica si la expresión contiene la variable.
     */
    public static boolean dependsOn(MathElement e, String variable) {
        if (e instanceof Var v) return v.getArgument().equalsIgnoreCase(variable);
        if (e instanceof Const) return false;
        if (e instanceof Function f) return dependsOn(f.getContent(), variable);
        if (e instanceof Operator op) return op.getContent().stream().anyMatch(x -> dependsOn(x, variable));
        if (e instanceof Pow p) return dependsOn(p.getArgument(), variable) || dependsOn(p.getExponent(), variable);
        if (e instanceof Root r) return dependsOn(r.getArgument(), variable) || dependsOn(r.getRoot(), variable);
        if (e instanceof Log l) return dependsOn(l.getArgument(), variable) || dependsOn(l.getBase(), variable);
        if (e instanceof Expression<?> expression && expression.getArgument() instanceof MathElement inner) {
            return dependsOn(inner, variable);
        }
        // Elemento desconocido: se asume que depende para no simplificar de forma incorrecta.
        return true;
    }
}
