package MathUtils.complex;

/**
 * Intérprete de expresiones con números complejos: {@code 3+4i}, {@code (1+i)^2/(2-i)},
 * {@code 2i}, {@code pi}, {@code e}. Admite {@code + - * /}, potencias enteras, paréntesis y
 * multiplicación implícita ({@code 2i}, {@code 3(1+i)}).
 */
final class ComplexParser {

    private final String text;
    private int position;

    private ComplexParser(String text) {
        this.text = text;
    }

    static Complex parse(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Escribe un número complejo. Ejemplo: 3+4i");
        ComplexParser parser = new ComplexParser(text.replace("−", "-").replace(',', '.'));
        Complex value = parser.sum();
        parser.skipSpaces();
        if (parser.position < parser.text.length()) throw parser.error("Carácter inesperado");
        return value;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " en la posición " + (position + 1) + " de \"" + text.strip()
                + "\". Ejemplo válido: 3+4i");
    }

    private void skipSpaces() {
        while (position < text.length() && Character.isWhitespace(text.charAt(position))) position++;
    }

    private boolean accept(char c) {
        skipSpaces();
        if (position < text.length() && text.charAt(position) == c) {
            position++;
            return true;
        }
        return false;
    }

    private Complex sum() {
        Complex value = product();
        while (true) {
            if (accept('+')) value = value.add(product());
            else if (accept('-')) value = value.subtract(product());
            else return value;
        }
    }

    private Complex product() {
        Complex value = unary();
        while (true) {
            if (accept('*')) {
                value = value.multiply(unary());
            } else if (accept('/')) {
                Complex divisor = unary();
                if (divisor.isZero()) throw error("No se puede dividir entre cero");
                value = value.divide(divisor);
            } else if (startsAtom()) {
                value = value.multiply(power());
            } else {
                return value;
            }
        }
    }

    private boolean startsAtom() {
        skipSpaces();
        if (position >= text.length()) return false;
        char c = text.charAt(position);
        return c == '(' || Character.isDigit(c) || c == '.' || Character.isLetter(c);
    }

    private Complex unary() {
        if (accept('-')) return unary().negate();
        if (accept('+')) return unary();
        return power();
    }

    private Complex power() {
        Complex base = atom();
        if (accept('^')) {
            boolean negative = accept('-');
            skipSpaces();
            int start = position;
            while (position < text.length() && Character.isDigit(text.charAt(position))) position++;
            if (start == position) throw error("La potencia debe ser un entero");
            int exponent = Integer.parseInt(text.substring(start, position));
            if (exponent > 1000) throw error("La potencia es demasiado grande");
            Complex result = base.pow(exponent);
            if (negative) {
                if (base.isZero()) throw error("No se puede dividir entre cero");
                result = result.reciprocal();
            }
            return result;
        }
        return base;
    }

    private Complex atom() {
        skipSpaces();
        if (position >= text.length()) throw error("Falta un número");
        char c = text.charAt(position);
        if (c == '(') {
            position++;
            Complex inner = sum();
            if (!accept(')')) throw error("Falta cerrar el paréntesis");
            return inner;
        }
        if (Character.isDigit(c) || c == '.') {
            int start = position;
            while (position < text.length() && (Character.isDigit(text.charAt(position)) || text.charAt(position) == '.')) {
                position++;
            }
            double number;
            try {
                number = Double.parseDouble(text.substring(start, position));
            } catch (NumberFormatException e) {
                throw error("Número no válido");
            }
            return Complex.real(number);
        }
        if (Character.isLetter(c)) {
            int start = position;
            while (position < text.length() && Character.isLetter(text.charAt(position))) position++;
            String name = text.substring(start, position).toLowerCase();
            return switch (name) {
                case "i", "j" -> Complex.I;
                case "pi" -> Complex.real(Math.PI);
                case "e" -> Complex.real(Math.E);
                default -> {
                    position = start;
                    throw error("Nombre desconocido '" + name + "'");
                }
            };
        }
        throw error("Carácter inesperado '" + c + "'");
    }
}
