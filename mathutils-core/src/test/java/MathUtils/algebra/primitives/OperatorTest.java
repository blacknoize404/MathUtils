package MathUtils.algebra.primitives;

import MathUtils.algebra.Const;
import MathUtils.algebra.Var;
import MathUtils.algebra.operators.SumOperator;
import MathUtils.algebra.operators.primitives.Operator;
import org.junit.jupiter.api.Test;

class OperatorTest {

    @Test
    void constructorTwoElements() {
        Operator op = new SumOperator(new Const(1), new Const(2));
        assert op.getContent().size() == 2;
    }

    @Test
    void constructorMultipleElements() {
        Operator op = new SumOperator(new Const(1), new Const(2), new Const(3), new Const(4));
        assert op.getContent().size() == 4;
    }

    @Test
    void constructorWithNumberAndElement() {
        Operator op = new SumOperator(5, new Var("x"));
        assert op.getContent().size() == 2;
    }

    @Test
    void constructorWithElementAndNumber() {
        Operator op = new SumOperator(new Var("x"), 5);
        assert op.getContent().size() == 2;
    }

    @Test
    void constructorWithTwoNumbers() {
        Operator op = new SumOperator(1, 2);
        assert op.getContent().size() == 2;
    }

    @Test
    void constructorWithNumbersAndRest() {
        Operator op = new SumOperator(1, 2, 3, 4, 5);
        assert op.getContent().size() == 5;
    }

    @Test
    void addElement() {
        Operator op = new SumOperator(new Const(1), new Const(2));
        op.addElement(new Const(3));
        assert op.getContent().size() == 3;
    }

    @Test
    void getContent() {
        Operator op = new SumOperator(new Const(1), new Const(2));
        assert op.getContent().get(0) instanceof Const;
        assert op.getContent().get(1) instanceof Const;
    }

    @Test
    void equalsToNull() {
        Operator op = new SumOperator(new Const(1), new Const(2));
        assert !op.equalsTo(null);
    }

    @Test
    void equalsToNonOperator() {
        Operator op = new SumOperator(new Const(1), new Const(2));
        assert !op.equalsTo(new Const(5));
    }

    @Test
    void equalsToDifferentClass() {
        Operator op1 = new SumOperator(new Const(1), new Const(2));
        Operator op2 = new MathUtils.algebra.operators.SubOperator(new Const(1), new Const(2));
        assert !op1.equalsTo(op2);
    }

    @Test
    void equalsToDifferentSize() {
        Operator op1 = new SumOperator(new Const(1), new Const(2));
        Operator op2 = new SumOperator(new Const(1), new Const(2), new Const(3));
        assert !op1.equalsTo(op2);
    }

    @Test
    void equalsToSame() {
        Operator op1 = new SumOperator(new Const(1), new Const(2));
        Operator op2 = new SumOperator(new Const(1), new Const(2));
        assert op1.equalsTo(op2);
    }

    @Test
    void equalsToDifferentElements() {
        Operator op1 = new SumOperator(new Const(1), new Const(2));
        Operator op2 = new SumOperator(new Const(1), new Const(3));
        assert !op1.equalsTo(op2);
    }

    @Test
    void equalsToSameMultipleElements() {
        Operator op1 = new SumOperator(new Const(1), new Const(2), new Const(3));
        Operator op2 = new SumOperator(new Const(1), new Const(2), new Const(3));
        assert op1.equalsTo(op2);
    }
}
