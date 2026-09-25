package barnesinside.mathutils.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduce la notación matemática que no está en el teclado a la sintaxis del intérprete, para que
 * se pueda escribir {@code 2π}, {@code √(x²+1)}, {@code x²·3}, {@code 30°} o {@code ∞} y significar
 * lo mismo que {@code 2pi}, {@code sqrt(x^2+1)}, {@code x^2*3}, {@code 30*pi/180} y {@code oo}.
 * <p>Las letras griegas no hacen falta aquí: el intérprete ya las acepta como nombres de variable.
 */
public final class Notation {

    private static final String SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹";
    private static final Pattern SUPERSCRIPT_RUN = Pattern.compile("[⁻⁺]?[⁰¹²³⁴⁵⁶⁷⁸⁹]+");
    private static final Pattern DEGREES = Pattern.compile("(\\d+(?:\\.\\d+)?)°");
    private static final Pattern ROOT = Pattern.compile("([√∛])(\\(|[\\p{L}_][\\p{L}\\p{N}_]*|\\d+(?:\\.\\d+)?)");

    private Notation() {
    }

    /** Sustituye los símbolos por su equivalente del teclado; el resto del texto no cambia. */
    public static String normalize(String source) {
        if (source == null || source.isEmpty()) return source;

        boolean plain = true;
        for (int i = 0; i < source.length() && plain; i++) if (source.charAt(i) > 0x7F) plain = false;
        if (plain) return source;

        String s = source;
        s = s.replace('−', '-').replace('–', '-').replace('×', '*').replace('·', '*').replace('⋅', '*')
                .replace('÷', '/').replace('∕', '/').replace("∞", "oo");
        s = DEGREES.matcher(s).replaceAll("($1*pi/180)");
        s = superscripts(s);
        s = roots(s);
        s = brackets(s);
        return s;
    }

    /**
     * {@code ⌊x⌋} pasa a {@code floor(x)}, {@code ⌈x⌉} a {@code ceil(x)} y {@code ⌊x⌉} a {@code round(x)}: se casa cada cierre con su
     * apertura, así que pueden anidarse ({@code ⌊⌈x⌉/2⌋}). Un signo sin pareja se deja como está y el intérprete lo rechaza.
     */
    private static String brackets(String s) {
        if (s.indexOf('⌊') < 0 && s.indexOf('⌈') < 0 && s.indexOf('⌋') < 0 && s.indexOf('⌉') < 0) return s;
        StringBuilder out = new StringBuilder();
        java.util.ArrayDeque<int[]> open = new java.util.ArrayDeque<>();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '⌊' || c == '⌈') {
                open.push(new int[]{out.length(), c});
                out.append(c == '⌊' ? "floor(" : "ceil(");
            } else if (c == '⌋' || c == '⌉') {
                if (open.isEmpty()) {
                    out.append(c);
                    continue;
                }
                int[] opener = open.pop();
                // ⌊ cerrado con ⌉ es el redondeo: se corrige el nombre que se escribió al abrir
                if (opener[1] == '⌊' && c == '⌉') out.replace(opener[0], opener[0] + "floor".length(), "round");
                out.append(')');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** {@code x²} pasa a {@code x^2}, {@code x⁻¹} a {@code x^-1} y {@code x¹²} a {@code x^12}. */
    private static String superscripts(String s) {
        Matcher m = SUPERSCRIPT_RUN.matcher(s);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String run = m.group();
            boolean negative = run.charAt(0) == '⁻';
            int start = run.charAt(0) == '⁻' || run.charAt(0) == '⁺' ? 1 : 0;
            StringBuilder digits = new StringBuilder();
            for (int i = start; i < run.length(); i++) digits.append(SUPERSCRIPTS.indexOf(run.charAt(i)));
            // Entre paréntesis, para que el signo y las varias cifras formen un solo exponente
            String exponent = negative ? "^(-" + digits + ")" : "^" + digits;
            m.appendReplacement(out, Matcher.quoteReplacement(exponent));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** {@code √(…)} y {@code √x} pasan a {@code sqrt(…)}; {@code ∛} a {@code cbrt(…)}. */
    private static String roots(String s) {
        Matcher m = ROOT.matcher(s);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String function = m.group(1).equals("√") ? "sqrt" : "cbrt";
            String argument = m.group(2);
            m.appendReplacement(out, Matcher.quoteReplacement(argument.equals("(")
                    ? function + "(" : function + "(" + argument + ")"));
        }
        m.appendTail(out);
        return out.toString();
    }
}
