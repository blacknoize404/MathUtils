package barnesinside.mathutils.cas.expr;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Expresión matemática simbólica, inmutable y en forma canónica.
 * <p>Convenciones de la forma canónica:
 * <ul>
 *   <li>la resta es {@code a + (-1)·b} y la división {@code a · b^(-1)}</li>
 *   <li>la raíz cuadrada es {@code x^(1/2)} y la exponencial es {@code e^x}</li>
 *   <li>las sumas y productos son n-arios, aplanados y ordenados de forma determinista</li>
 *   <li>los términos semejantes se combinan y las constantes numéricas se pliegan de forma exacta</li>
 * </ul>
 * Los registros son públicos para poder hacer <i>pattern matching</i> con {@code switch}, pero
 * <b>deben construirse con los métodos estáticos de esta interfaz</b>, que normalizan.
 */
public sealed interface Expr permits Expr.Num, Expr.Const, Expr.Sym, Expr.Add, Expr.Mul, Expr.Pow, Expr.Func {

    /** Número racional exacto. */
    record Num(Rational value) implements Expr {
        @Override
        public String toString() {
            return ExprPrinter.print(this);
        }
    }

    /** Constantes con nombre. */
    record Const(Kind kind) implements Expr {
        public enum Kind {PI, E, INFINITY, EULER_GAMMA}

        @Override
        public String toString() {
            return ExprPrinter.print(this);
        }
    }

    /** Variable o parámetro simbólico. */
    record Sym(String name) implements Expr {
        @Override
        public String toString() {
            return ExprPrinter.print(this);
        }
    }

    /** Suma n-aria. */
    record Add(List<Expr> terms) implements Expr {
        public Add {
            terms = List.copyOf(terms);
        }

        @Override
        public String toString() {
            return ExprPrinter.print(this);
        }
    }

    /** Producto n-ario. */
    record Mul(List<Expr> factors) implements Expr {
        public Mul {
            factors = List.copyOf(factors);
        }

        @Override
        public String toString() {
            return ExprPrinter.print(this);
        }
    }

    /** Potencia {@code base^exponent}. */
    record Pow(Expr base, Expr exponent) implements Expr {
        @Override
        public String toString() {
            return ExprPrinter.print(this);
        }
    }

    /** Función elemental aplicada a un argumento. */
    record Func(Fn fn, Expr arg) implements Expr {
        @Override
        public String toString() {
            return ExprPrinter.print(this);
        }
    }

    // ---------------------------------------------------------------- Constantes

    Expr ZERO = new Num(Rational.ZERO);
    Expr ONE = new Num(Rational.ONE);
    Expr MINUS_ONE = new Num(Rational.MINUS_ONE);
    Expr PI = new Const(Const.Kind.PI);
    Expr E = new Const(Const.Kind.E);
    Expr INFINITY = new Const(Const.Kind.INFINITY);
    /** La constante de Euler–Mascheroni γ ≈ 0,5772. */
    Expr EULER_GAMMA = new Const(Const.Kind.EULER_GAMMA);

    // ---------------------------------------------------------------- Constructores

    static Expr num(long value) {
        return new Num(Rational.of(value));
    }

    static Expr num(long numerator, long denominator) {
        return new Num(Rational.of(numerator, denominator));
    }

    static Expr num(Rational value) {
        return new Num(value);
    }

    static Expr sym(String name) {
        return new Sym(name);
    }

    static Expr add(Expr... terms) {
        return Normalizer.add(Arrays.asList(terms));
    }

    static Expr add(List<Expr> terms) {
        return Normalizer.add(terms);
    }

    static Expr mul(Expr... factors) {
        return Normalizer.mul(Arrays.asList(factors));
    }

    static Expr mul(List<Expr> factors) {
        return Normalizer.mul(factors);
    }

    static Expr pow(Expr base, Expr exponent) {
        return Normalizer.pow(base, exponent);
    }

    static Expr pow(Expr base, long exponent) {
        return Normalizer.pow(base, num(exponent));
    }

    static Expr func(Fn fn, Expr arg) {
        return Normalizer.func(fn, arg);
    }

    static Expr neg(Expr e) {
        return mul(MINUS_ONE, e);
    }

    static Expr sub(Expr a, Expr b) {
        return add(a, neg(b));
    }

    static Expr div(Expr a, Expr b) {
        return mul(a, pow(b, MINUS_ONE));
    }

    static Expr sqrt(Expr e) {
        return pow(e, num(1, 2));
    }

    static Expr exp(Expr e) {
        return pow(E, e);
    }

    static Expr ln(Expr e) {
        return func(Fn.LN, e);
    }

    static Expr sin(Expr e) {
        return func(Fn.SIN, e);
    }

    static Expr cos(Expr e) {
        return func(Fn.COS, e);
    }

    static Expr abs(Expr e) {
        return func(Fn.ABS, e);
    }

    /**
     * Ordena los factores de un producto como se escriben a mano (números, variables,
     * funciones, exponenciales). Es el orden con el que se imprimen los productos.
     */
    static List<Expr> displayOrder(List<Expr> factors) {
        return Display.sortedFactors(factors);
    }

    // ---------------------------------------------------------------- Consultas

    default boolean isZero() {
        return this instanceof Num n && n.value().isZero();
    }

    default boolean isOne() {
        return this instanceof Num n && n.value().isOne();
    }

    default boolean isNum() {
        return this instanceof Num;
    }

    /** Valor racional si la expresión es un número, o {@code null}. */
    default Rational rationalOrNull() {
        return this instanceof Num n ? n.value() : null;
    }

    /** Variables libres de la expresión (en orden de aparición). */
    default Set<String> variables() {
        Set<String> result = new LinkedHashSet<>();
        collectVariables(this, result);
        return result;
    }

    private static void collectVariables(Expr e, Set<String> out) {
        switch (e) {
            case Sym s -> out.add(s.name());
            case Add a -> a.terms().forEach(t -> collectVariables(t, out));
            case Mul m -> m.factors().forEach(t -> collectVariables(t, out));
            case Pow p -> {
                collectVariables(p.base(), out);
                collectVariables(p.exponent(), out);
            }
            case Func f -> collectVariables(f.arg(), out);
            case Num n -> { }
            case Const c -> { }
        }
    }

    /** Indica si la expresión no contiene la variable dada. */
    default boolean isFreeOf(String variable) {
        return !variables().contains(variable);
    }

    /**
     * Sustituye una variable por una expresión, normalizando el resultado.
     */
    default Expr substitute(String variable, Expr replacement) {
        return Substitution.apply(this, Map.of(variable, replacement));
    }

    default Expr substitute(Map<String, Expr> replacements) {
        return Substitution.apply(this, replacements);
    }

    /** Representación en LaTeX. */
    default String toLatex() {
        return ExprLatex.print(this);
    }

    /** Representación en MathML de presentación. */
    default String toMathML() {
        return ExprMathML.print(this);
    }

    /** Representación en ASCIIMath. */
    default String toAsciiMath() {
        return ExprAsciiMath.print(this);
    }

    /** Expande productos y potencias enteras de sumas; ver {@link Expand}. */
    default Expr expand() {
        return Expand.expand(this);
    }

    /** Representación en texto plano, interpretable por {@link ExprParser}. */
    String toString();
}
