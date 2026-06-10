package MathUtils.algebra.trigonometrics;

import MathUtils.algebra.Const;
import MathUtils.exceptions.FunctionEvaluationException;
import org.junit.jupiter.api.Test;

class SinTest {
    @Test
    void sen0() throws FunctionEvaluationException {

        Sin sin = new Sin(new Const(0));
        assert sin.eval() == 0d;

//        System.out.println(Math.sin(Math.toRadians(0)));
//        System.out.println(Math.sin(Math.toRadians(90)));
//        System.out.println(Math.sin(Math.toRadians(180)));
//        System.out.println(Math.sin(Math.toRadians(270)));
//        System.out.println(Math.sin(Math.toRadians(-360)) + Math.sin(Math.toRadians(180)) * (-360d / 180));

    }
//
//    @Test
//    void sen90() throws FunctionEvaluationException {
//
//        Sin sin = new Sin(new Const(Angle.fromAngle(90).getRadian()));
//        assert sin.eval() == 1;
//
//    }
//
//    @Test
//    void sen180() throws FunctionEvaluationException {
//
//        Sin sin = new Sin(new Const(Angle.fromAngle(180).getRadian()));
//        System.out.println(sin.eval());
//        assert sin.eval() == 0;
//
//    }
//
//    @Test
//    void sen270() throws FunctionEvaluationException {
//
//        Sin sin = new Sin(new Const(Angle.fromAngle(270).getRadian()));
//        System.out.println(sin.eval());
//        assert sin.eval() == -1d;
//
//    }
//
//    @Test
//    void sen360() throws FunctionEvaluationException {
//
//        Sin sin = new Sin(new Const(Angle.fromAngle(360).getRadian()));
//        System.out.println(sin.eval());
//        assert sin.eval() == 0;
//
//    }

}