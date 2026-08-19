package barnesinside.mathutils.cas.tree;

import barnesinside.mathutils.cas.syntax.SyntaxTools;

import java.util.List;

/**
 * Nodo del árbol de una expresión, pensado para dibujarlo en cualquier interfaz.
 *
 * @param id              identificador estable (preorden desde 0)
 * @param kind            tipo de nodo
 * @param label           texto corto para mostrar en el nodo ({@code +}, {@code ×}, {@code sin}, {@code x}, {@code 3})
 * @param latex           etiqueta en LaTeX
 * @param precedence      precedencia del operador (1 sumas, 2 productos, 3 signo, 4 potencias, 5 funciones, 6 hojas)
 * @param associativity   asociatividad del operador binario, o {@code null}
 * @param parentheses     cantidad de paréntesis con que estaba envuelto en el texto original
 * @param value           valor numérico del subárbol, o {@code null} si no se puede calcular (variables sin valor, indefinido)
 * @param text            texto de la subexpresión
 * @param evaluationOrder en qué paso del orden de operaciones se resuelve (desde 1), o {@code null}
 * @param children        hijos en orden de escritura
 */
public record TreeNode(int id, Kind kind, String label, String latex, int precedence,
                       SyntaxTools.Associativity associativity, int parentheses, Double value, String text,
                       Integer evaluationOrder, List<TreeNode> children) {

    /** Tipo de nodo. */
    public enum Kind {NUMBER, VARIABLE, CONSTANT, OPERATOR, NEGATION, FUNCTION}

    public TreeNode {
        children = List.copyOf(children);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }
}
