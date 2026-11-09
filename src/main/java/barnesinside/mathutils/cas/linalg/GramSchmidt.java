package barnesinside.mathutils.cas.linalg;

import barnesinside.mathutils.cas.expr.Expand;
import barnesinside.mathutils.cas.expr.Expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * El proceso de Gram–Schmidt: de unos vectores linealmente independientes saca una base ortogonal y otra ortonormal del mismo subespacio. Cada
 * vector pierde sus proyecciones sobre los anteriores ({@code uᵢ = vᵢ − Σ cⱼ uⱼ} con {@code cⱼ = (vᵢ·uⱼ)/(uⱼ·uⱼ)}) y después se divide por su
 * norma. Todo exacto: los coeficientes son racionales y las normas pueden llevar raíces.
 *
 * @since 0.10
 */
public final class GramSchmidt {

    /**
     * El trabajo con un vector.
     *
     * @param index        el lugar del vector (empieza en 0)
     * @param vector       el vector de partida
     * @param coefficients los coeficientes de proyección sobre los vectores ortogonales anteriores (vacío para el primero)
     * @param orthogonal   el vector ortogonal a los anteriores
     * @param normSquared  su norma al cuadrado {@code u·u}
     * @param unit         el vector unitario {@code u / ‖u‖}
     */
    public record Step(int index, List<Expr> vector, List<Expr> coefficients, List<Expr> orthogonal, Expr normSquared, List<Expr> unit) {
        /** El paso dicho con palabras (los vectores se cuentan desde 1). */
        public String describe(Locale locale) {
            String key = coefficients.isEmpty() ? "linalg.gs.first" : "linalg.gs.project";
            return RowReduction.message(locale, key, index + 1, show(orthogonal), normSquared, show(unit), terms(coefficients));
        }

        private static String terms(List<Expr> coefficients) {
            StringBuilder out = new StringBuilder();
            for (int j = 0; j < coefficients.size(); j++) {
                if (j > 0) out.append(" + ");
                out.append('(').append(coefficients.get(j)).append(")·u").append(j + 1);
            }
            return out.toString();
        }
    }

    /**
     * El resultado.
     *
     * @param input los vectores de partida
     * @param steps el trabajo con cada uno, en orden
     */
    public record Result(List<List<Expr>> input, List<Step> steps) {
        /** Los vectores ortogonales {@code u₁, u₂, …}. */
        public List<List<Expr>> orthogonal() {
            return steps.stream().map(Step::orthogonal).toList();
        }

        /** Los vectores unitarios {@code q₁, q₂, …}: una base ortonormal. */
        public List<List<Expr>> orthonormal() {
            return steps.stream().map(Step::unit).toList();
        }
    }

    private GramSchmidt() {
    }

    /**
     * Ortogonaliza los vectores.
     *
     * @throws IllegalArgumentException si no hay vectores, tienen distinto tamaño o son linealmente dependientes (alguno queda en cero)
     */
    public static Result of(List<List<Expr>> vectors) {
        if (vectors.isEmpty()) throw new IllegalArgumentException("No hay vectores");
        int size = vectors.get(0).size();
        for (List<Expr> v : vectors) if (v.size() != size) throw new IllegalArgumentException("Todos los vectores deben tener las mismas componentes");
        List<Step> steps = new ArrayList<>();
        List<List<Expr>> orthogonal = new ArrayList<>();
        for (int i = 0; i < vectors.size(); i++) {
            List<Expr> v = vectors.get(i);
            List<Expr> coefficients = new ArrayList<>();
            List<Expr> u = new ArrayList<>(v);
            for (List<Expr> previous : orthogonal) {
                Expr c = Expand.expand(Expr.div(dot(v, previous), dot(previous, previous)));
                coefficients.add(c);
                for (int k = 0; k < size; k++) u.set(k, Expand.expand(Expr.sub(u.get(k), Expr.mul(c, previous.get(k)))));
            }
            Expr normSquared = dot(u, u);
            if (ExactMatrix.isZero(normSquared)) throw new IllegalArgumentException("Los vectores son linealmente dependientes: el vector " + (i + 1) + " queda en cero");
            Expr norm = Expr.sqrt(normSquared);
            List<Expr> unit = new ArrayList<>();
            for (Expr component : u) unit.add(Expand.expand(Expr.div(component, norm)));
            steps.add(new Step(i, v, coefficients, u, normSquared, unit));
            orthogonal.add(u);
        }
        return new Result(vectors, steps);
    }

    /** El producto escalar de dos vectores. */
    public static Expr dot(List<Expr> a, List<Expr> b) {
        List<Expr> terms = new ArrayList<>();
        for (int k = 0; k < a.size(); k++) terms.add(Expr.mul(a.get(k), b.get(k)));
        return Expand.expand(Expr.add(terms));
    }

    static String show(List<Expr> vector) {
        StringBuilder out = new StringBuilder("(");
        for (int k = 0; k < vector.size(); k++) out.append(k > 0 ? ", " : "").append(vector.get(k));
        return out.append(')').toString();
    }
}
