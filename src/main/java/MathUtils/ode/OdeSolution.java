package MathUtils.ode;

/**
 * Trayectoria calculada por un solver: {@code y[i]} es el estado en {@code t[i]}.
 */
public record OdeSolution(double[] t, double[][] y) {

    /**
     * @return el estado en el último instante calculado
     */
    public double[] finalState() {
        return y[y.length - 1];
    }

    /**
     * @return el último instante calculado
     */
    public double finalTime() {
        return t[t.length - 1];
    }

    /**
     * @return la componente {@code index} del estado en cada instante
     */
    public double[] component(int index) {
        double[] values = new double[y.length];
        for (int i = 0; i < y.length; i++) values[i] = y[i][index];
        return values;
    }
}
