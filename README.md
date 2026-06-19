# MathUtils

![Java Version](https://img.shields.io/badge/Java-25-orange.svg)
![License](https://img.shields.io/badge/License-CC--BY--NC--SA%204.0-lightgrey.svg)
![Build](https://img.shields.io/badge/Build-passing-brightgreen.svg)

> **This library was born as a study project to link university mathematics coursework with infrastructure programming.**

Why study math and programming separately when you can unify them?

MathUtils solves a concrete challenge: connecting advanced calculus and linear algebra with modular software development. It enables mathematical computation through **algebraic expressions as first-class objects** and evaluable expression trees.

In Java, traditional math is evaluated statically. You provide values, the environment executes the operation, and returns a numeric result. This works for simple operations. The traditional approach completely limits deep algebraic manipulation.

Equations need a hierarchical structure. Algebraic expressions must be **first-class objects**. MathUtils is the answer to that technical challenge.

> **Spanish documentation**: [README.md](README.es.md)

## Features

### 1. Typed, evaluable expression trees
You build complex mathematical formulas by chaining methods on the `Function` class. Your development completely avoids external string chains prone to syntax errors.
```java
Var x = new Var("x");
Function f = new Function(x).sin().pow(2).mult(3).add(new Cos(x)); // 3·sin²(x) + cos(x)
double y = f.eval(new Evaluation("x", Math.PI)); // → 3·0 + (-1) = -1
```
- **Lazy evaluation with variable substitution:** the system computes exact values on demand via the immutable `Evaluation` record.
- **Structural equality** (`equalsTo`): two trees are equal if they share exactly the same shape and operands. The system surpasses traditional numeric comparison.
- **N-ary operators:** Structures like `SumOperator` and `MultOperator` accept multiple operands without artificial binary chaining.

### 2. Elementary function catalog
The library exposes production-ready components: `Const`, `Var`, `Sin`, `Cos`, `Tan`, `Cot`, `Log`, `LogN`, `Sqrt`, `Cbrt`, `Root`, `Pow`, `Fraction` — each is a `MathElement` and composes with operators.

### 3. 2D/3D vectors with documented mathematical contracts
Generic inheritance `Vector<V extends Vector<V>>` with:
- Addition, subtraction, dot product, cross product, norm, angle, unit vector, orthogonality.
- **Documentation with references to Stewart Calculus 7th ed.** (section and page) on every method.

### 4. Analytic geometry
Modules `Point2D`, `Point3D`, `Line2D`, `Line3D`, `Plane`, `Space`, and `Curve` perform absolute distance, intersection, and spatial projection calculations.

### 5. Combinatorics and statistics
- Combinatorial calculations via `Factorial` and `Combinatory` (binomial and multinomial models).
- Precise data analysis with `WeightedMean`.

### 6. Series, sequences, and utilities
Progression management via `Serie`, `Sequence`. Contains numerical base conversion utilities and angle management (`Angle` in degrees, radians, and gradians).

---

## Quick Start

```bash
mvn compile
mvn test
```

---

## Integration Example

```java
Var x = new Var("x");
Var y = new Var("y");

// Hierarchical tree construction: (x · y + sin(x)) / 2
Function f = new Function(x).mult(y).add(new Sin(x)).div(new Const(2));

// Evaluation with immutable variable passing
double r = f.eval(new Evaluation("x", 1.0), new Evaluation("y", 4.0));
// r ≈ 2.4207
```

---

## Project Structure

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

## Roadmap

> These topics are **planned** but **not implemented**. See `TODO.md` for details.

- **Symbolic calculus:** Automatic derivatives, numeric/symbolic integrals, gradients, and Jacobian matrices.
- **Algebraic interpreter:** Shunting Yard algorithm to transform direct text expressions into `Function` objects and clean LaTeX export.
- **Advanced linear algebra:** Native support for matrices, SVD decomposition, eigenvalues, and linear system solvers.
- Complex numbers
- ODEs / numerical analysis (root finding, interpolation, adaptive quadrature)
- Advanced probability, optimization, Fourier transforms, special functions, tensor algebra

---

## How to Contribute

Contributions are welcome:

1. Fork the project.
2. Create a branch with a new feature (git checkout -b feature/NewFeature).
3. Ensure all tests pass (mvn test).
4. Write corresponding unit tests for your addition.
5. Submit a Pull Request detailing the changes.

---

## License

This project is distributed under the CC-BY-NC-SA 4.0 license. Your academic use keeps the code open and accessible to the scientific community.