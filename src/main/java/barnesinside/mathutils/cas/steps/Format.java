package barnesinside.mathutils.cas.steps;

/**
 * Formato de salida de una resolución.
 */
public enum Format {
    /** Texto plano con sangrías y numeración (1., 1.1, …). */
    PLAIN,
    /** Markdown con listas anidadas y fórmulas entre {@code $…$} y {@code $$…$$}. */
    MARKDOWN,
    /** Fragmento LaTeX con {@code enumerate} anidados. */
    LATEX,
    /** JSON estructurado para que cualquier interfaz construya su propia vista. */
    JSON
}
