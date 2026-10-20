package barnesinside.mathutils.cas.ode;

import barnesinside.mathutils.cas.expr.Algebra;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.expr.Relation;
import barnesinside.mathutils.cas.ode.OdeSupport.Rec;
import barnesinside.mathutils.cas.steps.Rule;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static barnesinside.mathutils.cas.ode.OdeSupport.C;
import static barnesinside.mathutils.cas.ode.OdeSupport.eq;
import static barnesinside.mathutils.cas.ode.OdeSupport.polish;
import static barnesinside.mathutils.cas.ode.OdeSupport.primed;

/**
 * Los métodos de primer orden, {@code y' = f(x, y)}: cada uno devuelve la solución general como igualdad (explícita {@code y = …} si se
 * pudo despejar {@code y}; si no, implícita) y deja sus pasos en el registro, o devuelve vacío si no sirve para la ecuación.
 */
final class FirstOrder {

    private FirstOrder() {
    }

    // ================================================================ Separables

    /** Hay {@code g(x)} y {@code h(y)} con {@code f = g·h}, o vacío. */
    static Optional<Expr[]> separate(Expr f, String x, String y) {
        List<Expr> free = new java.util.ArrayList<>();
        List<Expr> onlyY = new java.util.ArrayList<>();
        boolean clean = true;
        for (Expr factor : Algebra.factors(f)) {
            if (factor.isFreeOf(y)) free.add(factor);
            else if (factor.isFreeOf(x)) onlyY.add(factor);
            else clean = false;
        }
        if (clean) return Optional.of(new Expr[]{polish(Expr.mul(free)), polish(Expr.mul(onlyY))});

        // Si no se ve a simple vista, se comprueba f(x,y)·f(x0,y0) = f(x,y0)·f(x0,y) y se separa con ese punto
        double[][] points = {{1, 1}, {2, 1}, {1, 2}, {2, 3}, {3, 2}, {-1, 1}, {1, -1}, {-2, 3}, {3, -2}};
        for (double[] p : points) {
            Expr x0 = Expr.num((long) p[0]);
            Expr y0 = Expr.num((long) p[1]);
            Expr f00 = f.substitute(Map.of(x, x0, y, y0));
            double value;
            try {
                value = Eval.value(f00);
            } catch (RuntimeException e) {
                continue;
            }
            if (Double.isNaN(value) || Double.isInfinite(value) || Math.abs(value) < 1e-12) continue;
            Expr g = polish(f.substitute(y, y0));
            Expr h = polish(Expr.div(f.substitute(x, x0), f00));
            if (OdeSupport.same(f, Expr.mul(g, h))) return Optional.of(new Expr[]{g, h});
            return Optional.empty();
        }
        return Optional.empty();
    }

    static Optional<Relation> separable(Rec rec, Expr f, String x, String y) {
        Optional<Expr[]> parts = separate(f, x, y);
        if (parts.isEmpty() || parts.get()[0].isZero() || parts.get()[1].isZero()) return Optional.empty();
        return separableCore(rec, parts.get()[0], parts.get()[1], x, y);
    }

    /** {@code y' = g(x)·h(y)}: separar, integrar los dos lados y, si se puede, despejar {@code y}. */
    private static Optional<Relation> separableCore(Rec rec, Expr g, Expr h, String x, String y) {
        Expr dx = Expr.sym("d" + x);
        Expr dy = Expr.sym("d" + y);
        Expr inverse = polish(Expr.div(Expr.ONE, h));
        rec.rel(Rule.ODE_SEPARATE, eq(Expr.mul(inverse, dy), Expr.mul(g, dx)), "g", g, "h", h, "x", Expr.sym(x), "y", Expr.sym(y));

        Optional<Expr> left = rec.integral(inverse, y);
        Optional<Expr> right = rec.integral(g, x);
        if (left.isEmpty() || right.isEmpty()) return Optional.empty();
        Expr c = Expr.sym(C);
        Expr rhs = polish(Expr.add(right.get(), c));
        rec.rel(Rule.ODE_INTEGRATE_SIDES, eq(left.get(), rhs), "f", inverse, "g", g, "x", Expr.sym(x), "y", Expr.sym(y));

        Expr f = Expr.mul(g, h);
        Optional<Expr> isolated = OdeSupport.isolate(left.get(), rhs, y).map(e -> polish(OdeSupport.absorb(e, C)));
        if (isolated.isPresent() && OdeSupport.satisfies(isolated.get(), f, x, y)) {
            Relation explicit = eq(Expr.sym(y), isolated.get());
            rec.rel(Rule.ODE_ISOLATE, explicit, "y", Expr.sym(y));
            return Optional.of(explicit);
        }
        return Optional.of(eq(left.get(), rhs));
    }

