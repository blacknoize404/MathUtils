package MathUtils.cas.steps;

import MathUtils.cas.expr.Expr;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convierte una {@link Solution} o una lista de {@link Step} a texto en cada {@link Format}.
 */
public final class StepRenderer {

    private StepRenderer() {
    }

    /** Renderiza enunciado, pasos y resultado. */
    public static String render(Solution solution, Locale locale, Format format) {
        return switch (format) {
            case PLAIN -> plain(solution, locale);
            case MARKDOWN -> markdown(solution, locale);
            case LATEX -> latex(solution, locale);
            case JSON -> json(solution, locale);
        };
    }

    /** Renderiza solo una lista de pasos. */
    public static String render(List<Step> steps, Locale locale, Format format) {
        StringBuilder sb = new StringBuilder();
        switch (format) {
            case PLAIN -> plainSteps(sb, steps, "", locale);
            case MARKDOWN -> markdownSteps(sb, steps, 0, locale);
            case LATEX -> latexSteps(sb, steps, locale);
            case JSON -> {
                sb.append('[');
                jsonSteps(sb, steps, locale);
                sb.append(']');
            }
        }
        return sb.toString();
    }

    // ================================================================ Enunciado y ecuaciones

    private static List<Segment> problemText(Solution s, Locale locale) {
        String base = "solution.kind." + s.kind().name();
        String key = base + (s.order() > 1 ? ".problem.order" : isDefinite(s) ? ".problem.definite" : ".problem");
        if (s.kind() == Solution.Kind.LIMIT) {
            Expr direction = s.extras().get("direction");
            if (direction instanceof Expr.Num n && n.value().signum() != 0) {
                key = base + ".problem." + (n.value().signum() > 0 ? "RIGHT" : "LEFT");
            }
        }

        Map<String, Expr> params = new HashMap<>(s.extras());
        params.put("f", s.problem());
        params.put("x", Expr.sym(s.variable()));
        params.put("k", Expr.num(s.order()));
        return Messages.interpolate(Messages.get(key, locale), params);
    }

    private static boolean isDefinite(Solution s) {
        return s.kind() == Solution.Kind.INTEGRAL && s.extras().containsKey("lower") && s.extras().containsKey("upper");
    }

    private static String statusText(Solution s, Locale locale) {
        String key = "solution.status." + s.status().name();
        if (isDefinite(s) && Messages.has(key + ".INTEGRAL", locale)) key += ".INTEGRAL";
        return Messages.get(key, locale);
    }

    /** Operación de un paso aplicada a una expresión, en texto plano. */
    private static String operationPlain(Rule.Category category, Map<String, Expr> params, Expr before) {
        Expr x = params.get("x");
        if (x == null) return before.toString();
        return switch (category) {
            case DERIVATIVE -> "d/d" + x + "[" + before + "]";
            case LIMIT -> "lim[" + x + " -> " + params.getOrDefault("a", Expr.ZERO) + "] " + before;
            case INTEGRAL -> "∫ " + before + " d" + x;
            default -> before.toString();
        };
    }

    private static String operationLatex(Rule.Category category, Map<String, Expr> params, Expr before) {
        Expr x = params.get("x");
        if (x == null) return before.toLatex();
        return switch (category) {
            case DERIVATIVE -> "\\frac{d}{d" + x.toLatex() + "}\\left[" + before.toLatex() + "\\right]";
            case LIMIT -> "\\lim_{" + x.toLatex() + " \\to " + params.getOrDefault("a", Expr.ZERO).toLatex() + "} "
                    + before.toLatex();
            case INTEGRAL -> "\\int " + before.toLatex() + " \\, d" + x.toLatex();
            default -> before.toLatex();
        };
    }

    private static String equationPlain(Step step) {
        if (step.before() == null || step.after() == null) return null;
        return operationPlain(step.rule().category(), step.params(), step.before()) + " = " + step.after();
    }

    private static String equationLatex(Step step) {
        if (step.before() == null || step.after() == null) return null;
        return operationLatex(step.rule().category(), step.params(), step.before()) + " = " + step.after().toLatex();
    }

    // ---------------------------------------------------------------- Resultado final

    private static Map<String, Expr> resultParams(Solution s) {
        Map<String, Expr> params = new HashMap<>(s.extras());
        params.put("x", Expr.sym(s.variable()));
        return params;
    }

    private static String directionSuffixPlain(Solution s) {
        if (s.extras().get("direction") instanceof Expr.Num n && n.value().signum() != 0) {
            return n.value().signum() > 0 ? "+" : "-";
        }
        return "";
    }

