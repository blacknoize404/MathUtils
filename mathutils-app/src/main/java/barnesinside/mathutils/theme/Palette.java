package barnesinside.mathutils.theme;

import javafx.scene.paint.Color;

/**
 * Colores del sistema de diseño para el dibujo en lienzo (el resto de la interfaz usa CSS).
 * Mantiene los mismos valores que los tokens: blanco/negro puros, un único acento menta y
 * los acentos extendidos solo para categorías y visualización de datos.
 */
public record Palette(Color bg, Color fg, Color muted, Color border, Color elevated, Color secondary,
                      Color accent, Color accentBg, Color violet, Color cyan, Color magenta, Color amber,
                      Color red, Color emerald) {

    private static final Color MINT = Color.web("#00FF88");

    public static final Palette LIGHT = new Palette(
            Color.web("#FFFFFF"), Color.web("#000000"), Color.web("#737373"), Color.web("#D4D4D4"),
            Color.web("#FFFFFF"), Color.web("#F5F5F5"), MINT, Color.web("#00FF88", 0.1),
            Color.web("#8B5CF6"), Color.web("#06B6D4"), Color.web("#EC4899"), Color.web("#FBBF24"),
            Color.web("#EF4444"), Color.web("#10B981"));

    public static final Palette DARK = new Palette(
            Color.web("#000000"), Color.web("#FFFFFF"), Color.web("#737373"), Color.web("#2D2D2D"),
            Color.web("#1F1F1F"), Color.web("#111111"), MINT, Color.web("#00FF88", 0.1),
            Color.web("#8B5CF6"), Color.web("#06B6D4"), Color.web("#EC4899"), Color.web("#FBBF24"),
            Color.web("#EF4444"), Color.web("#10B981"));

    /** Primer plano con la opacidad dada (equivale a {@code --color-fg-NN}). */
    public Color fg(double opacity) {
        return Color.color(fg.getRed(), fg.getGreen(), fg.getBlue(), opacity);
    }
}