    // ================================================================ Lineales

    static Optional<Relation> linear(Rec rec, Expr f, String x, String y) {
        Expr q = polish(f.substitute(y, Expr.ZERO));
        Expr p = polish(Expr.neg(Expr.sub(f.substitute(y, Expr.ONE), q)));
        Expr yy = Expr.sym(y);
        if (!OdeSupport.same(f, Expr.sub(q, Expr.mul(p, yy)))) return Optional.empty();
        return linearCore(rec, p, q, x, y);
    }

    /** {@code y' + P·y = Q}: factor integrante {@code μ = e^∫P}, {@code (μy)' = μQ}, integrar y despejar. */
    private static Optional<Relation> linearCore(Rec rec, Expr p, Expr q, String x, String y) {
        Expr yy = Expr.sym(y);
        Expr yp = primed(y, 1);
        Expr form = Expr.add(yp, Expr.mul(p, yy));
        rec.rel(Rule.ODE_LINEAR_FORM, eq(form, q), "P", p, "Q", q, "x", Expr.sym(x), "y", yy);

        Expr integralP = Expr.ZERO;
        if (!p.isZero()) {
            Optional<Expr> ip = rec.integral(p, x);
            if (ip.isEmpty()) return Optional.empty();
            integralP = ip.get();
        }
        Expr mu = OdeSupport.exponential(integralP);
        rec.rel(Rule.ODE_INTEGRATING_FACTOR, eq(Expr.sym("μ"), mu), "P", p, "mu", mu, "x", Expr.sym(x));
        rec.rel(Rule.ODE_MULTIPLY_FACTOR, eq(Expr.add(Expr.mul(mu, yp), Expr.mul(polish(Expr.mul(mu, p)), yy)), polish(Expr.mul(mu, q))),
                "mu", mu, "x", Expr.sym(x), "y", yy);

        Expr integralQ = Expr.ZERO;
        Expr muQ = polish(Expr.mul(mu, q));
        if (!muQ.isZero()) {
            Optional<Expr> iq = rec.integral(muQ, x);
            if (iq.isEmpty()) return Optional.empty();
            integralQ = iq.get();
        }
        Expr c = Expr.sym(C);
        rec.rel(Rule.ODE_LINEAR_INTEGRATE, eq(Expr.mul(mu, yy), polish(Expr.add(integralQ, c))), "f", muQ, "x", Expr.sym(x));

        Expr solution = polish(Expr.div(Expr.add(integralQ, c), mu));
        Expr f = Expr.sub(q, Expr.mul(p, yy));
        if (!OdeSupport.satisfies(solution, f, x, y)) return Optional.empty();
        Relation result = eq(yy, solution);
        rec.rel(Rule.ODE_ISOLATE, result, "y", yy);
        return Optional.of(result);
    }

    // ================================================================ Homogéneas

