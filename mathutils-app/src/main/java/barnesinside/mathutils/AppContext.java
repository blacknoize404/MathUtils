package barnesinside.mathutils;

import barnesinside.mathutils.theme.ThemeManager;

/**
 * Servicios compartidos de la aplicación: el tema y el idioma. Se crean una sola vez y los
 * componentes los consultan, de modo que cambiar el tema o el idioma actualiza toda la interfaz.
 */
public final class AppContext {

    private static ThemeManager theme;
    private static I18n i18n;
    private static javafx.application.HostServices hostServices;

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

    public static synchronized void setHostServices(javafx.application.HostServices services) {
        hostServices = services;
    }

    /** Abre un enlace en el navegador del sistema. */
    public static synchronized void openLink(String url) {
        if (hostServices != null) {
            hostServices.showDocument(url);
            return;
        }
        try {
            if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (java.io.IOException | RuntimeException e) {
            // Sin navegador disponible no hay nada más que hacer
        }
    }

    /** Sustituye los servicios (para pruebas). */
    public static synchronized void reset(ThemeManager newTheme, I18n newI18n) {
        theme = newTheme;
        i18n = newI18n;
    }
}
