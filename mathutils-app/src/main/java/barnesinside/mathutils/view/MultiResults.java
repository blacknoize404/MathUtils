package barnesinside.mathutils.view;

import MathUtils.cas.diff.MultiDerivatives;
import MathUtils.cas.expr.Expr;
import MathUtils.cas.steps.Solution;
import barnesinside.mathutils.AppContext;
import barnesinside.mathutils.I18n;
import barnesinside.mathutils.math.MathLayout;
import barnesinside.mathutils.math.MathView;
import barnesinside.mathutils.ui.Ui;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Presentación de los resultados con varias partes del cálculo con varias variables: la matriz del
 * gradiente, la hessiana o la jacobiana, y la suma de la divergencia o el laplaciano. Cada parte tiene
 * también su solución paso a paso, que se elige con el selector de {@link SolverView}.
 */
final class MultiResults {

    private MultiResults() {
    }

    /** Convierte el resultado del núcleo en las partes explicables y su resumen. */
    static SolverView.Computation of(MultiDerivatives.Result result) {
        I18n t = AppContext.i18n();
        List<SolverView.Part> parts = new ArrayList<>();
        for (MultiDerivatives.Entry entry : result.entries()) {
            parts.add(new SolverView.Part(label(result, entry), entry.solution()));
        }
        Node summary = result.isSolved() ? summary(result, t) : null;
        return new SolverView.Computation(parts, summary);
    }

    private static String label(MultiDerivatives.Result r, MultiDerivatives.Entry e) {
        List<String> v = r.variables();
        boolean several = r.functions().size() > 1;
        String f = several ? "f" + (e.row() + 1) : "f";
        return switch (r.kind()) {
            case GRADIENT -> "∂" + f + "/∂" + v.get(e.column());
            case HESSIAN -> "∂²f/∂" + v.get(e.column()) + "∂" + v.get(e.row());
            case JACOBIAN -> "∂" + f + "/∂" + v.get(e.column());
            case DIVERGENCE -> "∂F" + (e.row() + 1) + "/∂" + v.get(e.row());
            case LAPLACIAN -> "∂²f/∂" + v.get(e.column()) + "²";
        };
    }

    private static Node summary(MultiDerivatives.Result r, I18n t) {
        String title = t.get("derivative.mode." + r.kind().name().toLowerCase());
        if (r.total() != null) return totalCard(title, r, t);

        List<String> rowLabels = new ArrayList<>();
        List<String> columnLabels = new ArrayList<>();
        for (int i = 0; i < r.rows(); i++) {
            rowLabels.add(switch (r.kind()) {
                case HESSIAN -> "∂/∂" + r.variables().get(i);
                case JACOBIAN -> r.functions().size() > 1 ? "f" + (i + 1) : "f";
                default -> "";
            });
        }
        for (int j = 0; j < r.columns(); j++) columnLabels.add("∂/∂" + r.variables().get(j));

        Expr[][] cells = new Expr[r.rows()][r.columns()];
        for (int i = 0; i < r.rows(); i++) {
            for (int j = 0; j < r.columns(); j++) cells[i][j] = r.at(i, j).solution().result();
        }
        return matrixCard(title, rowLabels, columnLabels, cells);
    }

    private static Node totalCard(String title, MultiDerivatives.Result r, I18n t) {
        VBox terms = new VBox(6);
        for (MultiDerivatives.Entry e : r.entries()) {
            MathView view = MathView.empty(17);
            view.setBox(MathLayout.row(List.of(MathLayout.text(label(r, e) + " = ", 17), MathLayout.of(e.solution().result(), 17))));
            view.setInteractive(false);
            terms.getChildren().add(view);
        }
        MathView total = MathView.empty(26);
        total.setBox(MathLayout.row(List.of(MathLayout.text(t.get("derivative.total") + " = ", 26), MathLayout.of(r.total(), 26))));
        total.setInteractive(false);
        VBox box = new VBox(14, Ui.caption(title), terms, scroll(total));
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));
        return box;
    }

    /** Tarjeta con una cuadrícula de expresiones y los rótulos de sus filas y columnas. */
    static Node matrixCard(String title, List<String> rowLabels, List<String> columnLabels, Expr[][] cells) {
        GridPane grid = new GridPane();
        grid.setHgap(26);
        grid.setVgap(12);
        boolean rowHeaders = rowLabels.stream().anyMatch(l -> !l.isEmpty());
        int offset = rowHeaders ? 1 : 0;
        for (int j = 0; j < columnLabels.size(); j++) {
            Label header = Ui.label(columnLabels.get(j), "mono", "text-muted");
            GridPane.setHalignment(header, HPos.CENTER);
            grid.add(header, j + offset, 0);
        }
        for (int i = 0; i < cells.length; i++) {
            if (rowHeaders) grid.add(Ui.label(rowLabels.get(i), "mono", "text-muted"), 0, i + 1);
            for (int j = 0; j < cells[i].length; j++) {
                MathView view = MathView.empty(18);
                view.setBox(MathLayout.of(cells[i][j], 18));
                view.setInteractive(false);
                GridPane.setHalignment(view, HPos.CENTER);
                grid.add(view, j + offset, i + 1);
            }
        }
        HBox brackets = new HBox(grid);
        brackets.setAlignment(Pos.CENTER_LEFT);
        brackets.setPadding(new Insets(8, 18, 8, 18));
        brackets.setStyle("-fx-border-color: -bi-fg; -fx-border-width: 0 2 0 2;");
        brackets.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        ScrollPane scroller = new ScrollPane(brackets);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setFitToHeight(true);
        scroller.getStyleClass().add("animator-scroll");
        scroller.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        scroller.prefHeightProperty().bind(brackets.heightProperty().add(18));

        VBox box = new VBox(14, Ui.caption(title), scroller);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));
        return box;
    }

    private static ScrollPane scroll(MathView view) {
        ScrollPane pane = new ScrollPane(view);
        pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pane.setFitToHeight(true);
        pane.getStyleClass().add("animator-scroll");
        pane.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        pane.prefHeightProperty().bind(view.heightProperty().add(14));
        return pane;
    }

    @SuppressWarnings("unused")
    private static boolean unused(Solution s) {
        return s != null;
    }
}
