package barnesinside.mathutils.parser;

/**
 * Unidad léxica de una expresión.
 * @param type tipo de token
 * @param text texto original
 * @param position índice (desde 0) donde empieza en la expresión
 */
public record Token(Type type, String text, int position) {

    public enum Type {
        NUMBER, IDENTIFIER, OPERATOR, LEFT_PAREN, RIGHT_PAREN, COMMA,
        /** El factorial {@code !}, que va detrás de lo que afecta. */
        POSTFIX
    }
}
