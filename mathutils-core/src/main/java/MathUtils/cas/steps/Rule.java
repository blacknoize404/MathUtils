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
    DIFF_ORDER(Category.DERIVATIVE),

    // ---- Límites
    LIMIT_CONSTANT(Category.LIMIT),
    LIMIT_DIRECT(Category.LIMIT),
    LIMIT_ALGEBRA(Category.LIMIT),
    LIMIT_INFINITY_RATIONAL(Category.LIMIT),
    LIMIT_LHOPITAL_ZERO(Category.LIMIT),
    LIMIT_LHOPITAL_INFINITY(Category.LIMIT),
    LIMIT_PRODUCT_TO_QUOTIENT(Category.LIMIT),
    LIMIT_COMBINE_FRACTIONS(Category.LIMIT),
    LIMIT_CONJUGATE(Category.LIMIT),
    LIMIT_EXPONENTIAL_FORM(Category.LIMIT),
    LIMIT_SIDES(Category.LIMIT),
    LIMIT_OSCILLATES(Category.LIMIT),
    LIMIT_DIFFERENT_SIDES(Category.LIMIT),
    LIMIT_DOMINANT_TERMS(Category.LIMIT),
    LIMIT_ABSOLUTE_VALUE(Category.LIMIT),

    // ---- Integrales
    INT_CONSTANT(Category.INTEGRAL),
    INT_CONSTANT_MULTIPLE(Category.INTEGRAL),
    INT_SUM(Category.INTEGRAL),
    INT_POWER(Category.INTEGRAL),
    INT_RECIPROCAL(Category.INTEGRAL),
    INT_EXPONENTIAL_E(Category.INTEGRAL),
    INT_EXPONENTIAL(Category.INTEGRAL),
    INT_SIN(Category.INTEGRAL),
    INT_COS(Category.INTEGRAL),
    INT_TAN(Category.INTEGRAL),
    INT_COT(Category.INTEGRAL),
    INT_SEC(Category.INTEGRAL),
    INT_CSC(Category.INTEGRAL),
    INT_SEC_SQUARED(Category.INTEGRAL),
    INT_CSC_SQUARED(Category.INTEGRAL),
    INT_SEC_TAN(Category.INTEGRAL),
    INT_CSC_COT(Category.INTEGRAL),
    INT_SINH(Category.INTEGRAL),
    INT_COSH(Category.INTEGRAL),
    INT_TANH(Category.INTEGRAL),
    INT_ATAN_FORM(Category.INTEGRAL),
    INT_ASIN_FORM(Category.INTEGRAL),
    INT_LOG_FORM(Category.INTEGRAL),
    INT_ASINH_FORM(Category.INTEGRAL),
    INT_EXPAND(Category.INTEGRAL),
    INT_POLYNOMIAL_DIVISION(Category.INTEGRAL),
    INT_SUBSTITUTION(Category.INTEGRAL),
    INT_PARTS(Category.INTEGRAL),
    INT_CYCLE(Category.INTEGRAL),
    INT_PARTIAL_FRACTIONS(Category.INTEGRAL),
    INT_QUADRATIC_SPLIT(Category.INTEGRAL),
    INT_COMPLETE_SQUARE(Category.INTEGRAL),
    INT_REDUCTION(Category.INTEGRAL);

    /**
     * Familia de la regla; el renderizador la usa para mostrar la operación
     * ({@code d/dx[...]} para derivadas, {@code ∫ ... dx} para integrales, {@code lim} para límites).
     */
    public enum Category {GENERAL, DERIVATIVE, LIMIT, INTEGRAL}

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
