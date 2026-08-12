package MathUtils.cas.steps;

import MathUtils.cas.expr.Expr;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Acumula los pasos de una resolución como un árbol. Los motores de derivación e integración
 * registran aquí cada regla que aplican; el mismo objeto controla los límites de profundidad
 * y de tiempo para que ninguna resolución se cuelgue.
 */
public final class Trace {

    /** Se lanza cuando se supera la profundidad o el tiempo permitidos. */
    public static final class BudgetExceededException extends RuntimeException {
        public BudgetExceededException(String message) {
            super(message);
        }
    }

    private final int maxDepth;
    private final long deadlineNanos;
    private final Deque<List<Step>> frames = new ArrayDeque<>();

    /**
     * @param maxDepth profundidad máxima de anidamiento de pasos
     * @param timeBudgetMillis tiempo máximo en milisegundos (≤ 0 para no limitar)
     */
    public Trace(int maxDepth, long timeBudgetMillis) {
        this.maxDepth = maxDepth;
        this.deadlineNanos = timeBudgetMillis > 0 ? System.nanoTime() + timeBudgetMillis * 1_000_000 : Long.MAX_VALUE;
        frames.push(new ArrayList<>());
    }

    /** Traza con límites generosos: 200 niveles y 10 segundos. */
    public Trace() {
        this(200, 10_000);
    }

    /**
     * Ejecuta {@code body} dentro de un paso: lo que el cuerpo registre queda como subpasos y
     * su resultado es la expresión {@code after} del paso.
     *
     * @return el resultado del cuerpo
     * @throws BudgetExceededException si se supera la profundidad o el tiempo
     */
    public Expr step(Rule rule, Expr before, Map<String, Expr> params, Supplier<Expr> body) {
        checkBudget();
        if (frames.size() > maxDepth) throw new BudgetExceededException("Se superó la profundidad máxima de " + maxDepth);

        frames.push(new ArrayList<>());
        Expr after;
        List<Step> children;
        try {
            after = body.get();
        } finally {
            children = frames.pop();
        }
        frames.peek().add(new Step(rule, before, after, params, children));
        return after;
    }

    /**
     * Como {@link #step}, pero el cuerpo puede fallar devolviendo vacío: en ese caso el intento
     * y todo lo que registró se descartan y no queda rastro en los pasos.
     *
     * @return el resultado del cuerpo, o vacío si el intento no funcionó
     * @throws BudgetExceededException si se supera la profundidad o el tiempo
     */
    public java.util.Optional<Expr> attempt(Rule rule, Expr before, Map<String, Expr> params,
                                            Supplier<java.util.Optional<Expr>> body) {
        checkBudget();
        if (frames.size() > maxDepth) throw new BudgetExceededException("Se superó la profundidad máxima de " + maxDepth);

        frames.push(new ArrayList<>());
        java.util.Optional<Expr> result;
        List<Step> children;
        try {
            result = body.get();
        } finally {
            children = frames.pop();
        }
        result.ifPresent(after -> frames.peek().add(new Step(rule, before, after, params, children)));
        return result;
    }

    /** Registra un paso sin subpasos y devuelve su resultado. */
    public Expr leaf(Rule rule, Expr before, Expr after, Map<String, Expr> params) {
        checkBudget();
        frames.peek().add(new Step(rule, before, after, params, List.of()));
        return after;
    }

    /** Lanza {@link BudgetExceededException} si se agotó el tiempo. */
    public void checkBudget() {
        if (System.nanoTime() > deadlineNanos) throw new BudgetExceededException("Se agotó el tiempo disponible");
    }

    /** Pasos registrados hasta ahora en el nivel superior. */
    public List<Step> steps() {
        return List.copyOf(frames.peekLast());
    }

    /**
     * Construye un mapa de parámetros ordenado a partir de pares nombre/valor:
     * {@code params("x", x, "u", u)}.
     */
    public static Map<String, Expr> params(Object... namesAndValues) {
        if (namesAndValues.length % 2 != 0) throw new IllegalArgumentException("Se esperan pares nombre/valor");
        Map<String, Expr> map = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            map.put((String) namesAndValues[i], (Expr) namesAndValues[i + 1]);
        }
        return map;
    }
}
