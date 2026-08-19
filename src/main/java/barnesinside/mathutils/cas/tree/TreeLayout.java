package barnesinside.mathutils.cas.tree;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Disposición geométrica de un {@link TreeModel}, independiente de la interfaz.
 * <p>Las hojas ocupan posiciones consecutivas en el eje horizontal y cada nodo interno se
 * centra sobre sus hijos, de modo que ningún subárbol se solapa con otro. Las coordenadas están
 * en unidades de nodo: {@code x} en pasos de una hoja y {@code y} en niveles de profundidad.
 */
public final class TreeLayout {

    /** Posición de un nodo. */
    public record Position(double x, double y) {
    }

    private TreeLayout() {
    }

    /**
     * @return la posición de cada nodo, por identificador, en orden de preorden
     */
    public static Map<Integer, Position> compute(TreeModel model) {
        Map<Integer, Position> positions = new LinkedHashMap<>();
        place(model.root(), 0, new int[]{0}, positions);
        return positions;
    }

    /** Ancho del dibujo en unidades de hoja (cantidad de hojas, mínimo 1). */
    public static int width(TreeModel model) {
        return (int) model.nodes().stream().filter(TreeNode::isLeaf).count();
    }

    private static double place(TreeNode node, int depth, int[] nextLeaf, Map<Integer, Position> out) {
        double x;
        if (node.isLeaf()) {
            x = nextLeaf[0]++;
            out.put(node.id(), new Position(x, depth));
            return x;
        }

        // Se reserva el lugar en el mapa para conservar el preorden
        out.put(node.id(), null);
        double first = 0;
        double last = 0;
        for (int i = 0; i < node.children().size(); i++) {
            double childX = place(node.children().get(i), depth + 1, nextLeaf, out);
            if (i == 0) first = childX;
            last = childX;
        }
        x = (first + last) / 2;
        out.put(node.id(), new Position(x, depth));
        return x;
    }
}
