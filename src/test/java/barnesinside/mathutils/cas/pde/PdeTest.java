package barnesinside.mathutils.cas.pde;

import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.steps.Format;
import barnesinside.mathutils.cas.steps.Messages;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Step;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class PdeTest {

    private static Pde.Result solve(String a, String b, String c) {
        return Pde.solve(ExprParser.parse(a), ExprParser.parse(b), ExprParser.parse(c));
    }

    private static Pde.Result solve(String a, String b, String c, String x0, String y0, String u0) {
        Pde.Curve curve = new Pde.Curve(ExprParser.parse(x0), ExprParser.parse(y0), ExprParser.parse(u0), "s");
        return Pde.solve(ExprParser.parse(a), ExprParser.parse(b), ExprParser.parse(c), curve);
    }

    private static List<Rule> rules(List<Step> steps) {
        List<Rule> all = new ArrayList<>();
        for (Step step : steps) {
            all.add(step.rule());
            all.addAll(rules(step.substeps()));
        }
        return all;
    }

    private static double at(Expr e, double x, double y) {
        return Eval.value(e, Map.of("x", x, "y", y));
    }

    @Test
    void constantCoefficientsGiveFunctionsOfAStraightLine() {
        Pde.Result r = solve("1", "1", "0");
        assert r.solved() : r.solution();
        assert r.integrals().get(0).toString().equals("y - x") && r.integrals().get(1).toString().equals("u") : r.integrals();
        assert r.general().toString().equals("F(y - x, u) = 0") : r.general();
        assert r.explicit().toString().equals("u = f(y - x)") : r.explicit();
        assert r.particular() == null;
    }

    @Test
    void theSystemIsSolvedPairByPairWithItsSteps() {
        List<Rule> rules = rules(solve("1", "2", "u").solution().steps());
        for (Rule rule : List.of(Rule.PDE_CHARACTERISTICS, Rule.PDE_PAIR, Rule.PDE_SEPARABLE, Rule.PDE_ELIMINATE, Rule.PDE_SUBSTITUTE,
                Rule.PDE_EXPONENTIATE, Rule.PDE_FIRST_INTEGRAL, Rule.PDE_CHECK, Rule.PDE_GENERAL, Rule.PDE_EXPLICIT)) {
            assert rules.contains(rule) : rule + " en " + rules;
        }
    }

    @Test
    void homogeneousCoefficientsGiveRatios() {
        Pde.Result r = solve("x", "y", "u");
        assert r.solved() : r.solution();
        assert r.integrals().toString().equals("[y/x, u/x]") : r.integrals();
        assert r.explicit().toString().equals("u = f(y/x)*x") : r.explicit();
    }

    @Test
    void aCoefficientThatDependsOnTheUnknownIsHandledThroughAZeroRate() {
        Pde.Result burgers = solve("1", "u", "0");
        assert burgers.solved() && rules(burgers.solution().steps()).contains(Rule.PDE_ZERO_RATE) : burgers.solution();
        assert burgers.integrals().toString().equals("[-u*x + y, u]") : burgers.integrals();
        assert burgers.explicit().toString().equals("u = f(-u*x + y)") : burgers.explicit();
    }

    @Test
    void aLinearEquationUsesTheIntegratingFactor() {
        Pde.Result r = solve("1", "1", "u+x");
        assert r.solved() && rules(r.solution().steps()).contains(Rule.PDE_LINEAR) : r.solution();
        // u = f(y - x) e^x - x - 1 cumple u_x + u_y = u + x para cualquier f; con f = sen
        Expr u = r.explicit().right();
        assert u.toString().equals("f(y - x)*e^x - (x + 1)") : u;
    }

    @Test
    void theParticularSolutionMatchesTheDataAndTheEquation() {
        // u_x + u_y = 0, u(x, 0) = sin x  ->  u = sin(x - y)
        Pde.Result r = solve("1", "1", "0", "s", "0", "sin(s)");
        assert r.solved() && r.particular() != null : r.solution();
        Expr u = r.particular().right();
        for (double[] p : new double[][]{{0.5, 0.2}, {1.5, 0.9}, {2.2, 1.1}}) {
            assert Math.abs(at(u, p[0], p[1]) - Math.sin(p[0] - p[1])) < 1e-9 : u;
        }
        // x u_x + y u_y = u, u(s, 1) = s  ->  u = x
        Pde.Result homogeneous = solve("x", "y", "u", "s", "1", "s");
        assert homogeneous.particular().toString().equals("u = x") : homogeneous.particular();
        // u_x + 2 u_y = u, u(x, 0) = e^(-x^2)
        Pde.Result decay = solve("1", "2", "u", "s", "0", "exp(-s^2)");
        for (double[] p : new double[][]{{0.5, 0.2}, {1.5, 0.9}}) {
            double expected = Math.exp(-Math.pow(p[0] - p[1] / 2, 2) + p[1] / 2);
            assert Math.abs(at(decay.particular().right(), p[0], p[1]) - expected) < 1e-9 : decay.particular();
        }
    }

    @Test
    void anImplicitParticularSolutionIsKeptWhenTheUnknownCannotBeSolvedFor() {
        // u_x + u u_y = 0, u(s, 0) = s: la solución cumple u^2 = u x - y
        Pde.Result r = solve("1", "u", "0", "s", "0", "s");
        assert r.solved() && r.particular() != null : r.solution();
        double u = 0.8;
        double x = 1.3;
        double y = u * x - u * u;
        Expr right = r.particular().right();
        assert Math.abs(Eval.value(right, Map.of("x", x, "y", y, "u", u)) - u) < 1e-9 : r.particular();
    }

    @Test
    void aDataCurveTangentToTheCharacteristicsHasNoUniqueSolution() {
        Pde.Result r = solve("1", "1", "0", "s", "s", "1");
        assert !r.solved() && r.solution().result() == null;
        assert rules(r.solution().steps()).contains(Rule.PDE_DATA_CHARACTERISTIC) : r.solution();
    }

    @Test
    void whatCannotBeIntegratedIsSaidAndNeverGuessed() {
        Pde.Result r = solve("x^2+y", "1", "u*x");
        assert !r.solved() && r.integrals().isEmpty() && r.general() == null;
        List<Step> steps = r.solution().steps();
        assert steps.get(steps.size() - 1).rule() == Rule.PDE_NOT_SOLVABLE : steps;
        Pde.Result flat = solve("0", "0", "1");
        assert !flat.solved() && rules(flat.solution().steps()).contains(Rule.PDE_DEGENERATE) : flat.solution();
    }

    @Test
    void everyPdeStepIsExplainedInBothLanguages() {
        for (Rule rule : Rule.values()) {
            if (!rule.name().startsWith("PDE_")) continue;
            for (Locale locale : Messages.supportedLocales()) {
                assert Messages.has(rule.titleKey(), locale) && !Messages.title(rule, locale).isBlank() : rule + " sin título en " + locale;
                assert Messages.has(rule.textKey(), locale) : rule + " sin texto en " + locale;
            }
        }
        assert Messages.has("solution.kind.PDE.problem", Locale.ENGLISH) && Messages.has("solution.kind.PDE.problem", Locale.forLanguageTag("es"));
    }

    @Test
    void theSolutionIsRenderedInEveryFormatAndLanguage() {
        Pde.Result r = solve("1", "1", "0", "s", "0", "sin(s)");
        for (Locale locale : Messages.supportedLocales()) {
            for (Format format : Format.values()) {
                String text = r.solution().render(locale, format);
                assert !text.isBlank() && text.contains("sin") : format + " " + locale + ": " + text;
            }
        }
        assert r.solution().render(Locale.ENGLISH, Format.PLAIN).contains("characteristics");
        assert r.solution().render(Locale.forLanguageTag("es"), Format.PLAIN).contains("características");
    }

    @Test
    void anEquationIsReadFromItsText() {
        Pde.Equation e = Pde.parse("x*u_x + y*u_y = u", "x", "y", "u");
        assert e.a().toString().equals("x") && e.b().toString().equals("y") && e.c().toString().equals("u") : e;
        Pde.Equation moved = Pde.parse("u_x + 2 u_y - u = x", "x", "y", "u");
        assert moved.a().toString().equals("1") && moved.b().toString().equals("2") && moved.c().toString().equals("u + x") : moved;
        Pde.Equation braces = Pde.parse("u_{y} + u*u_{x} = 0", "x", "y", "u");
        assert braces.a().toString().equals("u") && braces.b().toString().equals("1") && braces.c().toString().equals("0") : braces;
        for (String bad : List.of("u_x + u_y", "u_x^2 + u_y = 0", "x + y = 0", "u_x*u_y = 1")) {
            boolean rejected = false;
            try {
                Pde.parse(bad, "x", "y", "u");
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            assert rejected : bad;
        }
    }
}
