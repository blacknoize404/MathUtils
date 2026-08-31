package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Relation;
import barnesinside.mathutils.cas.steps.Rule;
import barnesinside.mathutils.cas.steps.Step;
import barnesinside.mathutils.cas.steps.Trace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registra los pasos de una resolución de álgebra sobre un {@link Trace}. Cada paso lleva, si hay relación, el
 * enunciado tras el paso en los parámetros {@code l}, {@code op} y {@code r}, y el resto de parámetros por nombre.
 */
final class Rec {

    private final Trace trace;

    Rec(Trace trace) {
        this.trace = trace;
    }

    /** Un registro que no se conserva: sirve para comprobar si un método se puede aplicar. */
    static Rec scratch() {
        return new Rec(new Trace(60, 3000));
    }

    Trace trace() {
        return trace;
    }

    List<Step> steps() {
        return trace.steps();
    }

    /** Un paso que deja la relación indicada. Los extras son pares nombre/expresión. */
    void rel(Rule rule, Relation after, Object... extras) {
        trace.leaf(rule, null, null, params(after, extras));
    }

    /** Un paso sin relación (una observación). */
    void note(Rule rule, Object... extras) {
        trace.leaf(rule, null, null, params(null, extras));
    }

    /** Un paso cuyo cuerpo registra los subpasos que lo justifican; devuelve lo que devuelva el cuerpo. */
    @SuppressWarnings("unchecked")
    <T> T group(Rule rule, Relation after, Supplier<T> body, Object... extras) {
        Object[] holder = new Object[1];
        trace.step(rule, null, params(after, extras), () -> {
            holder[0] = body.get();
            return null;
        });
        return (T) holder[0];
    }

    private static Map<String, Expr> params(Relation relation, Object... extras) {
        Map<String, Expr> map = new LinkedHashMap<>();
        if (relation != null) map.putAll(relation.toParams());
        if (extras.length % 2 != 0) throw new IllegalArgumentException("Se esperan pares nombre/valor");
        for (int i = 0; i < extras.length; i += 2) map.put((String) extras[i], (Expr) extras[i + 1]);
        return map;
    }
}
