package barnesinside.mathutils;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Textos de la interfaz en español e inglés. El idioma es una propiedad observable: los
 * textos enlazados con {@link #bind(String, Object...)} se actualizan al cambiarlo.
 */
public final class I18n {

    public static final Locale SPANISH = Locale.forLanguageTag("es");
    public static final Locale ENGLISH = Locale.ENGLISH;

    private static final String BUNDLE = "mathutils.ui";
    private static final String PREFERENCE_KEY = "language";

    private final ObjectProperty<Locale> locale = new SimpleObjectProperty<>(SPANISH);
    private final Preferences preferences = Preferences.userNodeForPackage(I18n.class);

    public I18n() {
        locale.set("en".equals(preferences.get(PREFERENCE_KEY, "es")) ? ENGLISH : SPANISH);
        locale.addListener((obs, old, now) -> preferences.put(PREFERENCE_KEY, now.getLanguage()));
    }

    public ObjectProperty<Locale> localeProperty() {
        return locale;
    }

    public Locale locale() {
        return locale.get();
    }

    public void setLocale(Locale newLocale) {
        locale.set("en".equals(newLocale.getLanguage()) ? ENGLISH : SPANISH);
    }

    public boolean isEnglish() {
        return "en".equals(locale.get().getLanguage());
    }

    /** Texto en el idioma actual; si la clave no existe devuelve la propia clave. */
    public String get(String key, Object... args) {
        return get(locale.get(), key, args);
    }

    public static String get(Locale locale, String key, Object... args) {
        Locale target = "en".equals(locale.getLanguage()) ? ENGLISH : SPANISH;
        try {
            String text = ResourceBundle.getBundle(BUNDLE, target, I18n.class.getClassLoader()).getString(key);
            for (int i = 0; i < args.length; i++) text = text.replace("{" + i + "}", String.valueOf(args[i]));
            return text;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /** Texto enlazado: se recalcula al cambiar el idioma. */
    public StringBinding bind(String key, Object... args) {
        return Bindings.createStringBinding(() -> get(key, args), locale);
    }
}
