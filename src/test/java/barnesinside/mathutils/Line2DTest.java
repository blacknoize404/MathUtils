package barnesinside.mathutils;

import barnesinside.mathutils.line.Line;
import barnesinside.mathutils.line.Line2D;
import barnesinside.mathutils.line.Line3D;
import barnesinside.mathutils.point.Point2D;
import barnesinside.mathutils.point.Point3D;
import barnesinside.mathutils.vector.Vector2D;
import barnesinside.mathutils.vector.Vector3D;

import java.util.ArrayList;

public class Line2DTest {
    public static void main(String[] args) {
        Line2DIntersectedTest();
    }

    public static void Line2DIntersectedTest() {

        Line2D l1 = new Line2D(new Point2D(0,0), new Vector2D(2,1));
        Line2D l2 = new Line2D(new Point2D(0,0), new Vector2D(-2,1));

        System.out.println(l1.isCoincidentTo(l2));

    }
    public static void ListOfLinesEvaluated() {
        ArrayList<Line<?, ?, ?>> list = new ArrayList<>();

        Line2D l1 = new Line2D(new Point2D(2,1), new Vector2D(2,2));
        list.add(l1);

        Line2D l2 = new Line2D(new Point2D(2,4), Vector2D.ZERO());
        list.add(l2);

        Line3D l3 = new Line3D(new Point3D(2,4,3), new Vector3D(3,2,1));
        list.add(l3);

        for (Line<?, ?, ?> l: list) {
            System.out.println(l.eval(-5));
        }

    }
}
