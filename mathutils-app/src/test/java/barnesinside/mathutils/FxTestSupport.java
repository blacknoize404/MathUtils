package barnesinside.mathutils;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Utilidades para probar componentes JavaFX sin abrir ventanas: arranca el toolkit, ejecuta
 * código en el hilo de JavaFX y guarda capturas de escenas en {@code target/snapshots}.
 */
public final class FxTestSupport {

    private static boolean started;
    private static String unavailableReason;

    private FxTestSupport() {
    }

    /** Arranca el toolkit; si el entorno no lo permite (sin pantalla), salta el test. */
    public static synchronized void start() {
        if (started) return;
        // Tras un primer intento fallido el toolkit queda a medias: no se reintenta, porque un segundo
        // arranque parece "ya iniciado" y las tareas esperarían en vano hasta agotar el tiempo
        if (unavailableReason != null) Assumptions.abort(unavailableReason);
        // En Linux sin servidor gráfico (integración continua) ni se intenta arrancar el toolkit
        boolean windowsOrMac = System.getProperty("os.name", "").toLowerCase().matches(".*(win|mac).*");
        if (!windowsOrMac && System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null) {
            unavailableReason = "JavaFX no está disponible en este entorno: no hay pantalla";
            Assumptions.abort(unavailableReason);
        }
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException alreadyStarted) {
            // Ya estaba arrancado
        } catch (Throwable unavailable) {
            unavailableReason = "JavaFX no está disponible en este entorno: " + unavailable;
            Assumptions.abort(unavailableReason);
        }
        Platform.setImplicitExit(false);
        started = true;
    }

    /** Ejecuta en el hilo de JavaFX y devuelve el resultado (o relanza la excepción). */
    public static <T> T onFx(Callable<T> task) {
        start();
        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                future.complete(task.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new RuntimeException(cause);
        }
    }

    public static void onFx(Runnable task) {
        onFx(() -> {
            task.run();
            return null;
        });
    }

    /** Aplica estilos y distribución a la escena y la guarda como PNG. */
    public static File snapshot(Scene scene, String name) {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        WritableImage image = scene.snapshot(null);
        File dir = new File("target/snapshots");
        dir.mkdirs();
        File file = new File(dir, name + ".png");
        try {
            javax.imageio.ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    /** Cantidad de colores distintos en una imagen (para comprobar que no está en blanco). */
    public static int distinctColors(File png) {
        try {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(png);
            java.util.Set<Integer> colors = new java.util.HashSet<>();
            for (int y = 0; y < image.getHeight(); y += 2) {
                for (int x = 0; x < image.getWidth(); x += 2) colors.add(image.getRGB(x, y));
            }
            return colors.size();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Si el píxel existe en la imagen. */
    static boolean inside(PixelReader reader, int x, int y) {
        return reader != null && x >= 0 && y >= 0;
    }
}
