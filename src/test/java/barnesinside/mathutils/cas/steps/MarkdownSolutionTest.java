package barnesinside.mathutils.cas.steps;

import barnesinside.mathutils.cas.algebra.Equations;
import barnesinside.mathutils.cas.diff.Derivatives;
import barnesinside.mathutils.cas.expr.ExprParser;
import org.junit.jupiter.api.Test;

import java.util.Locale;

/** La solución completa como Markdown: con esquema, numerada y con las fórmulas en bloques. */
class MarkdownSolutionTest {

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final Locale EN = Locale.ENGLISH;

    @Test
    void theSolutionIsAnOutlineWithTitleProblemStepsAndResult() {
        String md = Derivatives.nth(ExprParser.parse("x^2*sin(x)"), "x", 1).render(ES, Format.MARKDOWN);
        int problem = md.indexOf("\n## Problema");
        int steps = md.indexOf("\n## Pasos");
        int result = md.indexOf("\n## Resultado");
        assert md.startsWith("# Solución paso a paso\n") : md;
        assert problem > 0 && steps > problem && result > steps : "las secciones salen en orden: " + md;
        // El problema lleva su enunciado y su fórmula destacada
        String problemText = md.substring(problem, steps);
        assert problemText.contains("Derivar $x^{2} \\sin\\left(x\\right)$") && problemText.contains("$$\\frac{d}{dx}") : problemText;
        // Los pasos son encabezados numerados, y los subpasos cuelgan de ellos
        assert md.contains("\n### 1. Regla del producto\n") : md;
        assert md.contains("\n#### 1.1 Regla de la potencia\n") && md.contains("\n#### 1.2 Derivada del seno\n") : md;
        // Las fórmulas van en bloques propios, no dentro de una lista
        assert !md.contains("\n- **") && !md.contains("\n  $$") : "sin viñetas anidadas: " + md;
        assert md.endsWith("$$\n") : md;
    }

    @Test
    void anEquationReadsAsOneFormulaInsteadOfFragments() {
        String md = Equations.solve("x^2-5*x+6=0").render(ES, Format.MARKDOWN);
        assert !md.contains("$=$") : "una igualdad no se parte en fórmulas sueltas: " + md;
        assert md.contains("$x - 3 = 0$") && md.contains("$x^{2} - 5 x + 6 = 0$") : md;
        assert !md.contains("\\mathrm{x - 3") : "un texto matemático ya escrito no va en redonda: " + md;
        assert md.contains("x - 3 = 0, x - 2 = 0") : md;
        assert md.contains("$$x \\in \\left\\{2, 3\\right\\}$$") : md;
    }

    @Test
    void theLanguageIsFollowed() {
        String md = Equations.solve("x^2-5*x+6=0").render(EN, Format.MARKDOWN);
        assert md.startsWith("# Step-by-step solution\n") && md.contains("\n## Problem\n") && md.contains("\n## Steps\n") && md.contains("\n## Result\n") : md;
        assert md.contains("\n#### 1.1 ") : md;
    }

    @Test
    void anUnsolvedProblemSaysSoInAQuote() {
        Solution s = Equations.solve("cos(x)^2 = cos(x^2)");
        if (!s.isSolved()) {
            String md = s.render(ES, Format.MARKDOWN);
            assert md.contains("## Resultado\n\n> ") : md;
        }
    }

    @Test
    void theStepListOfACardKeepsItsBullets() {
        // El Markdown de una lista de pasos (lo que se copia de una tarjeta) sigue siendo una lista
        var steps = Derivatives.nth(ExprParser.parse("x^2*sin(x)"), "x", 1).steps();
        String md = StepRenderer.render(steps, ES, Format.MARKDOWN);
        assert md.startsWith("- **Regla del producto**") : md;
    }
}