    static Optional<Relation> homogeneous(Rec rec, Expr f, String x, String y) {
        String v = f.variables().contains("v") ? "w" : "v";
        Expr t = Expr.sym("t");
        Expr xx = Expr.sym(x);
        Expr yy = Expr.sym(y);
        Expr vv = Expr.sym(v);
        if (f.variables().contains("t") || f.isFreeOf(y)) return Optional.empty();
        Map<String, Expr> scale = new LinkedHashMap<>();
        scale.put(x, Expr.mul(t, xx));
        scale.put(y, Expr.mul(t, yy));
        if (!OdeSupport.same(f.substitute(scale), f)) return Optional.empty();
        Expr fv = polish(f.substitute(Map.of(x, Expr.ONE, y, vv)));
        if (!fv.isFreeOf(x)) return Optional.empty();

        rec.rel(Rule.ODE_HOMOGENEOUS_TEST, eq(primed(y, 1), fv), "v", vv, "f", f, "x", xx, "y", yy);
        Expr h = polish(Expr.sub(fv, vv));
        rec.rel(Rule.ODE_HOMOGENEOUS_SUBSTITUTION, eq(Expr.mul(xx, primed(v, 1)), h), "v", vv, "x", xx, "y", yy);
        if (h.isZero()) return Optional.empty();

        Optional<Relation> inV = separableCore(rec, polish(Expr.div(Expr.ONE, xx)), h, x, v);
        if (inV.isEmpty()) return Optional.empty();
        Expr ratio = Expr.div(yy, xx);
        Relation r = inV.get();
        Relation result;
        if (r.left().equals(vv)) {
            Expr solution = polish(Expr.mul(xx, r.right()));
            if (!OdeSupport.satisfies(solution, f, x, y)) return Optional.empty();
            result = eq(yy, solution);
        } else {
            Expr left = polish(r.left().substitute(v, ratio));
            Optional<Expr> isolated = OdeSupport.isolate(left, r.right(), y).map(e -> polish(OdeSupport.absorb(e, C)));
            result = isolated.isPresent() && OdeSupport.satisfies(isolated.get(), f, x, y) ? eq(yy, isolated.get()) : eq(left, r.right());
        }
        rec.rel(Rule.ODE_HOMOGENEOUS_BACK, result, "v", vv, "x", xx, "y", yy);
        return Optional.of(result);
    }

    // ================================================================ Bernoulli

    static Optional<Relation> bernoulli(Rec rec, Expr f, String x, String y) {
        Map<Rational, Expr> groups = new LinkedHashMap<>();
        for (Expr term : Algebra.terms(Expand.expand(f))) {
            Optional<Map.Entry<Rational, Expr>> power = OdeSupport.powerTerm(term, y);
            if (power.isEmpty()) return Optional.empty();
            groups.merge(power.get().getKey(), power.get().getValue(), (a, b) -> Expr.add(a, b));
        }
        Rational one = Rational.ONE;
        Rational n = null;
        for (Rational k : groups.keySet()) {
            if (k.equals(one)) continue;
            if (n != null) return Optional.empty();
            n = k;
        }
        if (n == null || n.isZero() || n.equals(one)) return Optional.empty();
        Expr q = polish(groups.get(n));
        Expr p = groups.containsKey(one) ? polish(Expr.neg(groups.get(one))) : Expr.ZERO;
        Expr yy = Expr.sym(y);
        Expr nn = Expr.num(n);
        rec.rel(Rule.ODE_BERNOULLI_FORM, eq(Expr.add(primed(y, 1), Expr.mul(p, yy)), Expr.mul(q, Expr.pow(yy, nn))),
                "n", nn, "P", p, "Q", q, "y", yy);

        String z = f.variables().contains("z") ? "u" : "z";
        Expr zz = Expr.sym(z);
        Expr k = Expr.sub(Expr.ONE, nn);
        Expr p2 = polish(Expr.mul(k, p));
        Expr q2 = polish(Expr.mul(k, q));
        rec.rel(Rule.ODE_BERNOULLI_SUBSTITUTION, eq(Expr.add(primed(z, 1), Expr.mul(p2, zz)), q2),
                "n", nn, "z", zz, "y", yy, "k", polish(k));

        Optional<Relation> inZ = linearCore(rec, p2, q2, x, z);
        if (inZ.isEmpty()) return Optional.empty();
        Expr solution = polish(Expr.pow(inZ.get().right(), polish(Expr.div(Expr.ONE, k))));
        if (!OdeSupport.satisfies(solution, f, x, y)) return Optional.empty();
        Relation result = eq(yy, solution);
        rec.rel(Rule.ODE_BERNOULLI_BACK, result, "z", zz, "y", yy, "n", nn);
        return Optional.of(result);
    }

    // ================================================================ Exactas

