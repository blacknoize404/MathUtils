package MathUtils.algebra;

import org.junit.jupiter.api.Test;

class ConstTest {

    @Test
    void evalInteger() {
        Const c = new Const(42);
        assert c.eval() == 42;
    }

    @Test
    void evalDouble() {
        Const c = new Const(3.14);
        assert c.eval() == 3.14;
    }

    @Test
    void evalZero() {
        Const c = new Const(0);
        assert c.eval() == 0;
    }

    @Test
    void evalNegativeInteger() {
        Const c = new Const(-7);
        assert c.eval() == -7;
    }

    @Test
    void evalNegativeDouble() {
        Const c = new Const(-2.5);
        assert c.eval() == -2.5;
    }

    @Test
    void evalIntegerAsDouble() {
        Const c = new Const(5.0);
        assert c.eval() == 5;
    }

    @Test
    void equalsToSame() {
        assert new Const(5).equalsTo(new Const(5));
    }

    @Test
    void equalsToDifferent() {
        assert !new Const(5).equalsTo(new Const(7));
    }

    @Test
    void equalsToDoubleSame() {
        assert new Const(3.14).equalsTo(new Const(3.14));
    }

    @Test
    void equalsToDoubleDifferent() {
        assert !new Const(3.14).equalsTo(new Const(2.71));
    }

    @Test
    void testToString() {
        Const c = new Const(42);
        String s = c.toString();
        assert "42".equals(s);
    }

    @Test
    void testToStringDouble() {
        Const c = new Const(3.14);
        String s = c.toString();
        assert "3.14".equals(s);
    }
}
