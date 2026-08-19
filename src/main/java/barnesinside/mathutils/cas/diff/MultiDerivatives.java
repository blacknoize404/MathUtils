package barnesinside.mathutils.cas.diff;

import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Trace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cálculo con varias variables: derivadas parciales sucesivas (mixtas), gradiente, hessiana,
 * jacobiana, divergencia y laplaciano. Cada derivada es una {@link Solution} con sus pasos, así que
 * se puede explicar como cualquier otra.
 * <pre>{@code
 * Solution fxy = MultiDerivatives.mixed("x^2*y^3", List.of("x", "y"));     // ∂/∂y (∂f/∂x)
 * MultiDerivatives.Result grad = MultiDerivatives.gradient("x^2*y", List.of("x", "y"));
 * }</pre>
 */
public final class MultiDerivatives {

    /** Tipo de resultado. */
    public enum Kind {GRADIENT, HESSIAN, JACOBIAN, DIVERGENCE, LAPLACIAN}

    /**
     * Una derivada del resultado: la fila y la columna que ocupa y su solución.
     *
     * @param row    función (para jacobiana y divergencia) o variable de la primera derivada (hessiana)
     * @param column variable de derivación (o de la segunda derivada en la hessiana)
     */
    public record Entry(int row, int column, Solution solution) {
    }

    /**
     * @param kind      tipo de resultado
     * @param variables variables de derivación, en orden
     * @param functions funciones derivadas (una, salvo en jacobiana y divergencia)
     * @param entries   derivadas calculadas, por filas
     * @param rows      filas de la matriz de resultados
     * @param columns   columnas de la matriz de resultados
     * @param total     suma de las derivadas (divergencia y laplaciano), o {@code null}
     */
    public record Result(Kind kind, List<String> variables, List<Expr> functions, List<Entry> entries, int rows,
                         int columns, Expr total) {

        public Result {
            variables = List.copyOf(variables);
            functions = List.copyOf(functions);
            entries = List.copyOf(entries);
        }

        public boolean isSolved() {
            return entries.stream().allMatch(e -> e.solution().isSolved());
        }

        public Entry at(int row, int column) {
            return entries.get(row * columns + column);
        }
    }

    private MultiDerivatives() {
    }

    // ================================================================ Parciales

    /** Variables de {@code f} en orden alfabético: las que se usan cuando no se indican otras. */
    public static List<String> variablesOf(Expr... functions) {
        Set<String> names = new java.util.TreeSet<>();
        for (Expr f : functions) names.addAll(f.variables());
        return new ArrayList<>(names);
    }

    /**
     * Deriva sucesivamente respecto a cada variable de la lista (la primera, primero). Cada
     * derivación es un paso principal con sus reglas dentro. Con una sola variable es la derivada
     * parcial simple. En el resultado, los extras {@code var1}, {@code var2}… recuerdan el orden.
     */
    public static Solution mixed(Expr f, List<String> variables) {
        if (variables.isEmpty()) throw new IllegalArgumentException("Indica al menos una variable");
        Map<String, Expr> extras = new LinkedHashMap<>();
        for (int i = 0; i < variables.size(); i++) extras.put("var" + (i + 1), Expr.sym(variables.get(i)));
        if (variables.size() == 1) {
            Solution single = Derivatives.derivative(f, variables.get(0));
            return new Solution(single.kind(), single.status(), single.problem(), single.result(), single.steps(),
                    single.variable(), single.order(), extras);
        }

        Trace trace = new Trace();
        try {
            Expr current = f;
            for (int k = 0; k < variables.size(); k++) {
                String variable = variables.get(k);
                Expr before = current;
                Set<String> taken = new HashSet<>(before.variables());
                taken.addAll(variables);
                Differentiator differentiator = new Differentiator(variable, taken, trace);
                current = trace.step(Rule.DIFF_PARTIAL, before,
                        Trace.params("x", Expr.sym(variable), "k", Expr.num(k + 1), "n", Expr.num(variables.size())),
                        () -> differentiator.diff(before));
            }
            return new Solution(Solution.Kind.DERIVATIVE, Solution.Status.SOLVED, f, current, trace.steps(),
                    variables.get(0), variables.size(), extras);
        } catch (Trace.BudgetExceededException e) {
            return new Solution(Solution.Kind.DERIVATIVE, Solution.Status.UNSOLVED, f, null, trace.steps(),
                    variables.get(0), variables.size(), extras);
        }
    }

