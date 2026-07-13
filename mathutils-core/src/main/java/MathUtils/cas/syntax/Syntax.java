package MathUtils.cas.syntax;

import MathUtils.cas.expr.Rational;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.List;

/**
 * Árbol sintáctico de una expresión tal como se escribió: sin normalizar, conservando el orden
 * de los operandos, los paréntesis y las operaciones numéricas pendientes
 * ({@code 2 + 3*4} no se pliega a {@code 14}). Es la base del árbol visual y del orden de operaciones.
 */
public sealed interface Syntax permits Syntax.Num, Syntax.Var, Syntax.Const, Syntax.Neg, Syntax.Bin, Syntax.Call, Syntax.Group {

    /**
     * Número. {@code exact} es su valor racional exacto, o {@code null} si es una aproximación
     * (por ejemplo, {@code sin(1)}); {@code approx} siempre contiene el valor decimal.
     *
     * @param text texto con el que se muestra el número
     */
    record Num(String text, Rational exact, double approx) implements Syntax {

        /** Número exacto; se muestra como entero, decimal finito o fracción. */
        public static Num of(Rational value) {
            return new Num(format(value), value, value.doubleValue());
        }

        /** Número aproximado, mostrado con 10 cifras significativas. */
        public static Num approximate(double value) {
            return new Num(format(value), null, value);
        }

        public boolean isExact() {
            return exact != null;
        }

        /** Un número negativo o una fracción necesita paréntesis cuando es operando de otra operación. */
        public boolean needsParentheses() {
            return text.startsWith("-") || text.contains("/");
        }

        private static String format(Rational r) {
            if (r.isInteger()) return r.toString();
            BigInteger den = r.denominator();
            while (den.mod(BigInteger.TWO).signum() == 0) den = den.divide(BigInteger.TWO);
            while (den.mod(BigInteger.valueOf(5)).signum() == 0) den = den.divide(BigInteger.valueOf(5));
            if (!den.equals(BigInteger.ONE)) return r.toString();
            return new BigDecimal(r.numerator()).divide(new BigDecimal(r.denominator())).stripTrailingZeros().toPlainString();
        }

        private static String format(double d) {
            if (Double.isNaN(d)) return "NaN";
            if (Double.isInfinite(d)) return d > 0 ? "oo" : "-oo";
            BigDecimal rounded = new BigDecimal(d).round(new MathContext(10)).stripTrailingZeros();
            double abs = Math.abs(d);
            return abs != 0 && (abs >= 1e9 || abs < 1e-6) ? rounded.toString().replace("E+", "E") : rounded.toPlainString();
        }
    }

    /** Variable. */
    record Var(String name) implements Syntax {
    }

    /** Constante con nombre ({@code pi} o {@code e}). */
    record Const(String name, double value) implements Syntax {
    }

    /** Signo menos unario. */
    record Neg(Syntax operand) implements Syntax {
    }

    /** Operación binaria: uno de {@code + - * / ^}. */
    record Bin(char op, Syntax left, Syntax right) implements Syntax {
    }

    /** Llamada a función. */
    record Call(String name, List<Syntax> args) implements Syntax {
        public Call {
            args = List.copyOf(args);
        }
    }

    /** Operando que estaba entre paréntesis en el texto original. */
    record Group(Syntax inner) implements Syntax {
    }
}
