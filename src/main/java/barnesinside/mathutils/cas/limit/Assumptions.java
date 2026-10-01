package barnesinside.mathutils.cas.limit;

import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expr;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Parámetros que se suponen positivos mientras se resuelve algo (por ejemplo, {@code y > 0} en {@code ∫₀^∞ x·e^(−xy) dx}, que
 * solo converge así). El motor de límites los usa para decidir el signo de lo que los lleva.
 */
public final class Assumptions {

    private static final ThreadLocal<Set<String>> POSITIVE = ThreadLocal.withInitial(Set::of);
    private static final double[] SAMPLES = {1e-3, 0.1, 1, 10, 1e3};

    private Assumptions() {
    }

    /** Ejecuta {@code body} suponiendo positivos los nombres dados. */
    public static <T> T whilePositive(Set<String> names, Supplier<T> body) {
        Set<String> previous = POSITIVE.get();
        POSITIVE.set(names);
        try {
            return body.get();
        } finally {
            POSITIVE.set(previous);
        }
    }

    /**
     * El signo de {@code e} (1 o -1) si sus variables se suponen positivas y el signo es el mismo con valores muy distintos;
     * 0 si no se puede asegurar.
     */
    static int sign(Expr e) {
        Set<String> positive = POSITIVE.get();
        Set<String> names = e.variables();
        if (positive.isEmpty() || names.isEmpty() || !positive.containsAll(names)) return 0;
        int sign = 0;
        try {
            for (double sample : SAMPLES) {
                Map<String, Double> env = new java.util.HashMap<>();
                for (String name : names) env.put(name, sample);
                double v = Eval.value(e, env);
                int s = Double.isNaN(v) || v == 0 ? 0 : v > 0 ? 1 : -1;
                if (s == 0 || (sign != 0 && s != sign)) return 0;
                sign = s;
            }
        } catch (RuntimeException ex) {
            return 0;
        }
        return sign;
    }
}
