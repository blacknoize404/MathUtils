"""Inserta en los README la sección del motor simbólico y de MathUtils Studio (idempotente)."""
import sys
from pathlib import Path

root = Path(sys.argv[1])

en = """## Symbolic engine and MathUtils Studio

The repository is a Maven multi-module build: `mathutils-core` (the reusable library, no UI
dependencies, Java 21) and `mathutils-app` (the JavaFX desktop app).

**Step-by-step solvers** (`MathUtils.cas.*`): every result comes with the rule applied at each
step, explained in Spanish and English and renderable as plain text, Markdown, LaTeX or JSON.

```java
Solution d = Derivatives.derivative("x^2*sin(x)", "x");
Solution l = Limits.limit("(1 + 1/x)^x", "x", "oo");            // e
Solution i = Integrals.indefinite("e^x*sin(x)", "x");           // (e^x (sin x - cos x))/2
i.render(Locale.ENGLISH, Format.LATEX);
```

- **Derivatives:** sum, product, quotient, power, chain, exponential, logarithmic, trigonometric,
  inverse, hyperbolic, partial and higher-order.
- **Limits:** direct substitution, polynomial ratios and dominant terms at infinity, L'Hôpital,
  `0·∞`, `∞−∞`, `1^∞`, one-sided limits. Results are checked numerically.
- **Integrals:** table, linearity, polynomial division, expansion, substitution and integration by
  parts (including cyclic cases). Every result is verified by differentiating it, so the engine
  reports "unsolved" or "non-elementary" instead of returning a wrong antiderivative.

**MathUtils Studio** (`mathutils-app`): calculator with exact and decimal results, interactive
expression tree, order of operations, and the derivative, limit and integral solvers, with the
barnesinside visual identity. Run it with:

```bash
mvn javafx:run -pl mathutils-app
```

"""

es = """## Motor simbólico y MathUtils Studio

El repositorio es un build Maven multi-módulo: `mathutils-core` (la librería reutilizable, sin
dependencias de interfaz, Java 21) y `mathutils-app` (la aplicación de escritorio JavaFX).

**Solucionadores paso a paso** (`MathUtils.cas.*`): cada resultado incluye la regla aplicada en
cada paso, explicada en español e inglés y renderizable como texto plano, Markdown, LaTeX o JSON.

```java
Solution d = Derivatives.derivative("x^2*sin(x)", "x");
Solution l = Limits.limit("(1 + 1/x)^x", "x", "oo");            // e
Solution i = Integrals.indefinite("e^x*sin(x)", "x");           // (e^x (sin x - cos x))/2
i.render(Locale.forLanguageTag("es"), Format.LATEX);
```

- **Derivadas:** suma, producto, cociente, potencia, cadena, exponencial, logarítmica,
  trigonométricas e inversas, hiperbólicas, parciales y de orden superior.
- **Límites:** sustitución directa, cocientes de polinomios y términos dominantes en el infinito,
  L'Hôpital, `0·∞`, `∞−∞`, `1^∞` y límites laterales. Los resultados se comprueban numéricamente.
- **Integrales:** tabla, linealidad, división de polinomios, expansión, sustitución e integración
  por partes (incluidos los casos cíclicos). Cada resultado se verifica derivándolo, así que el
  motor responde "no resuelta" o "no elemental" en vez de devolver una antiderivada incorrecta.

**MathUtils Studio** (`mathutils-app`): calculadora con resultado exacto y decimal, árbol de
expresión interactivo, orden de operaciones y los solucionadores de derivadas, límites e
integrales, con la identidad visual de barnesinside. Para ejecutarla:

```bash
mvn javafx:run -pl mathutils-app
```

"""

for name, section, anchor in (("README.md", en, "## Project Structure"), ("README.es.md", es, "## Estructura del proyecto")):
    path = root / name
    text = path.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in text else "\n"
    text = text.replace("\r\n", "\n")
    if "mathutils-core" in text:
        continue
    assert anchor in text, anchor
    text = text.replace(anchor, section + anchor, 1)
    path.write_bytes(text.replace("\n", newline).encode("utf-8"))

todo = root / "TODO.md"
text = todo.read_bytes().decode("utf-8")
newline = "\r\n" if "\r\n" in text else "\n"
text = text.replace("\r\n", "\n")
text = text.replace("- [ ] Integrales definidas e indefinidas",
                    "- [ ] Integrales definidas e indefinidas (indefinidas por tabla, sustitución y partes: hecho; definidas, impropias, fracciones parciales y trigonométricas: pendiente)")
text = text.replace("- [ ] Simplificador: reglas de reescritura, normalización",
                    "- [x] Simplificador: reglas de reescritura, normalización (núcleo exacto en `cas.expr`)")
todo.write_bytes(text.replace("\n", newline).encode("utf-8"))
print("ok")