    /** Operación de la solución completa, en texto plano (puede ser una derivada de orden superior). */
    private static String resultOperationPlain(Solution s) {
        return switch (s.kind()) {
            case DERIVATIVE -> {
                String d = s.order() > 1 ? "d^" + s.order() + "/d" + s.variable() + "^" + s.order() : "d/d" + s.variable();
                yield d + "[" + s.problem() + "]";
            }
            case LIMIT -> "lim[" + s.variable() + " -> " + s.extras().getOrDefault("a", Expr.ZERO)
                    + directionSuffixPlain(s) + "] " + s.problem();
            case INTEGRAL -> (isDefinite(s) ? "∫[" + s.extras().get("lower") + ", " + s.extras().get("upper") + "] " : "∫ ")
                    + s.problem() + " d" + s.variable();
        };
    }

    private static String resultSuffixPlain(Solution s) {
        return s.kind() == Solution.Kind.INTEGRAL && !isDefinite(s) ? " + C" : "";
    }

    private static String resultOperationLatex(Solution s) {
        String v = Expr.sym(s.variable()).toLatex();
        return switch (s.kind()) {
            case DERIVATIVE -> {
                String frac = s.order() > 1
                        ? "\\frac{d^{" + s.order() + "}}{d" + v + "^{" + s.order() + "}}"
                        : "\\frac{d}{d" + v + "}";
                yield frac + "\\left[" + s.problem().toLatex() + "\\right]";
            }
            case LIMIT -> {
                String sign = directionSuffixPlain(s).isEmpty() ? "" : "^{" + directionSuffixPlain(s) + "}";
                yield "\\lim_{" + v + " \\to " + s.extras().getOrDefault("a", Expr.ZERO).toLatex() + sign + "} "
                        + s.problem().toLatex();
            }
            case INTEGRAL -> "\\int" + (isDefinite(s)
                    ? "_{" + s.extras().get("lower").toLatex() + "}^{" + s.extras().get("upper").toLatex() + "}" : "")
                    + " " + s.problem().toLatex() + " \\, d" + v;
        };
    }

    private static String resultSuffixLatex(Solution s) {
        return s.kind() == Solution.Kind.INTEGRAL && !isDefinite(s) ? " + C" : "";
    }

    private static String segmentsPlain(List<Segment> segments) {
        StringBuilder sb = new StringBuilder();
        for (Segment segment : segments) {
            switch (segment) {
                case Segment.Text t -> sb.append(t.value());
                case Segment.Math m -> sb.append(m.expr());
            }
        }
        return sb.toString();
    }

    private static String segmentsMath(List<Segment> segments, boolean escapeForLatex) {
        StringBuilder sb = new StringBuilder();
        for (Segment segment : segments) {
            switch (segment) {
                case Segment.Text t -> sb.append(escapeForLatex ? escapeLatex(t.value()) : t.value());
                case Segment.Math m -> sb.append('$').append(m.expr().toLatex()).append('$');
            }
        }
        return sb.toString();
    }

    // ================================================================ Texto plano

    private static String plain(Solution s, Locale locale) {
        StringBuilder sb = new StringBuilder();
        sb.append(segmentsPlain(problemText(s, locale))).append('\n');

        if (!s.steps().isEmpty()) {
            sb.append('\n').append(Messages.get("solution.steps", locale)).append(":\n");
            plainSteps(sb, s.steps(), "", locale);
        }

        sb.append('\n');
        if (s.isSolved()) {
            sb.append(Messages.get("solution.result", locale)).append(": ")
                    .append(resultOperationPlain(s))
                    .append(" = ").append(s.result()).append(resultSuffixPlain(s)).append('\n');
        } else {
            sb.append(statusText(s, locale)).append('\n');
        }
        return sb.toString();
    }

