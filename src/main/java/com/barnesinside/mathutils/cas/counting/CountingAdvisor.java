package com.barnesinside.mathutils.cas.counting;

import com.barnesinside.mathutils.cas.steps.Messages;

import java.util.Locale;

/**
 * Elige el método de conteo a partir de las preguntas clave de un problema: ¿importa el orden?, ¿se puede repetir?, ¿hay
 * elementos iguales?, ¿se usan todos?, ¿van en círculo?
 *
 * @since 0.11
 */
public final class CountingAdvisor {

    /**
     * Las respuestas.
     *
     * @param orderMatters  si cambiar el orden da un caso distinto
     * @param repetition    si un elemento se puede usar más de una vez
     * @param identical     si entre los elementos hay iguales entre sí
     * @param allElements   si se usan todos los elementos (frente a elegir solo algunos)
     * @param circular      si se colocan alrededor de un círculo, sin lugar de partida
     */
    public record Question(boolean orderMatters, boolean repetition, boolean identical, boolean allElements, boolean circular) {
    }

    /**
     * El método recomendado y por qué.
     *
     * @param method    el método de conteo
     * @param reasonKey la clave del motivo en los textos
     */
    public record Advice(CountingMethod method, String reasonKey) {
        /** El motivo en el idioma pedido. */
        public String reason(Locale locale) {
            return Messages.get(reasonKey, locale);
        }
    }

    private CountingAdvisor() {
    }

    /** Recomienda el método para las respuestas dadas. */
    public static Advice advise(Question q) {
        if (q.orderMatters()) {
            if (q.circular() && q.allElements() && !q.repetition() && !q.identical()) return of(CountingMethod.CIRCULAR);
            if (q.identical()) return of(CountingMethod.MULTINOMIAL);
            if (q.repetition()) return of(CountingMethod.VARIATION_REP);
            return of(q.allElements() ? CountingMethod.PERMUTATION : CountingMethod.VARIATION);
        }
        if (q.repetition()) return of(CountingMethod.COMBINATION_REP);
        if (q.allElements()) return new Advice(CountingMethod.COMBINATION, "counting.advice.ONE");
        return of(CountingMethod.COMBINATION);
    }

    private static Advice of(CountingMethod method) {
        return new Advice(method, "counting.advice." + method.name());
    }
}
