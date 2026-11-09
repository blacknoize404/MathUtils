package barnesinside.mathutils.cas.counting;

import barnesinside.mathutils.cas.steps.Messages;

import java.util.Locale;

/**
 * Los métodos de conteo que sabe resolver {@link CountingSolver}. El nombre y el uso de cada uno están en los recursos de
 * textos (español e inglés).
 *
 * @since 0.11
 */
public enum CountingMethod {
    SUM, PRODUCT, PERMUTATION, VARIATION, VARIATION_REP, COMBINATION, COMBINATION_REP, MULTINOMIAL, CIRCULAR,
    BINOMIAL, INCLUSION, DERANGEMENT, CATALAN, STIRLING, PARTITIONS;

    /** El nombre del método en el idioma pedido. */
    public String title(Locale locale) {
        return Messages.get("counting.method." + name() + ".name", locale);
    }

    /** Cuándo se usa, en una frase. */
    public String usage(Locale locale) {
        return Messages.get("counting.method." + name() + ".when", locale);
    }
}