    static Optional<Relation> exact(Rec rec, Expr f, String x, String y) {
        Algebra.Fraction fraction = Algebra.fraction(f);
        Expr m = polish(Expr.neg(fraction.numerator()));
        Expr n = polish(fraction.denominator());
        if (m.isZero() || n.isZero()) return Optional.empty();
        Expr xx = Expr.sym(x);
        Expr yy = Expr.sym(y);
        rec.rel(Rule.ODE_EXACT_FORM, eq(Expr.add(Expr.mul(m, Expr.sym("d" + x)), Expr.mul(n, Expr.sym("d" + y))), Expr.ZERO),
                "M", m, "N", n);

        Expr my = OdeSupport.diff(m, y);
        Expr nx = OdeSupport.diff(n, x);
        if (!OdeSupport.same(my, nx)) {
            // No es exacta: un factor integrante que dependa de una sola variable la puede volver exacta
            Expr byX = OdeSupport.independentOf(polish(Expr.div(Expr.sub(my, nx), n)), y);
            Expr byY = OdeSupport.independentOf(polish(Expr.div(Expr.sub(nx, my), m)), x);
            Expr mu;
            if (byX.isFreeOf(y)) {
                Optional<Expr> integral = rec.integral(byX, x);
                if (integral.isEmpty()) return Optional.empty();
                mu = OdeSupport.exponential(integral.get());
                rec.rel(Rule.ODE_NOT_EXACT, eq(my, nx), "My", my, "Nx", nx, "h", byX, "v", xx);
            } else if (byY.isFreeOf(x)) {
                Optional<Expr> integral = rec.integral(byY, y);
                if (integral.isEmpty()) return Optional.empty();
                mu = OdeSupport.exponential(integral.get());
                rec.rel(Rule.ODE_NOT_EXACT, eq(my, nx), "My", my, "Nx", nx, "h", byY, "v", yy);
            } else {
                return Optional.empty();
            }
            m = polish(Expr.mul(mu, m));
            n = polish(Expr.mul(mu, n));
            rec.rel(Rule.ODE_EXACT_FACTOR, eq(Expr.add(Expr.mul(m, Expr.sym("d" + x)), Expr.mul(n, Expr.sym("d" + y))), Expr.ZERO),
                    "mu", mu, "M", m, "N", n);
            my = OdeSupport.diff(m, y);
            nx = OdeSupport.diff(n, x);
            if (!OdeSupport.same(my, nx)) return Optional.empty();
        }
        rec.rel(Rule.ODE_EXACT_TEST, eq(my, nx), "M", m, "N", n, "x", xx, "y", yy);

        Optional<Expr> potential = rec.integral(m, x);
        if (potential.isEmpty()) return Optional.empty();
        Expr f0 = potential.get();
        rec.rel(Rule.ODE_EXACT_POTENTIAL, eq(Expr.sym("F"), Expr.add(f0, Expr.sym("g(" + y + ")"))), "F0", f0, "M", m, "x", xx, "y", yy);
        Expr gPrime = polish(Expr.sub(n, OdeSupport.diff(f0, y)));
        if (!gPrime.isFreeOf(x)) return Optional.empty();
        rec.rel(Rule.ODE_EXACT_FIND_G, eq(Expr.sym("g'(" + y + ")"), gPrime), "N", n, "x", xx, "y", yy);
        Expr g = Expr.ZERO;
        if (!gPrime.isZero()) {
            Optional<Expr> integral = rec.integral(gPrime, y);
            if (integral.isEmpty()) return Optional.empty();
            g = integral.get();
        }
        Expr potentialFunction = polish(Expr.add(f0, g));
        Relation implicit = eq(potentialFunction, Expr.sym(C));
        rec.rel(Rule.ODE_EXACT_RESULT, implicit, "x", xx, "y", yy);

        Optional<Expr> isolated = OdeSupport.isolate(potentialFunction, Expr.sym(C), y).map(OdeSupport::polish);
        if (isolated.isPresent() && OdeSupport.satisfies(isolated.get(), f, x, y)) {
            Relation explicit = eq(yy, isolated.get());
            rec.rel(Rule.ODE_ISOLATE, explicit, "y", yy);
            return Optional.of(explicit);
        }
        return Optional.of(implicit);
    }
}
