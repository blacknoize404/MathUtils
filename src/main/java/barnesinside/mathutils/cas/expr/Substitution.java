package barnesinside.mathutils.cas.expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sustitución de variables por expresiones, renormalizando el resultado.
 */
final class Substitution {

    private Substitution() {
    }

    static Expr apply(Expr e, Map<String, Expr> replacements) {
        return switch (e) {
            case Expr.Sym s -> replacements.getOrDefault(s.name(), s);
            case Expr.Num n -> n;
            case Expr.Const c -> c;
            case Expr.Add a -> Expr.add(mapAll(a.terms(), replacements));
            case Expr.Mul m -> Expr.mul(mapAll(m.factors(), replacements));
            case Expr.Pow p -> Expr.pow(apply(p.base(), replacements), apply(p.exponent(), replacements));
            case Expr.Func f -> Expr.func(f.fn(), apply(f.arg(), replacements));
        };
    }

    private static List<Expr> mapAll(List<Expr> items, Map<String, Expr> replacements) {
        List<Expr> out = new ArrayList<>(items.size());
        for (Expr item : items) out.add(apply(item, replacements));
        return out;
    }
}
