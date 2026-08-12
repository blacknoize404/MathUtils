package MathUtils.algebra.primitives;

import MathUtils.algebra.exponentials.Root;

/**
 * Una expresión matemática que contiene un argumento el cual posteriormente
 * se evalúa según el tipo de expresión que sea.
 * @param <T> el tipo de expresión, que puede ser {@link String} (para variables),
 *           {@link Number} (para constantes) y {@link MathElement} para funciones.
 */
public abstract class Expression<T> extends MathElement {

    /**
     * Generalmente es un MathElement
     */
    protected final T argument;

    public Expression(T argument) {
        this.argument = argument;
    }

    /**
     * Devuelve el contenido de la expresión
     * @return el contenido de la expresión
     */
    public T getArgument() {
        return argument;
    }

    @Override
    public boolean equalsTo(MathElement mathElement) {

        // Si es nulo el argumento debe retornar falso, porque se supone que para poder acceder a equalsTo
        // este elemento no puede ser nulo, pero el del argumento sí.
        if (mathElement == null) return false;

        // Si no es una expresión, automáticamente debe retornar, ya que estamos comparando expresiones
        if (!(mathElement instanceof Expression)) return false;

        // Si no son el mismo tipo de MathElement
        if (this.getClass() != mathElement.getClass()) {

            // Si son distintos pero ambos heredan de un mismo padre (como en el caso de Sqrt-Root
            //  y Cbrt-Root.
            if (this instanceof Root actualRoot && mathElement instanceof Root argumentRoot) {

                return  actualRoot
                            .getArgument()
                            .equalsTo(argumentRoot.getArgument()) &&
                        actualRoot
                            .getRoot()
                            .equalsTo(argumentRoot.getRoot());

            }

        }

        // Si los argumentos son numéricos...
        if (argument instanceof Number arg0Number &&
                ((Expression<?>) mathElement).argument instanceof Number arg1Number) {
            return arg0Number.doubleValue() == arg1Number.doubleValue();
        }

        // Si los argumentos son variables...
        if (argument instanceof String arg0String
                && ((Expression<?>) mathElement).argument instanceof String arg1String) {
            return arg0String.equalsIgnoreCase(arg1String);
        }

        // Si ambos argumentos son elementos matemáticos...
        if (argument instanceof MathElement arg0Element &&
                ((Expression<?>) mathElement).argument instanceof MathElement arg1Element) {
            return arg0Element.equalsTo(arg1Element);
        }

        return false;


//        throw new RuntimeException("No deberías estar viendo este error, ya que significa que" +
//                "has definido un tipo de expresión que no hereda de MathElement ni es numérico, " +
//                "ni String, ni MathElement");
    }

    @Override
    public String toString() {
        if (argument instanceof Double && (Double) argument == Math.E) return "E";
        return argument.toString();
    }
}
