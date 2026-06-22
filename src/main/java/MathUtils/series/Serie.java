package MathUtils.series;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.primitives.MathElement;
import MathUtils.exceptions.FunctionEvaluationException;

/**
 * Representará la suma evaluando una variable en un intervalo específico y devolviendo la suma entre
 * esos mathElements
 */
public final class Serie extends MathElement {

    private final MathElement content;
    private final String variable;
    private final int start;
    private final int end;

    public Serie(MathElement function, String variable, int start, int end) {
        this.content = function;
        this.variable = variable;
        this.start = start;
        this.end = end;

    }

    public Serie(Number constant, String variable, int start, int end) {
        this.content = new Const(constant);
        this.variable = variable;
        this.start = start;
        this.end = end;

    }

    @Override
    public double eval(Evaluation... evaluations) throws FunctionEvaluationException {

        // Cuento el resultado
        double result = 0;

        // Hago una nueva lista con las evaluaciones anteriores y un nuevo espacio para
        // añadir la evaluación de la variable de iteración
        Evaluation[] updatedEvaluations = new Evaluation[evaluations.length + 1];

        // Copio las evaluaciones anteriores
        System.arraycopy(evaluations, 0, updatedEvaluations, 0, evaluations.length);

        for (int k = start; k <= end; k++) {

            // Le añado la evaluación actual de la sumatoria a las evaluaciones
            updatedEvaluations[updatedEvaluations.length-1] = new Evaluation(variable, k);

            // Obtengo el resultado
            result += content.eval(updatedEvaluations);

        }

        return result;

    }

    @Override
    public boolean equalsTo(MathElement mathElement) {

        // Si es nulo el argumento debe retornar falso, porque se supone que para poder acceder a equalsTo
        // este elemento no puede ser nulo, pero el del argumento sí.
        if (mathElement == null) return false;

        // Si no es una Función, automáticamente debe retornar, ya que estamos comparando expresiones
        if (!(mathElement instanceof Serie serie)) return false;

        // Si los argumentos son otros elementos...
        return content.equalsTo(serie.content);

    }

}
