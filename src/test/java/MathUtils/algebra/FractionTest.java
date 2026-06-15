package MathUtils.algebra;

import org.junit.jupiter.api.Test;

class FractionTest {

    @Test
    void constructor() {
        Fraction f = new Fraction(3, 4);
        assert f.getNumerator() == 3;
        assert f.getDenominator() == 4;
    }

    @Test
    void isProper() {
        Fraction f = new Fraction(3, 4);
        assert !f.isImproper();
    }

    @Test
    void isImproper() {
        Fraction f = new Fraction(5, 3);
        assert f.isImproper();
    }

    @Test
    void sum() {
        Fraction a = new Fraction(1, 4);
        Fraction b = new Fraction(1, 4);
        a.sum(b);
        assert a.getNumerator() == 2;
        assert a.getDenominator() == 4;
    }

    @Test
    void sub() {
        Fraction a = new Fraction(3, 4);
        Fraction b = new Fraction(1, 4);
        a.sub(b);
        assert a.getNumerator() == 2;
        assert a.getDenominator() == 4;
    }

    @Test
    void multByInteger() {
        Fraction a = new Fraction(1, 4);
        a.mult(2);
        assert a.getNumerator() == 2;
        assert a.getDenominator() == 8;
    }

    @Test
    void multByFraction() {
        Fraction a = new Fraction(1, 2);
        Fraction b = new Fraction(2, 3);
        a.mult(b);
        assert a.getNumerator() == 2;
        assert a.getDenominator() == 6;
    }

    @Test
    void div() {
        Fraction a = new Fraction(1, 2);
        Fraction b = new Fraction(3, 4);
        a.div(b);
        assert a.getNumerator() == 4;
        assert a.getDenominator() == 6;
    }

    @Test
    void simplify() {
        Fraction f = new Fraction(6, 8);
        f.simplify();
        assert f.getNumerator() == 3;
        assert f.getDenominator() == 4;
    }

    @Test
    void simplifyAlreadySimplified() {
        Fraction f = new Fraction(3, 4);
        f.simplify();
        assert f.getNumerator() == 3;
        assert f.getDenominator() == 4;
    }

    @Test
    void eval() {
        Fraction f = new Fraction(1, 2);
        assert f.eval() == 0.5;
    }

    @Test
    void compareToEqual() {
        Fraction a = new Fraction(1, 2);
        Fraction b = new Fraction(2, 4);
        assert a.compareTo(b) == 0;
    }

    @Test
    void compareToLess() {
        Fraction a = new Fraction(1, 4);
        Fraction b = new Fraction(1, 2);
        assert a.compareTo(b) < 0;
    }

    @Test
    void compareToGreater() {
        Fraction a = new Fraction(3, 4);
        Fraction b = new Fraction(1, 4);
        assert a.compareTo(b) > 0;
    }

    @Test
    void testToString() {
        Fraction f = new Fraction(3, 4);
        String s = f.toString();
        assert s.contains("3") && s.contains("4");
    }

    @Test
    void setDenominatorZero() {
        Fraction f = new Fraction(1, 2);
        try {
            f.setDenominator(0);
            assert false : "Should have thrown ArithmeticException";
        } catch (ArithmeticException e) {
            assert true;
        }
    }
}
