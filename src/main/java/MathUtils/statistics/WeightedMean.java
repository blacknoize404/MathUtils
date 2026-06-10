package MathUtils.statistics;

/**
 * Permite calcular el promedio ponderado.
 * <p>El uso más habitual de este cálculo se vincula a ciertas evaluaciones.
 * Supongamos que, para completar un determinado curso, un alumno debe
 * rendir cinco exámenes corrientes y un examen final que equivale a los
 * otros cinco exámenes. Esto quiere decir que, si cada examen corriente
 * tiene una ponderación de 1, el examen final tendrá una ponderación de 5.
 *
 * <p>El estudiante en cuestión obtiene las siguientes notas: 6, 7, 5, 7 y 8
 * en los exámenes corrientes y 6 en el examen final. Apelando a la fórmula
 * ya mencionada, el promedio ponderado de las calificaciones de este alumno
 * será igual a la suma de cada una multiplicada por su ponderación
 * (6 x 1 + 7 x 1 + 5 x 1 + 7 x 1 + 8 x 1 + 6 x 5 = 63) dividida por la suma
 * de todas las ponderaciones (1 + 1 + 1 + 1 + 1 + 5 = 10). El promedio
 * ponderado en este caso, por lo tanto, es de 6,3.
 */
public class WeightedMean {
    double[] weights;
    double[] frequencies;

    // TODO: 20/12/2023 Generalizar a la librería usando la herencia de un primitivo que pueda evaluar
    public WeightedMean(double[] weights, double[] frequencies) {
        if (weights.length != frequencies.length)
            throw new IllegalArgumentException("Los pesos y las frecuencias deben ser del mismo tamaño");
        this.weights = weights;
        this.frequencies = frequencies;
    }
    // TODO: 20/12/2023 Testear este constructor
    /**
     * Creates an instance of WeightedMean with a 2d array complaining this format:
     * <table border="1">
     *   <tr>
     *     <th> weights </td> <th> frequency </td>
     *   </tr>
     *   <tr>
     *     <td> a_1 </td> <td> a_2 </td>
     *   </tr>
     *   <tr>
     *     <td> b_1 </td> <td> b_2 </td>
     *   </tr>
     *   <tr>
     *     <td> c_1 </td> <td> c_2 </td>
     *   </tr>
     * </table>
     * @param table an array with the previous format.
     */
    public WeightedMean(double[][] table) {

        weights = new double[table.length];
        frequencies = new double[table[0].length];

        for (int i = 0; i < table.length; i++) {
            weights[i] = table[i][0];
            frequencies[i] = table[i][1];
        }

    }

    /**
     * Evalúa dicho promedio ponderado.
     * Complejidad de O(n)
     * @return el valor del promedio ponderado
     */
    public double eval() {
        double sumWeightsProdFrequencies = 0;
        double sumWeights = 0;

        for (int i = 0; i < weights.length; i++) {
            sumWeightsProdFrequencies += weights[i] * frequencies[i];
            sumWeights += weights[i];
        }

        return sumWeightsProdFrequencies / sumWeights;

    }

}
