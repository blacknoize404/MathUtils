package MathUtils.cas.integral;

import MathUtils.calculus.Integration;
import MathUtils.cas.expr.Equivalence;
import MathUtils.cas.expr.Eval;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.diff.Derivatives;
import MathUtils.cas.steps.Format;
import MathUtils.cas.steps.Messages;
import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Solution;
import MathUtils.cas.steps.Step;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class IntegralsTest {

    private static Expr p(String s) {
        return ExprParser.parse(s);
    }

    private static List<Step> flatten(List<Step> steps) {
        List<Step> all = new ArrayList<>();
        for (Step s : steps) {
            all.add(s);
            all.addAll(flatten(s.substeps()));
        }
        return all;
    }

    private static boolean uses(Solution s, Rule rule) {
        return flatten(s.steps()).stream().anyMatch(step -> step.rule() == rule);
    }

    /**
     * Comprueba con dos oráculos independientes: la derivada del resultado debe ser el integrando
     * y la integral definida por Barrow debe coincidir con la cuadratura numérica en [a, b].
     */
    private static Solution check(String integrand, double a, double b) {
        Solution s = Integrals.indefinite(integrand, "x");
        assert s.isSolved() : integrand + " -> " + s.status() + "\n" + s;

        Expr f = p(integrand);
        Expr derivative = Derivatives.derivative(s.result(), "x").result();
        assert Equivalence.check(derivative, f) != Equivalence.Result.DIFFERENT
                : integrand + ": la derivada de " + s.result() + " es " + derivative;

        double exact = Eval.value(s.result(), Map.of("x", b)) - Eval.value(s.result(), Map.of("x", a));
        double numeric = Integration.simpson13(t -> Eval.value(f, Map.of("x", t)), a, b, 2000);
        assert Math.abs(exact - numeric) <= 1e-6 * (1 + Math.abs(numeric))
                : integrand + " en [" + a + ", " + b + "]: Barrow " + exact + " vs numérica " + numeric + " con " + s.result();
        return s;
    }

    private static Solution check(String integrand) {
        return check(integrand, 0.4, 1.3);
    }

    // ------------------------------------------------------------ Tabla

    @Test
    void powersAndConstants() {
        check("x^3", 0, 2);
        check("1/x", 1, 3);
        check("sqrt(x)", 1, 4);
        check("x^(-2)", 1, 3);
        check("x^(-1/2)", 1, 4);
        check("5", 0, 2);
        check("x", 0, 3);
    }

    @Test
    void exponentialsAndTrigonometric() {
        check("e^x");
        check("2^x");
        check("sin(x)");
        check("cos(x)");
        check("tan(x)", 0, 1);
        check("cot(x)", 0.5, 1.2);
        check("sec(x)", 0, 1);
        check("csc(x)", 0.5, 1.5);
        check("sec(x)^2", 0, 1);
        check("csc(x)^2", 0.5, 1.5);
        check("sec(x)*tan(x)", 0, 1);
        check("csc(x)*cot(x)", 0.5, 1.5);
        check("sinh(x)");
        check("cosh(x)");
        check("tanh(x)");
    }

    @Test
    void inverseTrigonometricForms() {
        check("1/(x^2 + 4)");
        check("1/(1 + x^2)");
        check("1/(x^2 - 4)", 3, 5);
        check("1/sqrt(4 - x^2)", 0, 1);
        check("1/sqrt(1 - x^2)", 0, 0.8);
        check("1/sqrt(x^2 + 1)");
        check("1/sqrt(x^2 - 1)", 2, 3);
        check("3/(x^2 + 9)");
    }

    @Test
    void tableHitsAreLeaves() {
        Solution s = Integrals.indefinite("x^3", "x");
        assert s.steps().size() == 1 && s.steps().get(0).rule() == Rule.INT_POWER;
        assert s.steps().get(0).params().get("n").equals(Expr.num(3));
        assert s.result().equals(p("x^4/4"));
        assert Integrals.indefinite("1/x", "x").steps().get(0).rule() == Rule.INT_RECIPROCAL;
        assert Integrals.indefinite("1/(x^2+4)", "x").steps().get(0).rule() == Rule.INT_ATAN_FORM;
    }

    // ------------------------------------------------------------ Linealidad y expansión

    @Test
    void sumsAndConstantMultiples() {
        Solution s = check("3x^2 + 2x - 5", 0, 2);
        assert uses(s, Rule.INT_SUM) && uses(s, Rule.INT_CONSTANT_MULTIPLE) && uses(s, Rule.INT_CONSTANT);
        check("4 sin(x) - 2 cos(x) + 7");
        check("x^2/3 + sqrt(x)/2", 1, 4);
    }

    @Test
    void expansion() {
        check("(x^2 + 1)*(x - 2)");
        check("(x + 1)/x", 1, 3);
        check("x^2*(x + 3)^2");
        check("(x^3 + 2x)/x");
        Solution s = Integrals.indefinite("(x^2 + 1)*(x - 2)", "x");
        assert uses(s, Rule.INT_EXPAND);
    }

    // ------------------------------------------------------------ Sustitución

    @Test
    void linearSubstitution() {
        check("sin(3x)");
        check("e^(2x)");
        check("(2x + 1)^5");
        check("1/(3x + 2)", 0, 2);
        check("cos(x/2)");
        check("sqrt(2x + 1)");
        check("1/(x + 1)^2");
        check("e^(-x)");
        check("sin(2x + 1)");
        check("x*(x + 1)^3");
        check("x*sqrt(x + 1)");
        check("1/(2x + 1)^3");
    }

    @Test
    void nonLinearSubstitution() {
        check("x/(x^2 + 1)");
        check("x*e^(x^2)");
        check("x^2*cos(x^3)");
        check("sin(x)*cos(x)");
        check("cos(x)/sin(x)", 0.5, 1.5);
        check("tan(x)*sec(x)^2", 0, 1);
        check("ln(x)/x", 1, 3);
        check("e^x/(1 + e^x)");
        check("x/sqrt(x^2 + 1)");
        check("sin(x)^2*cos(x)");
        check("1/(x*ln(x))", 2, 3);
        check("e^(sqrt(x))/sqrt(x)", 1, 4);
        check("cos(x)*e^(sin(x))");
        check("x*(x^2 + 1)^3");
        check("sin(x)/(1 + cos(x))", 0, 1);
    }

    @Test
    void substitutionStructure() {
        Solution s = Integrals.indefinite("x*e^(x^2)", "x");
        Step step = s.steps().get(0);
        assert step.rule() == Rule.INT_SUBSTITUTION;
        assert step.params().get("u").equals(p("x^2"));
        assert step.params().get("du").equals(p("2x"));
        assert step.params().get("s").equals(Expr.sym("u"));
        assert step.params().get("g").equals(p("e^u/2"));
        assert !step.substeps().isEmpty();
    }

    @Test
    void substitutionVariableAvoidsClashes() {
        Solution s = Integrals.indefinite("u*e^(u^2)", "u");
        assert s.isSolved();
        Step step = s.steps().get(0);
        assert !step.params().get("s").equals(Expr.sym("u")) : "la variable nueva debe ser distinta de la del problema";
    }

    // ------------------------------------------------------------ Por partes

    @Test
    void integrationByParts() {
        check("x*e^x");
        check("x^2*e^x");
        check("x*sin(x)");
        check("x*cos(2x)");
        check("ln(x)", 1, 3);
        check("x*ln(x)", 1, 3);
        check("x^2*ln(x)", 1, 3);
        check("atan(x)");
        check("asin(x)", 0, 0.8);
        check("acos(x)", 0, 0.8);
        check("x*atan(x)");
        check("x^3*e^x");
        check("ln(x)^2", 1, 3);
        check("x*sec(x)^2", 0, 1);
        check("x*2^x");
        check("x*e^(-x)");
        check("x^2*sin(x)");
        check("e^x*x^2 + x");
    }

    @Test
    void partsStructure() {
        Solution s = Integrals.indefinite("x*e^x", "x");
        Step step = s.steps().get(0);
        assert step.rule() == Rule.INT_PARTS;
        assert step.params().get("u").equals(Expr.sym("x"));
        assert step.params().get("dv").equals(p("e^x"));
        assert step.params().get("du").equals(Expr.ONE);
        assert step.params().get("v").equals(p("e^x"));
        assert Equivalence.areEqual(s.result(), p("(x - 1)*e^x"));
    }

    @Test
    void repeatedParts() {
        Solution s = Integrals.indefinite("x^2*e^x", "x");
        long parts = flatten(s.steps()).stream().filter(st -> st.rule() == Rule.INT_PARTS).count();
        assert parts == 2 : parts;
    }

    @Test
    void cyclicParts() {
        Solution s = check("e^x*sin(x)");
        assert uses(s, Rule.INT_CYCLE);
        assert Equivalence.areEqual(s.result(), p("e^x*(sin(x) - cos(x))/2"));

        check("e^(2x)*cos(x)");
        check("sin(x)*cos(x)*e^x".replace("*cos(x)", ""));
        Solution cosine = check("e^x*cos(x)");
        assert Equivalence.areEqual(cosine.result(), p("e^x*(sin(x) + cos(x))/2"));
    }

    // ------------------------------------------------------------ Honestidad

    @Test
    void nonElementaryIntegrals() {
        for (String f : new String[]{"e^(x^2)", "e^(-x^2)", "sin(x)/x", "cos(x)/x", "e^x/x", "1/ln(x)", "x^x",
                "sin(x^2)", "cos(x^2)", "3*e^(x^2)"}) {
            Solution s = Integrals.indefinite(f, "x");
            assert s.status() == Solution.Status.NON_ELEMENTAL : f + " -> " + s.status();
            assert s.result() == null;
        }
    }

    @Test
    void unknownIntegralsAreUnsolvedNotGuessed() {
        Solution s = Integrals.indefinite("sqrt(1 + x^3)", "x");
        assert s.status() == Solution.Status.UNSOLVED;
        assert s.result() == null;
    }

    @Test
    void failedAttemptsLeaveNoSteps() {
        Solution s = Integrals.indefinite("x*e^x", "x");
        // Solo el paso de partes (con sus subpasos): nada de sustituciones descartadas
        assert s.steps().size() == 1;
        assert flatten(s.steps()).stream().noneMatch(st -> st.rule() == Rule.INT_SUBSTITUTION);
    }

    // ------------------------------------------------------------ Otras variables y salida

    @Test
    void otherVariablesAreConstants() {
        Solution s = Integrals.indefinite("a*x^2 + b", "x");
        assert s.isSolved();
        assert Equivalence.areEqual(s.result(), p("a*x^3/3 + b*x"));
        Solution y = Integrals.indefinite("x*y", "y");
        assert Equivalence.areEqual(y.result(), p("x*y^2/2"));
    }

    @Test
    void renderingWithPlusC() {
        Solution s = Integrals.indefinite("x*e^x", "x");
        String es = s.render(Locale.forLanguageTag("es"), Format.PLAIN);
        assert es.contains("Calcular la integral de x*e^x respecto a x.") : es;
        assert es.contains("Integración por partes") : es;
        assert es.contains("Resultado: ∫ x*e^x dx = ") && es.contains(" + C") : es;

        String en = s.render(Locale.ENGLISH, Format.PLAIN);
        assert en.contains("Compute the integral of x*e^x with respect to x.") && en.contains("Integration by parts") : en;

        String latex = s.render(Locale.ENGLISH, Format.LATEX);
        assert latex.contains("\\int x e^{x} \\, dx") && latex.contains("+ C") : latex;
        MathUtils.cas.JsonValidator.assertValid(s.render(Locale.ENGLISH, Format.JSON));
    }

    @Test
    void renderedTextHasNoPlaceholders() {
        for (String f : new String[]{"x^3", "sin(3x)", "x*e^(x^2)", "x*e^x", "e^x*sin(x)", "1/(x^2+4)", "3x^2 + 2x",
                "(x+1)^2", "ln(x)", "e^(x^2)"}) {
            Solution s = Integrals.indefinite(f, "x");
            for (Locale locale : new Locale[]{Locale.forLanguageTag("es"), Locale.ENGLISH}) {
                for (Format format : Format.values()) {
                    String text = s.render(locale, format);
                    assert !text.contains("${") : f + ": " + text;
                }
            }
        }
    }

    @Test
    void everyRuleHasTexts() {
        for (Rule rule : Rule.values()) {
            for (Locale locale : Messages.supportedLocales()) {
                assert Messages.has(rule.titleKey(), locale) && Messages.has(rule.textKey(), locale) : rule;
            }
        }
    }

    @Test
    void parseErrorsAreReported() {
        try {
            Integrals.indefinite("x +", "x");
            assert false;
        } catch (MathUtils.exceptions.ExpressionParseException e) {
            assert true;
        }
    }

    // ------------------------------------------------------------ Solidez

    @Test
    void integratingTheDerivativeRecoversTheFunction() {
        String[] functions = {
                "x^2*sin(x)", "e^(3x)", "ln(x^2 + 1)", "atan(2x)", "x*e^(-x^2)", "sin(x)^3", "x^3 - 2x^2 + x",
                "e^x*cos(x)", "sqrt(x^2 + 1)", "x*ln(x)", "cos(x)^2", "(x + 1)^4", "x*sin(2x)", "ln(ln(x + 3))",
                "e^(sin(x))", "x^2*e^x", "sin(x)*e^(2x)", "atan(x)*x", "1/(x + 2)", "x/(x^2 + 3)", "tan(x)",
                "e^(x^2)", "(x^2 + 1)^3", "sin(x^2)", "x^5*ln(x)", "cos(3x)*sin(x)", "sqrt(x)*ln(x)"
        };
        int solved = 0;
        for (String source : functions) {
            Expr f = p(source);
            Expr derivative = Derivatives.derivative(f, "x").result();
            Solution s = Integrals.indefinite(derivative, "x");
            if (!s.isSolved()) continue;
            solved++;
            assert Equivalence.differByConstant(s.result(), f, "x")
                    : "∫(" + source + ")' dio " + s.result() + ", que no difiere de la función por una constante";
        }
        assert solved >= 15 : "Solo se resolvieron " + solved + " de " + functions.length;
    }

    @Test
    void neverClaimsAWrongAntiderivative() {
        java.util.Random random = new java.util.Random(11);
        int solved = 0;
        for (int i = 0; i < 80; i++) {
            Expr f = randomIntegrand(random, 2);
            Solution s;
            try {
                s = Integrals.indefinite(f, "x");
            } catch (RuntimeException e) {
                continue;
            }
            if (!s.isSolved()) continue;
            solved++;
            Expr derivative = Derivatives.derivative(s.result(), "x").result();
            assert Equivalence.check(derivative, f) != Equivalence.Result.DIFFERENT
                    : "∫ " + f + " dx = " + s.result() + " (su derivada es " + derivative + ")";
        }
        assert solved > 10 : solved;
    }

    private static Expr randomIntegrand(java.util.Random r, int depth) {
        Expr x = Expr.sym("x");
        if (depth == 0) {
            return switch (r.nextInt(5)) {
                case 0 -> x;
                case 1 -> Expr.num(r.nextInt(5) + 1);
                case 2 -> Expr.add(Expr.mul(Expr.num(r.nextInt(3) + 1), x), Expr.num(r.nextInt(4)));
                case 3 -> Expr.pow(x, Expr.num(r.nextInt(3) + 1));
                default -> Expr.mul(Expr.num(r.nextInt(3) + 2), x);
            };
        }
        Expr a = randomIntegrand(r, depth - 1);
        Expr b = randomIntegrand(r, depth - 1);
        return switch (r.nextInt(8)) {
            case 0 -> Expr.add(a, b);
            case 1 -> Expr.mul(a, b);
            case 2 -> Expr.sin(a);
            case 3 -> Expr.cos(a);
            case 4 -> Expr.exp(a);
            case 5 -> Expr.div(a, Expr.add(b, Expr.num(r.nextInt(4) + 1)));
            case 6 -> Expr.pow(a, Expr.num(r.nextInt(3) + 1));
            default -> Expr.mul(a, Expr.exp(b));
        };
    }
}
