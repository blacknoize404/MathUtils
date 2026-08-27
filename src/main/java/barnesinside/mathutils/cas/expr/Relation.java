package barnesinside.mathutils.cas.expr;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Una relación entre dos expresiones: una ecuación ({@code x^2 = 4}) o una inecuación ({@code x + 1 <= 3}).
 */
public record Relation(Expr left, Op op, Expr right) {

    /** La expresión {@code izquierda - derecha}: la relación pide que su signo sea el del operador. */
    public Expr difference() {
        return Expr.sub(left, right);
    }

    /** La misma relación con los lados intercambiados. */
    public Relation swapped() {
        return new Relation(right, op.swapped(), left);
    }

    public Relation withLeft(Expr newLeft) {
        return new Relation(newLeft, op, right);
    }

    public Relation withRight(Expr newRight) {
        return new Relation(left, op, newRight);
    }

    /** Variables libres de los dos lados. */
    public java.util.Set<String> variables() {
        java.util.Set<String> all = new java.util.LinkedHashSet<>(left.variables());
        all.addAll(right.variables());
        return all;
    }

    /**
     * La relación como parámetros de un paso: {@code l}, {@code op} y {@code r}. El operador va como una variable
     * llamada con su símbolo para que las plantillas de texto lo intercalen como cualquier otra expresión.
     */
    public Map<String, Expr> toParams() {
        Map<String, Expr> params = new LinkedHashMap<>();
        params.put("l", left);
        params.put("op", Expr.sym(op.symbol()));
        params.put("r", right);
        return params;
    }

    /** La relación guardada por {@link #toParams()}, o vacío si los parámetros no la traen. */
    public static Optional<Relation> fromParams(Map<String, Expr> params) {
        Expr l = params.get("l");
        Expr o = params.get("op");
        Expr r = params.get("r");
        if (l == null || r == null || !(o instanceof Expr.Sym s)) return Optional.empty();
        for (Op op : Op.values()) if (op.symbol().equals(s.name())) return Optional.of(new Relation(l, op, r));
        return Optional.empty();
    }

    /** Texto plano: {@code x^2 - 4 <= 0}. */
    @Override
    public String toString() {
        return left + " " + op.ascii() + " " + right;
    }

    public String toLatex() {
        String symbol = switch (op) {
            case EQ -> "=";
            case NE -> "\\neq";
            case LT -> "<";
            case LE -> "\\leq";
            case GT -> ">";
            case GE -> "\\geq";
        };
        return left.toLatex() + " " + symbol + " " + right.toLatex();
    }

    /**
     * Interpreta un texto como relación: dos expresiones separadas por {@code =}, {@code <}, {@code >},
     * {@code <=}, {@code >=}, {@code !=} (también {@code ≤ ≥ ≠ ==} y {@code <>}). Vacío si no hay un
     * operador de relación fuera de paréntesis o si alguno de los lados no es una expresión.
     */
    public static Optional<Relation> parse(String text) {
        if (text == null) return Optional.empty();
        String s = text.strip();
        if (s.contains("->") || s.contains("→") || s.contains("∫") || s.matches("(?s).*\blim\b.*")) return Optional.empty();

        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '[') depth++;
            else if (c == ')' || c == ']') depth--;
            if (depth != 0) continue;

            Op op = null;
            int length = 1;
            if (c == '<' && i + 1 < s.length() && s.charAt(i + 1) == '=') {
                op = Op.LE;
                length = 2;
            } else if (c == '>' && i + 1 < s.length() && s.charAt(i + 1) == '=') {
                op = Op.GE;
                length = 2;
            } else if (c == '!' && i + 1 < s.length() && s.charAt(i + 1) == '=') {
                op = Op.NE;
                length = 2;
            } else if (c == '<' && i + 1 < s.length() && s.charAt(i + 1) == '>') {
                op = Op.NE;
                length = 2;
            } else if (c == '=' && i + 1 < s.length() && s.charAt(i + 1) == '=') {
                op = Op.EQ;
                length = 2;
            } else if (c == '=') {
                op = Op.EQ;
            } else if (c == '<') {
                op = Op.LT;
            } else if (c == '>') {
                op = Op.GT;
            } else if (c == '≤') {
                op = Op.LE;
            } else if (c == '≥') {
                op = Op.GE;
            } else if (c == '≠') {
                op = Op.NE;
            }
            if (op == null) continue;

            Optional<Expr> left = ExprParser.tryParse(s.substring(0, i));
            Optional<Expr> right = ExprParser.tryParse(s.substring(i + length));
            if (left.isEmpty() || right.isEmpty()) return Optional.empty();
            // Un segundo operador de relación (a < b < c) no es una relación de dos lados
            String rest = s.substring(i + length);
            if (rest.matches("(?s).*[<>=≤≥≠].*")) return Optional.empty();
            return Optional.of(new Relation(left.get(), op, right.get()));
        }
        return Optional.empty();
    }
}
