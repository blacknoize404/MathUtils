package MathUtils.algebra.exponentials;

import MathUtils.algebra.Const;
import MathUtils.algebra.Evaluation;
import MathUtils.algebra.operators.DivOperator;
import MathUtils.algebra.primitives.Expression;
import MathUtils.algebra.primitives.MathElement;
import MathUtils.exceptions.FunctionEvaluationException;

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
        double exp = 1.0 / root.eval(variables);
        if (arg < 0) {
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
