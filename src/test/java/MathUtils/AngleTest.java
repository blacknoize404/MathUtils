package MathUtils;

import org.junit.jupiter.api.Test;

class AngleTest {

    @Test
    void fromAngle() {
        Angle a = Angle.fromAngle(90);
        assert Math.abs(a.getAngle() - 90) < 1e-10;
        assert Math.abs(a.getRadian() - Math.PI / 2) < 1e-10;
    }

    @Test
    void fromRadians() {
        Angle a = Angle.fromRadians(Math.PI);
        assert Math.abs(a.getRadian() - Math.PI) < 1e-10;
        assert Math.abs(a.getAngle() - 180) < 1e-10;
    }

    @Test
    void fromAngleZero() {
        Angle a = Angle.fromAngle(0);
        assert Math.abs(a.getAngle()) < 1e-10;
        assert Math.abs(a.getRadian()) < 1e-10;
    }

    @Test
    void fromRadiansZero() {
        Angle a = Angle.fromRadians(0);
        assert Math.abs(a.getRadian()) < 1e-10;
        assert Math.abs(a.getAngle()) < 1e-10;
    }

    @Test
    void setRadian() {
        Angle a = Angle.fromAngle(0);
        a.setRadian(Math.PI / 2);
        assert Math.abs(a.getRadian() - Math.PI / 2) < 1e-10;
        assert Math.abs(a.getAngle() - 90) < 1e-10;
    }

    @Test
    void setAngle() {
        Angle a = Angle.fromRadians(0);
        a.setAngle(180);
        assert Math.abs(a.getAngle() - 180) < 1e-10;
        assert Math.abs(a.getRadian() - Math.PI) < 1e-10;
    }

    @Test
    void testToString() {
        Angle a = Angle.fromAngle(45);
        String s = a.toString();
        assert s.contains("45");
    }
}
