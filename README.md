# MathUtils

![Java Version](https://img.shields.io/badge/Java-25-orange.svg)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![Build](https://img.shields.io/badge/Build-passing-brightgreen.svg)

> MathUtils started as a study project to link university mathematics with programming.

MathUtils is a Java library where algebraic expressions are objects: you build them as evaluable trees, evaluate them lazily and manipulate them symbolically.

Plain Java evaluates math eagerly. You pass values in and get a number out, so the formula itself cannot be inspected or transformed. Keeping the expression as a tree makes that possible, and it is the base for the derivatives, limits and integrals with steps of the symbolic engine.

> **Spanish documentation**: [README.md](README.es.md)

> **MathUtils Studio**, the desktop app built on this library (step-by-step solvers, expression trees, matrices,
> statistics and more), has its own repository: [blacknoize404/MathUtils-Studio](https://github.com/blacknoize404/MathUtils-Studio).

## Features

### 1. Typed, evaluable expression trees
You build formulas by chaining methods on the `Function` class, so there are no strings to get wrong.
```java
Var x = new Var("x");
Function f = new Function(x).sin().pow(2).mult(3).add(new Cos(x)); // 3·sin²(x) + cos(x)
double y = f.eval(new Evaluation("x", Math.PI)); // → 3·0 + (-1) = -1
```
- **Lazy evaluation with variable substitution:** values are computed on demand from the immutable `Evaluation` record.
- **Structural equality** (`equalsTo`): two trees are equal if they have exactly the same shape and operands, which is stricter than comparing numeric values.
- **N-ary operators:** Structures like `SumOperator` and `MultOperator` accept multiple operands without artificial binary chaining.

### 2. Elementary function catalog
The library includes `Const`, `Var`, `Sin`, `Cos`, `Tan`, `Cot`, `Log`, `LogN`, `Sqrt`, `Cbrt`, `Root`, `Pow`, `Fraction`; each is a `MathElement` and composes with operators.

### 3. 2D/3D vectors with documented mathematical contracts
Generic inheritance `Vector<V extends Vector<V>>` with:
- Addition, subtraction, dot product, cross product, norm, angle, unit vector, orthogonality.
- **Documentation with references to Stewart Calculus 7th ed.** (section and page) on every method.

### 4. Analytic geometry
Modules `Point2D`, `Point3D`, `Line2D`, `Line3D`, `Plane`, `Space`, and `Curve` perform absolute distance, intersection, and spatial projection calculations.

### 5. Combinatorics and statistics
- Combinatorial calculations via `Factorial` and `Combinatory` (binomial and multinomial models).
- Weighted mean with `WeightedMean`.

### 6. Series, sequences, and utilities
Progressions with `Serie` and `Sequence`. Also numerical base conversion utilities and angle management (`Angle` in degrees, radians, and gradians).

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

## Symbolic engine

The desktop app that uses it lives in
[MathUtils Studio](https://github.com/blacknoize404/MathUtils-Studio).

**Step-by-step solvers** (`barnesinside.mathutils.cas.*`): every result comes with the rule applied at each
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
  parts (including cyclic cases); partial fractions with reduction formulas; powers and products of
  trigonometric functions, half angles and `tan(x/2)`; roots of linear expressions, completing the
  square and trigonometric substitutions; special functions (`erf`, `Si`, `Ci`, `Ei`, `li`) for the
  classic non-elementary integrals such as `sin(x)/x`, `e^x/x`, `1/ln(x)` and `e^(-x²)`.
- **Several variables:** `MultiDerivatives` computes successive (mixed) partial derivatives, gradient,
  Hessian, Jacobian, divergence and Laplacian; `MultipleIntegrals.iterated` integrates from the inside out
  with inner limits that depend on the outer variables; `MultiLimits.limit` proves that a limit at a point
  does not exist (different paths or iterated limits) or proves its value (continuity or a polar bound), and
  otherwise only offers a candidate, because checking paths is not a proof.
- **Symbolic algebra:** `Expand.expand` applies the distributive property, the binomial theorem and the
  multinomial theorem; `Factor.factor` pulls out common factors and uses rational roots, differences of
  squares, sums and differences of cubes, perfect squares and grouping (over the rationals, so `x² − 2`
  stays as it is); `Pattern` finds and rewrites subexpressions with wildcards, for example
  `sin(a)² + cos(a)²` → `1`. Expressions export to LaTeX, MathML and ASCIIMath
  (`toLatex()`, `toMathML()`, `toAsciiMath()`).
- **Equations and inequalities:** `Equations.solve("x^2 - 5x + 6 = 0")` and `Inequalities.solve("(x - 1)/(x + 2) >= 0")`
  explain every step and answer with a `SolutionSet` (values, intervals or periodic families). They cover first and second
  degree, polynomials (rational roots, substitution), fractions, radicals, absolute value, exponentials, logarithms and
  trigonometry, with a numerical search when there is no formula. `Equations.applicableMethods` lists the methods that work
  (`FACTORING`, `COMPLETE_SQUARE`, `QUADRATIC_FORMULA`, `RATIONAL_ROOTS`...) and `solve` takes the one you pick.
  `Factoring.factor(expr, FactorMethod.TRINOMIAL)` does the same for polynomials (common factor, difference of squares,
  trinomials, grouping, synthetic division...), and `Derivatives.applicableMethods` and `Integrals.applicableMethods` list
  the ways to approach a derivative or an integral.
- **Notation:** `2π`, `√(x²+1)`, `x⁻¹`, `30°`, `∞` and Greek letters are accepted anywhere an expression is.
- **Definite and improper integrals:** `Integrals.definite("e^(-x^2)", "x", "0", "oo")` applies the
  fundamental theorem with limits at infinite or singular endpoints, splits at interior singularities,
  uses odd/even symmetry and reports divergence. Every exact value is checked against double-exponential
  numerical quadrature, and the decimal value comes with the solution. Every result is verified by differentiating it, so the
  engine reports "unsolved" or "non-elementary" instead of returning a wrong antiderivative.

### Using it from another project

The library is published through [JitPack](https://jitpack.io/#blacknoize404/BI.MathUtils) from each release tag:

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

It is a Java module named `barnesinside.mathutils` (`requires barnesinside.mathutils;`).

## Project Structure

```
src/main/java/barnesinside/mathutils/
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

> Derivatives, integrals, limits, the expression parser, matrices, complex numbers and ODE solvers are already implemented. What is still missing is listed in `TODO.md`.

- Eigenvalues, eigenvectors, SVD and vector spaces
- Interpolation, adaptive quadrature and more root-finding methods
- Line and surface integrals
- More probability distributions, hypothesis tests and regression
- Graph algorithms
- Optimization, Fourier transforms and signal processing
- Gamma, Bessel and other special functions
- Tensor algebra

---

## How to Contribute

Contributions are welcome:

1. Fork the project.
2. Create a branch with a new feature (git checkout -b feature/NewFeature).
3. Ensure all tests pass (mvn test).
4. Write corresponding unit tests for your addition.
5. Submit a Pull Request detailing the changes.

---

## Acknowledgements

To my Continuous Mathematics professor at the University of Holguín, Alberto, and to my tutor, Yisel Clavel Quintero, for their unconditional support.

## Project history

MathUtils started in 2023 as a personal library called JavaMath, written without version control. The first commits in git are from March 2024, made in a repository of personal libraries and brought here; the repository of MathUtils itself was created in June 2026.

## License

This project is distributed under the [Apache License 2.0](LICENSE). You can use it in academic, open source and commercial projects. Versions published before the change of license (up to `v0.7.0`) remain available under the terms they were released with.