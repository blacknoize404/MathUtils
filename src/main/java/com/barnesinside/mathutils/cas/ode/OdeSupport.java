package com.barnesinside.mathutils.cas.ode;

import com.barnesinside.mathutils.cas.diff.Derivatives;
import com.barnesinside.mathutils.cas.expr.Algebra;
import com.barnesinside.mathutils.cas.expr.Equivalence;
import com.barnesinside.mathutils.cas.expr.Expr;
import com.barnesinside.mathutils.cas.expr.Fn;
import com.barnesinside.mathutils.cas.expr.Polish;
import com.barnesinside.mathutils.cas.expr.Rational;
import com.barnesinside.mathutils.cas.expr.Relation;
import com.barnesinside.mathutils.cas.integral.Integrals;
import com.barnesinside.mathutils.cas.steps.Rule;
import com.barnesinside.mathutils.cas.steps.Solution;
import com.barnesinside.mathutils.cas.steps.Step;
import com.barnesinside.mathutils.cas.steps.Trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** Lo que comparten los métodos: registro de pasos, derivar e integrar con pasos, despejar y comprobar. */
final class OdeSupport {

    /** Nombre de la constante de integración de las ecuaciones de primer orden. */
    static final String C = "C";

    private OdeSupport() {
    }

    // ================================================================ Registro de pasos

    /** Registra los pasos de una resolución. Cada paso con relación lleva {@code l}, {@code op} y {@code r}. */
    static final class Rec {
        private final Trace trace = new Trace(200, 10_000);

        List<Step> steps() {
            return trace.steps();
        }

        Trace trace() {
            return trace;
        }

        void rel(Rule rule, Relation after, Object... extras) {
            trace.leaf(rule, null, null, params(after, extras));
        }

        void note(Rule rule, Object... extras) {
            trace.leaf(rule, null, null, params(null, extras));
        }

        /** Una integral con sus pasos anidados; devuelve su resultado sin constante, o vacío si no se sabe resolver. */
        Optional<Expr> integral(Expr integrand, String variable) {
            Solution s = Integrals.indefinite(integrand, variable);
            if (!s.isSolved()) return Optional.empty();
            Expr result = Polish.best(s.result());
            trace.add(new Step(Rule.ODE_INTEGRAL, integrand, result, Trace.params("f", integrand, "x", Expr.sym(variable)), s.steps()));
            return Optional.of(result);
        }

        private static Map<String, Expr> params(Relation relation, Object... extras) {
            Map<String, Expr> map = new LinkedHashMap<>();
            if (relation != null) map.putAll(relation.toParams());
            if (extras.length % 2 != 0) throw new IllegalArgumentException("Se esperan pares nombre/valor");
            for (int i = 0; i < extras.length; i += 2) map.put((String) extras[i], (Expr) extras[i + 1]);
            return map;
        }
    }

    static Relation eq(Expr left, Expr right) {
        return new Relation(left, com.barnesinside.mathutils.cas.expr.Op.EQ, right);
    }

    // ================================================================ Símbolos

    /** La derivada de orden {@code k} del símbolo: {@code y'}, {@code y''}… */
    static Expr primed(String dependent, int k) {
        return Expr.sym(dependent + "'".repeat(k));
    }

    static Expr sym(String name) {
        return Expr.sym(name);
    }

    // ================================================================ Álgebra

    static Expr map(Expr e, UnaryOperator<Expr> f) {
        Expr rebuilt = switch (e) {
            case Expr.Add a -> Expr.add(a.terms().stream().map(t -> map(t, f)).toList());
            case Expr.Mul m -> Expr.mul(m.factors().stream().map(t -> map(t, f)).toList());
            case Expr.Pow p -> Expr.pow(map(p.base(), f), map(p.exponent(), f));
            case Expr.Func fn -> Expr.func(fn.fn(), map(fn.arg(), f));
            default -> e;
        };
        return f.apply(rebuilt);
    }

    /** Quita los valores absolutos: la constante de integración recoge el signo. */
    static Expr dropAbs(Expr e) {
        return map(e, x -> x instanceof Expr.Func f && f.fn() == Fn.ABS ? f.arg() : x);
    }

    /**
     * {@code e^integral} como factor integrante: sin valores absolutos (la constante recoge el signo) y con los logaritmos deshechos,
     * {@code e^(k·ln u + r) = u^k·e^r}.
     */
    static Expr exponential(Expr integral) {
        List<Expr> factors = new ArrayList<>();
        List<Expr> rest = new ArrayList<>();
        for (Expr t : Algebra.terms(dropAbs(integral))) {
            if (t instanceof Expr.Func f && f.fn() == Fn.LN) {
                factors.add(f.arg());
            } else if (t instanceof Expr.Mul m && m.factors().size() == 2 && m.factors().get(0) instanceof Expr.Num k
                    && m.factors().get(1) instanceof Expr.Func f && f.fn() == Fn.LN) {
                factors.add(Expr.pow(f.arg(), Expr.num(k.value())));
            } else {
                rest.add(t);
            }
        }
        factors.add(Expr.exp(Expr.add(rest)));
        return polish(Expr.mul(factors));
    }

    static boolean isZero(Expr e) {
        return e.isZero() || Equivalence.areEqual(e, Expr.ZERO);
    }

    /**
     * {@code e} sin {@code variable} si en realidad no depende de ella (un cociente que no se simplificó del todo), o {@code e} tal cual.
     * Se comprueba que valga lo mismo con la variable sustituida por dos números distintos.
     */
    static Expr independentOf(Expr e, String variable) {
        if (e.isFreeOf(variable)) return e;
        try {
            Expr first = e.substitute(variable, Expr.num(3));
            Expr second = e.substitute(variable, Expr.num(-2));
            if (same(e, first) && same(e, second)) return polish(first);
        } catch (ArithmeticException ex) {
            // un punto de la sustitución es singular: no se puede decir
        }
        return e;
    }

