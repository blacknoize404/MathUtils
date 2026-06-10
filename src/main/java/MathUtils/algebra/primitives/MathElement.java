package MathUtils.algebra.primitives;

import MathUtils.algebra.Evaluation;
import MathUtils.exceptions.FunctionEvaluationException;

/**
 * Clase padre de todos los elementos matemáticos que construyen
 * una función, ya sean operadores, funciones, constantes o variables.
 */
public abstract class MathElement {

    /**
     * Evalúa dicho elemento teniendo en cuenta y sustituyendo las variables
     * por su valor correspondiente.
     * @param evaluations las evaluaciones de las variables de tipo {@link Evaluation} opcionales por si acaso es necesario evaluarlas
     * @return la evaluación de la variable
     * @throws FunctionEvaluationException en caso de que una de las variables no se le haya
     * definido un valor.
     */
    public abstract double eval(Evaluation... evaluations) throws FunctionEvaluationException;

    /**
     * Comprueba si este elemento es igual a otro pasado en el argumento.
     * @param mathElement elemento a comparar.
     * @return el resultado de la comparación, si son iguales, true, en caso contrario, false.
     */
    public abstract boolean equalsTo(MathElement mathElement);

}
