package MathUtils.cas.diff;

import MathUtils.calculus.NumericDifferentiation;
import MathUtils.cas.expr.Equivalence;
import MathUtils.cas.expr.Eval;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.steps.Format;
import MathUtils.cas.steps.Messages;
import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Segment;
import MathUtils.cas.steps.Solution;
import MathUtils.cas.steps.Step;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class DerivativesTest {

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final Locale EN = Locale.ENGLISH;

    private static Expr p(String s) {
        return ExprParser.parse(s);
    }

    private static Solution d(String f) {
        return Derivatives.derivative(f, "x");
    }

    private static void assertResult(String f, String expected) {
        Solution s = d(f);
        assert s.isSolved() : f;
        assert Equivalence.areEqual(p(expected), s.result())
                : "d/dx " + f + " -> esperado " + expected + " pero fue " + s.result();
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

    // ------------------------------------------------------------ Resultados

    @Test
    void basicRules() {
        assertResult("5", "0");
        assertResult("x", "1");
        assertResult("y", "0");
        assertResult("x^3", "3x^2");
        assertResult("3x", "3");
        assertResult("x^2 + 3x + 1", "2x + 3");
        assertResult("sqrt(x)", "1/(2sqrt(x))");
        assertResult("1/x", "-1/x^2");
        assertResult("1/x^2", "-2/x^3");
        assertResult("x^(2/3)", "2/(3 x^(1/3))");
    }

    @Test
    void productAndQuotient() {
        assertResult("x^2 sin(x)", "2x sin(x) + x^2 cos(x)");
        assertResult("x e^x", "e^x + x e^x");
        assertResult("sin(x) cos(x)", "cos(x)^2 - sin(x)^2");
        assertResult("x sin(x) cos(x)", "sin(x) cos(x) + x cos(x)^2 - x sin(x)^2");
        assertResult("(x^2+1)/(x-3)", "(2x (x-3) - (x^2+1))/(x-3)^2");
        assertResult("x/(x+1)", "1/(x+1)^2");
    }

    @Test
    void chainRule() {
        assertResult("sin(x^2)", "2x cos(x^2)");
        assertResult("e^(2x)", "2 e^(2x)");
        assertResult("(x^2+1)^3", "6x (x^2+1)^2");
        assertResult("sqrt(x^2+1)", "x/sqrt(x^2+1)");
        assertResult("ln(x^2+1)", "2x/(x^2+1)");
        assertResult("sin(cos(x))", "-sin(x) cos(cos(x))");
        assertResult("e^(sin(x))", "cos(x) e^(sin(x))");
    }

    @Test
    void exponentialsAndLogs() {
        assertResult("e^x", "e^x");
        assertResult("2^x", "2^x ln(2)");
        assertResult("ln(x)", "1/x");
        assertResult("log(x, 2)", "1/(x ln(2))");
        assertResult("x^x", "x^x (ln(x) + 1)");
        assertResult("x^(2x)", "x^(2x) (2 ln(x) + 2)");
    }

    @Test
    void trigonometricAndInverse() {
        assertResult("sin(x)", "cos(x)");
        assertResult("cos(x)", "-sin(x)");
        assertResult("tan(x)", "sec(x)^2");
        assertResult("cot(x)", "-csc(x)^2");
        assertResult("sec(x)", "sec(x) tan(x)");
        assertResult("csc(x)", "-csc(x) cot(x)");
        assertResult("asin(x)", "1/sqrt(1-x^2)");
        assertResult("acos(x)", "-1/sqrt(1-x^2)");
        assertResult("atan(x)", "1/(1+x^2)");
        assertResult("sinh(x)", "cosh(x)");
        assertResult("cosh(x)", "sinh(x)");
        assertResult("tanh(x)", "1 - tanh(x)^2");
        assertResult("abs(x)", "x/abs(x)");
        assertResult("atan(2x)", "2/(1+4x^2)");
    }

    @Test
    void partialDerivatives() {
        assertResult("x*y", "y");
        assertResult("x^2*y + y^3", "2x*y");
        Solution wrtY = Derivatives.derivative("x^2*y + y^3", "y");
        assert wrtY.result().equals(p("x^2 + 3y^2"));
        assert uses(Derivatives.derivative("x + y^2", "y"), Rule.DIFF_OTHER_VARIABLE);
        assert uses(wrtY, Rule.DIFF_CONSTANT_MULTIPLE);
    }

    @Test
    void higherOrder() {
        Solution s = Derivatives.nth(p("x^4"), "x", 3);
        assert s.result().equals(p("24x"));
        assert s.order() == 3;
        assert s.steps().size() == 3;
        assert s.steps().stream().allMatch(st -> st.rule() == Rule.DIFF_ORDER);
        assert Derivatives.nth(p("sin(x)"), "x", 4).result().equals(p("sin(x)"));
    }

    @Test
    void invalidOrderThrows() {
        try {
            Derivatives.nth(p("x"), "x", 0);
            assert false;
        } catch (IllegalArgumentException e) {
            assert true;
        }
    }

    // ------------------------------------------------------------ Estructura de pasos

    @Test
    void powerRuleIsALeaf() {
        Solution s = d("x^3");
        assert s.steps().size() == 1;
        Step step = s.steps().get(0);
        assert step.rule() == Rule.DIFF_POWER;
        assert step.substeps().isEmpty();
        assert step.before().equals(p("x^3")) && step.after().equals(p("3x^2"));
        assert step.params().get("n").equals(Expr.num(3));
    }

    @Test
    void productRuleStructure() {
        Solution s = d("x^2 sin(x)");
        assert s.steps().size() == 1;
        Step product = s.steps().get(0);
        assert product.rule() == Rule.DIFF_PRODUCT;
        assert product.params().get("f").equals(p("x^2"));
        assert product.params().get("g").equals(p("sin(x)"));
        assert product.substeps().size() == 2;
        assert product.substeps().get(0).rule() == Rule.DIFF_POWER;
        assert product.substeps().get(1).rule() == Rule.DIFF_SIN;
    }

    @Test
    void chainRuleStructure() {
        Solution s = d("sin(x^2)");
        Step chain = s.steps().get(0);
        assert chain.rule() == Rule.DIFF_CHAIN;
        assert chain.params().get("u").equals(p("x^2"));
        assert chain.substeps().size() == 2;

        Step outer = chain.substeps().get(0);
        assert outer.rule() == Rule.DIFF_SIN;
        assert outer.before().equals(p("sin(u)")) && outer.after().equals(p("cos(u)"));
        assert outer.variable().equals(Expr.sym("u"));

        assert chain.substeps().get(1).rule() == Rule.DIFF_POWER;
    }

    @Test
    void genericLetterAvoidsClashes() {
        Solution s = d("sin(u*x)");
        Step chain = s.steps().get(0);
        assert chain.substeps().get(0).variable().equals(Expr.sym("v")) : "la letra genérica debe evitar la u del problema";
        assert s.result().equals(p("u cos(u x)"));
    }

    @Test
    void ruleSelection() {
        assert uses(d("3x"), Rule.DIFF_CONSTANT_MULTIPLE);
        assert uses(d("x + 1"), Rule.DIFF_SUM);
        assert uses(d("(x^2+1)/(x-3)"), Rule.DIFF_QUOTIENT);
        assert uses(d("x^x"), Rule.DIFF_POWER_GENERAL);
        assert uses(d("2^x"), Rule.DIFF_EXPONENTIAL);
        assert uses(d("e^x"), Rule.DIFF_EXPONENTIAL_E);
        assert uses(d("e^(2x)"), Rule.DIFF_CHAIN);
        assert uses(d("ln(x)"), Rule.DIFF_LN);
        assert !uses(d("sin(x)"), Rule.DIFF_CHAIN);
        assert uses(d("5"), Rule.DIFF_CONSTANT);
    }

    // ------------------------------------------------------------ Corrección numérica

    @Test
    void agreesWithNumericDifferentiation() {
        String[] functions = {
                "x^3 - 2x^2 + x - 5", "sin(x) cos(x)", "sin(x^2)", "tan(x)", "cot(x)", "sec(x)", "csc(x)",
                "(x^2+1)/(x-3)", "1/x", "ln(x^2+1)", "log(x, 2)", "e^(2x)", "2^x", "x^x", "sqrt(x)", "cbrt(x)",
                "x*x*x", "x/2/3", "atan(x)", "asin(x/3)", "acos(x/3)", "sinh(x)", "cosh(2x)", "tanh(x)",
                "e^x sin(x)", "x^2 e^(-x)", "ln(sin(x))", "sqrt(1 + x^2)", "(x+1)^(x)", "x^(sin(x))",
                "sin(x)/x", "cos(x)^2", "abs(x)", "erf(x)", "x^(2/3)", "ln(ln(x+3))", "atan(x)*ln(x+2)",
                "(2x+1)^5/(x^2+1)^2", "sin(cos(tan(x/4)))", "e^(e^(x/3))"
        };
        double[] points = {0.4, 1.3, 2.1};

        for (String source : functions) {
            Expr f = p(source);
            Expr df = Derivatives.derivative(f, "x").result();
            for (double x : points) {
                double symbolic = Eval.value(df, Map.of("x", x));
                double numeric = NumericDifferentiation.richardson(t -> Eval.value(f, Map.of("x", t)), x, 1e-3);
                if (!Double.isFinite(symbolic) || !Double.isFinite(numeric)) continue;
                assert Math.abs(symbolic - numeric) < 1e-5 * (1 + Math.abs(numeric))
                        : "d/dx " + source + " en " + x + ": simbólica " + symbolic + " vs numérica " + numeric;
            }
        }
    }

    @Test
    void everyIntermediateStepIsConsistent() {
        // En cada paso, 'after' debe ser la derivada numérica de 'before' respecto a la variable del paso
        String[] functions = {"x^2 sin(x)", "sin(x^2)", "(x^2+1)/(x-3)", "e^(2x) cos(x)", "x^x", "sqrt(x^2+1)", "ln(sin(x))"};
        for (String source : functions) {
            for (Step step : flatten(d(source).steps())) {
                if (step.before() == null || step.after() == null || step.variable() == null) continue;
                if (step.rule().category() != Rule.Category.DERIVATIVE || step.rule() == Rule.DIFF_ORDER) continue;
                String v = ((Expr.Sym) step.variable()).name();
                if (step.rule() == Rule.DIFF_CHAIN || step.rule() == Rule.DIFF_SUM
                        || step.rule() == Rule.DIFF_PRODUCT || step.rule() == Rule.DIFF_QUOTIENT
                        || step.rule() == Rule.DIFF_CONSTANT_MULTIPLE || step.rule() == Rule.DIFF_POWER_GENERAL) {
                    // Pasos compuestos: su 'after' es la derivada completa de 'before'
                }
                for (double value : new double[]{0.45, 1.35}) {
                    java.util.Map<String, Double> env = new java.util.HashMap<>();
                    for (String name : step.before().variables()) env.put(name, 0.7 + name.length() * 0.1);
                    env.put(v, value);
                    double numeric = NumericDifferentiation.richardson(t -> {
                        java.util.Map<String, Double> e2 = new java.util.HashMap<>(env);
                        e2.put(v, t);
                        return Eval.value(step.before(), e2);
                    }, value, 1e-3);
                    double symbolic = Eval.value(step.after(), env);
                    if (!Double.isFinite(numeric) || !Double.isFinite(symbolic)) continue;
                    assert Math.abs(numeric - symbolic) < 1e-4 * (1 + Math.abs(numeric))
                            : source + ": paso " + step.rule() + " " + step.before() + " -> " + step.after();
                }
            }
        }
    }

    // ------------------------------------------------------------ Presupuesto

    @Test
    void deeplyNestedExpressionsReportUnsolvedInsteadOfHanging() {
        Expr e = Expr.sym("x");
        for (int i = 0; i < 400; i++) e = Expr.sin(e);
        Solution s = Derivatives.derivative(e, "x");
        assert s.status() == Solution.Status.UNSOLVED;
        assert s.result() == null;
    }

    // ------------------------------------------------------------ Textos y renderizado

    @Test
    void everyRuleHasTitleAndTextInBothLanguages() {
        for (Rule rule : Rule.values()) {
            for (Locale locale : Messages.supportedLocales()) {
                assert Messages.has(rule.titleKey(), locale) : rule + " sin título en " + locale;
                assert Messages.has(rule.textKey(), locale) : rule + " sin texto en " + locale;
                assert !Messages.title(rule, locale).isBlank();
            }
        }
    }

    @Test
    void messagesFallBackToSpanish() {
        assert Messages.title(Rule.DIFF_PRODUCT, Locale.FRENCH).equals(Messages.title(Rule.DIFF_PRODUCT, ES));
        assert !Messages.title(Rule.DIFF_PRODUCT, EN).equals(Messages.title(Rule.DIFF_PRODUCT, ES));
        assert Messages.title(Rule.DIFF_PRODUCT, ES).equals("Regla del producto");
        assert Messages.title(Rule.DIFF_PRODUCT, EN).equals("Product rule");
    }

    @Test
    void renderedTextHasNoUnresolvedPlaceholders() {
        String[] functions = {"x^2 sin(x)", "sin(x^2)", "(x^2+1)/(x-3)", "x^x", "2^x", "3x", "e^(2x)", "ln(x)", "y + x",
                "atan(x)", "tanh(x)", "abs(x)", "erf(x)", "x^(1/3)", "5"};
        for (String source : functions) {
            Solution s = d(source);
            for (Locale locale : new Locale[]{ES, EN}) {
                for (Format format : Format.values()) {
                    String text = s.render(locale, format);
                    assert !text.contains("${") : source + " " + locale + " " + format + ": " + text;
                    assert !text.isBlank();
                }
            }
        }
    }

    @Test
    void plainRenderingInSpanish() {
        String text = d("x^2 sin(x)").render(ES, Format.PLAIN);
        assert text.contains("Derivar x^2*sin(x) respecto a x.") : text;
        assert text.contains("1. Regla del producto") : text;
        assert text.contains("1.1. Regla de la potencia") : text;
        assert text.contains("1.2. Derivada del seno") : text;
        assert text.contains("Aquí f = x^2 y g = sin(x).") : text;
        assert text.contains("d/dx[x^2*sin(x)] = x^2*cos(x) + 2*x*sin(x)") : text;
        assert text.contains("Resultado:") : text;
    }

    @Test
    void plainRenderingInEnglish() {
        String text = d("x^2 sin(x)").render(EN, Format.PLAIN);
        assert text.contains("Differentiate x^2*sin(x) with respect to x.") : text;
        assert text.contains("1. Product rule") : text;
        assert text.contains("Here f = x^2 and g = sin(x).") : text;
        assert text.contains("Result:") : text;
    }

    @Test
    void higherOrderRendering() {
        String text = Derivatives.nth(p("x^4"), "x", 2).render(ES, Format.PLAIN);
        assert text.contains("Derivar x^4 2 veces respecto a x.") : text;
        assert text.contains("d^2/dx^2[x^4] = 12*x^2") : text;
    }

    @Test
    void markdownAndLatexRendering() {
        Solution s = d("sin(x^2)");
        String markdown = s.render(EN, Format.MARKDOWN);
        assert markdown.contains("- **Chain rule**") : markdown;
        assert markdown.contains("$$\\frac{d}{dx}\\left[\\sin\\left(x^{2}\\right)\\right] = 2 x \\cos\\left(x^{2}\\right)$$") : markdown;

        String latex = s.render(EN, Format.LATEX);
        assert latex.contains("\\begin{enumerate}") && latex.contains("\\end{enumerate}") : latex;
        assert latex.contains("\\item \\textbf{Chain rule}") : latex;
        assert latex.contains("\\frac{d}{du}") : latex;
    }

    @Test
    void explanationExposesMathSegments() {
        Step product = d("x^2 sin(x)").steps().get(0);
        List<Segment> segments = product.explanation(ES);
        long math = segments.stream().filter(seg -> seg instanceof Segment.Math).count();
        assert math == 2;
        assert segments.stream().anyMatch(seg -> seg instanceof Segment.Math m && m.expr().equals(p("x^2")));
    }

    @Test
    void jsonIsWellFormed() {
        String json = d("x^2 sin(x) + ln(x)").render(ES, Format.JSON);
        JsonCheck.assertValid(json);
        assert json.contains("\"rule\":\"DIFF_SUM\"") && json.contains("\"rule\":\"DIFF_PRODUCT\"");
        assert json.contains("\"status\":\"SOLVED\"") && json.contains("\"variable\":\"x\"");
        JsonCheck.assertValid(Derivatives.nth(p("x^4"), "x", 2).render(EN, Format.JSON));
    }

    /** Validador mínimo de JSON para comprobar que la salida está bien formada. */
    private static final class JsonCheck {
        private final String s;
        private int i;

        private JsonCheck(String s) {
            this.s = s;
        }

        static void assertValid(String json) {
            JsonCheck c = new JsonCheck(json);
            c.value();
            c.ws();
            assert c.i == json.length() : "JSON con texto sobrante en " + c.i;
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private void expect(char c) {
            ws();
            assert i < s.length() && s.charAt(i) == c : "Se esperaba '" + c + "' en " + i + ": " + s.substring(Math.max(0, i - 20), Math.min(s.length(), i + 20));
            i++;
        }

        private void value() {
            ws();
            char c = s.charAt(i);
            switch (c) {
                case '{' -> {
                    i++;
                    ws();
                    if (s.charAt(i) == '}') {
                        i++;
                        return;
                    }
                    do {
                        ws();
                        string();
                        expect(':');
                        value();
                        ws();
                    } while (s.charAt(i) == ',' && ++i > 0);
                    expect('}');
                }
                case '[' -> {
                    i++;
                    ws();
                    if (s.charAt(i) == ']') {
                        i++;
                        return;
                    }
                    do {
                        value();
                        ws();
                    } while (s.charAt(i) == ',' && ++i > 0);
                    expect(']');
                }
                case '"' -> string();
                default -> {
                    int start = i;
                    while (i < s.length() && ",]} \n".indexOf(s.charAt(i)) < 0) i++;
                    String token = s.substring(start, i);
                    assert token.equals("null") || token.equals("true") || token.equals("false") || token.matches("-?\\d+(\\.\\d+)?")
                            : "Valor inválido: " + token;
                }
            }
        }

        private void string() {
            expect('"');
            while (s.charAt(i) != '"') {
                if (s.charAt(i) == '\\') i++;
                i++;
            }
            i++;
        }
    }
}
