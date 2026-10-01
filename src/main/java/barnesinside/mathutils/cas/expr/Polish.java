package barnesinside.mathutils.cas.expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Deja un resultado en su forma más corta: entre la expresión, su desarrollo y su desarrollo con el factor común sacado, elige la
 * de menos nodos que sea equivalente. Si ninguna es más corta, devuelve la misma expresión. Sirve para que
 * {@code -(x²e^(-x) − 2(−xe^(-x) − e^(-x)))} se muestre como {@code -e^(-x)(x² + 2x + 2)}.
 */
public final class Polish {

    private Polish() {
    }

    /** La forma más corta equivalente a {@code e} (la propia {@code e} si no hay una más corta). */
    public static Expr best(Expr e) {
        try {
            List<Expr> candidates = new ArrayList<>();
            Expr expanded = Expand.expand(e);
            candidates.add(expanded);
            candidates.add(commonFactor(expanded));
            candidates.add(commonFactor(e));
            try {
                candidates.add(Algebra.together(e));
            } catch (RuntimeException ignored) {
                // Sin forma común
            }

            Expr best = e;
            int bestSize = size(e);
            for (Expr candidate : candidates) {
                int s = size(candidate);
                if (s < bestSize && Equivalence.areEqual(e, candidate)) {
                    best = candidate;
                    bestSize = s;
                }
            }
            return best;
        } catch (RuntimeException ex) {
            return e;
        }
    }

    /** Cantidad de nodos de la expresión: lo que se compara para decidir qué forma es más corta. */
    static int size(Expr e) {
        return switch (e) {
            case Expr.Num n -> 1;
            case Expr.Sym s -> 1;
            case Expr.Const c -> 1;
            case Expr.Func f -> 1 + size(f.arg());
            case Expr.Pow p -> 1 + size(p.base()) + size(p.exponent());
            case Expr.Add a -> 1 + a.terms().stream().mapToInt(Polish::size).sum();
            case Expr.Mul m -> 1 + m.factors().stream().mapToInt(Polish::size).sum();
        };
    }

    /** Si todos los sumandos comparten factores (e^(-x), x…), los saca: {@code a·f + b·f = f·(a + b)}. */
    static Expr commonFactor(Expr e) {
        if (!(e instanceof Expr.Add sum)) return e;
        List<List<Expr>> terms = new ArrayList<>();
        for (Expr term : sum.terms()) terms.add(term instanceof Expr.Mul m ? new ArrayList<>(m.factors()) : new ArrayList<>(List.of(term)));

        List<Expr> common = new ArrayList<>();
        for (Expr factor : terms.get(0)) {
            if (factor instanceof Expr.Num) continue;
            if (terms.stream().allMatch(t -> t.contains(factor))) common.add(factor);
        }
        if (common.isEmpty()) return e;

        List<Expr> inner = new ArrayList<>();
        for (List<Expr> term : terms) {
            List<Expr> rest = new ArrayList<>(term);
            for (Expr factor : common) rest.remove(factor);
            inner.add(Expr.mul(rest));
        }
        Expr innerSum = Expr.add(inner);
        // Si todos los sumandos de dentro son negativos, el signo sale fuera
        boolean allNegative = inner.stream().allMatch(Polish::isNegative);
        List<Expr> factors = new ArrayList<>(common);
        if (allNegative) {
            factors.add(Expr.MINUS_ONE);
            factors.add(Expr.neg(innerSum));
        } else {
            factors.add(innerSum);
        }
        return Expr.mul(factors);
    }

    private static boolean isNegative(Expr e) {
        return switch (e) {
            case Expr.Num n -> n.value().signum() < 0;
            case Expr.Mul m -> m.factors().get(0) instanceof Expr.Num n && n.value().signum() < 0;
            default -> false;
        };
    }
}