    static boolean same(Expr a, Expr b) {
        return Equivalence.areEqual(a, b);
    }

    static boolean different(Expr a, Expr b) {
        return Equivalence.check(a, b) == Equivalence.Result.DIFFERENT;
    }

    static Expr diff(Expr f, String variable) {
        return Polish.best(Derivatives.differentiate(f, variable, new Trace()));
    }

    static Expr polish(Expr e) {
        return Polish.best(e);
    }

    /** {@code e^(R + C)} es {@code C·e^R}: la constante nueva recoge el factor positivo y el signo. */
    static Expr absorb(Expr e, String constant) {
        Expr c = Expr.sym(constant);
        return map(e, x -> {
            if (x instanceof Expr.Pow p && p.base().equals(Expr.E)) {
                if (p.exponent().equals(c)) return c;
                if (p.exponent() instanceof Expr.Add a && a.terms().contains(c)) {
                    List<Expr> rest = new ArrayList<>(a.terms());
                    rest.remove(c);
                    return Expr.mul(c, Expr.exp(Expr.add(rest)));
                }
            }
            return x;
        });
    }

    /** Un solo factor de {@code e} depende de {@code y}: devuelve el resto de factores, o vacío. */
    private static Optional<Expr> restOfFactors(Expr.Mul m, String y, Expr[] dependent) {
        List<Expr> free = new ArrayList<>();
        Expr found = null;
        for (Expr f : m.factors()) {
            if (f.isFreeOf(y)) free.add(f);
            else if (found == null) found = f;
            else return Optional.empty();
        }
        if (found == null) return Optional.empty();
        dependent[0] = found;
        return Optional.of(Expr.mul(free));
    }

    /**
     * Despeja {@code y} de {@code l = r}, deshaciendo una a una las operaciones que le hacen a {@code y} (sumas, productos, potencias de
     * exponente racional, logaritmo, exponencial y funciones trigonométricas). Vacío si alguna no se puede deshacer.
     */
    static Optional<Expr> isolate(Expr l, Expr r, String y) {
        if (l instanceof Expr.Sym s && s.name().equals(y)) return Optional.of(r);
        if (l.isFreeOf(y)) return Optional.empty();
        switch (l) {
            case Expr.Add a -> {
                List<Expr> free = new ArrayList<>();
                Expr dependent = null;
                for (Expr t : a.terms()) {
                    if (t.isFreeOf(y)) free.add(t);
                    else if (dependent == null) dependent = t;
                    else return Optional.empty();
                }
                if (dependent == null) return Optional.empty();
                return isolate(dependent, Expr.sub(r, Expr.add(free)), y);
            }
            case Expr.Mul m -> {
                Expr[] dependent = new Expr[1];
                Optional<Expr> rest = restOfFactors(m, y, dependent);
                if (rest.isEmpty()) return Optional.empty();
                return isolate(dependent[0], Expr.div(r, rest.get()), y);
            }
            case Expr.Pow p -> {
                if (p.base().equals(Expr.E) && !p.exponent().isFreeOf(y)) return isolate(p.exponent(), Expr.ln(r), y);
                if (p.base().isFreeOf(y)) return Optional.empty();
                if (!(p.exponent() instanceof Expr.Num n) || n.value().isZero()) return Optional.empty();
                Rational k = n.value();
                // Una raíz de índice par tiene dos ramas: no se deshace
                if (k.numerator().testBit(0) == false) return Optional.empty();
                return isolate(p.base(), Expr.pow(r, Expr.num(k.reciprocal())), y);
            }
            case Expr.Func f -> {
                Expr inverse = switch (f.fn()) {
                    case LN -> Expr.exp(r);
                    case ABS -> r;
                    case ATAN -> Expr.func(Fn.TAN, r);
                    case TAN -> Expr.func(Fn.ATAN, r);
                    case SIN -> Expr.func(Fn.ASIN, r);
                    case COS -> Expr.func(Fn.ACOS, r);
                    default -> null;
                };
                return inverse == null ? Optional.empty() : isolate(f.arg(), inverse, y);
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    /** Comprueba que {@code y = solution} cumple {@code y' = f(x, y)}: nunca se da por buena una solución que no se verifica. */
    static boolean satisfies(Expr solution, Expr f, String x, String y) {
        Expr derivative = Derivatives.differentiate(solution, x, new Trace());
        return !different(derivative, f.substitute(y, solution));
    }

    /** {@code e} como {@code coeficiente·y^k} (con el coeficiente libre de {@code y}), o vacío. */
    static Optional<Map.Entry<Rational, Expr>> powerTerm(Expr e, String y) {
        Rational exponent = Rational.ZERO;
        List<Expr> coefficient = new ArrayList<>();
        List<Expr> factors = e instanceof Expr.Mul m ? m.factors() : List.of(e);
        for (Expr f : factors) {
            if (f.isFreeOf(y)) coefficient.add(f);
            else if (f instanceof Expr.Sym s && s.name().equals(y)) exponent = exponent.add(Rational.ONE);
            else if (f instanceof Expr.Pow p && p.base() instanceof Expr.Sym s && s.name().equals(y) && p.exponent() instanceof Expr.Num n) {
                exponent = exponent.add(n.value());
            } else return Optional.empty();
        }
        return Optional.of(Map.entry(exponent, Expr.mul(coefficient)));
    }
}
