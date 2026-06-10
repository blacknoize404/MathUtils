package MathUtils.vector;

import MathUtils.Angle;
import MathUtils.exceptions.ZeroVectorException;

// TODO: 8/12/2023 Revisar qué podría faltar de vectores
public abstract class Vector<V extends Vector<V>> implements Comparable<V> {

    /**
     * Determina si el vector es un vector nulo o cero.
     * <p>Stewart 7.ª Ed. Sección 12.2. Vectores. p. 792
     * <p>Definición de vector, penúltima y última oración:
     * <p>El vector cero, denotado por 0, tiene longitud 0. Es el único
     * vector sin dirección específica.
     * @return el resultado de comprobar si el vector es cero.
     */
    public abstract boolean isZero();

    /**
     * Devuelve un vector con los valores absolutos de cada uno de sus componentes
     * @return dicho vector
     */
    public abstract V abs();

    /**
     * Realiza la suma vectorial entre el vector de la instancia y el del argumento.
     * <p>Stewart 7.ª Ed. Sección 12.2. Vectores. p. 792
     * <p>Definición de suma vectorial:
     * <p>Si u y v son vectores colocados de modo que el punto inicial de v esté en el punto
     * terminal de v, entonces la suma de u + v es el vector del punto inicial de u al punto
     * terminal de v.
     * @param a vector a sumar
     * @return el vector resultado de la operación
     * @see #subtract(Vector)
     */
    public abstract V sum(V a);

    /**
     * Realiza la resta vectorial entre el vector de la instancia y el del argumento.
     * <p>Stewart 7.ª Ed. Sección 12.2. Vectores. p. 792
     * <p>Ídem a definición de suma vectorial
     * @param a vector a restar
     * @return el vector resultado de la operación
     * @see #sum(Vector) sum
     */
    public abstract V subtract(V a);

    /**
     * Realiza el producto entre un escalar y el vector de la instancia.
     * <p>Stewart 7.ª Ed. Sección 12.2. Vectores. p. 793
     * <p>Definición de multiplicación por un escalar:
     * <p>Si c es un escalar y v es un vector, entonces el múltiplo escalar
     * cv es el vector cuya longitud es |c| multiplicado por la longitud
     * de v y cuya dirección es la misma que v si c > 0 y es opuesta a v
     * si c < 0.
     * <p>Si c = 0 o v = 0, entonces cv = 0.
     * @param scalar escalar a multiplicar
     * @return el vector resultado de la operación
     * @see #divide(Number) divide
     */
    public abstract V multiply(Number scalar);

    /**
     * Realiza el producto entre un escalar y el vector de la instancia.
     * <p>Stewart 7.ª Ed. Sección 12.2. Vectores. p. 793
     * <p>Ídem de división por un escalar.
     * @param scalar escalar a dividir
     * @return el vector resultado de la operación
     * @see #multiply(Number) multiply
     */
    public abstract V divide(Number scalar);

    /**
     * Calcula la magnitud, longitud o norma del vector usando el teorema de Pitágoras.
     * <p>Stewart 7.ª Ed. Sección 12.2. Vectores. p. 794
     * <p>Ejemplo 3:
     * <p>La longitud del vector bidimensional:
     * <p>{@code <a1, a2> = sqrt(a1^2 + a2^2)}
     * <p>La longitud del vector tridimensional:
     * <p>{@code <a1, a2, a3> = sqrt(a1^2 + a2^2 + a3^2)}
     *
     * @return dicha magnitud, longitud o norma.
     * @see #getNorm()
     * @see #getMagnitude()
     */
    public abstract double getLength();

    /**
     * Alias de {@link #getLength()}.
     * @return dicha magnitud, longitud o norma.
     * @see #getLength()
     * @see #getMagnitude()
     */
    public double getNorm() {
        return getLength();
    }

    /**
     * Alias de {@link #getLength()}.
     * @return dicha magnitud, longitud o norma.
     * @see #getLength()
     * @see #getNorm()
     */
    public double getMagnitude() {
        return getLength();
    }

    /**
     * Realiza el producto punto entre el vector de la instancia
     * y el del argumento.
     * <p>Stewart 7.ª Ed. Sección 12.3. Vectores. p. 800.
     * <p>Definición de producto punto:
     * <p>Si {@code a = <a1, a2, a3} y {@code b = <b1, b2, b3>},
     * entonces el producto punto de <b>a</b> y <b>b</b> es el
     * escalar <b>a</b> * <b>b</b> dado por:
     * <p><b>a</b> * <b>b</b> = a1 * b1 + a2 * b2 + a3 * b3</p>
     *
     * @param a vector a multiplicar
     * @return el valor escalar resultante
     * @see #getScalarProduct(Vector)
     */
    public abstract double getDotProduct(V a);

