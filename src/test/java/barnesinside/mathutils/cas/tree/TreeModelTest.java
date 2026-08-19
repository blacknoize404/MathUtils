package barnesinside.mathutils.cas.tree;

import barnesinside.mathutils.cas.JsonValidator;
import barnesinside.mathutils.cas.expr.ExprParser;
import barnesinside.mathutils.cas.syntax.SyntaxTools;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class TreeModelTest {

    private static TreeModel t(String source) {
        return TreeModel.of(source);
    }

    private static List<String> labels(TreeModel model) {
        return model.nodes().stream().map(TreeNode::label).toList();
    }

    // ------------------------------------------------------------ Estructura

    @Test
    void structureFollowsTheWrittenExpression() {
        TreeModel m = t("2 + 3*4");
        assert labels(m).equals(List.of("+", "2", "×", "3", "4"));
        assert m.size() == 5 && m.depth() == 3;
        assert m.root().kind() == TreeNode.Kind.OPERATOR;
        assert m.root().children().get(1).label().equals("×");
    }

    @Test
    void idsArePreorderFromZero() {
        TreeModel m = t("(1 + 2)*3");
        for (int i = 0; i < m.nodes().size(); i++) assert m.nodes().get(i).id() == i;
        assert m.find(1).get().label().equals("+");
        assert m.find(99).isEmpty();
    }

    @Test
    void parenthesesAreCountedOnTheContents() {
        TreeModel m = t("(1 + 2)*3");
        TreeNode plus = m.find(1).get();
        assert plus.parentheses() == 1;
        assert m.root().parentheses() == 0;
        assert t("((x))").root().parentheses() == 2;
    }

    @Test
    void nodeKindsAndLabels() {
        TreeModel m = t("-x + sin(pi) - e + 7");
        assert m.nodes().stream().anyMatch(n -> n.kind() == TreeNode.Kind.NEGATION && n.label().equals("−"));
        assert m.nodes().stream().anyMatch(n -> n.kind() == TreeNode.Kind.FUNCTION && n.label().equals("sin"));
        assert m.nodes().stream().anyMatch(n -> n.kind() == TreeNode.Kind.CONSTANT && n.label().equals("π"));
        assert m.nodes().stream().anyMatch(n -> n.kind() == TreeNode.Kind.CONSTANT && n.label().equals("e"));
        assert m.nodes().stream().anyMatch(n -> n.kind() == TreeNode.Kind.VARIABLE && n.label().equals("x"));
        assert m.nodes().stream().anyMatch(n -> n.kind() == TreeNode.Kind.NUMBER && n.label().equals("7"));
        assert labels(t("a/b - c")).containsAll(List.of("÷", "−"));
    }

    @Test
    void precedenceAndAssociativity() {
        TreeModel m = t("a + b*c^d");
        TreeNode plus = m.root();
        TreeNode times = plus.children().get(1);
        TreeNode power = times.children().get(1);
        assert plus.precedence() < times.precedence() && times.precedence() < power.precedence();
        assert power.associativity() == SyntaxTools.Associativity.RIGHT;
        assert plus.associativity() == SyntaxTools.Associativity.LEFT;
        assert m.find(1).get().precedence() > power.precedence() : "las hojas tienen la precedencia más alta";
        assert t("sin(x)").root().precedence() == SyntaxTools.FUNCTION_PRECEDENCE;
    }

    // ------------------------------------------------------------ Valores y orden

    @Test
    void valuesOfEverySubtree() {
        TreeModel m = t("2 + 3*4");
        assert m.root().value() == 14;
        assert m.root().children().get(1).value() == 12;
        assert m.root().children().get(0).value() == 2;
    }

    @Test
    void valuesUseTheVariablesGiven() {
        TreeModel without = t("x*2 + 1");
        assert without.root().value() == null;
        assert without.find(2).get().value() == null || without.find(2).get().label().equals("2");

        TreeModel with = TreeModel.of("x*2 + 1", Map.of("x", 5.0));
        assert with.root().value() == 11;
        assert with.root().children().get(0).value() == 10;
    }

    @Test
    void undefinedSubtreesHaveNoValue() {
        assert t("1/0").root().value() == null;
        assert t("ln(0)").root().value() == null;
        assert t("sqrt(-1)").root().value() == null;
    }

    @Test
    void evaluationOrderIsAttachedToOperations() {
        TreeModel m = t("2 + 3*4");
        assert m.root().evaluationOrder() == 2;
        assert m.root().children().get(1).evaluationOrder() == 1;
        assert m.root().children().get(0).evaluationOrder() == null;

        TreeModel groups = t("(1 + 2)*(3 + 4)");
        assert groups.find(1).get().evaluationOrder() == 1;
        assert groups.find(4).get().evaluationOrder() == 2;
        assert groups.root().evaluationOrder() == 3;
    }

    @Test
    void nodeTextIsTheSubexpression() {
        TreeModel m = t("2 + 3*4");
        assert m.root().text().equals("2 + 3*4");
        assert m.root().children().get(1).text().equals("3*4");
    }

    // ------------------------------------------------------------ Forma canónica

    @Test
    void symbolicTreeUsesTheCanonicalForm() {
        TreeModel m = TreeModel.ofExpr(ExprParser.parse("x*x + 2x + 1"));
        assert m.root().label().equals("+");
        assert m.root().children().size() == 3 : "la suma canónica es n-aria";
        assert m.nodes().stream().anyMatch(n -> n.label().equals("^"));

        TreeModel folded = TreeModel.ofExpr(ExprParser.parse("2 + 3*4"));
        assert folded.size() == 1 && folded.root().label().equals("14");
    }

    @Test
    void symbolicTreeValues() {
        TreeModel m = TreeModel.ofExpr(ExprParser.parse("x^2 + 1"), Map.of("x", 3.0));
        assert m.root().value() == 10;
        assert TreeModel.ofExpr(ExprParser.parse("x + 1")).root().value() == null;
    }

    @Test
    void symbolicFunctionNodes() {
        TreeModel m = TreeModel.ofExpr(ExprParser.parse("sin(2x)"));
        assert m.root().kind() == TreeNode.Kind.FUNCTION && m.root().label().equals("sin");
        assert m.root().children().size() == 1;
    }

    // ------------------------------------------------------------ JSON

    @Test
    void jsonIsWellFormed() {
        JsonValidator.assertValid(t("2 + 3*4").toJson());
        JsonValidator.assertValid(TreeModel.of("sin(x)^2 + log(8, 2)/y", Map.of("x", 1.0)).toJson());
        JsonValidator.assertValid(TreeModel.ofExpr(ExprParser.parse("e^(-x^2)")).toJson());
        String json = t("2 + 3*4").toJson();
        assert json.contains("\"label\":\"×\"") && json.contains("\"evaluationOrder\":1") && json.contains("\"value\":14.0");
    }

    // ------------------------------------------------------------ Disposición

    @Test
    void layoutPlacesEveryNodeOnItsDepthLevel() {
        TreeModel m = t("2 + 3*4");
        Map<Integer, TreeLayout.Position> layout = TreeLayout.compute(m);
        assert layout.size() == m.size();
        assert layout.get(0).y() == 0 && layout.get(1).y() == 1 && layout.get(2).y() == 1;
        assert layout.get(3).y() == 2 && layout.get(4).y() == 2;
    }

    @Test
    void layoutCentersParentsOverTheirChildren() {
        TreeModel m = t("(a + b)*(c + d)");
        Map<Integer, TreeLayout.Position> layout = TreeLayout.compute(m);
        for (TreeNode n : m.nodes()) {
            if (n.isLeaf()) continue;
            double first = layout.get(n.children().get(0).id()).x();
            double last = layout.get(n.children().get(n.children().size() - 1).id()).x();
            assert layout.get(n.id()).x() == (first + last) / 2;
        }
    }

    @Test
    void layoutNeverOverlaps() {
        for (String source : new String[]{"2 + 3*4", "((a+b)*(c+d))/((e+f)*(g+h))", "sin(x)^2 + cos(x)^2", "1+2+3+4+5+6+7+8", "x"}) {
            TreeModel m = t(source);
            Map<Integer, TreeLayout.Position> layout = TreeLayout.compute(m);
            Set<String> seen = new HashSet<>();
            for (TreeLayout.Position p : layout.values()) {
                assert seen.add(p.x() + "," + p.y()) : source + ": dos nodos en " + p;
            }
            double maxX = layout.values().stream().mapToDouble(TreeLayout.Position::x).max().getAsDouble();
            assert maxX == TreeLayout.width(m) - 1 : source;
        }
    }

    @Test
    void layoutOfSingleNode() {
        TreeModel m = t("x");
        assert TreeLayout.width(m) == 1;
        assert TreeLayout.compute(m).get(0).equals(new TreeLayout.Position(0, 0));
    }

    @Test
    void layoutKeepsPreorderOfKeys() {
        TreeModel m = t("(1 + 2)*3");
        List<Integer> keys = List.copyOf(TreeLayout.compute(m).keySet());
        assert keys.equals(List.of(0, 1, 2, 3, 4));
    }
}
