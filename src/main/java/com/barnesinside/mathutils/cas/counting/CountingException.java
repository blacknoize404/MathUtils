package com.barnesinside.mathutils.cas.counting;

import com.barnesinside.mathutils.cas.steps.Messages;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * Un problema de conteo mal planteado (un valor negativo, elegir más elementos de los que hay…). El mensaje se puede pedir en
 * español o en inglés con {@link #message(Locale)}; {@link #getMessage()} lo da en español.
 *
 * @since 0.11
 */
public final class CountingException extends IllegalArgumentException {

    private final String key;
    private final String[] arguments;

    CountingException(String key, Object... arguments) {
        super(format(key, Locale.forLanguageTag("es"), strings(arguments)));
        this.key = key;
        this.arguments = strings(arguments);
    }

    private static String[] strings(Object[] values) {
        String[] out = new String[values.length];
        for (int i = 0; i < values.length; i++) out[i] = String.valueOf(values[i]);
        return out;
    }

    private static String format(String key, Locale locale, String[] arguments) {
        return MessageFormat.format(Messages.get("counting.error." + key, locale), (Object[]) arguments);
    }

    /** La clave del error (la parte final de {@code counting.error.<clave>}). */
    public String key() {
        return key;
    }

    /** El mensaje en el idioma pedido. */
    public String message(Locale locale) {
        return format(key, locale, arguments);
    }
}
