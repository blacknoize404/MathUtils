package barnesinside.mathutils.cas.expr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Conjunto de soluciones de una ecuación o inecuación en una variable real.
 */
public sealed interface SolutionSet {

    /** Ningún número cumple la relación. */
    record Empty() implements SolutionSet {
        @Override
        public String toString() {
            return "∅";
        }
    }

    /** Todos los números reales la cumplen. */
    record AllReals() implements SolutionSet {
        @Override
        public String toString() {
            return "ℝ";
        }
    }

    /**
     * Un número finito de valores aislados, sin repetir y de menor a mayor. Si es {@code approximate}, los valores
     * se han encontrado numéricamente y se escriben en decimales.
     */
    record Finite(List<Expr> values, boolean approximate) implements SolutionSet {
        public Finite {
            values = List.copyOf(values);
        }

        public Finite(List<Expr> values) {
            this(values, false);
        }

        @Override
        public String toString() {
            return "{" + values.stream().map(v -> approximate ? decimal(Eval.value(v)) : v.toString())
                    .collect(Collectors.joining(", ")) + "}";
        }
    }

    /** Unión de intervalos disjuntos, de izquierda a derecha. */
    record Intervals(List<Interval> parts) implements SolutionSet {
        public Intervals {
            parts = List.copyOf(parts);
        }

        @Override
        public String toString() {
            return parts.stream().map(Interval::toString).collect(Collectors.joining(" ∪ "));
        }
    }

    /**
     * Familias infinitas de soluciones periódicas: cada expresión depende del parámetro entero {@code parameter}
     * (por ejemplo {@code pi/6 + 2*pi*k}).
     */
    record Periodic(List<Expr> families, String parameter) implements SolutionSet {
        public Periodic {
            families = List.copyOf(families);
        }

        @Override
        public String toString() {
            return "{" + families.stream().map(Expr::toString).collect(Collectors.joining(", ")) + " : " + parameter + " ∈ ℤ}";
        }
    }

    // ---------------------------------------------------------------- Constructores

    static SolutionSet empty() {
        return new Empty();
    }

    static SolutionSet allReals() {
        return new AllReals();
    }

    /** Un decimal con seis cifras significativas y sin ceros de más: {@code 1.41421}. */
    static String decimal(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return String.valueOf(value);
        java.math.BigDecimal rounded = new java.math.BigDecimal(value).round(new java.math.MathContext(6));
        String text = rounded.stripTrailingZeros().toPlainString();
        return text.equals("-0") ? "0" : text;
    }

    /** Como {@link #finite(List)}, pero los valores son aproximaciones numéricas. */
    static SolutionSet approximate(List<Expr> values) {
        return finite(values) instanceof Finite f ? new Finite(f.values(), true) : new Empty();
    }

    /** Valores aislados, ordenados y sin repeticiones; sin valores es el conjunto vacío. */
    static SolutionSet finite(List<Expr> values) {
        // Con letras sin valor (parámetros) no hay orden numérico ni se pueden comparar: se quedan como vienen, sin repetidos exactos
        if (values.stream().anyMatch(v -> !v.variables().isEmpty())) {
            List<Expr> distinct = new ArrayList<>();
            for (Expr v : values) if (!distinct.contains(v)) distinct.add(v);
            return distinct.isEmpty() ? new Empty() : new Finite(distinct);
        }
        List<Expr> sorted = new ArrayList<>();
        for (Expr v : values) {
            boolean seen = sorted.stream().anyMatch(o -> o.equals(v) || close(o, v));
            if (!seen) sorted.add(v);
        }
        sorted.sort(Comparator.comparingDouble(Eval::value));
        return sorted.isEmpty() ? new Empty() : new Finite(sorted);
    }

