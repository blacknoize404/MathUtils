package MathUtils.cas.expr;

import java.util.Comparator;
import java.util.List;

/**
 * Orden total y determinista de expresiones, usado para que las sumas y productos
 * tengan una única forma canónica.
 */
final class ExprOrder implements Comparator<Expr> {

    static final ExprOrder INSTANCE = new ExprOrder();

    private ExprOrder() {
    }

    private static int rank(Expr e) {
        return switch (e) {
            case Expr.Num n -> 0;
            case Expr.Const c -> 1;
            case Expr.Sym s -> 2;
            case Expr.Func f -> 3;
            case Expr.Pow p -> 4;
            case Expr.Mul m -> 5;
            case Expr.Add a -> 6;
        };
    }

    @Override
    public int compare(Expr a, Expr b) {
        int byRank = Integer.compare(rank(a), rank(b));
        if (byRank != 0) return byRank;

        return switch (a) {
            case Expr.Num n -> n.value().compareTo(((Expr.Num) b).value());
            case Expr.Const c -> c.kind().compareTo(((Expr.Const) b).kind());
            case Expr.Sym s -> s.name().compareTo(((Expr.Sym) b).name());
            case Expr.Func f -> {
                Expr.Func g = (Expr.Func) b;
                int byFn = f.fn().compareTo(g.fn());
                yield byFn != 0 ? byFn : compare(f.arg(), g.arg());
            }
            case Expr.Pow p -> {
                Expr.Pow q = (Expr.Pow) b;
                int byBase = compare(p.base(), q.base());
                yield byBase != 0 ? byBase : compare(p.exponent(), q.exponent());
            }
            case Expr.Mul m -> compareLists(m.factors(), ((Expr.Mul) b).factors());
            case Expr.Add s -> compareLists(s.terms(), ((Expr.Add) b).terms());
        };
    }

    private int compareLists(List<Expr> x, List<Expr> y) {
        int n = Math.min(x.size(), y.size());
        for (int i = 0; i < n; i++) {
            int c = compare(x.get(i), y.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(x.size(), y.size());
    }
}
