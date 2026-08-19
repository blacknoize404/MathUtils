package barnesinside.mathutils.cas.steps;

import barnesinside.mathutils.cas.expr.Expr;

/**
 * Fragmento de una explicación: texto corrido o una expresión matemática intercalada.
 * Permite que cada interfaz decida cómo pintar las fórmulas (texto, LaTeX o un componente gráfico).
 */
public sealed interface Segment permits Segment.Text, Segment.Math {

    /** Texto corrido. */
    record Text(String value) implements Segment {
    }

    /** Expresión matemática intercalada en el texto. */
    record Math(Expr expr) implements Segment {
    }
}
