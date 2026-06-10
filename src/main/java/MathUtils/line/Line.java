package MathUtils.line;

import MathUtils.point.Point;
import MathUtils.vector.Vector;

// TODO: 8/12/2023 Implementar la funcionalidad de recta
public abstract class Line <
        L extends Line <L, P, V>,
        P extends Point <P, V>,
        V extends Vector <V>
        > {

    protected final P displacement;
    protected final V direction;

    /**
     * Crea una nueva recta
     * @param displacement punto que define el desplazamiento de la recta
     * @param direction dirección en la cual apunta la recta
     */
    public Line(P displacement, V direction) {
        this.displacement = displacement;
        this.direction = direction;
    }

    /**
     * Dado un valor t, devuelve el punto donde la partícula
     * se encuentra en ese justo valor.
     * @param t dicho valor que representa la ubicación de la
     *          partícula.
     * @return el punto
     */
    public abstract P eval(double t);

    // Relaciones entre rectas

    /**
     * Comprueba si es una recta paralela evaluando si los
     * vectores direccionales unitarios son iguales.
     * @param line la recta a comparar
     * @return true si es paralelo, de lo contrario, false
     */
    public boolean isParallelTo(L line) {
        return getUnitDirectionVector().isEqualTo(line.getUnitDirectionVector());
    }

    /**
     * Comprueba si es una recta paralela evaluando si los
     * vectores direccionales son ortogonales.
     * @param line la recta a comparar
     * @return true si es perpendicular, de lo contrario, false
     */
    public boolean isPerpendicularTo(L line) {
        return getDirectionVector().isOrthogonalTo(line.getDirectionVector());
    }

    /**
     * Comprueba si ambas rectas son coincidentes
     * Para ello sus vectores direccionales unitarios deben ser iguales
     * y dos valores evaluados en ambas rectas también deben ser iguales
     * @param line recta a comprobar
     * @return un booleano como respuesta al predicado
     */
    public boolean isCoincidentTo(L line) {
        if (!isParallelTo(line)) return false;
        return line.eval(0).isEqualTo(eval(0)) && line.eval(1).isEqualTo(eval(1));
    }

    public abstract boolean isIntersectedBy(L line);

    public V getUnitDirectionVector() {
        return direction.divide(direction.getLength());
    }

    public V getDirectionVector() { return direction; }

    public abstract String getParametricEquation();

    public abstract String getSymmetricEquation();

    public abstract String getGeneralEquation();

}
