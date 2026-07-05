# TODO / Roadmap

---

## Cálculo (Integrales y Derivadas)
- [x] Diferenciación simbólica (reglas de derivación, regla de la cadena)
- [x] Integración numérica: regla del trapecio, Simpson 1/3, Simpson 3/8, cuadratura gaussiana (implementado: trapecio, Simpson 1/3, Simpson 3/8 y Gauss-Legendre de 5 puntos)
- [ ] Integrales definidas e indefinidas
- [x] Derivadas parciales y gradiente
- [x] Jacobiano y Hessiana

## Parser de Expresiones (Shunting Yard)
- [x] Tokenizer (números, variables, funciones, operadores, paréntesis)
- [x] Algoritmo Shunting Yard: infijo → postfijo (RPN)
- [x] Constructor de árbol `Function` desde RPN
- [x] Soporte: precedencia, asociatividad, funciones n-arias, constantes (PI, E), variables (funciones de aridad fija: sin, cos, tan, cot, sqrt, cbrt, ln, exp, log, root, pow)
- [x] Manejo de errores con posiciones (línea/columna)

## Conversor Función-a-Texto
- [x] Serialización a string legible
- [ ] Pretty-printing: texto plano y LaTeX (hechos), MathML (pendiente)
- [x] Simplificación antes de salida (`x + 0` → `x`, `x * 1` → `x`, `x^1` → `x`)

## Álgebra Lineal
- [x] Clase `Matrix` (genérica, mutable/inmutable) (inmutable)
- [x] Operaciones: suma, resta, multiplicación, traspuesta, traza
- [x] Determinante (expansión por cofactores, LU) (LU con pivoteo; cofactores pendiente)
- [x] Inversa (Gauss-Jordan, adjunta) (vía LU; Gauss-Jordan/adjunta pendiente)
- [ ] Autovalores y autovectores (método QR, potencia)
- [ ] Solvers: Gauss, LU (hecho), Cholesky, QR (pendiente)
- [ ] SVD (descomposición en valores singulares)
- [ ] Espacios vectoriales: base, dimensión, rango, nulidad

## Números Complejos
- [x] `Complex` (record o class inmutable)
- [x] Aritmética: +, -, *, /, pow, root, exp, log
- [x] Trigonométricas e hiperbólicas complejas (sin, cos, tan, sinh, cosh, tanh)
- [x] Conversión rectangular ↔ polar
- [x] Raíces n-ésimas, logaritmo complejo

## Ecuaciones Diferenciales
- [x] ODE solver base (interfaz `ODESolver`)
- [x] Euler, Heun, Punto medio, Runge-Kutta 4 (RK4)
- [x] Paso adaptativo (RK45, Dormand-Prince)
- [x] Sistemas de EDOs (vectoriales)
- [x] Problemas de valores en la frontera (shooting, diferencias finitas)

## Análisis Numérico
- [ ] Búsqueda de raíces: bisección, Newton-Raphson, secante (hecho), Brent, Muller (pendiente)
- [ ] Interpolación: Lagrange, Newton (diferencias divididas), splines cúbicos
- [x] Diferenciación numérica (diferencias finitas, Richardson)
- [ ] Cuadratura adaptativa (Gauss-Kronrod)

## Cálculo Multivariable
- [x] Derivadas parciales (orden superior)
- [ ] Gradiente (hecho), divergencia, rotacional (pendiente)
- [ ] Integrales dobles/triples (iteradas, cambio de variables)
- [ ] Integrales de línea y superficie (teoremas de Green, Stokes, Gauss)
- [x] Jacobiano y Hessiana (matrices, determinantes)

## Probabilidad y Estadística
- [ ] Distribuciones: Normal, Binomial, Poisson, Exponencial, Uniforme (hechas); Gamma, Beta, Chi-cuadrado, t-Student, F (pendientes)
- [ ] PDF, CDF, cuantiles, generación de muestras: Box-Muller y transformada inversa (hecho), Ziggurat (pendiente)
- [ ] Tests: t-test (1/2 muestras, pareado), chi-cuadrado (bondad, independencia), ANOVA (1/2 vías)
- [ ] Regresión: lineal simple/múltiple, polinomial, logística, ridge/LASSO
- [x] Descriptivos: media, mediana, moda, varianza, desviación, asimetría, curtosis, percentiles

## Matemáticas Discretas
- [x] Teoría de números: criba de Eratóstenes, factorización, MCD/MCM extendido, φ(n), congruencias, teorema chino del residuo
- [ ] Grafos: BFS/DFS, Dijkstra, A*, Floyd-Warshall, Kruskal/Prim (MST), Ford-Fulkerson (max flow)
- [x] Combinatoria: permutaciones, combinaciones, particiones, números de Stirling (2.ª especie), catalanes
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
- [ ] Error function: erf y erfc (hechas); erfi e inversa (pendientes)
- [ ] Hipergeométricas (₁F₁, ₂F₁, pFq)
- [ ] Integrales elípticas (F, E, Π de Legendre; formas de Carlson)

## Álgebra Simbólica
- [ ] Simplificador: reglas de reescritura, normalización
- [ ] Expandir: distributiva, binomio de Newton, multinomial
- [ ] Factorizar: común, diferencia de cuadrados, trinomios, agrupación
- [ ] Sustitución y pattern matching
- [ ] Exportación LaTeX (hecha), MathML / ASCIIMath (pendientes)

## Álgebra Tensorial
- [ ] Tensor N-dimensional (índices covariantes/contravariantes)
- [ ] Operaciones: contracción, producto tensorial, producto exterior, simetrización/antisimetrización
- [ ] Notación Einstein (suma implícita)
- [ ] Métrica, levantamiento/bajada de índices
