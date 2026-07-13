package MathUtils.cas.syntax;

import java.util.ArrayList;
import java.util.List;

/**
 * Imprime un {@link Syntax} como texto infijo, con la posibilidad de marcar un subárbol
 * (para resaltar la operación que se está resolviendo).
 */
public final class SyntaxPrinter {

    /**
     * Fragmento de texto, resaltado o no.
     */
    public record Piece(String text, boolean highlighted) {
    }

    private SyntaxPrinter() {
    }

    public static String print(Syntax tree) {
        StringBuilder sb = new StringBuilder();
        for (Piece piece : pieces(tree, null)) sb.append(piece.text());
        return sb.toString();
    }

    /**
     * Imprime el árbol dividido en fragmentos; los que corresponden a {@code highlight}
     * (comparado por identidad, no por valor) llevan {@code highlighted = true}.
     */
    public static List<Piece> pieces(Syntax tree, Syntax highlight) {
        List<Piece> pieces = new ArrayList<>();
        emit(tree, highlight, false, pieces);
        return merge(pieces);
    }

    private static void emit(Syntax node, Syntax highlight, boolean inside, List<Piece> out) {
        boolean on = inside || node == highlight;
        switch (node) {
            case Syntax.Num n -> out.add(new Piece(n.text(), on));
            case Syntax.Var v -> out.add(new Piece(v.name(), on));
            case Syntax.Const c -> out.add(new Piece(c.name(), on));
            case Syntax.Neg n -> {
                out.add(new Piece("-", on));
                operand(n.operand(), highlight, on, out, true);
            }
            case Syntax.Bin b -> {
                operand(b.left(), highlight, on, out, false);
                String op = switch (b.op()) {
                    case '+' -> " + ";
                    case '-' -> " - ";
                    default -> String.valueOf(b.op());
                };
                out.add(new Piece(op, on));
                operand(b.right(), highlight, on, out, false);
            }
            case Syntax.Call c -> {
                out.add(new Piece(c.name() + "(", on));
                for (int i = 0; i < c.args().size(); i++) {
                    if (i > 0) out.add(new Piece(", ", on));
                    emit(c.args().get(i), highlight, on, out);
                }
                out.add(new Piece(")", on));
            }
            case Syntax.Group g -> {
                out.add(new Piece("(", on));
                emit(g.inner(), highlight, on, out);
                out.add(new Piece(")", on));
            }
        }
    }

    /** Un número negativo o fraccionario, o una negación anidada, se escribe entre paréntesis como operando. */
    private static void operand(Syntax node, Syntax highlight, boolean inside, List<Piece> out, boolean underNegation) {
        boolean wrap = (node instanceof Syntax.Num n && n.needsParentheses())
                || (underNegation && node instanceof Syntax.Neg);
        boolean on = inside || node == highlight;
        if (wrap) out.add(new Piece("(", on));
        emit(node, highlight, inside, out);
        if (wrap) out.add(new Piece(")", on));
    }

    private static List<Piece> merge(List<Piece> pieces) {
        List<Piece> merged = new ArrayList<>();
        for (Piece p : pieces) {
            if (!merged.isEmpty() && merged.get(merged.size() - 1).highlighted() == p.highlighted()) {
                Piece last = merged.remove(merged.size() - 1);
                merged.add(new Piece(last.text() + p.text(), p.highlighted()));
            } else {
                merged.add(p);
            }
        }
        return merged;
    }
}
