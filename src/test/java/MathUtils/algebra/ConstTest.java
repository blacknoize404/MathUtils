package MathUtils.algebra;

import org.junit.jupiter.api.Test;

class ConstTest {

    @Test
    void fractionalConstantEvaluation() {

        Const a = new Const(1/8d);

        assert a.eval() == 0.125;

    }

    @Test
    void equal() {
        Const c = new Const(5);
        Const d = new Const(5);

        assert c.equalsTo(d);
    }

    @Test
    void notEqual() {
        Const c = new Const(5);
        Const e = new Const(6);
        assert !c.equalsTo(e);
    }
}