package barnesinside.mathutils.cas.algebra;

import barnesinside.mathutils.cas.diff.Derivatives;
import barnesinside.mathutils.cas.expr.Eval;
import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;
import barnesinside.mathutils.cas.expr.Op;
import barnesinside.mathutils.cas.expr.Rational;
import barnesinside.mathutils.cas.expr.Relation;
import barnesinside.mathutils.cas.expr.SolutionSet;
import barnesinside.mathutils.cas.steps.Trace;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Autovalores y autovectores de una matriz cuadrada de expresiones: el polinomio característico {@code det(A − λI)}, sus raíces reales con su
 * multiplicidad y, para cada una, una base del espacio que le corresponde (el núcleo de {@code A − λI}).
 *
 * @since 0.10
 */
public final class Eigen {

    /** Letra del polinomio característico. */
    public static final String VARIABLE = "λ";

    /** El mayor tamaño que se calcula: el determinante se desarrolla por cofactores. */
    public static final int MAX_SIZE = 6;

    /**
     * Un autovalor.
     *
     * @param value     el autovalor
     * @param algebraic cuántas veces es raíz del polinomio característico
     * @param vectors   base del autoespacio (tantos vectores como su multiplicidad geométrica)
     */
    public record Pair(Expr value, int algebraic, List<List<Expr>> vectors) {
        public int geometric() {
            return vectors.size();
        }
    }

    /**
     * El resultado.
     *
     * @param polynomial el polinomio característico, desarrollado, en {@link #VARIABLE}
     * @param pairs      los autovalores reales, de menor a mayor
     * @param complete   si los autovalores reales encontrados suman el grado del polinomio (si no, hay raíces complejas o sin forma cerrada)
     */
    public record Result(Expr polynomial, List<Pair> pairs, boolean complete) {
        public boolean diagonalizable() {
            return complete && pairs.stream().allMatch(p -> p.geometric() == p.algebraic());
        }
    }

    private Eigen() {
    }

