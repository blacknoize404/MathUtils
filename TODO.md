# TODO / Roadmap

---

## Cálculo (Integrales y Derivadas)
- [ ] Diferenciación simbólica (reglas de derivación, regla de la cadena)
- [ ] Integración numérica: regla del trapecio, Simpson 1/3, Simpson 3/8, cuadratura gaussiana
- [ ] Integrales definidas e indefinidas
- [ ] Derivadas parciales y gradiente
- [ ] Jacobiano y Hessiana

## Parser de Expresiones (Shunting Yard)
- [ ] Tokenizer (números, variables, funciones, operadores, paréntesis)
- [ ] Algoritmo Shunting Yard: infijo → postfijo (RPN)
- [ ] Constructor de árbol `Function` desde RPN
- [ ] Soporte: precedencia, asociatividad, funciones n-arias, constantes (PI, E), variables
- [ ] Manejo de errores con posiciones (línea/columna)

## Conversor Función-a-Texto
- [ ] Serialización a string legible
- [ ] Pretty-printing: texto plano, LaTeX, MathML
- [ ] Simplificación antes de salida (`x + 0` → `x`, `x * 1` → `x`, `x^1` → `x`)

## Álgebra Lineal
- [ ] Clase `Matrix` (genérica, mutable/inmutable)
- [ ] Operaciones: suma, resta, multiplicación, traspuesta, traza
- [ ] Determinante (expansión por cofactores, LU)
- [ ] Inversa (Gauss-Jordan, adjunta)
- [ ] Autovalores y autovectores (método QR, potencia)
- [ ] Solvers: Gauss, LU, Cholesky, QR
- [ ] SVD (descomposición en valores singulares)
- [ ] Espacios vectoriales: base, dimensión, rango, nulidad

## Números Complejos
- [ ] `Complex` (record o class inmutable)
- [ ] Aritmética: +, -, *, /, pow, root, exp, log
- [ ] Trigonométricas e hiperbólicas complejas
- [ ] Conversión rectangular ↔ polar
- [ ] Raíces n-ésimas, logaritmo complejo

## Ecuaciones Diferenciales
- [ ] ODE solver base (interfaz `ODESolver`)
- [ ] Euler, Heun, Punto medio, Runge-Kutta 4 (RK4)
- [ ] Paso adaptativo (RK45, Dormand-Prince)
- [ ] Sistemas de EDOs (vectoriales)
- [ ] Problemas de valores en la frontera (shooting, diferencias finitas)

## Análisis Numérico
- [ ] Búsqueda de raíces: bisección, Newton-Raphson, secante, Brent, Muller
- [ ] Interpolación: Lagrange, Newton (diferencias divididas), splines cúbicos
- [ ] Diferenciación numérica (diferencias finitas, Richardson)
- [ ] Cuadratura adaptativa (Gauss-Kronrod)

## Cálculo Multivariable
- [ ] Derivadas parciales (orden superior)
- [ ] Gradiente, divergencia, rotacional
- [ ] Integrales dobles/triples (iteradas, cambio de variables)
- [ ] Integrales de línea y superficie (teoremas de Green, Stokes, Gauss)
- [ ] Jacobiano y Hessiana (matrices, determinantes)

## Probabilidad y Estadística
- [ ] Distribuciones: Normal, Binomial, Poisson, Exponencial, Gamma, Beta, Chi-cuadrado, t-Student, F, Uniforme
- [ ] PDF, CDF, cuantiles, generación de muestras (Box-Muller, Ziggurat)
- [ ] Tests: t-test (1/2 muestras, pareado), chi-cuadrado (bondad, independencia), ANOVA (1/2 vías)
- [ ] Regresión: lineal simple/múltiple, polinomial, logística, ridge/LASSO
- [ ] Descriptivos: media, mediana, moda, varianza, desviación, asimetría, curtosis, percentiles

## Matemáticas Discretas
- [ ] Teoría de números: criba de Eratóstenes, factorización, MCD/MCM extendido, φ(n), congruencias, teorema chino del residuo
- [ ] Grafos: BFS/DFS, Dijkstra, A*, Floyd-Warshall, Kruskal/Prim (MST), Ford-Fulkerson (max flow)
- [ ] Combinatoria: permutaciones, combinaciones, particiones, números de Stirling, catalanes
- [ ] Relaciones de recurrencia: solución cerrada, función generadora

## Optimización
- [ ] Descenso de gradiente: batch, estocástico, mini-batch, momentum, Nesterov, Adam, RMSprop
- [ ] Programación lineal: Simplex (fase I/II), dualidad
- [ ] Optimización convexa: gradiente conjugado, Newton, interior point
- [ ] Restricciones: multiplicadores de Lagrange, KKT, penalty/barrier methods

## Análisis de Fourier y Procesamiento de Señales
- [ ] DFT / FFT (Cooley-Tukey, radix-2, mixed-radix)
- [ ] DCT (tipos I-IV), DST
- [ ] Filtros FIR/IIR: diseño (ventanas, Parks-McClellan), aplicación
- [ ] Convolución (lineal, circular), correlación, autocorrelación
- [ ] Espectrograma, STFT, wavelets (Haar, Daubechies)

## Funciones Especiales
- [ ] Gamma, log-Gamma, Beta, Beta incompleta
- [ ] Bessel (J, Y, I, K), esféricas, de Hankel
- [ ] Error function (erf, erfc, erfi, inversa)
- [ ] Hipergeométricas (₁F₁, ₂F₁, pFq)
- [ ] Integrales elípticas (F, E, Π de Legendre; formas de Carlson)

## Álgebra Simbólica
- [ ] Simplificador: reglas de reescritura, normalización
- [ ] Expandir: distributiva, binomio de Newton, multinomial
- [ ] Factorizar: común, diferencia de cuadrados, trinomios, agrupación
- [ ] Sustitución y pattern matching
- [ ] Exportación LaTeX / MathML / ASCIIMath

## Álgebra Tensorial
- [ ] Tensor N-dimensional (índices covariantes/contravariantes)
- [ ] Operaciones: contracción, producto tensorial, producto exterior, simetrización/antisimetrización
- [ ] Notación Einstein (suma implícita)
- [ ] Métrica, levantamiento/bajada de índices
