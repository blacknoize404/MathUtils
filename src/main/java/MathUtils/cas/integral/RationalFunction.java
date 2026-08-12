package MathUtils.cas.integral;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.expr.Rational;
import MathUtils.cas.poly.Polynomial;

/**
 * Cociente de polinomios reducido, con denominador mónico. Permite operar con funciones racionales
 * de forma exacta, por ejemplo al reescribir senos y cosenos con {@code t = tan(x/2)}.
 */
record RationalFunction(Polynomial numerator, Polynomial denominator) {

    static RationalFunction of(Polynomial numerator, Polynomial denominator) {
        if (denominator.isZero()) throw new ArithmeticException("Denominador cero");
        if (numerator.isZero()) return new RationalFunction(Polynomial.ZERO, Polynomial.ONE);
        Polynomial g = numerator.gcd(denominator);
        Polynomial n = g.degree() > 0 ? numerator.divide(g).quotient() : numerator;
        Polynomial d = g.degree() > 0 ? denominator.divide(g).quotient() : denominator;
        Rational k = d.leading().reciprocal();
        return new RationalFunction(n.scale(k), d.scale(k));
    }

    static RationalFunction constant(Rational value) {
        return new RationalFunction(Polynomial.of(value), Polynomial.ONE);
    }

    RationalFunction add(RationalFunction o) {
        return of(numerator.multiply(o.denominator).add(o.numerator.multiply(denominator)),
                denominator.multiply(o.denominator));
    }

    RationalFunction multiply(RationalFunction o) {
        return of(numerator.multiply(o.numerator), denominator.multiply(o.denominator));
    }

    RationalFunction reciprocal() {
        return of(denominator, numerator);
    }

    RationalFunction pow(int exponent) {
        RationalFunction base = exponent < 0 ? reciprocal() : this;
        RationalFunction result = constant(Rational.ONE);
        for (int i = 0; i < Math.abs(exponent); i++) result = result.multiply(base);
        return result;
    }

    Expr toExpr(String variable) {
        return Expr.div(numerator.toExpr(variable), denominator.toExpr(variable));
    }
}
