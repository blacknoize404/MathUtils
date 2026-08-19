package barnesinside.mathutils.algebra.logarithmics;

import barnesinside.mathutils.algebra.Const;
import barnesinside.mathutils.algebra.Evaluation;
import barnesinside.mathutils.algebra.primitives.Expression;
import barnesinside.mathutils.algebra.primitives.MathElement;
import barnesinside.mathutils.exceptions.FunctionEvaluationException;

// TODO: 12/12/2023 Testar los límites del logaritmo
public class Log extends Expression<MathElement> {

    protected final MathElement base;

    public Log(Number argument, Number base) {
        super(new Const(argument));
        this.base = new Const(base);
    }

    public Log(MathElement argument, Number base) {
        super(argument);
        this.base = new Const(base);
    }

    public Log(MathElement argument, MathElement base) {
        super(argument);
        this.base = base;
    }

    public MathElement getBase() {
        return base;
    }

    /**
     * Evalúa el logaritmo para una base dada usando las propiedades de cambio de base
     * de logaritmo.
     * @return el resultado de la operación
     */
    @Override
    public double eval(Evaluation... variables) throws FunctionEvaluationException {

        double argumentValue = argument.eval(variables);
        double baseValue = base.eval(variables);

        if (argumentValue <= 0) throw new IllegalArgumentException("El argumento debe ser mayor que 0");
        if (baseValue == 1) throw new IllegalArgumentException("La base tiene que ser distinta de 1");

        return Math.log(argumentValue) / Math.log(baseValue);

    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("Log_");
        sb.append("{").append(base).append("}");
        sb.append("(").append(argument).append(")");
        return sb.toString();
    }
}
