package MathUtils;

import MathUtils.point.Point2D;

public class Vector2DCloneTest {

    public static void main(String[] args) {
        Point2D v1 = new Point2D(1,1);
        Point2D v2 = v1.clone();

        v2.setX(2);

        System.out.println();

    }

}
