package barnesinside.mathutils;

import barnesinside.mathutils.theme.ThemeManager;

/**
 * Servicios compartidos de la aplicación: el tema y el idioma. Se crean una sola vez y los
 * componentes los consultan, de modo que cambiar el tema o el idioma actualiza toda la interfaz.
 */
public final class AppContext {

    private static ThemeManager theme;
    private static I18n i18n;

    private AppContext() {
    }

    public static synchronized ThemeManager theme() {
        if (theme == null) theme = new ThemeManager();
        return theme;
    }

    public static synchronized I18n i18n() {
        if (i18n == null) i18n = new I18n();
        return i18n;
    }

    /** Sustituye los servicios (para pruebas). */
    public static synchronized void reset(ThemeManager newTheme, I18n newI18n) {
        theme = newTheme;
        i18n = newI18n;
    }
}
