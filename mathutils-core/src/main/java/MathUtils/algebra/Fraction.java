package MathUtils.algebra;

import MathUtils.algebra.primitives.MathElement;

// TODO: 25/3/2024 23:01 Hacer que el numerador y el denominador sean Elementos matemáticos
/**
 * Representa una fracción y las operaciones que se pueden realizar con esta
 * @author Barnés Inside
 * @version 0.2
 */
public class Fraction extends MathElement implements Comparable<Fraction> {

    private int numerator;

    private int denominator;

    /**
     * @throws ArithmeticException si el denominador es 0
     */
    public Fraction(int numerator, int denominator) {
        if (denominator == 0) throw new ArithmeticException("The denominator cannot be 0");
        if (denominator < 0) {
            numerator = Math.negateExact(numerator);
            denominator = Math.negateExact(denominator);
        }
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * Realiza una suma entre la fracción de la instancia y la pasada como argumento
     * @param to fracción a sumar
     * @return una instancia del resultado de la operación
     */
    public Fraction sum(Fraction to) {
        int mcm = getMinimumCommonMultiplier(this.denominator, to.denominator);
        int num = Math.addExact(
                Math.multiplyExact(this.numerator, mcm / this.denominator),
                Math.multiplyExact(to.numerator, mcm / to.denominator));
        this.numerator = num;
        this.denominator = mcm;
        return this;
    }

    /**
     * Realiza una resta entre la fracción de la instancia y la pasada como argumento
     * @param to fracción a sustraer
     * @return una instancia del resultado de la operación
     */
    public Fraction sub(Fraction to) {
        int mcm = getMinimumCommonMultiplier(this.denominator, to.denominator);
        int num = Math.subtractExact(
                Math.multiplyExact(this.numerator, mcm / this.denominator),
                Math.multiplyExact(to.numerator, mcm / to.denominator));
        this.numerator = num;
        this.denominator = mcm;
        return this;
    }

    /**
     * Realiza una multiplicación entre la fracción de la instancia y un entero
     * @param to entero a multiplicar
     * @return una instancia del resultado de la operación
     */
    public Fraction mult(Integer to) {
        this.numerator = Math.multiplyExact(this.numerator, to);
        return this;
    }

    /**
     * Realiza una multiplicación entre la fracción de la instancia y la recibida en el argumento
     * @param to fracción a multiplicar
     * @return una instancia del resultado de la operación
     */
    public Fraction mult(Fraction to) {
        int num = Math.multiplyExact(this.numerator, to.numerator);
        int den = Math.multiplyExact(this.denominator, to.denominator);
        this.numerator = num;
        this.denominator = den;
        return this;
    }

    /**
     * Realiza una división entre la fracción de la instancia y la pasada como argumento
     * @param to fracción a dividir
     * @return una instancia del resultado de la operación
     * @throws ArithmeticException si se divide entre una fracción nula
     */
    public Fraction div(Fraction to) {
        if (to.numerator == 0) throw new ArithmeticException("Cannot divide by a zero fraction");
        int num = Math.multiplyExact(this.numerator, to.denominator);
        int den = Math.multiplyExact(this.denominator, to.numerator);
        if (den < 0) {
            num = Math.negateExact(num);
            den = Math.negateExact(den);
        }
        this.numerator = num;
        this.denominator = den;
        return this;
    }

    /**
     * Simplifica la fracción a través de la obtención del máximo común divisor
     * @return la instancia de la fracción simplificada
     * @throws ArithmeticException en caso de que el denominador sea nulo (0),
     * ya que la división no está definida con un denominador 0
     */
    public Fraction simplify() throws ArithmeticException {
        int gcd = getGreatestCommonDivisor(numerator, denominator);
        numerator /= gcd;
        denominator /= gcd;
        return this;
    }

    /**
     * Calcula el mínimo común múltiplo entre dos números usando la fórmula
     * del máximo común divisor
     * @param a número 1
     * @param b número 2
     * @return el mínimo común múltiplo de dichos números
     */
    private int getMinimumCommonMultiplier(int a, int b) {
        return Math.multiplyExact(a / getGreatestCommonDivisor(a, b), b);
    }

    /**
     * Calcula el mayor común divisor de dos números usando el algoritmo euclidiano
     * @param numerator numerador
     * @param denominator denominador
     * @return el máximo común divisor (siempre no negativo).
     */
    private int getGreatestCommonDivisor(int numerator, int denominator) {
        int a = Math.abs(numerator);
        int b = Math.abs(denominator);
        while (b != 0) {
            int t = a % b;
            a = b;
            b = t;
        }
        return a;
    }

    /**
     * Compara las fracciones usando como referencia su valor en expresión decimal
     * @param o el objeto a ser comparado
     * @return el resultado de la comparación, siendo -1, 0 o 1, cuando la fracción sea menor,
     * igual, o mayor que la del argumento respectivamente.
     */
    @Override
    public int compareTo(Fraction o) {
        return Long.compare((long) numerator * o.denominator, (long) o.numerator * denominator);
    }

    public int getNumerator() {
        return numerator;
    }

    public void setNumerator(int numerator) {
        this.numerator = numerator;
    }

    public int getDenominator() {
        return denominator;
    }

    public void setDenominator(int denominator) throws ArithmeticException {
        if (denominator == 0) throw new ArithmeticException("The denominator cannot be 0");
        if (denominator < 0) {
            this.numerator = Math.negateExact(this.numerator);
            denominator = Math.negateExact(denominator);
        }
        this.denominator = denominator;
    }

    /**
     * Comprueba si es impropia o no. Una fracción se considera impropia cuando su numerador
     * es mayor que su denominador
     * @return si es impropia devuelve true, en caso contrario, false
     */
    public boolean isImproper() {
        return Math.abs((long) numerator) > denominator;
    }

    @Override
    public String toString() {
        return "(" + getNumerator() + "/" + getDenominator() + ')';
    }

    /**
     * Calcula la expresión decimal de la fracción
     * @return su valor en expresión decimal
     */
    @Override
    public double eval(Evaluation... evaluations) {
        return (double) getNumerator() / getDenominator();
    }

    @Override
    public boolean equalsTo(MathElement mathElement) {
        return mathElement instanceof Fraction other && compareTo(other) == 0;
    }

}
