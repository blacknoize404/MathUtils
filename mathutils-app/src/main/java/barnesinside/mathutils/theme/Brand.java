package barnesinside.mathutils.theme;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Scale;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Recursos de la identidad barnesinside: tipografías Geist y Geist Mono, iconos Lucide y logos.
 * Los archivos se generan con {@code tools/build_assets.py} desde el sistema de diseño.
 */
public final class Brand {

    public static final String SANS = "Geist";
    public static final String SANS_MEDIUM = "Geist Medium";
    public static final String SANS_SEMIBOLD = "Geist SemiBold";
    public static final String MONO = "Geist Mono";
    public static final String MONO_MEDIUM = "Geist Mono Medium";

    private static final String[] FONT_FILES = {
            "Geist-Regular.ttf", "Geist-Bold.ttf", "Geist-Medium.ttf", "Geist-SemiBold.ttf", "Geist-Italic.ttf",
            "GeistMono-Regular.ttf", "GeistMono-Bold.ttf", "GeistMono-Medium.ttf"
    };

    private static boolean loaded;
    private static final Map<String, String[]> ICON_PATHS = new HashMap<>();

    private Brand() {
    }

    /** Registra las fuentes y lee los iconos. Es seguro llamarlo varias veces. */
    public static synchronized void load() {
        if (loaded) return;
        for (String file : FONT_FILES) {
            try (InputStream in = Brand.class.getResourceAsStream("/mathutils/fonts/" + file)) {
                if (in != null) Font.loadFont(in, 12);
            } catch (IOException e) {
                // Sin la fuente se usa la del sistema
            }
        }
        try (InputStream in = Brand.class.getResourceAsStream("/mathutils/icons/icons.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                for (String name : props.stringPropertyNames()) {
                    ICON_PATHS.put(name, props.getProperty(name).split("\\|"));
                }
            }
        } catch (IOException e) {
            // Sin iconos se muestran vacíos
        }
        loaded = true;
    }

    // ---------------------------------------------------------------- Tipografía

    public static Font sans(double size) {
        load();
        return Font.font(SANS, FontWeight.NORMAL, FontPosture.REGULAR, size);
    }

    public static Font sansMedium(double size) {
        load();
        return Font.font(SANS_MEDIUM, size);
    }

    public static Font sansSemiBold(double size) {
        load();
        return Font.font(SANS_SEMIBOLD, size);
    }

    public static Font sansBold(double size) {
        load();
        return Font.font(SANS, FontWeight.BOLD, FontPosture.REGULAR, size);
    }

    public static Font sansItalic(double size) {
        load();
        return Font.font(SANS, FontWeight.NORMAL, FontPosture.ITALIC, size);
    }

    public static Font mono(double size) {
        load();
        return Font.font(MONO, FontWeight.NORMAL, FontPosture.REGULAR, size);
    }

    public static Font monoMedium(double size) {
        load();
        return Font.font(MONO_MEDIUM, size);
    }

    public static Font monoBold(double size) {
        load();
        return Font.font(MONO, FontWeight.BOLD, FontPosture.REGULAR, size);
    }

    // ---------------------------------------------------------------- Iconos

    /**
     * Icono Lucide (trazo de 2 px sobre 24×24) escalado al tamaño pedido. El color lo da la
     * clase de estilo {@code icon} ({@code -bi-fg}); se puede cambiar con {@code -fx-stroke}.
     */
    public static Node icon(String name, double size) {
        load();
        String[] paths = ICON_PATHS.get(name);
        Group group = new Group();
        if (paths != null) {
            for (String d : paths) {
                SVGPath path = new SVGPath();
                path.setContent(d);
                path.setFill(null);
                path.setStrokeWidth(2);
                path.setStrokeLineCap(StrokeLineCap.ROUND);
                path.setStrokeLineJoin(StrokeLineJoin.ROUND);
                path.getStyleClass().add("icon");
                group.getChildren().add(path);
            }
        }
        group.getTransforms().add(new Scale(size / 24, size / 24, 0, 0));

        Pane box = new Pane(group);
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        box.setMouseTransparent(true);
        box.getStyleClass().add("icon-box");
        return box;
    }

    public static boolean hasIcon(String name) {
        load();
        return ICON_PATHS.containsKey(name);
    }

    // ---------------------------------------------------------------- Logos

    /** La marca de MathUtils (blanca sobre fondos oscuros y negra sobre claros), con su cuadrado menta. */
    public static Image mark(boolean darkBackground) {
        return new Image(Brand.class.getResourceAsStream("/mathutils/brand/mathutils_mark_"
                + (darkBackground ? "blanco" : "negro") + ".png"));
    }

    /** El icono de la aplicación para la ventana y la barra de tareas. */
    public static Image appIcon() {
        return new Image(Brand.class.getResourceAsStream("/mathutils/brand/mathutils_app_icon.png"));
    }

    /** Variantes de los logos oficiales de barnesinside, que se muestran en «Acerca de». */
    public enum Logo {ISOTIPO, IMAGOTIPO}

    /**
     * Logo oficial para el tema indicado: blanco sobre fondos oscuros y negro sobre claros.
     */
    public static Image logo(Logo logo, boolean darkBackground) {
        String kind = logo == Logo.ISOTIPO ? "isotipo" : "imagotipo";
        String color = darkBackground ? "blanco" : "negro";
        return new Image(Brand.class.getResourceAsStream("/mathutils/brand/barnesinside_" + kind + "_" + color + ".png"));
    }
}
