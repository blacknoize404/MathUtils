# MathUtils

Librería Java para computación matemática con **expresiones algebraicas como objetos de primera clase** y árboles de expresión evaluables.

> **Documentación principal en inglés**: [README.en.md](README.en.md)
> 
## Características

### 1. Árboles de expresión tipados y evaluables
Construyes fórmulas encadenando métodos sobre `Function`, no con strings:
```java
Var x = new Var("x");
Function f = new Function(x).sin().pow(2).mult(3).add(new Cos(x)); // 3·sen²(x) + cos(x)
double y = f.eval(new Evaluation("x", Math.PI)); // → 3·0 + (-1) = -1
```
- **Evaluación perezosa con sustitución de variables** vía `Evaluation` (record inmutable).
- **Igualdad estructural** (`equalsTo`): dos árboles son iguales si tienen la misma forma y operandos, no solo el mismo valor numérico.
- **Operadores n-arios** (`SumOperator`, `MultOperator`, …): aceptan ≥2 operandos, no solo binarios.

### 2. Catálogo de funciones elementales listo para usar
`Const`, `Var`, `Sin`, `Cos`, `Tan`, `Cot`, `Log`, `LogN`, `Sqrt`, `Cbrt`, `Root`, `Pow`, `Fraction` — cada uno es un `MathElement` y compone con los operadores.

### 3. Vectores 2D/3D con contratos matemáticos documentados
Herencia genérica `Vector<V extends Vector<V>>` con:
- Suma, resta, producto escalar, producto cruz, norma, ángulo, vector unitario, ortogonalidad.
- **Documentación con referencias a Stewart Cálculo 7.ª ed.** (sección y página) en cada método.

### 4. Geometría analítica básica
`Point2D`, `Point3D`, `Line2D`, `Line3D`, `Plane`, `Space`, `Curve` — operaciones de distancia, intersección, proyección.

### 5. Combinatoria y estadística
`Factorial`, `Combinatory` (binomial, multinomial), `WeightedMean`.

### 6. Series, sucesiones y utilidades
`Serie`, `Sequence`, conversiones de base (`Conversions`), ángulos (`Angle` con grados/radianes/gradianes).

---

## Qué NO trae (roadmap explícito)

> Estos temas están **planificados** pero **no implementados**. Ver `TODO.md` para detalle.

- Cálculo: derivadas simbólicas, integrales numéricas/simbólicas, gradiente, jacobiano
- Parser Shunting Yard (texto → `Function`) y serializador (`Function` → texto/LaTeX)
- Álgebra lineal: matrices, autovalores, SVD, solvers lineales
- Números complejos
- EDOs / análisis numérico (raíces, interpolación, cuadratura adaptativa)
- Probabilidad avanzada, optimización, transformadas de Fourier, funciones especiales, álgebra tensorial

---

## Inicio rápido

```bash
mvn compile
mvn test
```

```java
// Ejemplo completo
Var x = new Var("x"), y = new Var("y");
Function f = new Function(x).mult(y).add(new Sin(x)).div(new Const(2));
double r = f.eval(new Evaluation("x", 1.0), new Evaluation("y", 4.0));
// r = (1·4 + sen(1)) / 2 ≈ 2.4207
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

## Licencia

CC-BY-NC-SA 4.0

---

**English version**: [README.en.md](README.en.md)