package barnesinside.mathutils.cas.expr;

import java.util.Map;
import java.util.Set;

/**
 * La notación de potencia de una función, {@code sin^2(x)}, como la escribe cualquiera a mano: se reescribe a
 * {@code (sin(x))^2} antes de interpretar el texto. Con exponente {@code -1} en seno, coseno y tangente se
 * entiende como la función inversa ({@code sin^-1(x)} es {@code asin(x)}), que es lo que significa esa notación.
 */
final class FunctionPowers {

    private static final Set<String> FUNCTIONS = Set.of("sin", "cos", "tan", "cot", "sec", "csc", "sinh", "cosh",
            "tanh", "asin", "acos", "atan", "ln", "log", "exp", "erf", "abs");
    private static final Map<String, String> INVERSES = Map.of("sin", "asin", "cos", "acos", "tan", "atan");

    private FunctionPowers() {
    }

    /** El texto con las potencias de funciones reescritas; igual si no hay ninguna. */
    static String rewrite(String source) {
        if (source == null || source.indexOf('^') < 0) return source;
        String text = source;
        int from = 0;
        while (true) {
            Match m = find(text, from);
            if (m == null) return text;
            String call = m.name + text.substring(m.argsStart, m.argsEnd + 1);
            String replacement;
            if (m.exponent.equals("-1") && INVERSES.containsKey(m.name)) {
                replacement = INVERSES.get(m.name) + text.substring(m.argsStart, m.argsEnd + 1);
            } else {
                String exponent = m.exponent.startsWith("(") ? m.exponent : (m.exponent.startsWith("-") || m.exponent.startsWith("+")
                        ? "(" + m.exponent + ")" : m.exponent);
                replacement = "(" + call + ")^" + exponent;
            }
            text = text.substring(0, m.start) + replacement + text.substring(m.argsEnd + 1);
            from = m.start + replacement.length();
        }
    }

    private record Match(int start, String name, String exponent, int argsStart, int argsEnd) {
    }

    private static Match find(String s, int from) {
        int i = from;
        while (i < s.length()) {
            if (!Character.isLetter(s.charAt(i)) || (i > 0 && Character.isLetter(s.charAt(i - 1)))) {
                i++;
                continue;
            }
            int end = i;
            while (end < s.length() && Character.isLetter(s.charAt(end))) end++;
            String word = s.substring(i, end);
            int at = skipSpaces(s, end);
            if (FUNCTIONS.contains(word) && at < s.length() && s.charAt(at) == '^') {
                int exponentStart = skipSpaces(s, at + 1);
                int exponentEnd = exponentEnd(s, exponentStart);
                if (exponentEnd > exponentStart) {
                    int open = skipSpaces(s, exponentEnd);
                    if (open < s.length() && s.charAt(open) == '(') {
                        int close = matching(s, open);
                        if (close > 0) {
                            return new Match(i, word, s.substring(exponentStart, exponentEnd).replace(" ", ""), open, close);
                        }
                    }
                }
            }
            i = end;
        }
        return null;
    }

    /** Fin del exponente: un número (con signo), un grupo entre paréntesis o un identificador. */
    private static int exponentEnd(String s, int start) {
        if (start >= s.length()) return start;
        int i = start;
        if (s.charAt(i) == '-' || s.charAt(i) == '+') i++;
        if (i >= s.length()) return start;
        char c = s.charAt(i);
        if (c == '(') {
            int close = matching(s, i);
            return close < 0 ? start : close + 1;
        }
        if (Character.isDigit(c) || c == '.') {
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
            return i;
        }
        if (Character.isLetter(c)) {
            // Una sola letra: sin^n(x); una palabra seguida de '(' sería otra función
            return i + 1 < s.length() && Character.isLetter(s.charAt(i + 1)) ? start : i + 1;
        }
        return start;
    }

    private static int skipSpaces(String s, int i) {
        while (i < s.length() && s.charAt(i) == ' ') i++;
        return i;
    }

    private static int matching(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            if (s.charAt(i) == '(') depth++;
            else if (s.charAt(i) == ')' && --depth == 0) return i;
        }
        return -1;
    }
}
