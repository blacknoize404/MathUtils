# MathUtils

Java library for mathematical computation with **algebraic expressions as first-class objects** and evaluable expression trees.

> **Main documentation in Spanish**: [README.md](README.md)

## Features

### 1. Typed, evaluable expression trees
Build formulas by chaining methods on `Function`, not with strings:
```java
Var x = new Var("x");
Function f = new Function(x).sin().pow(2).mult(3).add(new Cos(x)); // 3·sin²(x) + cos(x)
double y = f.eval(new Evaluation("x", Math.PI)); // → 3·0 + (-1) = -1
```
- **Lazy evaluation with variable substitution** via `Evaluation` (immutable record).
- **Structural equality** (`equalsTo`): two trees are equal if they have the same shape and operands, not just the same numeric value.
- **N-ary operators** (`SumOperator`, `MultOperator`, …): accept ≥2 operands, not just binary.

### 2. Ready-to-use elementary function catalog
`Const`, `Var`, `Sin`, `Cos`, `Tan`, `Cot`, `Log`, `LogN`, `Sqrt`, `Cbrt`, `Root`, `Pow`, `Fraction` — each is a `MathElement` and composes with operators.

### 3. 2D/3D vectors with documented mathematical contracts
Generic inheritance `Vector<V extends Vector<V>>` with:
- Addition, subtraction, dot product, cross product, norm, angle, unit vector, orthogonality.
- **Documentation with references to Stewart Calculus 7th ed.** (section and page) on every method.

### 4. Basic analytic geometry
`Point2D`, `Point3D`, `Line2D`, `Line3D`, `Plane`, `Space`, `Curve` — distance, intersection, projection operations.

### 5. Combinatorics and statistics
`Factorial`, `Combinatory` (binomial, multinomial), `WeightedMean`.

### 6. Series, sequences, and utilities
`Serie`, `Sequence`, base conversions (`Conversions`), angles (`Angle` with degrees/radians/gradians).

---

## What it does NOT include (explicit roadmap)

> These topics are **planned** but **not implemented**. See `TODO.md` for details.

- Calculus: symbolic derivatives, numeric/symbolic integrals, gradient, jacobian
- Shunting Yard parser (text → `Function`) and serializer (`Function` → text/LaTeX)
- Linear algebra: matrices, eigenvalues, SVD, linear solvers
- Complex numbers
- ODEs / numerical analysis (root finding, interpolation, adaptive quadrature)
- Advanced probability, optimization, Fourier transforms, special functions, tensor algebra

---

## Quick start

```bash
mvn compile
mvn test
```

```java
// Complete example
Var x = new Var("x"), y = new Var("y");
Function f = new Function(x).mult(y).add(new Sin(x)).div(new Const(2));
double r = f.eval(new Evaluation("x", 1.0), new Evaluation("y", 4.0));
// r = (1·4 + sin(1)) / 2 ≈ 2.4207
```

---

## Project structure

```
src/main/java/MathUtils/
├── algebra/
│   ├── primitives/     # MathElement, Expression, Operator (base)
│   ├── operators/      # Sum, Sub, Mult, Div (n-ary)
│   ├── trigonometrics/ # Sin, Cos, Tan, Cot
│   ├── exponentials/   # Pow, Root, Sqrt, Cbrt
│   ├── logarithmics/   # Log, LogN
│   ├── Const.java
│   ├── Var.java
│   ├── Function.java   # Fluent chaining + eval(Evaluation...)
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

## License

CC-BY-NC-SA 4.0

---

**Versión en español**: [README.md](README.en.md)