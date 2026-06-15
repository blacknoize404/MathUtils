# MathUtils

![Java Version](https://img.shields.io/badge/Java-25-orange.svg)
![License](https://img.shields.io/badge/License-CC--BY--NC--SA%204.0-lightgrey.svg)
![Build](https://img.shields.io/badge/Build-passing-brightgreen.svg)

> **Esta librería nació como un proyecto de estudio para vincular la matemática universitaria con la programación de infraestructura.**

¿Por qué estudiar matemáticas y programación por separado si puedes unificarlas? 

MathUtils resuelve un desafío concreto: conectar el cálculo avanzado y el álgebra lineal con el desarrollo de software modular.

En Java, la matemática tradicional se evalúa de forma estática. Tú entregas los valores, el entorno ejecuta la operación y devuelve un resultado numérico. Este enfoque funciona para operaciones simples. El esquema tradicional limita por completo la manipulación algebraica profunda.

Las ecuaciones necesitan una estructura jerárquica. Las expresiones algebraicas deben ser **objetos de primera clase**. MathUtils es la respuesta a ese desafío técnico.

> **Documentación en inglés / English version**: [README.en.md](README.en.md)

## Características Principales

### 1. Árboles de expresión tipados y evaluables
Tú construyes fórmulas matemáticas complejas encadenando métodos sobre la clase `Function`. Tu desarrollo prescinde por completo de cadenas de texto externas propensas a errores de sintaxis.
```java
Var x = new Var("x");
Function f = new Function(x).sin().pow(2).mult(3).add(new Cos(x)); // 3·sen²(x) + cos(x)
double y = f.eval(new Evaluation("x", Math.PI)); // → 3·0 + (-1) = -1
```
- **Evaluación perezosa con sustitución de variables:** el sistema calcula los valores exactos bajo demanda mediante el registro inmutable `Evaluation`.
- **Igualdad estructural** (`equalsTo`): dos árboles son iguales si comparten exactamente la misma forma y operandos. El sistema supera la comparación numérica tradicional.
- **Operadores n-arios:**: Estructuras como `SumOperator` y `MultOperator` admiten múltiples operandos sin encadenamientos binario artificiales.

### 2. Catálogo de funciones elementales
La librería expone componentes listos para producción: `Const`, `Var`, `Sin`, `Cos`, `Tan`, `Cot`, `Log`, `LogN`, `Sqrt`, `Cbrt`, `Root`, `Pow`, `Fraction` — cada uno es un `MathElement` y compone con los operadores.

### 3. Vectores 2D/3D con contratos matemáticos documentados
Herencia genérica `Vector<V extends Vector<V>>` con:
- Suma, resta, producto escalar, producto cruz, norma, ángulo, vector unitario, ortogonalidad.
- **Documentación con referencias a Stewart Cálculo 7.ª ed.** (sección y página) en cada método.

### 4. Geometría analítica
Los módulos `Point2D`, `Point3D`, `Line2D`, `Line3D`, `Plane`, `Space` y `Curve` ejecutan cálculos de distancia absoluta, intersección y proyección espacial.

### 5. Combinatoria y estadística
- Cálculos combinatorios mediante `Factorial` y `Combinatory` (modelos binomiales y multinomiales).
- Análisis de datos preciso con `WeightedMean`.

### 6. Series, sucesiones y utilidades
Gestión de progresiones mediante `Serie`, `Sequence`. Contiene utilidades de conversión de base numérica y administración de ángulos (`Angle` en grados, radianes y gradianes).

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
