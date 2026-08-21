# MathUtils

![Java Version](https://img.shields.io/badge/Java-25-orange.svg)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![Build](https://img.shields.io/badge/Build-passing-brightgreen.svg)

> MathUtils nació como un proyecto de estudio para vincular la matemática universitaria con la programación.

MathUtils es una librería de Java donde las expresiones algebraicas son objetos: se construyen como árboles evaluables, se evalúan de forma perezosa y se manipulan simbólicamente.

Java evalúa la matemática de forma inmediata. Entregas los valores y recibes un número, así que la fórmula en sí no se puede inspeccionar ni transformar. Mantener la expresión como un árbol lo permite, y es la base de las derivadas, límites e integrales con pasos del motor simbólico.

> **Documentación en inglés / English version**: [README.md](README.md)

> **MathUtils Studio**, la aplicación de escritorio construida sobre esta librería (solucionadores paso a paso,
> árboles de expresión, matrices, estadística y más), tiene su propio repositorio: [blacknoize404/MathUtils-Studio](https://github.com/blacknoize404/MathUtils-Studio).

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

## Motor simbólico

La librería no tiene dependencias de interfaz y necesita Java 21. La aplicación de escritorio que la usa vive en
[MathUtils Studio](https://github.com/blacknoize404/MathUtils-Studio).

**Solucionadores paso a paso** (`barnesinside.mathutils.cas.*`): cada resultado incluye la regla aplicada en
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
  `e^x/x`, `1/ln(x)` y `e^(-x²)`.
- **Varias variables:** `MultiDerivatives` calcula derivadas parciales sucesivas (mixtas), gradiente,
  hessiana, jacobiana, divergencia y laplaciano; `MultipleIntegrals.iterated` integra de dentro hacia fuera
  con límites interiores que dependen de las variables exteriores; `MultiLimits.limit` demuestra que un
  límite en un punto no existe (caminos distintos o límites iterados) o demuestra su valor (continuidad o una
  cota en polares) y, si no, solo ofrece un candidato, porque comprobar caminos no es una demostración.
- **Notación:** `2π`, `√(x²+1)`, `x⁻¹`, `30°`, `∞` y las letras griegas se aceptan en cualquier expresión.
- **Integrales definidas e impropias:** `Integrals.definite("e^(-x^2)", "x", "0", "oo")` aplica la regla
  de Barrow con límites en los extremos infinitos o singulares, parte el intervalo en las singularidades
  interiores, usa la simetría par e impar e informa cuando diverge. Cada valor exacto se contrasta con
  cuadratura numérica de doble exponencial, y el valor decimal acompaña a la solución. Cada resultado se
  verifica derivándolo, así que el motor responde "no resuelta" o "no elemental" en vez de devolver
  una antiderivada incorrecta.

### Usarla desde otro proyecto

La librería se publica con [JitPack](https://jitpack.io/#blacknoize404/BI.MathUtils) a partir de cada etiqueta de versión:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.blacknoize404</groupId>
  <artifactId>BI.MathUtils</artifactId>
  <version>v0.7.0</version>
</dependency>
```

Es un módulo Java llamado `barnesinside.mathutils` (`requires barnesinside.mathutils;`).

## Estructura del proyecto

```
src/main/java/barnesinside/mathutils/
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

## Agradecimientos

A mi profesor de Matemática Continua de la Universidad de Holguín, Alberto, y a mi tutora, Yisel Clavel Quintero, por su apoyo incondicional.

## Historial del proyecto

MathUtils empezó en 2023 como una librería personal llamada JavaMath, escrita sin control de versiones. Los primeros commits en git son de marzo de 2024, hechos en un repositorio de librerías personales y traídos aquí; el repositorio de MathUtils como tal se creó en junio de 2026.

## Licencia

Este proyecto se distribuye bajo la [Apache License 2.0](LICENSE). Puedes usarlo en proyectos académicos, de código abierto y comerciales. Las versiones publicadas antes del cambio de licencia (hasta `v0.7.0`) siguen disponibles con los términos con los que se publicaron.
