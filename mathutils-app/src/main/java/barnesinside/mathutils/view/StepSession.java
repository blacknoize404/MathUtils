package barnesinside.mathutils.view;

import MathUtils.cas.expr.Expr;
import MathUtils.cas.steps.Solution;
import MathUtils.cas.steps.Step;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado compartido entre la lista de pasos y el animador: la solución mostrada, sus pasos en orden
 * de lectura (cada paso seguido de sus subpasos) y cuál es el paso seleccionado. Seleccionar un paso
 * en la lista mueve el animador y moverlo en el animador resalta el paso en la lista.
 */
final class StepSession {

    /**
     * Un paso con su posición en el recorrido.
     *
     * @param index  posición en el orden de lectura, desde 0
     * @param number numeración jerárquica: {@code 1}, {@code 1.1}, {@code 1.2}…
     * @param depth  nivel de anidación, 0 para los pasos principales
     * @param parent índice del paso que contiene a este, o {@code -1}
     */
    record Frame(int index, String number, int depth, int parent, Step step) {

        Expr before() {
            return step.before();
        }

        Expr after() {
            return step.after();
        }

        /** Si el paso transforma una expresión que se puede dibujar como árbol. */
        boolean hasExpressions() {
            return step.before() != null || step.after() != null;
        }
    }

    private final ObjectProperty<Solution> solution = new SimpleObjectProperty<>();
    private final IntegerProperty position = new SimpleIntegerProperty(-1);
    private List<Frame> frames = List.of();

    ObjectProperty<Solution> solutionProperty() {
        return solution;
    }

    IntegerProperty positionProperty() {
        return position;
    }

    List<Frame> frames() {
        return frames;
    }

    int size() {
        return frames.size();
    }

    /** El paso seleccionado, o {@code null} si no hay pasos. */
    Frame current() {
        int p = position.get();
        return p >= 0 && p < frames.size() ? frames.get(p) : null;
    }

    /** Pasos principales (los de nivel 0), en orden. */
    List<Frame> topLevel() {
        return frames.stream().filter(f -> f.depth() == 0).toList();
    }

    /** Carga una solución nueva y selecciona su primer paso. */
    void load(Solution newSolution) {
        List<Frame> list = new ArrayList<>();
        if (newSolution != null) collect(newSolution.steps(), "", 0, -1, list);
        frames = List.copyOf(list);
        solution.set(newSolution);
        position.set(frames.isEmpty() ? -1 : 0);
    }

    void clear() {
        load(null);
    }

    /** Selecciona un paso (se ajusta al rango válido). */
    void go(int index) {
        if (frames.isEmpty()) return;
        position.set(Math.max(0, Math.min(frames.size() - 1, index)));
    }

    private static void collect(List<Step> steps, String prefix, int depth, int parent, List<Frame> out) {
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            String number = prefix.isEmpty() ? String.valueOf(i + 1) : prefix + "." + (i + 1);
            int index = out.size();
            out.add(new Frame(index, number, depth, parent, step));
            collect(step.substeps(), number, depth + 1, index, out);
        }
    }
}
