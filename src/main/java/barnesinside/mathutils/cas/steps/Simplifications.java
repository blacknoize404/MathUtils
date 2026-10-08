package barnesinside.mathutils.cas.steps;

import barnesinside.mathutils.cas.expr.Expr;

import java.util.Arrays;

/**
 * Qué se hizo al simplificar: un «Simplificar» sin más no dice nada, así que se mira lo de antes y lo de después y se le da el nombre de
 * lo que cambió (quitar un cero, arreglar signos, reducir una fracción, agrupar términos, multiplicar números u ordenar factores).
 * Si no se reconoce, queda «Simplificar».
 */
final class Simplifications {

    private Simplifications() {
    }

    /** La regla que mejor describe el paso de {@code before} a {@code after}. */
    static Rule classify(Expr before, Expr after) {
        if (before == null || after == null) return Rule.SIMPLIFY;
        if (hasNeutral(before) && !hasNeutral(after)) return Rule.SIMPLIFY_NEUTRAL;
        String b = plain(before);
        String a = plain(after);
        if (negatives(before) > negatives(after) && (b.contains("(-") || b.contains("-(")) && !a.contains("(-")) return Rule.SIMPLIFY_SIGNS;
        if (denominators(before) > denominators(after)) return Rule.SIMPLIFY_FRACTION;
        if (before instanceof Expr.Add sum && !(after instanceof Expr.Add later && later.terms().size() >= sum.terms().size())) {
            return Rule.SIMPLIFY_COLLECT;
        }
        if (numbers(before) >= 2 && numbers(after) < numbers(before)) return Rule.SIMPLIFY_NUMBERS;
        if (sameLetters(b, a)) return Rule.SIMPLIFY_ORDER;
        return Rule.SIMPLIFY;
    }

    private static String plain(Expr e) {
        return e.toString().replace(" ", "");
    }

    /** Si hay un sumando 0, un factor 1 o un exponente 1 en cualquier parte. */
    private static boolean hasNeutral(Expr e) {
        return switch (e) {
            case Expr.Add sum -> sum.terms().stream().anyMatch(t -> t.isZero() || hasNeutral(t));
            case Expr.Mul product -> product.factors().stream().anyMatch(f -> f.isOne() || hasNeutral(f));
            case Expr.Pow p -> p.exponent().isOne() || hasNeutral(p.base()) || hasNeutral(p.exponent());
            case Expr.Func f -> hasNeutral(f.arg());
            default -> false;
        };
    }

    /** Cuántos factores numéricos negativos hay. */
    private static int negatives(Expr e) {
        return switch (e) {
            case Expr.Num n -> n.value().signum() < 0 ? 1 : 0;
            case Expr.Add sum -> sum.terms().stream().mapToInt(Simplifications::negatives).sum();
            case Expr.Mul product -> product.factors().stream().mapToInt(Simplifications::negatives).sum();
            case Expr.Pow p -> negatives(p.base());
            case Expr.Func f -> negatives(f.arg());
            default -> 0;
        };
    }

    /** Cuántas potencias con exponente negativo (partes de abajo de una fracción) hay. */
    private static int denominators(Expr e) {
        return switch (e) {
            case Expr.Pow p -> (p.exponent() instanceof Expr.Num n && n.value().signum() < 0 ? 1 : 0) + denominators(p.base());
            case Expr.Add sum -> sum.terms().stream().mapToInt(Simplifications::denominators).sum();
            case Expr.Mul product -> product.factors().stream().mapToInt(Simplifications::denominators).sum();
            case Expr.Func f -> denominators(f.arg());
            default -> 0;
        };
    }

    /** Cuántos números (distintos de 1 y -1) se multiplican en el nivel de arriba. */
    private static int numbers(Expr e) {
        if (e instanceof Expr.Mul product) {
            return (int) product.factors().stream().filter(f -> f instanceof Expr.Num n && !n.value().abs().equals(barnesinside.mathutils.cas.expr.Rational.ONE)).count()
                    + product.factors().stream().filter(f -> f instanceof Expr.Mul).mapToInt(Simplifications::numbers).sum();
        }
        return 0;
    }

    /** Si los dos textos tienen exactamente los mismos caracteres (solo cambia el orden en que se escribe). */
    private static boolean sameLetters(String a, String b) {
        char[] x = a.replaceAll("[()*]", "").toCharArray();
        char[] y = b.replaceAll("[()*]", "").toCharArray();
        Arrays.sort(x);
        Arrays.sort(y);
        return Arrays.equals(x, y);
    }
}
