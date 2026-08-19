package barnesinside.mathutils.algebra.exponentials;

import barnesinside.mathutils.algebra.Const;
import barnesinside.mathutils.algebra.Evaluation;
import barnesinside.mathutils.algebra.operators.DivOperator;
import barnesinside.mathutils.algebra.primitives.Expression;
import barnesinside.mathutils.algebra.primitives.MathElement;
import barnesinside.mathutils.exceptions.FunctionEvaluationException;

public class Root extends Expression<MathElement> {

    protected final MathElement root;

    public Root(MathElement argument, Number rootValue) {
        super(argument);
        this.root = new Const(rootValue);
    }

    public Root(Number argument, MathElement rootValue) {
        super(new Const(argument));
        this.root = rootValue;
    }

    public Root(Number argument, Number rootValue) {
        super(new Const(argument));
        this.root = new Const(rootValue);
    }

    public Root(MathElement argument, MathElement rootValue) {
        super(argument);
        this.root = rootValue;
    }

    @Override
    public double eval(Evaluation... variables) throws FunctionEvaluationException {
        double arg = argument.eval(variables);
        double index = root.eval(variables);
        if (index == 0) throw new ArithmeticException("El índice de la raíz no puede ser 0");
        double exp = 1.0 / index;
        if (arg < 0) {
            // Solo existe raíz real de un negativo cuando el índice es un entero impar
            boolean oddIndex = index % 1 == 0 && Math.abs(index % 2) == 1;
            if (!oddIndex) throw new ArithmeticException("No existe raíz real de un número negativo con índice " + index);
            return -Math.pow(-arg, exp);
        }
        return Math.pow(arg, exp);
    }

    public MathElement getRoot() {
        return root;
    }

    public Pow toPow() {
        return new Pow(argument, new DivOperator(1, root));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        return sb.append(argument.toString())
                .append(")^")
                .append("{1/")
                .append(root.toString())
                .append("}")
                .toString();
    }
}
