# MathUtils

![Java Version](https://img.shields.io/badge/Java-25-orange.svg)
![License](https://img.shields.io/badge/License-CC--BY--NC--SA%204.0-lightgrey.svg)
![Build](https://img.shields.io/badge/Build-passing-brightgreen.svg)

> MathUtils nació como un proyecto de estudio para vincular la matemática universitaria con la programación.

MathUtils es una librería de Java donde las expresiones algebraicas son objetos: se construyen como árboles evaluables, se evalúan de forma perezosa y se manipulan simbólicamente.

Java evalúa la matemática de forma inmediata. Entregas los valores y recibes un número, así que la fórmula en sí no se puede inspeccionar ni transformar. Mantener la expresión como un árbol lo permite, y es la base de las derivadas, límites e integrales con pasos del motor simbólico.

> **Documentación en inglés / English version**: [README.md](README.md)

## Características Principales

### 1. Árboles de expresión tipados y evaluables
Las fórmulas se construyen encadenando métodos de la clase `Function`, así que no hay cadenas de texto que escribir mal.
```java
Var x = new Var("x");
Function f = new Function(x).sin().pow(2).mult(3).add(new Cos(x)); // 3·sen²(x) + cos(x)
double y = f.eval(new Evaluation("x", Math.PI)); // → 3·0 + (-1) = -1
```
- **Evaluación perezosa con sustitución de variables:** los valores se calculan bajo demanda a partir del registro inmutable `Evaluation`.
- **Igualdad estructural** (`equalsTo`): dos árboles son iguales si tienen exactamente la misma forma y operandos, lo que es más estricto que comparar valores numéricos.
- **Operadores n-arios:** estructuras como `SumOperator` y `MultOperator` admiten múltiples operandos sin encadenamientos binarios artificiales.

### 2. Catálogo de funciones elementales
La librería incluye `Const`, `Var`, `Sin`, `Cos`, `Tan`, `Cot`, `Log`, `LogN`, `Sqrt`, `Cbrt`, `Root`, `Pow`, `Fraction`; cada uno es un `MathElement` y compone con los operadores.

### 3. Vectores 2D/3D con contratos matemáticos documentados
Herencia genérica `Vector<V extends Vector<V>>` con:
- Suma, resta, producto escalar, producto cruz, norma, ángulo, vector unitario, ortogonalidad.
- **Documentación con referencias a Stewart Cálculo 7.ª ed.** (sección y página) en cada método.

### 4. Geometría analítica
Los módulos `Point2D`, `Point3D`, `Line2D`, `Line3D`, `Plane`, `Space` y `Curve` ejecutan cálculos de distancia absoluta, intersección y proyección espacial.

### 5. Combinatoria y estadística
- Cálculos combinatorios mediante `Factorial` y `Combinatory` (modelos binomiales y multinomiales).
- Media ponderada con `WeightedMean`.

### 6. Series, sucesiones y utilidades
Progresiones con `Serie` y `Sequence`. También utilidades de conversión de base numérica y administración de ángulos (`Angle` en grados, radianes y gradianes).

---

## Inicio rápido

```bash
mvn compile
mvn test
```

---

## Ejemplo de integración

```Java
Var x = new Var("x");
Var y = new Var("y");

// Construcción del árbol jerárquico: (x · y + sen(x)) / 2
Function f = new Function(x).mult(y).add(new Sin(x)).div(new Const(2));

// Evaluación con paso de variables inmutables
double r = f.eval(new Evaluation("x", 1.0), new Evaluation("y", 4.0));
// r ≈ 2.4207
```

---

## Motor simbólico y MathUtils Studio

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
  por partes (incluidos los casos cíclicos); fracciones parciales con fórmulas de reducción;
  potencias y productos de funciones trigonométricas, ángulo mitad y `tan(x/2)`; raíces de
  expresiones lineales, completar cuadrados y sustituciones trigonométricas; funciones especiales
  (`erf`, `Si`, `Ci`, `Ei`, `li`) para las integrales no elementales clásicas como `sin(x)/x`,
  `e^x/x`, `1/ln(x)` y `e^(-x²)`. Cada resultado se
  verifica derivándolo, así que el motor responde "no resuelta" o "no elemental" en vez de devolver
  una antiderivada incorrecta.

**MathUtils Studio** (`mathutils-app`): calculadora con resultado exacto y decimal, árbol de
expresión interactivo, orden de operaciones y los solucionadores de derivadas, límites e
integrales, con la identidad visual de barnesinside. Para ejecutarla:

```bash
mvn javafx:run -pl mathutils-app
```

## Estructura del proyecto

```
src/main/java/MathUtils/
├── algebra/
│   ├── primitives/     # MathElement, Expression, Operator (base)
│   ├── operators/      # Sum, Sub, Mult, Div (n-arios)
│   ├── trigonometrics/ # Sin, Cos, Tan, Cot
│   ├── exponentials/   # Pow, Root, Sqrt, Cbrt
│   ├── logarithmics/   # Log, LogN
│   ├── Const.java
│   ├── Var.java
│   ├── Function.java   # Flujo encadenado + eval(Evaluation...)
│   └── Evaluation.java # record(var, value)
├── vector/             # Vector, Vector2D, Vector3D
├── point/              # Point, Point2D, Point3D
├── line/               # Line, Line2D, Line3D, Plane, Space, Curve
├── combinatory/        # Factorial, Combinatory
├── statistics/         # WeightedMean
├── series/             # Serie
├── sequences/          # Sequence
├── informatics/        # Conversions
├── Angle.java
└── exceptions/
```

---

## Roadmap

> Estos temas están **planificados** pero **no implementados**. Ver `TODO.md` para detalle.

- **Cálculo simbólico:** Derivadas automáticas, integrales numéricas, gradientes y matrices jacobianas.
- **Intérprete algebraico:** Algoritmo Shunting Yard para transformar expresiones de texto directo a objetos Function y exportación limpia a LaTeX.
- **Álgebra lineal avanzada:** Soporte nativo para matrices, descomposición SVD, autovalores y resolutores de sistemas lineales.
- Números complejos
- EDOs / análisis numérico (raíces, interpolación, cuadratura adaptativa)
- Probabilidad avanzada, optimización, transformadas de Fourier, funciones especiales, álgebra tensorial

---

## Cómo contribuir
Las contribuciones son bienvenidas:

1. Haz un Fork del proyecto.
2. Crea una rama con una nueva característica (git checkout -b feature/NuevaCaracteristica).
3. Asegúrate de que todas las pruebas pasen (mvn test).
4. Escribe las pruebas unitarias correspondientes para tu adición.
5. Envía un Pull Request detallando los cambios.

---

## Licencia

Este proyecto se distribuye bajo la licencia CC-BY-NC-SA 4.0. Tu uso académico mantiene el código abierto y accesible para la comunidad científica.
