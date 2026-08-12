package MathUtils.point;

import MathUtils.vector.Vector;

/**
 * Clase abstracta que representa el concepto del punto
 * @param <P> Usado para definir el tipo de retorno como vector (debe coincidir con la
 *           dimensión del punto a la hora de implementarlo.
 */
public abstract class Point<P extends Point<?, ?>, V extends Vector<?>> implements Cloneable {

    /**
     * Devuelve el punto como un vector
     * @return el vector resultante
     */
    public abstract V AsVector();

    /**
     * Comprueba si un punto es igual a otro
     * @param point el punto al cual este punto es igual
     * @return la validación de la operación
     */
    public abstract boolean isEqualTo(P point);

    /**
     * Hace una copia de la instancia del punto
     * @return dicha copia
     */
    @Override
    public P clone() {
        try {
            @SuppressWarnings("unchecked")
            P cloned = (P) super.clone();
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

}