    /**
     * Alias de {@link #getDotProduct(Vector)}.
     * @return el valor escalar resultante
     * @see #getDotProduct(Vector)
     */
    public double getScalarProduct(V a) {
        return getDotProduct(a);
    }

    /**
     * Realiza el producto cruz entre el vector de la instancia y el del argumento
     * usando la regla del determinante o regla de Sarrus.
     * <p>Stewart 7.ª Ed. Sección 12.4. Vectores. p. 808.
     * @param a vector de entrada con el cual calcular el producto cruz
     * @return retorna un vector ortogonal al vector de entrada y al vector de la instancia,
     * este vector es normal al plano.
     */
    public abstract Vector3D getCrossProduct(V a);

    // TODO: 9/12/2023 Documentar
    public Vector3D getNormalVector(V a) {
        return getCrossProduct(a);
    }

    public V getUnitNormalVector(V a) {
        @SuppressWarnings("unchecked")
        V normal = (V) this.getCrossProduct(a);
        return normal.divide(normal.getLength());
    }

    /**
     * <p>Comprueba si los vectores son iguales usando la propiedad del producto punto.
     * <p>Stewart 7.ª Ed. Sección 12.3. El producto punto. p. 801
     * <p>Propiedades del producto punto:
     * <p>1. <b>a</b> * <b>a</b> = | <b>a</b> |^2
     * <p>Por ello, si el producto escalar entre vectores <b>a</b> y <b>b</b> es igual
     * al cuadrado de la norma de <b>a</b> o <b>b</b>, entonces, por transitividad
     * <b>a</b> = <b>b</b></p>
     * @param a Vectorial.Vector a comparar
     * @return un booleano indicando sin son o no iguales.
     */
    public abstract boolean isEqualTo(V a);

    /**
     * Obtiene el ángulo comprendido entre el vector de la instancia
     * y el del argumento.
     * <p>Stewart 7.ª Ed. Sección 12.3. El producto punto. p. 802
     * <p>Corolario 6:
     * <p>Si <b>&theta</b> es el ángulo entre los vectores no cero <b>a</b> y <b>b</b>, entonces:
     * <p>cos <b>&theta</b> = (<b>a</b> * <b>b</b>) / (|<b>a</b>| * |<b>b</b>|)
     * @param a Vectorial.Vector a calcular el ángulo
     * @return el ángulo comprendido entre los vectores
     */
    public abstract Angle getAngleBetween(V a) throws ZeroVectorException;

    /**
     * Determina si el vector de instancia y el del argumento son ortogonales (perpendiculares)
     * <p>Stewart 7.ª Ed. Sección 12.3. El producto punto. p. 803
     * <p>Corolario 7:
     * <p>Dos vectores <b>a</b> y <b>b</b> son ortogonales si y solo si <b>a</b> * <b>b</b> = 0.
     * @param a Vectorial.Vector con el cual determinar si son ortogonales
     * @return si son ortogonales o no.
     */
    public boolean isOrthogonalTo(V a) {
        return this.getDotProduct(a) == 0d;
    }

    /**
     * Compara vectores usando su norma
     * @param o el vector a ser comparado
     * @return el resultado de la comparación
     */
    @Override
    public int compareTo(V o) {
        // Multiplico por 100 para no perder tanta precisión al convertir a entero.
        return (int) ((this.getLength() - o.getLength()) * 100);
    }

    /**
     * Obtiene la representación del vector en un String
     * <p>Stewart 7.ª Ed. Sección 12.2. Componentes.
     * <p>Si se coloca el punto inicial de un vector <b>a</b> en el origen
     * de un sistema de coordenadas rectangulares, entonces el
     * punto terminal de <b>a</b> tiene coordenadas de la forma {@code (a1, a2)}
     * o {@code (a1, a2, a3)}, lo cual depende de si el sistema de coordenadas
     * es de dos o tres dimensiones.
     * <p>Estas coordenadas se llaman componentes de a y se escriben:
     * <p>{@code a = <x, y>; b = <x, y, z>}
     * @return dicha representación.
     */
    @Override
    public abstract String toString();
}
