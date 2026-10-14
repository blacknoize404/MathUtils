package barnesinside.mathutils.cas.integral;

import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Solution;
import org.junit.jupiter.api.Test;

import static barnesinside.mathutils.cas.integral.IntegralChecks.check;
import static barnesinside.mathutils.cas.integral.IntegralChecks.uses;

class RationalIntegralsTest {

    @Test
    void distinctLinearFactors() {
        check("1/(x^2-1)", 2, 5);
        check("1/(x*(x+1))", 1, 4);
        check("(3x+5)/((x-1)*(x+2))", 2, 5);
        check("1/((x-1)*(x-2)*(x-3))", 4, 6);
        check("x/(2x^2-5x+2)", 3, 6);
        check("1/(x^2-5x+6)", 4, 7);
    }

    @Test
    void repeatedLinearFactors() {
        check("1/(x^2*(x+1))", 1, 3);
        check("1/((x-1)^2*(x+2))", 2, 4);
        check("(x+3)/(x+1)^2", 0.5, 3);
        check("1/(x^3-x^2)", 2, 5);
        check("(2x+1)/(x^2*(x-1)^2)", 2, 5);
    }

    @Test
    void irreducibleQuadraticFactors() {
        check("1/((x+1)*(x^2+1))", 0.2, 3);
        check("1/(x^3+1)", 0.2, 3);
        check("(x^2+1)/((x-1)*(x^2+x+1))", 2, 5);
        check("1/(x^3+x)", 1, 3);
        check("(3x+2)/(x^2+x+1)", -1, 2);
        check("x/(x^2+2x+5)", -1, 2);
        check("1/(x^2+2x+5)", -1, 2);
    }

    @Test
    void repeatedQuadraticFactors() {
        check("1/(x^2+1)^2", -1, 2);
        check("1/(x^2+4)^3", -1, 2);
        check("x/(x^2+x+1)^2", -1, 2);
        check("1/(x*(x^2+1)^2)", 1, 3);
    }

    @Test
    void quadraticsWithIrrationalRoots() {
        check("1/(x^2-2)", 2, 4);
        check("1/(x^2-2x-1)", 3, 5);
        check("(x+1)/(x^2-3)", 2, 4);
        check("1/(x^2-2)^2", 2, 4);
        check("1/((x-1)*(x^2-2))", 2, 4);
    }

    @Test
    void divisionThenPartialFractions() {
        check("x^3/(x^2-1)", 2, 4);
        check("(x^4+1)/(x^3+x)", 1, 3);
        check("(x^2+1)/(x^2-1)", 2, 4);
    }

    @Test
    void stepsDescribeTheDecomposition() {
        Solution s = check("1/(x^2-3x+2)", 3, 5);
        assert uses(s, Rule.INT_PARTIAL_FRACTIONS);
        // La descomposición se cuenta en tres pasos: factorizar, plantear con coeficientes y resolverlos
        assert uses(s, Rule.INT_PF_FACTOR) && uses(s, Rule.INT_PF_SETUP) && uses(s, Rule.INT_PF_SOLVE);
        s = check("(3x+2)/(x^2+x+1)", -1, 2);
        assert uses(s, Rule.INT_QUADRATIC_SPLIT);
        s = check("1/(x^2+2x+5)", -1, 2);
        assert uses(s, Rule.INT_COMPLETE_SQUARE);
        s = check("1/(x^2+1)^2", -1, 2);
        assert uses(s, Rule.INT_REDUCTION);
    }

    @Test
    void unfactorableDenominatorsAreUnsolved() {
        for (String f : new String[]{"1/(x^4+x+1)", "1/(x^5+x+1)"}) {
            Solution s = Integrals.indefinite(f, "x");
            assert !s.isSolved() : f + " no debe resolverse: " + s.result();
        }
    }
}
