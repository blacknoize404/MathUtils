package MathUtils.cas.steps;

/**
 * Regla matemática o transformación que justifica un {@link Step}.
 * <p>Cada constante tiene un título y un texto en los recursos {@code messages_es.properties}
 * y {@code messages_en.properties} (claves {@code rule.<NOMBRE>.title} y {@code rule.<NOMBRE>.text}).
 * Un test comprueba que ninguna regla se quede sin traducción.
 */
public enum Rule {

    // ---- Generales
    SIMPLIFY(Category.GENERAL),

    // ---- Derivadas
    DIFF_CONSTANT(Category.DERIVATIVE),
    DIFF_OTHER_VARIABLE(Category.DERIVATIVE),
    DIFF_VARIABLE(Category.DERIVATIVE),
    DIFF_CONSTANT_MULTIPLE(Category.DERIVATIVE),
    DIFF_SUM(Category.DERIVATIVE),
    DIFF_POWER(Category.DERIVATIVE),
    DIFF_PRODUCT(Category.DERIVATIVE),
    DIFF_QUOTIENT(Category.DERIVATIVE),
    DIFF_CHAIN(Category.DERIVATIVE),
    DIFF_EXPONENTIAL_E(Category.DERIVATIVE),
    DIFF_EXPONENTIAL(Category.DERIVATIVE),
    DIFF_LN(Category.DERIVATIVE),
    DIFF_POWER_GENERAL(Category.DERIVATIVE),
    DIFF_SIN(Category.DERIVATIVE),
    DIFF_COS(Category.DERIVATIVE),
    DIFF_TAN(Category.DERIVATIVE),
    DIFF_COT(Category.DERIVATIVE),
    DIFF_SEC(Category.DERIVATIVE),
    DIFF_CSC(Category.DERIVATIVE),
    DIFF_ASIN(Category.DERIVATIVE),
    DIFF_ACOS(Category.DERIVATIVE),
    DIFF_ATAN(Category.DERIVATIVE),
    DIFF_SINH(Category.DERIVATIVE),
    DIFF_COSH(Category.DERIVATIVE),
    DIFF_TANH(Category.DERIVATIVE),
    DIFF_ABS(Category.DERIVATIVE),
    DIFF_ERF(Category.DERIVATIVE),
    DIFF_ORDER(Category.DERIVATIVE);

    /**
     * Familia de la regla; el renderizador la usa para mostrar la operación
     * ({@code d/dx[...]} para derivadas, {@code ∫ ... dx} para integrales).
     */
    public enum Category {GENERAL, DERIVATIVE}

    private final Category category;

    Rule(Category category) {
        this.category = category;
    }

    public Category category() {
        return category;
    }

    /** Clave del título en los recursos de texto. */
    public String titleKey() {
        return "rule." + name() + ".title";
    }

    /** Clave del texto explicativo en los recursos de texto. */
    public String textKey() {
        return "rule." + name() + ".text";
    }
}
