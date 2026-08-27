package barnesinside.mathutils.cas.expr;

/**
 * Operador de una relación entre dos expresiones: igualdad, desigualdad estricta o no estricta y distinto.
 */
public enum Op {
    EQ("=", "="),
    NE("≠", "!="),
    LT("<", "<"),
    LE("≤", "<="),
    GT(">", ">"),
    GE("≥", ">=");

    private final String symbol;
    private final String ascii;

    Op(String symbol, String ascii) {
        this.symbol = symbol;
        this.ascii = ascii;
    }

    /** El símbolo matemático: {@code ≤}. */
    public String symbol() {
        return symbol;
    }

    /** El símbolo que se escribe con el teclado: {@code <=}. */
    public String ascii() {
        return ascii;
    }

    /** Si es una desigualdad de orden ({@code <}, {@code ≤}, {@code >}, {@code ≥}). */
    public boolean isInequality() {
        return this == LT || this == LE || this == GT || this == GE;
    }

    /** Si el extremo queda incluido: {@code ≤}, {@code ≥} y {@code =}. */
    public boolean isInclusive() {
        return this == LE || this == GE || this == EQ;
    }

    /** El operador al intercambiar los dos lados: {@code a < b} es {@code b > a}. */
    public Op swapped() {
        return switch (this) {
            case LT -> GT;
            case LE -> GE;
            case GT -> LT;
            case GE -> LE;
            default -> this;
        };
    }

    /** El operador que se cumple justo cuando no se cumple este: {@code <} pasa a {@code ≥}. */
    public Op complement() {
        return switch (this) {
            case EQ -> NE;
            case NE -> EQ;
            case LT -> GE;
            case LE -> GT;
            case GT -> LE;
            case GE -> LT;
        };
    }

    /**
     * Si la relación con este operador se cumple cuando el signo de {@code izquierda - derecha} es {@code sign}
     * (negativo, cero o positivo).
     */
    public boolean holdsForSign(int sign) {
        return switch (this) {
            case EQ -> sign == 0;
            case NE -> sign != 0;
            case LT -> sign < 0;
            case LE -> sign <= 0;
            case GT -> sign > 0;
            case GE -> sign >= 0;
        };
    }
}
