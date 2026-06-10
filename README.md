# MathUtils

Librería Java para operaciones matemáticas y expresiones algebraicas.

## Características

- **Álgebra**: Constantes, variables, operadores (+, -, *, /), funciones (sen, cos, tan, log, sqrt, pow, etc.)
- **Vectores**: Operaciones con vectores 2D y 3D
- **Geometría**: Puntos, líneas, planos, curvas
- **Estadísticas**: Media ponderada, combinatoria (factorial, combinaciones)
- **Series y Sucesiones**: Series y sucesiones matemáticas
- **Ángulos**: Conversiones grado/radian y operaciones

## Compilar

```bash
mvn compile
```

## Tests

```bash
mvn test
```

## TODO

- [ ] **Integrales y Derivadas**
  - Diferenciación simbólica
  - Integración numérica (regla de Simpson, regla del trapecio, cuadratura gaussiana)
  - Integrales definidas e indefinidas
  - Derivadas parciales y gradiente

- [ ] **Parser de Expresiones (Algoritmo Shunting Yard)**
  - Parser texto-a-función: Convertir expresiones en string (ej. `"3 * x + sen(y)"`) a objetos `Function` ejecutables
  - Manejo de precedencia y asociatividad de operadores
  - Soporte para funciones, constantes y variables
  - Paréntesis y expresiones anidadas

- [ ] **Conversor Función-a-Texto**
  - Serializar objetos `Function` a expresiones string legibles
  - Pretty-printing con formato opcional (LaTeX, texto plano)
  - Simplificación antes de salida (ej. `x + 0` → `x`)

- [ ] **Álgebra Lineal**
  - Operaciones con matrices (suma, multiplicación, traspuesta, inversa, determinante)
  - Autovalores y autovectores
  - Solvers de sistemas lineales (eliminación gaussiana, descomposición LU)
  - Espacios vectoriales, base, dimensión
  - Descomposición en Valores Singulares (SVD)

- [ ] **Números Complejos**
  - Aritmética compleja (suma, resta, mult, div, pot, raíz)
  - Funciones complejas (exp, log, sen, cos, etc.)
  - Conversión polar/rectangular
  - Helpers para visualización en plano complejo

- [ ] **Ecuaciones Diferenciales**
  - Solvers ODE (Euler, Runge-Kutta 4º orden, paso adaptativo)
  - Sistemas de EDOs
  - Problemas de valores en la frontera

- [ ] **Análisis Numérico**
  - Búsqueda de raíces (Newton-Raphson, bisección, secante, método de Brent)
  - Interpolación (Lagrange, Newton, splines)
  - Diferenciación numérica
  - Integración numérica (cuadratura adaptativa)

- [ ] **Cálculo Multivariable**
  - Derivadas parciales
  - Gradiente, divergencia, rotacional
  - Integrales múltiples (doble, triple)
  - Integrales de línea y superficie
  - Matrices Jacobiana y Hessiana

- [ ] **Probabilidad y Estadística**
  - Distribuciones de probabilidad (normal, binomial, Poisson, exponencial, etc.)
  - Generación de números aleatorios
  - Tests de hipótesis (t-test, chi-cuadrado, ANOVA)
  - Regresión (lineal, polinomial, logística)
  - Estadísticos descriptivos (media, mediana, moda, varianza, asimetría, curtosis)

- [ ] **Matemáticas Discretas**
  - Teoría de números (primos, MCD/MCM, aritmética modular, función φ de Euler)
  - Teoría de grafos (recorrido, camino más corto, MST, flujo)
  - Combinatoria (permutaciones, combinaciones, particiones)
  - Relaciones de recurrencia

- [ ] **Optimización**
  - Descenso de gradiente (y variantes: momentum, Adam)
  - Programación lineal (método Simplex)
  - Optimización convexa
  - Optimización con restricciones (multiplicadores de Lagrange)

- [ ] **Análisis de Fourier y Procesamiento de Señales**
  - Implementación DFT / FFT
  - Transformadas discretas de coseno/seno
  - Filtrado (paso bajo, paso alto, paso banda)
  - Convolución y correlación

- [ ] **Funciones Especiales**
  - Funciones Gamma, Beta
  - Funciones de Bessel
  - Función de error (erf)
  - Funciones hipergeométricas
  - Integrales elípticas

- [ ] **Álgebra Simbólica**
  - Simplificación de expresiones (combinar términos semejantes, factorizar, expandir)
  - Pattern matching y reglas de reescritura
  - Identidades algebraicas
  - Exportación a LaTeX

- [ ] **Álgebra Tensorial**
  - Tensores N-dimensionales
  - Operaciones tensoriales (contracción, producto exterior)
  - Soporte notación Einstein

## Estructura del Proyecto

```
src/main/java/MathUtils/
├── algebra/           # Expresiones y funciones algebraicas
│   ├── primitives/    # Clases base (Expression, MathElement, Operator)
│   ├── operators/     # Operadores aritméticos
│   ├── trigonometrics/# Funciones trigonométricas
│   ├── exponentials/  # Potencias, raíces
│   ├── logarithmics/  # Logaritmos
│   ├── Const.java     # Constantes (PI, E, etc.)
│   ├── Var.java       # Variables
│   ├── Function.java  # Abstracción de función
│   └── Evaluation.java# Evaluación de expresiones
├── vector/            # Operaciones vectoriales (2D, 3D)
├── point/             # Geometría de puntos
├── line/              # Geometría de líneas
├── combinatory/       # Factoriales, combinaciones
├── statistics/        # Funciones estadísticas
├── series/            # Series matemáticas
├── sequences/         # Sucesiones matemáticas
├── informatics/       # Conversiones de base numérica
└── exceptions/        # Excepciones personalizadas
```

## Licencia

CC-BY-NC-SA 4.0

---

**Versión en inglés**: [README.en.md](README.en.md)