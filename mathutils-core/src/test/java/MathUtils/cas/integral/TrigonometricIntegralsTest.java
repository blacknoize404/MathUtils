package MathUtils.cas.integral;

import MathUtils.cas.steps.Rule;
import MathUtils.cas.steps.Solution;
import org.junit.jupiter.api.Test;

import static MathUtils.cas.integral.IntegralChecks.check;
import static MathUtils.cas.integral.IntegralChecks.uses;

class TrigonometricIntegralsTest {

    @Test
    void oddPowersOfSineAndCosine() {
        check("sin(x)^3");
        check("cos(x)^3");
        check("sin(x)^5");
        check("sin(x)^3*cos(x)^2");
        check("sin(x)^2*cos(x)^3");
        check("sin(x)^3*cos(x)^3");
        check("sin(x)^4*cos(x)");
    }

    @Test
    void evenPowersUseHalfAngles() {
        check("sin(x)^2");
        check("cos(x)^2");
        check("sin(x)^4");
        check("cos(x)^4");
        check("sin(x)^2*cos(x)^2");
        check("sin(x)^2*cos(x)^4");
    }

    @Test
    void tangentsAndSecants() {
        check("tan(x)^2", 0.1, 1.2);
        check("tan(x)^3", 0.1, 1.2);
        check("tan(x)^4", 0.1, 1.2);
        check("cot(x)^2", 0.3, 1.2);
        check("cot(x)^3", 0.3, 1.2);
        check("sec(x)^3", 0.1, 1.2);
        check("sec(x)^5", 0.1, 1.2);
        check("csc(x)^3", 0.3, 1.2);
        check("tan(x)*sec(x)^2", 0.1, 1.2);
        check("tan(x)^2*sec(x)^2", 0.1, 1.2);
        check("tan(x)^3*sec(x)", 0.1, 1.2);
        check("tan(x)^2*sec(x)", 0.1, 1.2);
        check("sec(x)^4", 0.1, 1.2);
        check("sec(x)^2*tan(x)^3", 0.1, 1.2);
        check("1/cos(x)", 0.1, 1.2);
        check("1/sin(x)^2", 0.3, 1.2);
    }

    @Test
    void productsOfDifferentAngles() {
        check("sin(3x)*cos(2x)");
        check("sin(x)*sin(2x)");
        check("cos(x)*cos(5x)");
        check("sin(2x)*sin(3x)");
        check("cos(4x)*sin(x)");
    }

    @Test
    void linearArguments() {
        check("sin(2x)^2");
        check("cos(3x)^3");
        check("sin(2x+1)^3");
    }

    @Test
    void rationalFunctionsOfSineAndCosine() {
        check("1/(1+cos(x))", 0.2, 2);
        check("1/(2+sin(x))", 0.2, 3);
        check("1/(1+sin(x))", 0.2, 1.4);
        check("1/(sin(x)+cos(x))", 0.2, 0.7);
        check("1/(3+2*cos(x))", 0.2, 3);
        check("sin(x)/(1+cos(x))", 0.2, 2);
    }

    @Test
    void stepsNameTheTechnique() {
        assert uses(check("sin(x)^3"), Rule.INT_TRIG_ODD_POWER);
        assert uses(check("sin(x)^2"), Rule.INT_TRIG_HALF_ANGLE);
        assert uses(check("sin(3x)*cos(2x)"), Rule.INT_TRIG_PRODUCT_TO_SUM);
        assert uses(check("tan(x)^4", 0.1, 1.2), Rule.INT_TRIG_TAN_REDUCTION);
        assert uses(check("sec(x)^3", 0.1, 1.2), Rule.INT_TRIG_SEC_REDUCTION);
        assert uses(check("1/(2+sin(x))", 0.2, 3), Rule.INT_TRIG_WEIERSTRASS);
    }

    @Test
    void tableCoversReciprocals() {
        Solution s = check("1/cos(x)", 0.1, 1.2);
        assert s.steps().size() == 1;
    }
}
