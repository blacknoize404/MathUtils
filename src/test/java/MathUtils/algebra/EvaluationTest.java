package MathUtils.algebra;

import org.junit.jupiter.api.Test;

class EvaluationTest {

    @Test
    void createAndAccess() {
        Evaluation e = new Evaluation("x", 42);
        assert "x".equals(e.var());
        assert e.value().equals(42);
    }

    @Test
    void createDoubleValue() {
        Evaluation e = new Evaluation("y", 3.14);
        assert "y".equals(e.var());
        assert e.value().equals(3.14);
    }

    @Test
    void testToString() {
        Evaluation e = new Evaluation("x", 10);
        String s = e.toString();
        assert s.contains("x") && s.contains("10");
    }
}
