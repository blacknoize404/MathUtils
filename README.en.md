# MathUtils

Java library for mathematical operations and algebraic expressions.

> **Main documentation is in Spanish**: [README.md](README.md)

## Features

- **Algebra**: Constants, variables, operators (+, -, *, /), functions (sin, cos, tan, log, sqrt, pow, etc.)
- **Vectors**: 2D and 3D vector operations
- **Geometry**: Points, lines, planes, curves
- **Statistics**: Weighted mean, combinatorics (factorial, combinations)
- **Series & Sequences**: Mathematical series and sequences
- **Angles**: Degree/radian conversions and operations

## Build

```bash
mvn compile
```

## Test

```bash
mvn test
```

## TODO

- [ ] **Integrals and Derivatives**
  - Symbolic differentiation
  - Numerical integration (Simpson's rule, trapezoidal rule, Gaussian quadrature)
  - Definite and indefinite integrals
  - Partial derivatives and gradient

- [ ] **Expression Parser (Shunting Yard Algorithm)**
  - Text-to-function parser: Convert string expressions (e.g., `"3 * x + sin(y)"`) to executable `Function` objects
  - Handle operator precedence and associativity
  - Support for functions, constants, and variables
  - Parentheses and nested expressions

- [ ] **Function-to-Text Converter**
  - Serialize `Function` objects back to readable string expressions
  - Pretty-printing with optional formatting (LaTeX, plain text)
  - Simplification before output (e.g., `x + 0` → `x`)

- [ ] **Linear Algebra**
  - Matrix operations (addition, multiplication, transpose, inverse, determinant)
  - Eigenvalues and eigenvectors
  - Linear system solvers (Gaussian elimination, LU decomposition)
  - Vector spaces, basis, dimension
  - Singular Value Decomposition (SVD)

- [ ] **Complex Numbers**
  - Complex arithmetic (add, sub, mult, div, pow, root)
  - Complex functions (exp, log, sin, cos, etc.)
  - Polar/rectangular conversion
  - Complex plane visualization helpers

- [ ] **Differential Equations**
  - ODE solvers (Euler, Runge-Kutta 4th order, adaptive step size)
  - Systems of ODEs
  - Boundary value problems

- [ ] **Numerical Analysis**
  - Root finding (Newton-Raphson, bisection, secant, Brent's method)
  - Interpolation (Lagrange, Newton, spline)
  - Numerical differentiation
  - Numerical integration (adaptive quadrature)

- [ ] **Multivariable Calculus**
  - Partial derivatives
  - Gradient, divergence, curl
  - Multiple integrals (double, triple)
  - Line and surface integrals
  - Jacobian and Hessian matrices

- [ ] **Probability & Statistics**
  - Probability distributions (normal, binomial, Poisson, exponential, etc.)
  - Random number generation
  - Hypothesis testing (t-test, chi-square, ANOVA)
  - Regression (linear, polynomial, logistic)
  - Descriptive statistics (mean, median, mode, variance, skewness, kurtosis)

- [ ] **Discrete Mathematics**
  - Number theory (primes, GCD/LCM, modular arithmetic, Euler's totient)
  - Graph theory (traversal, shortest path, MST, flow)
  - Combinatorics (permutations, combinations, partitions)
  - Recurrence relations

- [ ] **Optimization**
  - Gradient descent (and variants: momentum, Adam)
  - Linear programming (Simplex method)
  - Convex optimization
  - Constrained optimization (Lagrange multipliers)

- [ ] **Fourier Analysis & Signal Processing**
  - DFT / FFT implementation
  - Discrete cosine/sine transforms
  - Filtering (low-pass, high-pass, band-pass)
  - Convolution and correlation

- [ ] **Special Functions**
  - Gamma, Beta functions
  - Bessel functions
  - Error function (erf)
  - Hypergeometric functions
  - Elliptic integrals

- [ ] **Symbolic Algebra**
  - Expression simplification (combine like terms, factor, expand)
  - Pattern matching and rewrite rules
  - Algebraic identities
  - LaTeX export

- [ ] **Tensor Algebra**
  - N-dimensional tensors
  - Tensor operations (contraction, outer product)
  - Einstein notation support

## Project Structure

```
src/main/java/MathUtils/
├── algebra/           # Algebraic expressions and functions
│   ├── primitives/    # Base classes (Expression, MathElement, Operator)
│   ├── operators/     # Arithmetic operators
│   ├── trigonometrics/# Trigonometric functions
│   ├── exponentials/  # Powers, roots
│   ├── logarithmics/  # Logarithms
│   ├── Const.java     # Constants (PI, E, etc.)
│   ├── Var.java       # Variables
│   ├── Function.java  # Function abstraction
│   └── Evaluation.java# Expression evaluation
├── vector/            # Vector operations (2D, 3D)
├── point/             # Point geometry
├── line/              # Line geometry
├── combinatory/       # Factorials, combinations
├── statistics/        # Statistical functions
├── series/            # Mathematical series
├── sequences/         # Mathematical sequences
├── informatics/       # Number base conversions
└── exceptions/        # Custom exceptions
```

## License

CC-BY-NC-SA 4.0