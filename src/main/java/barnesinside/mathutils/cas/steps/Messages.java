package barnesinside.mathutils.cas.steps;

import barnesinside.mathutils.cas.expr.Expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Acceso a los textos localizados (español e inglés) de las reglas y de la presentación.
 * Cualquier idioma distinto del inglés usa español, que es el idioma por defecto.
 */
public final class Messages {

    private static final String BASE = "barnesinside.mathutils.cas.steps.messages";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(\\w+)}");

    private Messages() {
    }

    /** Idiomas con textos disponibles. */
    public static List<Locale> supportedLocales() {
        return List.of(Locale.forLanguageTag("es"), Locale.ENGLISH);
    }

    private static ResourceBundle bundle(Locale locale) {
        Locale target = locale != null && "en".equals(locale.getLanguage())
                ? Locale.ENGLISH
                : Locale.forLanguageTag("es");
        return ResourceBundle.getBundle(BASE, target, Messages.class.getModule());
    }

    /**
     * Texto de una clave.
     * @throws MissingResourceException si la clave no existe
     */
    public static String get(String key, Locale locale) {
        return bundle(locale).getString(key);
    }

    /** Indica si la clave existe en el idioma pedido. */
    public static boolean has(String key, Locale locale) {
        return bundle(locale).containsKey(key);
    }

    public static String title(Rule rule, Locale locale) {
        return get(rule.titleKey(), locale);
    }

    /**
     * Texto explicativo de una regla. Los marcadores {@code ${nombre}} se sustituyen por la
     * expresión del parámetro correspondiente, devuelta como segmento matemático. Un marcador
     * sin parámetro se deja como texto literal.
     */
    public static List<Segment> explanation(Rule rule, Map<String, Expr> params, Locale locale) {
        return interpolate(get(rule.textKey(), locale), params);
    }

    /**
     * Interpola una plantilla sustituyendo cada {@code ${nombre}} por un texto. Un marcador sin
     * valor se deja literal.
     */
    public static String interpolateText(String template, Map<String, String> values) {
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String value = values.get(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : m.group()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Interpola una plantilla con parámetros; ver {@link #explanation(Rule, Map, Locale)}.
     */
    public static List<Segment> interpolate(String template, Map<String, Expr> params) {
        List<Segment> segments = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(template);
        int last = 0;
        while (m.find()) {
            Expr value = params.get(m.group(1));
            if (value == null) continue; // se deja literal dentro del siguiente tramo de texto
            if (m.start() > last) segments.add(new Segment.Text(template.substring(last, m.start())));
            segments.add(new Segment.Math(value));
            last = m.end();
        }
        if (last < template.length()) segments.add(new Segment.Text(template.substring(last)));
        return segments;
    }
}
