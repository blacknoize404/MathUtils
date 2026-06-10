package MathUtils.algebra.operators.primitives;

import MathUtils.algebra.Const;
import MathUtils.algebra.primitives.MathElement;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * Representa la clase base para cualquier operador matemático de dos
 * argumentos.
 */
public abstract class Operator extends MathElement {

    /**
     * Conforma el conjunto de elementos a operar.
     */
    protected final LinkedList<MathElement> elements = new LinkedList<>();

    /**
     * Construye la operación con n >= 2 elementos matemáticos.
     * @param first primer elemento.
     * @param second segundo elemento.
     * @param elements valores restantes en caso de una operación encadenada.
     */
    public Operator(MathElement first, MathElement second, MathElement... elements) {
        this.elements.add(first);
        this.elements.add(second);
        this.elements.addAll(List.of(elements));
    }

    /**
     * Construye la operación teniendo un valor numérico, y n >= 1 elementos matemáticos genéricos.
     * @param constant constante a evaluar la operación.
     * @param secondElement segundo elemento obligatorio con el cual realizar la operación.
     * @param elements elementos restantes en caso de una operación encadenada.
     */
    public Operator(Number constant, MathElement secondElement, MathElement... elements) {
        this(new Const(constant), secondElement, elements);
    }

    /**
     * Construye la operación con un elemento matemático n >= 1 valores numéricos.
     * @param first primer elemento.
     * @param constant constante.
     * @param constants valores restantes en caso de una operación encadenada.
     */
    public Operator(MathElement first, Number constant, Number... constants) {
        this(first, new Const(constant), convertElementList(constants));
    }

    /**
     * Construye la operación con n >= 2 valores numéricos.
     * @param firstConstant primer valor.
     * @param secondConstant segundo valor.
     * @param constants valores restantes en caso de una operación encadenada.
     */
    public Operator(Number firstConstant, Number secondConstant, Number... constants) {
        this(new Const(firstConstant), new Const(secondConstant), convertElementList(constants));
    }

    /**
     * Convierte un arreglo de números en una colección de elementos matemáticos constantes.
     * @param constants arreglo de números a convertir
     * @return una colección de elementos matemáticos constantes.
     */
    private static MathElement[] convertElementList(Number... constants) {
        return Arrays.stream(constants).map(Const::new).toArray(MathElement[]::new);
    }

    protected static String generateStringRepresentation(String separator, Collection<MathElement> elements) {
        return '(' +
                String.join(
                        separator,
                        elements.stream().map(MathElement::toString).toList()
                ) +
                ')';
    }

    /**
     * Agrega un nuevo elemento a operar
     * @param element el elemento a agregar a la operación
     */
    public void addElement(MathElement element) {
        this.elements.add(element);
    }

    /**
     * Obtiene una lista de elementos a operar
     * @return dicha lista
     */
    public List<MathElement> getContent() {
        return elements;
    }

    /**
     * Evalúa si este elemento matemático es igual en valores al del argumento.
     * Si el elemento del argumento es nulo, o no es instancia de operador, o no
     * son el mismo tipo de operador, retornará false.
     * En caso contrario, comprueba si ambos operadores tienen la misma cantidad
     * de elementos, si no coincide, retornará false.
     * En caso contrario, comprueba si cada elemento es igual al elemento en la
     * misma posición del otro {@link MathElement}.
     * Si al menos uno es distinto, devolverá false.
     * Si todos son iguales, devolverá true.
     * @param mathElement elemento a comparar.
     * @return la comprobación de igualdad.
     */
    @Override
    public boolean equalsTo(MathElement mathElement) {

        // Si es nulo el argumento debe retornar falso, porque se supone que para poder acceder a equalsTo
        // este elemento no puede ser nulo, pero el del argumento sí.
        if (mathElement == null) return false;

        // Si no es una expresión, automáticamente debe retornar, ya que estamos comparando expresiones
        if (!(mathElement instanceof Operator)) return false;

        // Si no son el mismo tipo de operador
        if (this.getClass() != mathElement.getClass()) return false;

        // Si la cantidad de elementos a operar no es la misma
        if (elements.size() != ((Operator) mathElement).elements.size()) return false;

        // Compara cada elemento de forma individual
        for (int i = 0; i < elements.size(); i++) {
            MathElement c1 = elements.get(i);
            MathElement c2 = ((Operator) mathElement).elements.get(i);
            if (!c1.equalsTo(c2)) return false;
        }

        // Si pasa todos estos filtros, significa que son iguales
        return true;

    }

//    public void showTreeNodes() {
//        BreathFirstSearchPrintTreeNodes.printNodes(this);
//    }

}