    private static void plainSteps(StringBuilder sb, List<Step> steps, String number, Locale locale) {
        int depth = number.isEmpty() ? 0 : (int) number.chars().filter(c -> c == '.').count();
        String indent = "   ".repeat(depth);
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            String label = number.isEmpty() ? String.valueOf(i + 1) : number + (i + 1);

            sb.append(indent).append(label).append(". ").append(step.title(locale)).append('\n');
            sb.append(indent).append("   ").append(segmentsPlain(step.explanation(locale))).append('\n');
            String equation = equationPlain(step);
            if (equation != null) sb.append(indent).append("   ").append(equation).append('\n');

            plainSteps(sb, step.substeps(), label + ".", locale);
        }
    }

    // ================================================================ Markdown

    private static String markdown(Solution s, Locale locale) {
        StringBuilder sb = new StringBuilder();
        sb.append(segmentsMath(problemText(s, locale), false)).append("\n\n");

        if (!s.steps().isEmpty()) {
            sb.append("**").append(Messages.get("solution.steps", locale)).append("**\n\n");
            markdownSteps(sb, s.steps(), 0, locale);
        }

        sb.append('\n');
        if (s.isSolved()) {
            sb.append("**").append(Messages.get("solution.result", locale)).append("**\n\n$$")
                    .append(resultOperationLatex(s))
                    .append(" = ").append(s.result().toLatex()).append(resultSuffixLatex(s)).append("$$\n");
        } else {
            sb.append(statusText(s, locale)).append('\n');
        }
        return sb.toString();
    }

    private static void markdownSteps(StringBuilder sb, List<Step> steps, int depth, Locale locale) {
        String indent = "  ".repeat(depth);
        for (Step step : steps) {
            sb.append(indent).append("- **").append(step.title(locale)).append("**. ")
                    .append(segmentsMath(step.explanation(locale), false)).append('\n');
            String equation = equationLatex(step);
            if (equation != null) sb.append('\n').append(indent).append("  $$").append(equation).append("$$\n\n");
            markdownSteps(sb, step.substeps(), depth + 1, locale);
        }
    }

    // ================================================================ LaTeX

    private static String latex(Solution s, Locale locale) {
        StringBuilder sb = new StringBuilder();
        sb.append(segmentsMath(problemText(s, locale), true)).append("\n\n");

        if (!s.steps().isEmpty()) {
            sb.append("\\textbf{").append(escapeLatex(Messages.get("solution.steps", locale))).append("}\n");
            latexSteps(sb, s.steps(), locale);
        }

        sb.append('\n');
        if (s.isSolved()) {
            sb.append("\\textbf{").append(escapeLatex(Messages.get("solution.result", locale))).append("}\n\\[")
                    .append(resultOperationLatex(s))
                    .append(" = ").append(s.result().toLatex()).append(resultSuffixLatex(s)).append("\\]\n");
        } else {
            sb.append(escapeLatex(statusText(s, locale))).append('\n');
        }
        return sb.toString();
    }

    private static void latexSteps(StringBuilder sb, List<Step> steps, Locale locale) {
        if (steps.isEmpty()) return;
        sb.append("\\begin{enumerate}\n");
        for (Step step : steps) {
            sb.append("\\item \\textbf{").append(escapeLatex(step.title(locale))).append("}. ")
                    .append(segmentsMath(step.explanation(locale), true)).append('\n');
            String equation = equationLatex(step);
            if (equation != null) sb.append("\\[").append(equation).append("\\]\n");
            latexSteps(sb, step.substeps(), locale);
        }
        sb.append("\\end{enumerate}\n");
    }

    private static String escapeLatex(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\textbackslash{}");
                case '&', '%', '$', '#', '_', '{', '}' -> sb.append('\\').append(c);
                case '~' -> sb.append("\\textasciitilde{}");
                case '^' -> sb.append("\\textasciicircum{}");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    // ================================================================ JSON

    private static String json(Solution s, Locale locale) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"kind\":").append(quote(s.kind().name()));
        sb.append(",\"status\":").append(quote(s.status().name()));
        sb.append(",\"variable\":").append(quote(s.variable()));
        sb.append(",\"order\":").append(s.order());
        sb.append(",\"problem\":").append(jsonExpr(s.problem()));
        sb.append(",\"result\":").append(s.result() == null ? "null" : jsonExpr(s.result()));
        sb.append(",\"extras\":{");
        boolean first = true;
        for (Map.Entry<String, Expr> e : s.extras().entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(quote(e.getKey())).append(':').append(jsonExpr(e.getValue()));
        }
        sb.append("},\"steps\":[");
        jsonSteps(sb, s.steps(), locale);
        sb.append("]}");
        return sb.toString();
    }

    private static void jsonSteps(StringBuilder sb, List<Step> steps, Locale locale) {
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) sb.append(',');
            Step step = steps.get(i);
            sb.append("{\"rule\":").append(quote(step.rule().name()));
            sb.append(",\"title\":").append(quote(step.title(locale)));
            sb.append(",\"explanation\":[");
            List<Segment> segments = step.explanation(locale);
            for (int k = 0; k < segments.size(); k++) {
                if (k > 0) sb.append(',');
                switch (segments.get(k)) {
                    case Segment.Text t -> sb.append("{\"text\":").append(quote(t.value())).append('}');
                    case Segment.Math m -> sb.append("{\"math\":").append(jsonExpr(m.expr())).append('}');
                }
            }
            sb.append(']');
            sb.append(",\"before\":").append(step.before() == null ? "null" : jsonExpr(step.before()));
            sb.append(",\"after\":").append(step.after() == null ? "null" : jsonExpr(step.after()));
            sb.append(",\"params\":{");
            boolean first = true;
            for (Map.Entry<String, Expr> p : step.params().entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(quote(p.getKey())).append(':').append(jsonExpr(p.getValue()));
            }
            sb.append("},\"substeps\":[");
            jsonSteps(sb, step.substeps(), locale);
            sb.append("]}");
        }
    }

    private static String jsonExpr(Expr e) {
        return "{\"text\":" + quote(e.toString()) + ",\"latex\":" + quote(e.toLatex()) + "}";
    }

    private static String quote(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }
}
