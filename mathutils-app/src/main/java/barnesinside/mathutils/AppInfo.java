package barnesinside.mathutils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Datos de la aplicación que se muestran en «Acerca de»: versión, autoría y enlaces. */
public final class AppInfo {

    public static final String NAME = "MathUtils Studio";
    public static final String AUTHOR = "barnesinside";
    public static final String AUTHOR_URL = "https://barnesinside.is-a.dev";
    public static final String SOURCE_URL = "https://github.com/blacknoize404/MathUtils";

    private AppInfo() {
    }

    /** Versión de la aplicación (la del POM, sin {@code -SNAPSHOT}), o {@code desarrollo} si se ejecuta desde el código. */
    public static String version() {
        Properties properties = new Properties();
        try (InputStream in = AppInfo.class.getResourceAsStream("/mathutils/version.properties")) {
            if (in != null) properties.load(in);
        } catch (IOException e) {
            // Se muestra la versión de desarrollo
        }
        String version = properties.getProperty("version", "");
        if (version.isBlank() || version.contains("${")) return "desarrollo";
        return version.replace("-SNAPSHOT", "");
    }
}
