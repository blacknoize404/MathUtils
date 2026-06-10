/**
 * Librería orientada a las matemáticas, estadísticas, construcción y cálculo computacional en general.
 */
module MathUtils {
    requires java.desktop;
    exports MathUtils;
    exports MathUtils.vector;
    exports MathUtils.algebra;
    exports MathUtils.algebra.exponentials;
    exports MathUtils.algebra.operators.primitives;
    exports MathUtils.algebra.operators;
    exports MathUtils.algebra.logarithmics;
    exports MathUtils.algebra.primitives;
    exports MathUtils.algebra.trigonometrics;
    exports MathUtils.exceptions;
    exports MathUtils.point;
    exports MathUtils.line;
    exports MathUtils.informatics;
    exports MathUtils.statistics;
    exports MathUtils.series;
}