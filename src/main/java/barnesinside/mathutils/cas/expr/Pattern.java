package barnesinside.mathutils.cas.expr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Búsqueda y reescritura de subexpresiones por patrón.
 * <p>Un patrón es una expresión en la que algunas variables, las <i>comodines</i>, representan cualquier
 * subexpresión. Todas las apariciones de un mismo comodín deben coincidir con la misma subexpresión.
 * Las sumas y los productos se comparan sin importar el orden de sus términos.
 * <p>Los patrones se normalizan como cualquier expresión, así que hay que escribirlos en forma canónica
 * ({@code a + a} se normaliza a {@code 2a}).
 * <p>Si un comodín es uno de los términos de una suma o de los factores de un producto, además de
 * representar un término representa todos los que sobren: {@code a + sin(x)} coincide con
 * {@code y + z + sin(x)} y liga {@code a} a {@code y + z}.
 *
 * <pre>{@code
 * Expr identity = ExprParser.parse("sin(a)^2 + cos(a)^2");
 * Pattern.rewrite(ExprParser.parse("sin(3x)^2 + cos(3x)^2 + y"), identity, Expr.ONE, Set.of("a"));  // 1 + y
 * }</pre>
 */
public final class Pattern {

    /** Una subexpresión que coincide con un patrón y lo que representa cada comodín. */
    public record Match(Expr subexpression, Map<String, Expr> bindings) {
        public Match {
            bindings = Map.copyOf(bindings);
        }
    }

    /** Máximo de términos de una suma o producto en los que se busca un subconjunto. */
    private static final int MAX_SUBSET_ITEMS = 14;

    private Pattern() {
    }

    /**
     * Comprueba si la expresión completa coincide con el patrón.
     *
     * @return lo que representa cada comodín, o vacío si no coincide
     */
    public static Optional<Map<String, Expr>> match(Expr pattern, Expr target, Set<String> wildcards) {
        Map<String, Expr> bindings = new HashMap<>();
        return unify(pattern, target, wildcards, bindings) ? Optional.of(Map.copyOf(bindings)) : Optional.empty();
    }

    /** Todas las subexpresiones (la propia expresión incluida) que coinciden con el patrón, en preorden. */
    public static List<Match> findAll(Expr pattern, Expr target, Set<String> wildcards) {
        List<Match> found = new ArrayList<>();
        collect(pattern, target, wildcards, found);
        return found;
    }

    /**
     * Sustituye cada subexpresión que coincide con el patrón por el reemplazo, con los comodines ya
     * resueltos. Recorre la expresión de arriba abajo y no vuelve a mirar dentro de lo que ya sustituyó.
     * En una suma o un producto con más términos que el patrón, también sustituye un subconjunto de términos
     * y deja intactos los demás.
     */
    public static Expr rewrite(Expr target, Expr pattern, Expr replacement, Set<String> wildcards) {
        Map<String, Expr> bindings = new HashMap<>();
        if (unify(pattern, target, wildcards, bindings)) return replacement.substitute(bindings);

        switch (target) {
            case Expr.Add a when pattern instanceof Expr.Add pa -> {
                Expr subset = rewriteSubset(a.terms(), pa.terms(), true, pattern, replacement, wildcards);
                if (subset != null) return subset;
            }
            case Expr.Mul m when pattern instanceof Expr.Mul pm -> {
                Expr subset = rewriteSubset(m.factors(), pm.factors(), false, pattern, replacement, wildcards);
                if (subset != null) return subset;
            }
            default -> { }
        }

        return switch (target) {
            case Expr.Num n -> n;
            case Expr.Const c -> c;
            case Expr.Sym s -> s;
            case Expr.Func f -> Expr.func(f.fn(), rewrite(f.arg(), pattern, replacement, wildcards));
            case Expr.Pow p -> Expr.pow(rewrite(p.base(), pattern, replacement, wildcards),
                    rewrite(p.exponent(), pattern, replacement, wildcards));
            case Expr.Add a -> Expr.add(rewriteAll(a.terms(), pattern, replacement, wildcards));
            case Expr.Mul m -> Expr.mul(rewriteAll(m.factors(), pattern, replacement, wildcards));
        };
    }

    // ---------------------------------------------------------------- búsqueda

    private static void collect(Expr pattern, Expr target, Set<String> wildcards, List<Match> found) {
        Map<String, Expr> bindings = new HashMap<>();
        if (unify(pattern, target, wildcards, bindings)) found.add(new Match(target, bindings));

        switch (target) {
            case Expr.Func f -> collect(pattern, f.arg(), wildcards, found);
            case Expr.Pow p -> {
                collect(pattern, p.base(), wildcards, found);
                collect(pattern, p.exponent(), wildcards, found);
            }
            case Expr.Add a -> a.terms().forEach(t -> collect(pattern, t, wildcards, found));
            case Expr.Mul m -> m.factors().forEach(t -> collect(pattern, t, wildcards, found));
            default -> { }
        }
    }

