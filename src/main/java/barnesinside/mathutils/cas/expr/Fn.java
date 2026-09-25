package barnesinside.mathutils.cas.expr;

/**
 * Funciones elementales de un argumento que el núcleo reconoce.
 * La exponencial se representa como {@code e^x} (una {@link Pow}) y la raíz como potencia
 * de exponente racional, por lo que no aparecen aquí.
 */
public enum Fn {
    SIN("sin"), COS("cos"), TAN("tan"), COT("cot"), SEC("sec"), CSC("csc"),
    ASIN("asin"), ACOS("acos"), ATAN("atan"),
    SINH("sinh"), COSH("cosh"), TANH("tanh"),
    LN("ln"), ABS("abs"), ERF("erf"),
    SI("Si"), CI("Ci"), EI("Ei"), LI("li"),
    /** El factorial {@code n!}, que para valores no enteros es {@code Γ(x + 1)}. */
    FACTORIAL("factorial"),
    /** El suelo {@code ⌊x⌋}: el mayor entero que no pasa de x. */
    FLOOR("floor"),
    /** El techo {@code ⌈x⌉}: el menor entero que no es menor que x. */
    CEIL("ceil"),
    /** El redondeo {@code ⌊x⌉}: el entero más cercano; las mitades van al más lejano de cero. */
    ROUND("round");

    private final String symbol;

    Fn(String symbol) {
        this.symbol = symbol;
    }

    /** Nombre usado al imprimir y al interpretar texto. */
    public String symbol() {
        return symbol;
    }

    /** Funciones impares: {@code f(-x) = -f(x)}. */
    public boolean isOdd() {
        return switch (this) {
            case SIN, TAN, COT, CSC, ASIN, ATAN, SINH, TANH, ERF, SI -> true;
            default -> false;
        };
    }

    /** Funciones pares: {@code f(-x) = f(x)}. */
    public boolean isEven() {
        return switch (this) {
            case COS, SEC, COSH, ABS -> true;
            default -> false;
        };
    }

    /**
     * @return la función con ese nombre, aceptando alias habituales ({@code arcsin}, {@code arctan}…)
     */
    public static java.util.Optional<Fn> bySymbol(String name) {
        String n = name.toLowerCase();
        switch (n) {
            case "arcsin" -> n = "asin";
            case "arccos" -> n = "acos";
            case "arctan", "atn" -> n = "atan";
            case "tg" -> n = "tan";
            case "ctg" -> n = "cot";
            case "cosec" -> n = "csc";
            case "fact" -> n = "factorial";
            case "ceiling", "techo" -> n = "ceil";
            case "suelo" -> n = "floor";
            case "redondear" -> n = "round";
            case "log" -> { return java.util.Optional.empty(); }
            default -> { }
        }
        for (Fn fn : values()) if (fn.symbol.equalsIgnoreCase(n)) return java.util.Optional.of(fn);
        return java.util.Optional.empty();
    }
}
