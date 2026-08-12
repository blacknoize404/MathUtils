package MathUtils.cas.integral;

import MathUtils.cas.expr.Eval;
import MathUtils.cas.expr.ExprParser;
import MathUtils.cas.steps.Rule;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static MathUtils.cas.integral.IntegralChecks.check;
import static MathUtils.cas.integral.IntegralChecks.uses;

class SpecialFunctionIntegralsTest {

    private static double value(String expression, double x) {
        return Eval.value(ExprParser.parse(expression), Map.of("x", x));
    }

    @Test
    void specialFunctionValues() {
        assert Math.abs(value("Si(x)", 1) - 0.9460830703671830) < 1e-10;
        assert Math.abs(value("Si(x)", -2) + 1.6054129768026948) < 1e-10;
        assert Math.abs(value("Ci(x)", 1) - 0.3374039229009681) < 1e-10;
        assert Math.abs(value("Ei(x)", 1) - 1.8951178163559368) < 1e-10;
        assert Math.abs(value("Ei(x)", -1) + 0.2193839343955203) < 1e-10;
        assert Math.abs(value("li(x)", 2) - 1.0451637801174927) < 1e-10;
        assert Double.isNaN(value("Ci(x)", -1));
    }

    @Test
    void integralsOfSpecialIntegrands() {
        check("sin(x)/x");
        check("cos(x)/x");
        check("e^x/x");
        check("1/ln(x)", 1.5, 3);
        check("x/ln(x)", 1.5, 3);
        check("x^2/ln(x)", 1.5, 3);
        check("e^(-x^2)", -1, 2);
        check("e^(-3x^2)", -1, 2);
        check("3*sin(x)/x");
        check("sin(2x)/x");
        check("e^(2x)/x");
        check("e^x/x^2");
        check("e^x/x^3");
    }

    @Test
    void integralsOfTheSpecialFunctions() {
        check("erf(x)", -1, 2);
        check("Si(x)");
        check("Ci(x)");
        check("Ei(x)");
        check("x*erf(x)", -1, 2);
    }

    @Test
    void stepsExplainTheSpecialFunction() {
        assert uses(check("sin(x)/x"), Rule.INT_SI_FORM);
        assert uses(check("cos(x)/x"), Rule.INT_CI_FORM);
        assert uses(check("e^x/x"), Rule.INT_EI_FORM);
        assert uses(check("1/ln(x)", 1.5, 3), Rule.INT_LI_FORM);
        assert uses(check("e^(-x^2)", -1, 2), Rule.INT_ERF_FORM);
        assert uses(check("e^x/x^2"), Rule.INT_EI_REDUCTION);
    }
}
