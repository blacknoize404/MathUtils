package MathUtils.combinatory;

import MathUtils.algebra.exponentials.Pow;
import MathUtils.algebra.operators.DivOperator;
import MathUtils.algebra.operators.MultOperator;
import MathUtils.algebra.primitives.MathElement;

public class Combinatory {

    public static MathElement getPermutationsWithoutRepetitionOf(int n) {
        return Factorial.of(n);
    }

    public static MathElement getPermutationsWithRepetitionOf(int n, int a, int b, int c) {
        return new DivOperator(
                Factorial.of(n),
                new MultOperator(
                        Factorial.of(a),
                        Factorial.of(b),
                        Factorial.of(c)
                )
        );
    }

    /**
     * Obtiene la cantidad de variaciones sin repetición de un conjunto n,
     * dado un tamaño de población m.
     * @param n tamaño de la población
     * @param m tamaño de la muestra
     * @return cantidad de variaciones
     */
    public static MathElement getVariationsWithoutRepetitionOf(int n, int m) {
        return new DivOperator(
                Factorial.of(n),
                Factorial.of(n - m)
        );
    }

    /**
     * Obtiene la cantidad de variaciones con repetición de un conjunto n,
     * dado un tamaño de población m.
     * @param n tamaño de la población
     * @param m tamaño de la muestra
     * @return cantidad de variaciones
     */
    public static MathElement getVariationsWithRepetitionOf(int n, int m) {
        return new Pow(n, m);
    }

    /**
     * Obtiene la cantidad de combinaciones posibles de un conjunto n,
     * dado un tamaño de población m.
     * @param n tamaño de la población
     * @param m tamaño de la muestra
     * @return cantidad de variaciones
     */
    public static MathElement getCombinationsOf(int n, int m) {
        return new DivOperator(
                Factorial.of(n),
                new MultOperator(
                        Factorial.of(m),
                        Factorial.of(n - m )
                )
        );
    }

}