    private static List<Expr> rewriteAll(List<Expr> items, Expr pattern, Expr replacement, Set<String> wildcards) {
        List<Expr> out = new ArrayList<>(items.size());
        for (Expr item : items) out.add(rewrite(item, pattern, replacement, wildcards));
        return out;
    }

    /** Busca un subconjunto de los términos que coincida con el patrón y lo sustituye. */
    private static Expr rewriteSubset(List<Expr> items, List<Expr> patternItems, boolean sum,
                                      Expr pattern, Expr replacement, Set<String> wildcards) {
        int k = patternItems.size();
        int n = items.size();
        if (n <= k || n > MAX_SUBSET_ITEMS) return null;

        int[] chosen = new int[k];
        for (int i = 0; i < k; i++) chosen[i] = i;
        while (true) {
            List<Expr> subset = new ArrayList<>(k);
            for (int index : chosen) subset.add(items.get(index));

            Map<String, Expr> bindings = new HashMap<>();
            if (unifyItems(patternItems, subset, sum, wildcards, bindings, false)) {
                List<Expr> result = new ArrayList<>();
                boolean[] taken = new boolean[n];
                for (int index : chosen) taken[index] = true;
                for (int i = 0; i < n; i++) if (!taken[i]) result.add(rewrite(items.get(i), pattern, replacement, wildcards));
                result.add(replacement.substitute(bindings));
                return sum ? Expr.add(result) : Expr.mul(result);
            }

            int i = k - 1;
            while (i >= 0 && chosen[i] == n - k + i) i--;
            if (i < 0) return null;
            chosen[i]++;
            for (int j = i + 1; j < k; j++) chosen[j] = chosen[j - 1] + 1;
        }
    }

    // ---------------------------------------------------------------- unificación

    private static boolean isWildcard(Expr e, Set<String> wildcards) {
        return e instanceof Expr.Sym s && wildcards.contains(s.name());
    }

    private static boolean unify(Expr p, Expr t, Set<String> wildcards, Map<String, Expr> bindings) {
        if (p instanceof Expr.Sym s && wildcards.contains(s.name())) {
            Expr bound = bindings.get(s.name());
            if (bound == null) {
                bindings.put(s.name(), t);
                return true;
            }
            return bound.equals(t);
        }
        return switch (p) {
            case Expr.Num n -> n.equals(t);
            case Expr.Const c -> c.equals(t);
            case Expr.Sym s -> s.equals(t);
            case Expr.Func pf -> t instanceof Expr.Func tf && pf.fn() == tf.fn()
                    && unify(pf.arg(), tf.arg(), wildcards, bindings);
            case Expr.Pow pp -> t instanceof Expr.Pow tp
                    && unify(pp.base(), tp.base(), wildcards, bindings)
                    && unify(pp.exponent(), tp.exponent(), wildcards, bindings);
            case Expr.Add pa -> t instanceof Expr.Add ta && unifyItems(pa.terms(), ta.terms(), true, wildcards, bindings, true);
            case Expr.Mul pm -> t instanceof Expr.Mul tm && unifyItems(pm.factors(), tm.factors(), false, wildcards, bindings, true);
        };
    }

    /** Une los términos de un patrón con los de la expresión, sin importar el orden. */
    private static boolean unifyItems(List<Expr> patternItems, List<Expr> targetItems, boolean sum,
                                      Set<String> wildcards, Map<String, Expr> bindings, boolean absorb) {
        // Primero los términos que no son comodines: fijan más y descartan antes
        List<Expr> fixed = new ArrayList<>();
        List<Expr> loose = new ArrayList<>();
        for (Expr item : patternItems) (isWildcard(item, wildcards) ? loose : fixed).add(item);

        Expr absorber = null;
        if (absorb && targetItems.size() > patternItems.size() && !loose.isEmpty()) absorber = loose.remove(0);
        else if (targetItems.size() != patternItems.size()) return false;

        List<Expr> ordered = new ArrayList<>(fixed);
        ordered.addAll(loose);
        return assign(ordered, 0, targetItems, new boolean[targetItems.size()], absorber, sum, wildcards, bindings);
    }

    private static boolean assign(List<Expr> ordered, int index, List<Expr> targetItems, boolean[] used, Expr absorber,
                                  boolean sum, Set<String> wildcards, Map<String, Expr> bindings) {
        if (index == ordered.size()) {
            List<Expr> rest = new ArrayList<>();
            for (int i = 0; i < used.length; i++) if (!used[i]) rest.add(targetItems.get(i));
            if (absorber == null) return rest.isEmpty();
            if (rest.isEmpty()) return false;
            Expr combined = sum ? Expr.add(rest) : Expr.mul(rest);
            return unify(absorber, combined, wildcards, bindings);
        }
        for (int i = 0; i < used.length; i++) {
            if (used[i]) continue;
            Map<String, Expr> snapshot = new HashMap<>(bindings);
            if (unify(ordered.get(index), targetItems.get(i), wildcards, bindings)) {
                used[i] = true;
                if (assign(ordered, index + 1, targetItems, used, absorber, sum, wildcards, bindings)) return true;
                used[i] = false;
            }
            bindings.clear();
            bindings.putAll(snapshot);
        }
        return false;
    }
}
