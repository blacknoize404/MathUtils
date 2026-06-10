package MathUtils.algebra;

import MathUtils.exceptions.FunctionEvaluationException;
import MathUtils.algebra.primitives.Expression;

import java.util.Arrays;
import java.util.Optional;

/**
 * Representa el concepto de variable
 */
public final class Var extends Expression<String> {

    public Var(String argument) {
        super(argument);
    }

    @Override
    public double eval(Evaluation... evaluations) throws FunctionEvaluationException {

        // Buscar si la variable existe
        Optional<Evaluation> varValue = Arrays
                .stream(evaluations)
                .filter(doubleVarValue -> doubleVarValue.var().equalsIgnoreCase(argument))
                .findFirst();

        // Emite la excepción si no encontró la variable
        if (varValue.isEmpty()) {
            throw new FunctionEvaluationException("There is no evaluation registered for " + argument + ".");
        }

        // Sustituye la variable por el valor
        return varValue.get().value().doubleValue();
    }

}