    /** El polinomio característico {@code det(A − λI)}, desarrollado. */
    public static Expr characteristic(List<List<Expr>> a) {
        check(a);
        int n = a.size();
        List<List<Expr>> m = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Expr> row = new ArrayList<>();
            for (int j = 0; j < n; j++) row.add(i == j ? Expr.sub(a.get(i).get(j), Expr.sym(VARIABLE)) : a.get(i).get(j));
            m.add(row);
        }
        return Expand.expand(determinant(m));
    }

    public static Result of(List<List<Expr>> a) {
        Expr polynomial = characteristic(a);
        int degree = a.size();
        Relation zero = new Relation(polynomial, Op.EQ, Expr.ZERO);
        List<Expr> roots = new ArrayList<>();
        if (Equations.solve(zero, VARIABLE).answer() instanceof SolutionSet.Finite finite) roots.addAll(finite.values());
        List<Pair> pairs = new ArrayList<>();
        int found = 0;
        for (Expr root : roots) {
            int multiplicity = multiplicity(polynomial, root);
            found += multiplicity;
            pairs.add(new Pair(root, multiplicity, nullSpace(a, root)));
        }
        pairs.sort(java.util.Comparator.comparingDouble(p -> Eval.value(p.value())));
        return new Result(polynomial, pairs, found == degree);
    }

    private static void check(List<List<Expr>> a) {
        int n = a.size();
        if (n == 0 || a.stream().anyMatch(row -> row.size() != n)) throw new IllegalArgumentException("La matriz tiene que ser cuadrada");
        if (n > MAX_SIZE) throw new IllegalArgumentException("Los autovalores se calculan hasta matrices de " + MAX_SIZE + "×" + MAX_SIZE);
    }

    private static Expr determinant(List<List<Expr>> m) {
        int n = m.size();
        if (n == 1) return m.get(0).get(0);
        if (n == 2) return Expr.sub(Expr.mul(m.get(0).get(0), m.get(1).get(1)), Expr.mul(m.get(0).get(1), m.get(1).get(0)));
        List<Expr> terms = new ArrayList<>();
        for (int j = 0; j < n; j++) {
            List<List<Expr>> minor = new ArrayList<>();
            for (int i = 1; i < n; i++) {
                List<Expr> row = new ArrayList<>(m.get(i));
                row.remove(j);
                minor.add(row);
            }
            Expr term = Expr.mul(m.get(0).get(j), determinant(minor));
            terms.add(j % 2 == 0 ? term : Expr.neg(term));
        }
        return Expand.expand(Expr.add(terms));
    }

    /** Cuántas veces es {@code root} raíz de {@code p}: cuántas derivadas sucesivas se anulan en ella. */
    private static int multiplicity(Expr p, Expr root) {
        int count = 0;
        Expr current = p;
        while (count < 12) {
            double value = Eval.value(current.substitute(VARIABLE, root));
            if (Math.abs(value) > 1e-7) break;
            count++;
            current = Expand.expand(Derivatives.differentiate(current, VARIABLE, new Trace()));
        }
        return Math.max(count, 1);
    }

    private static boolean zero(Expr e) {
        return Math.abs(Eval.value(e)) < 1e-9;
    }

    /** Una base del núcleo de {@code A − λI}, por eliminación de Gauss–Jordan. */
    private static List<List<Expr>> nullSpace(List<List<Expr>> a, Expr lambda) {
        int n = a.size();
        List<List<Expr>> m = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Expr> row = new ArrayList<>();
            for (int j = 0; j < n; j++) row.add(Expand.expand(i == j ? Expr.sub(a.get(i).get(j), lambda) : a.get(i).get(j)));
            m.add(row);
        }
        int[] pivotColumn = new int[n];
        int rank = 0;
        boolean[] isPivot = new boolean[n];
        for (int c = 0; c < n && rank < n; c++) {
            int p = -1;
            for (int r = rank; r < n; r++) {
                if (!zero(m.get(r).get(c))) {
                    p = r;
                    break;
                }
            }
            if (p < 0) continue;
            java.util.Collections.swap(m, rank, p);
            Expr pivot = m.get(rank).get(c);
            for (int j = 0; j < n; j++) m.get(rank).set(j, Expand.expand(Expr.div(m.get(rank).get(j), pivot)));
            for (int r = 0; r < n; r++) {
                if (r == rank || zero(m.get(r).get(c))) continue;
                Expr factor = m.get(r).get(c);
                for (int j = 0; j < n; j++) m.get(r).set(j, Expand.expand(Expr.sub(m.get(r).get(j), Expr.mul(factor, m.get(rank).get(j)))));
            }
            pivotColumn[rank] = c;
            isPivot[c] = true;
            rank++;
        }
        List<List<Expr>> basis = new ArrayList<>();
        for (int f = 0; f < n; f++) {
            if (isPivot[f]) continue;
            List<Expr> v = new ArrayList<>();
            for (int i = 0; i < n; i++) v.add(i == f ? Expr.ONE : Expr.ZERO);
            for (int r = 0; r < rank; r++) v.set(pivotColumn[r], Expand.expand(Expr.neg(m.get(r).get(f))));
            basis.add(cleared(v));
        }
        return basis;
    }

    /** Si todas las componentes son racionales, el vector con componentes enteras sin factor común (y la primera no nula, positiva). */
    private static List<Expr> cleared(List<Expr> v) {
        BigInteger lcm = BigInteger.ONE;
        for (Expr e : v) {
            if (!(e instanceof Expr.Num n)) return v;
            BigInteger d = n.value().denominator();
            lcm = lcm.multiply(d).divide(lcm.gcd(d));
        }
        BigInteger gcd = BigInteger.ZERO;
        List<BigInteger> integers = new ArrayList<>();
        for (Expr e : v) {
            Rational r = ((Expr.Num) e).value();
            BigInteger k = r.numerator().multiply(lcm.divide(r.denominator()));
            integers.add(k);
            gcd = gcd.gcd(k);
        }
        if (gcd.signum() == 0) return v;
        boolean flip = false;
        for (BigInteger k : integers) {
            if (k.signum() != 0) {
                flip = k.signum() < 0;
                break;
            }
        }
        List<Expr> out = new ArrayList<>();
        for (BigInteger k : integers) {
            BigInteger scaled = k.divide(gcd);
            out.add(Expr.num(Rational.of(flip ? scaled.negate() : scaled)));
        }
        return out;
    }
}