    public static Solution mixed(String source, List<String> variables) {
        return mixed(ExprParser.parse(source), variables);
    }

    // ================================================================ Operadores

    /** Gradiente: la derivada parcial de {@code f} respecto a cada variable. */
    public static Result gradient(Expr f, List<String> variables) {
        List<Entry> entries = new ArrayList<>();
        for (int j = 0; j < variables.size(); j++) {
            entries.add(new Entry(0, j, Derivatives.derivative(f, variables.get(j))));
        }
        return new Result(Kind.GRADIENT, variables, List.of(f), entries, 1, variables.size(), null);
    }

    /** Hessiana: las segundas derivadas {@code ∂²f/∂xj∂xi}, con la fila {@code i} derivada primero. */
    public static Result hessian(Expr f, List<String> variables) {
        int n = variables.size();
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                entries.add(new Entry(i, j, mixed(f, List.of(variables.get(i), variables.get(j)))));
            }
        }
        return new Result(Kind.HESSIAN, variables, List.of(f), entries, n, n, null);
    }

    /** Jacobiana: la derivada de cada función respecto a cada variable (una fila por función). */
    public static Result jacobian(List<Expr> functions, List<String> variables) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < functions.size(); i++) {
            for (int j = 0; j < variables.size(); j++) {
                entries.add(new Entry(i, j, Derivatives.derivative(functions.get(i), variables.get(j))));
            }
        }
        return new Result(Kind.JACOBIAN, variables, functions, entries, functions.size(), variables.size(), null);
    }

    /** Divergencia de un campo: {@code Σ ∂Fi/∂xi}; hace falta una función por variable. */
    public static Result divergence(List<Expr> field, List<String> variables) {
        if (field.size() != variables.size()) {
            throw new IllegalArgumentException("La divergencia necesita tantas funciones como variables");
        }
        List<Entry> entries = new ArrayList<>();
        List<Expr> parts = new ArrayList<>();
        boolean solved = true;
        for (int i = 0; i < field.size(); i++) {
            Solution s = Derivatives.derivative(field.get(i), variables.get(i));
            entries.add(new Entry(i, i, s));
            solved &= s.isSolved();
            if (s.isSolved()) parts.add(s.result());
        }
        return new Result(Kind.DIVERGENCE, variables, field, entries, 1, field.size(), solved ? Expr.add(parts) : null);
    }

    /** Laplaciano: {@code Σ ∂²f/∂xi²}. */
    public static Result laplacian(Expr f, List<String> variables) {
        List<Entry> entries = new ArrayList<>();
        List<Expr> parts = new ArrayList<>();
        boolean solved = true;
        for (int j = 0; j < variables.size(); j++) {
            Solution s = Derivatives.nth(f, variables.get(j), 2);
            entries.add(new Entry(0, j, s));
            solved &= s.isSolved();
            if (s.isSolved()) parts.add(s.result());
        }
        return new Result(Kind.LAPLACIAN, variables, List.of(f), entries, 1, variables.size(), solved ? Expr.add(parts) : null);
    }

    /** Interpreta varias funciones separadas por «;» (para jacobiana y divergencia). */
    public static List<Expr> parseFunctions(String text) {
        List<Expr> functions = new ArrayList<>();
        for (String part : text.split(";")) if (!part.isBlank()) functions.add(ExprParser.parse(part));
        return functions;
    }
}
