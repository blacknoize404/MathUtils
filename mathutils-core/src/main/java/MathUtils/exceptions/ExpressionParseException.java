package MathUtils.exceptions;

/**
 * Ocurre cuando una expresión de texto no se puede interpretar.
 * Indica la posición del error (índice, línea y columna, estas últimas desde 1).
 */
public class ExpressionParseException extends RuntimeException {

    private final int position;
    private final int line;
    private final int column;

    public ExpressionParseException(String message, String source, int position) {
        super(message + " (línea " + lineOf(source, position) + ", columna " + columnOf(source, position) + ")");
        this.position = position;
        this.line = lineOf(source, position);
        this.column = columnOf(source, position);
    }

    private static int lineOf(String source, int position) {
        int line = 1;
        for (int i = 0; i < Math.min(position, source.length()); i++) {
            if (source.charAt(i) == '\n') line++;
        }
        return line;
    }

    private static int columnOf(String source, int position) {
        int lastNewLine = source.lastIndexOf('\n', Math.min(position, source.length()) - 1);
        return position - lastNewLine;
    }

    public int getPosition() {
        return position;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
