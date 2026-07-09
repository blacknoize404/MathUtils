package MathUtils.cas.expr;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Comprobación de equivalencia entre expresiones: primero simbólica (forma canónica y expansión)
 * y, si no basta, numérica en puntos de muestra dentro del dominio común.
 * <p>El resultado numérico es una evidencia fuerte, no una demostración; por eso existe
 * {@link Result#UNKNOWN} para cuando casi no hay puntos de muestra válidos.
 */
public final class Equivalence {

    public enum Result {EQUAL, DIFFERENT, UNKNOWN}

    private static final int TRIALS = 80;
    private static final int REQUIRED_VALID_POINTS = 12;
    private static final long SEED = 20_240_611L;

    private Equivalence() {
    }

    /**
     * Decide si dos expresiones son iguales en todo su dominio común.
     */
    public static Result check(Expr a, Expr b) {
        if (a.equals(b)) return Result.EQUAL;
        if (Expand.expand(a).equals(Expand.expand(b))) return Result.EQUAL;
        return compareNumerically(a, b, false);
    }

    public static boolean areEqual(Expr a, Expr b) {
        return check(a, b) == Result.EQUAL;
    }

    /**
     * Decide si {@code a - b} es constante respecto a la variable (útil para antiderivadas,
     * que están definidas salvo una constante).
     */
    public static boolean differByConstant(Expr a, Expr b, String variable) {
        if (a.equals(b)) return true;
        Expr difference = Expand.expand(Expr.sub(a, b));
        if (difference.isFreeOf(variable)) return true;
        return compareNumerically(a, b, true) == Result.EQUAL;
    }

    private static Result compareNumerically(Expr a, Expr b, boolean upToConstant) {
        Set<String> names = new LinkedHashSet<>(a.variables());
        names.addAll(b.variables());

        Random random = new Random(SEED);
        int valid = 0;
        Double offset = null;

        for (int trial = 0; trial < TRIALS; trial++) {
            Map<String, Double> env = new HashMap<>();
            for (String name : names) env.put(name, sample(random, trial));

            double va = Eval.value(a, env);
            double vb = Eval.value(b, env);
            if (!Double.isFinite(va) || !Double.isFinite(vb)) continue;

            if (upToConstant) {
                double d = va - vb;
                if (offset == null) offset = d;
                else if (!close(d, offset)) return Result.DIFFERENT;
            } else if (!close(va, vb)) {
                return Result.DIFFERENT;
            }
            valid++;
        }
        return valid >= REQUIRED_VALID_POINTS ? Result.EQUAL : Result.UNKNOWN;
    }

    private static boolean close(double x, double y) {
        return Math.abs(x - y) <= 1e-8 * (1 + Math.max(Math.abs(x), Math.abs(y)));
    }

    /** Alterna rangos para cubrir negativos, positivos, valores pequeños y mayores que 1. */
    private static double sample(Random random, int trial) {
        return switch (trial % 4) {
            case 0 -> random.nextDouble() * 6 - 3;
            case 1 -> 0.1 + random.nextDouble() * 4.9;
            case 2 -> random.nextDouble() * 2 - 1;
            default -> 1 + random.nextDouble() * 4;
        };
    }
}
