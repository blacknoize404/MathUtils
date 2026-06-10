package MathUtils.algebra;

import MathUtils.algebra.exponentials.Pow;
import MathUtils.algebra.exponentials.Root;
import MathUtils.algebra.operators.DivOperator;
import MathUtils.algebra.operators.MultOperator;
import MathUtils.algebra.operators.SubOperator;
import MathUtils.algebra.operators.SumOperator;
import MathUtils.algebra.primitives.MathElement;
import MathUtils.algebra.trigonometrics.Cos;
import MathUtils.algebra.trigonometrics.Cot;
import MathUtils.algebra.trigonometrics.Sin;
import MathUtils.algebra.trigonometrics.Tan;
import MathUtils.exceptions.FunctionEvaluationException;

/**
 * Representa una función general, puede ser multivariable.
 * @implNote
 * Recibirá un {@link MathElement} cualesquiera:
 * {@snippet :new Function(new MathElement())}
 * O una constante:
 * {@snippet :new Function(10)}
 * {@snippet :new Function(new Const(10))}
 * También permite el encadenamiento de operaciones, por ejemplo:
 * {@snippet :new Function(new Var("t")).pow(new Const(2)).div(new Sqrt(3))}
 * La evaluación con variables se realiza pasándole como argumento
 * a {@link #eval(Evaluation...)} un arreglo de {@link Evaluation}
 * los cuales contienen la dos atributos:
 * {@snippet :
 *  String var; // Contiene el nombre de la variable;
 *  Number value; // Contiene el valor a evaluar en la función;
 * }
 *
 */
public final class Function extends MathElement {

    private final MathElement content;

    public Function(MathElement content) {
        this.content = content;
    }

    public Function(Number constant) {
        this.content = new Const(constant);
    }

    @Override
    public double eval(Evaluation... evaluations) throws FunctionEvaluationException {
        return content.eval(evaluations);
    }

    @Override
    public boolean equalsTo(MathElement mathElement) {

        // Si es nulo el argumento debe retornar falso, porque se supone que para poder acceder a equalsTo
        // este elemento no puede ser nulo, pero el del argumento sí.
        if (mathElement == null) return false;

        // Si no es una Función, automáticamente debe retornar, ya que estamos comparando expresiones
        if (!(mathElement instanceof Function function)) return false;

        // Si los argumentos son otros elementos...
        return content.equalsTo(function.content);

    }

    public Function sum(MathElement element) {
        return new Function(new SumOperator(content, element));
    }

    public Function sub(MathElement element) {
        return new Function(new SubOperator(content, element));
    }

    public Function mult(MathElement element) {
        return new Function(new MultOperator(content, element));
    }

    public Function div(MathElement element) {
        return new Function(new DivOperator(content, element));
    }

    public Function pow(MathElement exponent) {
        return new Function(new Pow(content, exponent));
    }

    public Function root(MathElement root) {
        return new Function(new Root(content, root));
    }

    public Function sin() {
        return new Function(new Sin(content));
    }

    public Function cos() {
        return new Function(new Cos(content));
    }

    public Function tan() {
        return new Function(new Tan(content));
    }

    public Function cot() {
        return new Function(new Cot(content));
    }

    public MathElement getContent() {
        return content;
    }
}
