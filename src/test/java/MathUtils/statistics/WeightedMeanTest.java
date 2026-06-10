package MathUtils.statistics;

import org.junit.jupiter.api.Test;

class WeightedMeanTest {

    /**
     * In a class of 20, eight students averaged a score of 86,
     * seven students had an average of 74, and five students
     * had an average test score of 98. What is the average test
     * score for the entire class?
     */
    @Test
    void testData() {

        double[] students = {8, 7, 5};
        double[] notes = {86, 74, 98};

        WeightedMean mean = new WeightedMean(students, notes);

        assert mean.eval() == 84.8;

    }

    /**
     * In a certain college, 20% of students have an average weight
     * of 140 lbs, 35% have an average weight of 160 lbs, 30% of
     * students have an average weight of 175 lbs, and 15% of students
     * have an average weight of 195 lbs. Based on this data, what is
     * the average weight of all students in this school?
     */
    @Test
    void testData2() {

        double[] studentsPercent = {0.2, 0.35, 0.3, 0.15};
        double[] notes = {140, 160, 175, 195};

        WeightedMean mean = new WeightedMean(studentsPercent, notes);

        assert mean.eval() == 165.75;

    }

    /**
     * Tenemos datos de una gráfica de evaluaciones de 0 a 10 de un producto
     * y cuántos usuarios han evaluado en sus respectivas puntuaciones.
     * Para 0 -> 1385, 1 -> 1210, 2 -> 1497, 3 -> 1577, 4 -> 1411, 5 -> 1356
     * 6 -> 1297, 7 -> 815, 8 -> 454, 9 -> 140, 10 -> 108.
     * ¿Cuál es el promedio de evaluación dados estos datos?
     */
    @Test
    void testData3() {

        double[] rating = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        double[] ratingFrequency = {1385, 1210, 1497, 1577, 1411, 1356, 1297, 815, 454, 140, 108};

        WeightedMean mean = new WeightedMean(ratingFrequency, rating);

        assert Math.round(mean.eval() * 100) / 100d == 3.63;

    }


}