    /** Une intervalos: los ordena y junta los que se solapan o se tocan sin dejar un hueco. */
    static SolutionSet intervals(List<Interval> parts) {
        List<Interval> sorted = new ArrayList<>(parts);
        sorted.sort(Comparator.comparingDouble(i -> i.lower() == null ? Double.NEGATIVE_INFINITY : Eval.value(i.lower())));

        List<Interval> merged = new ArrayList<>();
        for (Interval next : sorted) {
            if (merged.isEmpty()) {
                merged.add(next);
                continue;
            }
            Interval last = merged.get(merged.size() - 1);
            boolean touches = last.upper() == null || next.lower() == null
                    || Eval.value(next.lower()) < Eval.value(last.upper()) - 1e-12
                    || (close(next.lower(), last.upper()) && (last.upperClosed() || next.lowerClosed()));
            if (!touches) {
                merged.add(next);
                continue;
            }
            Expr upper;
            boolean upperClosed;
            if (last.upper() == null || next.upper() == null) {
                upper = null;
                upperClosed = false;
            } else {
                double a = Eval.value(last.upper());
                double b = Eval.value(next.upper());
                if (Math.abs(a - b) <= 1e-12) {
                    upper = last.upper();
                    upperClosed = last.upperClosed() || next.upperClosed();
                } else if (a > b) {
                    upper = last.upper();
                    upperClosed = last.upperClosed();
                } else {
                    upper = next.upper();
                    upperClosed = next.upperClosed();
                }
            }
            merged.set(merged.size() - 1, new Interval(last.lower(), last.lowerClosed(), upper, upperClosed));
        }
        if (merged.isEmpty()) return new Empty();
        if (merged.size() == 1 && merged.get(0).lower() == null && merged.get(0).upper() == null) return new AllReals();
        return new Intervals(merged);
    }

    private static boolean close(Expr a, Expr b) {
        double x = Eval.value(a);
        double y = Eval.value(b);
        return Math.abs(x - y) <= 1e-12 * Math.max(1, Math.max(Math.abs(x), Math.abs(y)));
    }

    // ---------------------------------------------------------------- Consultas

    /** Si el número pertenece al conjunto. Para las familias periódicas, si es (aproximadamente) de alguna. */
    default boolean contains(double x) {
        return switch (this) {
            case Empty e -> false;
            case AllReals a -> true;
            case Finite f -> f.values().stream().anyMatch(v -> Math.abs(Eval.value(v) - x) <= 1e-9 * Math.max(1, Math.abs(x)));
            case Intervals i -> i.parts().stream().anyMatch(p -> p.contains(x));
            case Periodic p -> {
                for (Expr family : p.families()) {
                    for (int k = -50; k <= 50; k++) {
                        double v = Eval.value(family.substitute(p.parameter(), Expr.num(k)));
                        if (Math.abs(v - x) <= 1e-9 * Math.max(1, Math.abs(x))) yield true;
                    }
                }
                yield false;
            }
        };
    }

    default boolean isEmpty() {
        return this instanceof Empty;
    }

    /** Los valores o extremos que delimitan el conjunto, útiles para dibujarlo. */
    default List<Expr> points() {
        List<Expr> points = new ArrayList<>();
        switch (this) {
            case Finite f -> points.addAll(f.values());
            case Intervals i -> {
                for (Interval p : i.parts()) {
                    if (p.lower() != null) points.add(p.lower());
                    if (p.upper() != null) points.add(p.upper());
                }
            }
            case Periodic p -> {
                for (Expr family : p.families()) {
                    for (int k = -2; k <= 2; k++) points.add(family.substitute(p.parameter(), Expr.num(k)));
                }
            }
            default -> { }
        }
        return points;
    }

    /** Texto plano: {@code {-2, 3}}, {@code (-∞, 2] ∪ (3, ∞)}, {@code ℝ} o {@code ∅}. */
    String toString();

    default String toLatex() {
        return switch (this) {
            case Empty e -> "\\varnothing";
            case AllReals a -> "\\mathbb{R}";
            case Finite f -> "\\left\\{" + f.values().stream().map(Expr::toLatex).collect(Collectors.joining(", ")) + "\\right\\}";
            case Intervals i -> i.parts().stream().map(Interval::toLatex).collect(Collectors.joining(" \\cup "));
            case Periodic p -> "\\left\\{" + p.families().stream().map(Expr::toLatex).collect(Collectors.joining(", "))
                    + " : " + Expr.sym(p.parameter()).toLatex() + " \\in \\mathbb{Z}\\right\\}";
        };
    }
}
