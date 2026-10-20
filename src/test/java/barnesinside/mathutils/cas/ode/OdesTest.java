package barnesinside.mathutils.cas.ode;

import barnesinside.mathutils.cas.diff.Derivatives;
import barnesinside.mathutils.cas.expr.Equivalence;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Relation;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Messages;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import barnesinside.mathutils.cas.steps.Step;
import barnesinside.mathutils.cas.steps.Trace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class OdesTest {

    private static List<Rule> rules(List<Step> steps) {
        List<Rule> all = new ArrayList<>();
        for (Step s : steps) {
            all.add(s.rule());
            all.addAll(rules(s.substeps()));
        }
        return all;
    }

    private static void uses(Solution s, Rule... expected) {
        List<Rule> all = rules(s.steps());
        for (Rule r : expected) assert all.contains(r) : r + " falta en " + all;
    }

    /** La solución explícita cumple la ecuación: se sustituye {@code y} y sus derivadas y la diferencia es cero. */
    private static void satisfies(String text, Solution s) {
        assert s.isSolved() : text + " " + s.status();
        for (Relation r : Odes.solutions(s)) {
            if (!r.left().equals(Expr.sym("y"))) continue;
            Relation ode = Odes.parse(text);
            Expr difference = ode.difference();
            int order = Odes.orderOf(difference, "y");
            Expr derivative = r.right();
            Expr substituted = difference.substitute("y", r.right());
            for (int k = 1; k <= order; k++) {
                derivative = Derivatives.differentiate(derivative, "x", new Trace());
                substituted = substituted.substitute("y" + "'".repeat(k), derivative);
            }
            assert Equivalence.areEqual(substituted, Expr.ZERO) : text + " con " + r + " deja " + substituted;
        }
    }

    private static Solution solve(String text, Rule... expected) {
        Solution s = Odes.solve(text);
        satisfies(text, s);
        uses(s, expected);
        // Los pasos se pueden escribir en los dos idiomas y en todos los formatos
        for (Locale locale : Messages.supportedLocales()) {
            for (Format f : Format.values()) assert !s.render(locale, f).isBlank();
        }
        return s;
    }

    @Test
    void separableEquationsAreSeparatedAndIntegrated() {
        Solution s = solve("y' = x*y", Rule.ODE_SEPARATE, Rule.ODE_INTEGRATE_SIDES, Rule.ODE_ISOLATE);
        assert Odes.solutions(s).getFirst().left().equals(Expr.sym("y")) : s;
        solve("y' = -2*x*y", Rule.ODE_SEPARATE);
        solve("y' = y^2", Rule.ODE_SEPARATE);
        solve("y' = (1 + y^2)*cos(x)", Rule.ODE_SEPARATE);
    }

    @Test
    void linearEquationsUseTheIntegratingFactor() {
        solve("y' + 2*y = e^x", Rule.ODE_LINEAR_FORM, Rule.ODE_INTEGRATING_FACTOR, Rule.ODE_MULTIPLY_FACTOR, Rule.ODE_LINEAR_INTEGRATE);
        solve("y' - y/x = x^2", Rule.ODE_INTEGRATING_FACTOR);
        solve("y' + y*cos(x) = sin(x)*cos(x)", Rule.ODE_INTEGRATING_FACTOR);
    }

    @Test
    void bernoulliEquationsBecomeLinear() {
        solve("y' + y = y^2*e^x", Rule.ODE_BERNOULLI_FORM, Rule.ODE_BERNOULLI_SUBSTITUTION, Rule.ODE_BERNOULLI_BACK, Rule.ODE_LINEAR_FORM);
        solve("y' = y + x*y^3", Rule.ODE_BERNOULLI_SUBSTITUTION);
    }

    @Test
    void exactEquationsGiveAnImplicitSolution() {
        String text = "(2*x*y + 3)*dx + (x^2 - 1)*dy = 0";
        Solution s = Odes.solve(text, OdeMethod.EXACT);
        assert s.isSolved() : s.status();
        uses(s, Rule.ODE_EXACT_FORM, Rule.ODE_EXACT_TEST, Rule.ODE_EXACT_POTENTIAL, Rule.ODE_EXACT_FIND_G, Rule.ODE_EXACT_RESULT);
        // F(x, y) = C con F = x²y + 3x − y
        Relation r = Odes.solutions(s).getFirst();
        assert r.right().equals(Expr.sym("C")) : r;
        Expr f = r.left();
        double dx = Math.abs(barnesinside.mathutils.cas.expr.Eval.value(Expr.sub(f.substitute("x", Expr.num(2)).substitute("y", Expr.num(3)),
                Expr.add(Expr.mul(Expr.num(4), Expr.num(3)), Expr.num(6), Expr.num(-3)))));
        assert dx < 1e-9 : f;
    }

    @Test
    void anIntegratingFactorOfOneVariableMakesItExact() {
        Solution s = Odes.solve("y*dx + (x^2*y - x)*dy = 0", OdeMethod.EXACT);
        assert s.isSolved() : s.status();
        uses(s, Rule.ODE_NOT_EXACT, Rule.ODE_EXACT_FACTOR);
    }

    @Test
    void homogeneousEquationsUseTheSubstitution() {
        Solution s = Odes.solve("y' = (x + y)/(x - y)", OdeMethod.HOMOGENEOUS);
        assert s.isSolved() : s.status();
        uses(s, Rule.ODE_HOMOGENEOUS_TEST, Rule.ODE_HOMOGENEOUS_SUBSTITUTION, Rule.ODE_SEPARATE, Rule.ODE_HOMOGENEOUS_BACK);
        Solution explicit = Odes.solve("y' = (y^2 + x*y)/x^2", OdeMethod.HOMOGENEOUS);
        assert explicit.isSolved() : explicit.status();
        satisfies("y' = (y^2 + x*y)/x^2", explicit);
    }

    @Test
    void anImplicitEquationHasABranchPerRoot() {
        Solution s = Odes.solve("(y')^2 = y");
        assert s.isSolved() : s.status();
        assert Odes.solutions(s).size() == 2 : Odes.solutions(s);
        uses(s, Rule.ODE_SOLVE_FOR_DERIVATIVE, Rule.ODE_BRANCH, Rule.ODE_SEPARATE);
    }

    @Test
    void aMethodThatDoesNotApplyIsSaidSo() {
        Solution s = Odes.solve("y' = x + y", OdeMethod.SEPARABLE);
        assert s.status() == Solution.Status.UNSOLVED;
        uses(s, Rule.ODE_NOT_APPLICABLE);
        List<OdeMethod> methods = Odes.applicableMethods(Odes.parse("y' = x*y"), "x", "y");
        assert methods.getFirst() == OdeMethod.AUTO && methods.contains(OdeMethod.SEPARABLE) && methods.contains(OdeMethod.LINEAR) : methods;
        assert !methods.contains(OdeMethod.HOMOGENEOUS) && !methods.contains(OdeMethod.CONSTANT_COEFFICIENTS) : methods;
    }

    @Test
    void constantCoefficientsUseTheCharacteristicEquation() {
        solve("y'' - 3*y' + 2*y = 0", Rule.ODE_CHAR_EQUATION, Rule.ODE_ROOT_REAL, Rule.ODE_HOMOGENEOUS_SOLUTION);
        solve("y'' + y = 0", Rule.ODE_ROOT_COMPLEX);
        solve("y'' - 2*y' + y = 0", Rule.ODE_ROOT_REPEATED);
        solve("y''' - y = 0", Rule.ODE_ROOT_REAL, Rule.ODE_ROOT_COMPLEX);
        solve("y'''' - y = 0", Rule.ODE_ROOT_COMPLEX);
        solve("y'' + 2*y' - 2*y = 0", Rule.ODE_ROOT_REAL);
        solve("y'' + 2*y' + 5*y = 0", Rule.ODE_ROOT_COMPLEX);
        solve("2*y'' - 3*y' + y = 0", Rule.ODE_ROOT_REAL);
    }

    @Test
    void undeterminedCoefficientsFindAParticularSolution() {
        solve("y'' + y = x^2", Rule.ODE_UC_FORM, Rule.ODE_UC_SOLVE, Rule.ODE_GENERAL_SOLUTION);
        solve("y'' - 3*y' + 2*y = e^(3*x)", Rule.ODE_UC_FORM);
        solve("y'' + y = sin(2*x)", Rule.ODE_UC_FORM);
        // Resonancia: el término independiente es solución de la homogénea
        solve("y'' - y = e^x", Rule.ODE_UC_SOLVE);
        solve("y'' + y = sin(x)", Rule.ODE_UC_SOLVE);
        solve("y'' + 4*y' + 4*y = e^(-2*x)", Rule.ODE_UC_SOLVE);
        solve("y'' - 2*y' + y = x*e^x*cos(x)", Rule.ODE_UC_FORM);
    }

    @Test
    void variationOfParametersHandlesOtherRightSides() {
        solve("y'' + y = tan(x)", Rule.ODE_WRONSKIAN, Rule.ODE_VARIATION, Rule.ODE_GENERAL_SOLUTION);
        Solution forced = Odes.solve("y'' - 2*y' + y = e^x", OdeMethod.VARIATION_OF_PARAMETERS);
        satisfies("y'' - 2*y' + y = e^x", forced);
        uses(forced, Rule.ODE_VARIATION);
    }

    @Test
    void everyDifferentialEquationRuleAndMethodIsWrittenInBothLanguages() {
        for (Rule rule : Rule.values()) {
            if (rule.category() != Rule.Category.ODE) continue;
            for (Locale locale : Messages.supportedLocales()) {
                assert Messages.has(rule.titleKey(), locale) && Messages.has(rule.textKey(), locale) : rule + " " + locale;
            }
        }
        for (OdeMethod method : OdeMethod.values()) {
            assert !method.title(Locale.forLanguageTag("es")).isBlank() && !method.description(Locale.ENGLISH).isBlank() : method;
        }
        for (Locale locale : Messages.supportedLocales()) {
            for (String key : List.of("solution.kind.ODE.problem", "solution.kind.ODE.problem.order", "solution.kind.ODE.problem.system")) {
                assert Messages.has(key, locale) : key + " " + locale;
            }
        }
    }
}
