package barnesinside.mathutils.cas;

/**
 * Validador mínimo de JSON para comprobar en los tests que la salida está bien formada.
 */
public final class JsonValidator {

    private final String s;
    private int i;

    private JsonValidator(String s) {
        this.s = s;
    }

    /** Falla (con {@code assert}) si el texto no es un JSON válido. */
    public static void assertValid(String json) {
        JsonValidator v = new JsonValidator(json);
        v.value();
        v.skipWhitespace();
        assert v.i == json.length() : "JSON con texto sobrante en la posición " + v.i;
    }

    private void skipWhitespace() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private void expect(char c) {
        skipWhitespace();
        assert i < s.length() && s.charAt(i) == c
                : "Se esperaba '" + c + "' en " + i + ": " + s.substring(Math.max(0, i - 20), Math.min(s.length(), i + 20));
        i++;
    }

    private void value() {
        skipWhitespace();
        switch (s.charAt(i)) {
            case '{' -> {
                i++;
                skipWhitespace();
                if (s.charAt(i) == '}') {
                    i++;
                    return;
                }
                do {
                    skipWhitespace();
                    string();
                    expect(':');
                    value();
                    skipWhitespace();
                } while (s.charAt(i) == ',' && ++i > 0);
                expect('}');
            }
            case '[' -> {
                i++;
                skipWhitespace();
                if (s.charAt(i) == ']') {
                    i++;
                    return;
                }
                do {
                    value();
                    skipWhitespace();
                } while (s.charAt(i) == ',' && ++i > 0);
                expect(']');
            }
            case '"' -> string();
            default -> {
                int start = i;
                while (i < s.length() && ",]} \n".indexOf(s.charAt(i)) < 0) i++;
                String token = s.substring(start, i);
                assert token.equals("null") || token.equals("true") || token.equals("false")
                        || token.matches("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?") : "Valor inválido: " + token;
            }
        }
    }

    private void string() {
        expect('"');
        while (s.charAt(i) != '"') {
            if (s.charAt(i) == '\\') i++;
            i++;
        }
        i++;
    }
}
