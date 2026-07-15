package MathUtils.parser;

import java.util.List;

/**
 * Construye un árbol de tipo {@code T} a partir de los elementos que reconoce
 * {@link ExpressionParser}. Permite reutilizar el mismo análisis sintáctico para distintas
 * representaciones (árbol {@code MathElement}, expresiones simbólicas…).
 * <p>Para señalar un error de significado (función desconocida, argumentos incorrectos…) las
 * implementaciones lanzan {@link IllegalArgumentException}; el parser la convierte en una
 * {@link MathUtils.exceptions.ExpressionParseException} con la posición exacta.
 *
 * @param <T> tipo de los nodos del árbol resultante
 */
public interface TreeBuilder<T> {

    /** Literal numérico tal como aparece en el texto ({@code "3.5"}, {@code "2e-3"}). */
    T number(String text);

    /** Identificador que no es una llamada a función: variable o constante ({@code pi}, {@code e}). */
    T variable(String name);

    /** Signo menos unario. */
    T negate(T operand);

    /**
     * Operador binario.
     * @param operator uno de {@code + - * / ^}
     */
    T binary(char operator, T left, T right);

    /** Llamada a función con sus argumentos ya construidos. */
    T function(String name, List<T> arguments);

    /**
     * Se llama cuando un operando estaba entre paréntesis en el texto original (no cuando son
     * los paréntesis de una llamada a función). Por defecto no hace nada; los constructores que
     * quieren conservar la estructura escrita (como el árbol sintáctico) lo sobrescriben.
     */
    default T group(T inner) {
        return inner;
    }

    /**
     * Indica si {@code name} es una función conocida. Se usa con la multiplicación implícita para
     * distinguir {@code sin(x)} (llamada) de {@code x(x+1)} (producto).
     */
    boolean isFunction(String name);
}
