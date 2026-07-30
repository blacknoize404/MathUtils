package barnesinside.mathutils.ui;

import java.util.Locale;

/** Formato de números para mostrar resultados: enteros limpios, fracciones sencillas y decimales cortos. */
public final class Numbers {

    private static final int MAX_DENOMINATOR = 1000;

    private Numbers() {
    }

    /** Decimal con hasta seis cifras significativas y sin ceros sobrantes. */
    public static String decimal(double v) {
        if (Double.isNaN(v)) return "NaN";
        if (Double.isInfinite(v)) return v > 0 ? "∞" : "−∞";
        if (Math.abs(v) < 1e-12) return "0";
        if (Math.abs(v - Math.rint(v)) < 1e-9 && Math.abs(v) < 1e15) return String.valueOf((long) Math.rint(v));
        String s = String.format(Locale.ROOT, "%.6g", v);
        if (s.contains("e")) return s;
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    /** La fracción {@code p/q} (denominador hasta 1000) que representa {@code v}, o {@code null}. */
    public static String fraction(double v) {
        if (!Double.isFinite(v) || Math.abs(v - Math.rint(v)) < 1e-9) return null;
        for (int q = 2; q <= MAX_DENOMINATOR; q++) {
            double p = Math.rint(v * q);
            if (Math.abs(v - p / q) < 1e-9 * Math.max(1, Math.abs(v))) {
                long num = (long) p;
                return (num < 0 ? "−" : "") + Math.abs(num) + "/" + q;
            }
        }
        return null;
    }

    /** La fracción si existe y el decimal si no. */
    public static String exact(double v) {
        String fraction = fraction(v);
        return fraction != null ? fraction : decimal(v);
    }

    /** Fracción y decimal juntos cuando hay fracción: {@code 1/3 ≈ 0.333333}. */
    public static String both(double v) {
        String fraction = fraction(v);
        return fraction != null ? fraction + " ≈ " + decimal(v) : decimal(v);
    }
}
