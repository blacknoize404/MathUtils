package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Op;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.expr.Relation;
import barnesinside.mathutils.cas.expr.SolutionSet;
import barnesinside.mathutils.cas.poly.Factor;
import barnesinside.mathutils.cas.poly.Polynomial;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ecuaciones en una variable real, paso a paso.
 * <pre>{@code
 * Solution s = Equations.solve("x^2 - 5x + 6 = 0");
 * s.answer();                                          // {2, 3}
 * Equations.applicableMethods(rel, "x");               // AUTO, FACTORING, COMPLETE_SQUARE, QUADRATIC_FORMULA
 * Equations.solve(rel, "x", SolveMethod.COMPLETE_SQUARE);
 * }</pre>
 * Cubre primer y segundo grado, polinomios de grado mayor, fracciones algebraicas, radicales, valor absoluto,
 * exponenciales, logaritmos y trigonometría; lo que no tiene fórmula se busca numéricamente y se marca como aproximado
 * (el extra {@code approximate} vale 1). Solo se dan soluciones reales. Las inecuaciones se resuelven con
 * {@link Inequalities}; {@link #solve(Relation, String, SolveMethod)} las reenvía allí.
 */
public final class Equations {

    private Equations() {
    }

    /** Clase de ecuación según su forma. */
    enum Kind {CONSTANT, POLYNOMIAL, ABSOLUTE, RADICAL, LOG_EXP, TRIG, RATIONAL, OTHER}

    // ================================================================ API

    /**
     * Resuelve el texto como ecuación o inecuación en su única variable ({@code x} si hay varias).
     * @throws IllegalArgumentException si el texto no es una relación entre dos expresiones
     */
    public static Solution solve(String text) {
        Relation relation = Relation.parse(text).orElseThrow(() -> new IllegalArgumentException("No es una ecuación: " + text));
        return solve(relation, mainVariable(relation));
    }

    public static Solution solve(Relation relation, String variable) {
        return solve(relation, variable, SolveMethod.AUTO);
    }

    /**
     * Resuelve con el método pedido. Si el método no sirve para la ecuación, el estado es
     * {@link Solution.Status#UNSOLVED} y hay un paso que lo dice.
     */
    public static Solution solve(Relation relation, String variable, SolveMethod method) {
        if (relation.op() != Op.EQ) return Inequalities.solve(relation, variable, method);

        Rec rec = new Rec(new Trace(200, 10_000));
        Expr problem = relation.difference();
        for (String other : relation.variables()) {
            if (!other.equals(variable)) return unsolved(relation, variable, rec);
        }
        if (method != SolveMethod.AUTO && !applicableMethods(relation, variable).contains(method)) {
            rec.note(Rule.POLY_NOT_APPLICABLE, "f", problem);
            return unsolved(relation, variable, rec);
        }

        EquationSolver solver = new EquationSolver(rec, variable, method);
        try {
            SolutionSet answer = solver.solve(relation);
            Map<String, Expr> extras = solver.approximate() ? Map.of("approximate", Expr.ONE) : Map.of();
            return new Solution(Solution.Kind.EQUATION, Solution.Status.SOLVED, problem, null, rec.steps(), variable, 1,
                    extras, relation, answer);
        } catch (Trace.BudgetExceededException | ArithmeticException | IllegalArgumentException e) {
            // Sin tiempo, sin fórmula o con demasiadas soluciones: se dice sin inventar una respuesta, y nunca se lanza
            if (e instanceof EquationSolver.TooManyRootsException) rec.note(Rule.ALG_NUMERIC_TOO_MANY, "f", problem, "n", Expr.num(EquationSolver.MAX_NUMERIC_ROOTS));
            return unsolved(relation, variable, rec);
        }
    }

    private static Solution unsolved(Relation relation, String variable, Rec rec) {
        return new Solution(Solution.Kind.EQUATION, Solution.Status.UNSOLVED, relation.difference(), null, rec.steps(), variable,
                1, Map.of(), relation, null);
    }

    /**
     * Los métodos con los que se puede resolver la ecuación. Vacío si ninguno sirve; si no, empieza por
     * {@link SolveMethod#AUTO}.
     */
    public static List<SolveMethod> applicableMethods(Relation relation, String variable) {
        List<SolveMethod> methods = new ArrayList<>();
        Expr f = relation.difference();
        EquationSolver probe = new EquationSolver(Rec.scratch(), variable, SolveMethod.AUTO);

        switch (probe.kind(f)) {
            case CONSTANT -> { }
            case POLYNOMIAL -> {
                Polynomial p = Polynomial.from(f, variable).orElseThrow();
                switch (p.degree()) {
                    case 1 -> methods.add(SolveMethod.ISOLATE);
                    case 2 -> {
                        Rational a = p.coefficient(2);
                        Rational b = p.coefficient(1);
                        Rational c = p.coefficient(0);
                        Rational d = b.multiply(b).subtract(Rational.of(4).multiply(a).multiply(c));
                        if (b.isZero()) methods.add(SolveMethod.SQUARE_ROOT);
                        if (EquationSolver.isRationalSquare(d)) methods.add(SolveMethod.FACTORING);
                        methods.add(SolveMethod.COMPLETE_SQUARE);
                        methods.add(SolveMethod.QUADRATIC_FORMULA);
                    }
                    default -> {
                        Expr expanded = Expand.expand(p.toExpr(variable));
                        if (Factoring.applicableMethods(expanded).size() > 1) methods.add(SolveMethod.FACTORING);
                        if (Factor.rationalRoot(p).isPresent()) methods.add(SolveMethod.RATIONAL_ROOTS);
                        int k = 0;
                        for (int i = 1; i <= p.degree(); i++) if (!p.coefficient(i).isZero()) k = gcd(k, i);
                        if (k >= 2) methods.add(SolveMethod.SUBSTITUTION);
                        methods.add(SolveMethod.NUMERIC);
                    }
                }
            }
            case ABSOLUTE -> methods.addAll(List.of(SolveMethod.ABSOLUTE_CASES, SolveMethod.NUMERIC));
            case RADICAL -> methods.addAll(List.of(SolveMethod.ISOLATE_RADICAL, SolveMethod.NUMERIC));
            case LOG_EXP -> methods.addAll(List.of(SolveMethod.LOGARITHMS, SolveMethod.NUMERIC));
            case TRIG -> methods.addAll(List.of(SolveMethod.INVERSE_TRIG, SolveMethod.NUMERIC));
            case RATIONAL -> methods.addAll(List.of(SolveMethod.ZERO_NUMERATOR, SolveMethod.NUMERIC));
            case OTHER -> methods.add(SolveMethod.NUMERIC);
        }
        if (!methods.isEmpty()) methods.add(0, SolveMethod.AUTO);
        return methods;
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    /** La única variable de la relación, {@code x} si hay varias y la contiene, o la primera por orden alfabético. */
    public static String mainVariable(Relation relation) {
        List<String> variables = relation.variables().stream().sorted().toList();
        if (variables.size() == 1) return variables.get(0);
        if (variables.contains("x")) return "x";
        return variables.isEmpty() ? "x" : variables.get(0);
    }

    /** Si el resultado de la ecuación son solo aproximaciones numéricas. */
    public static boolean isApproximate(Solution s) {
        return s.extras().containsKey("approximate");
    }

    static Optional<SolutionSet> answerOf(Solution s) {
        return Optional.ofNullable(s.answer());
    }
}
