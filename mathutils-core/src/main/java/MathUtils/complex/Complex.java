package MathUtils.complex;

/**
 * Número complejo inmutable en forma rectangular {@code re + im·i}.
 * <p>Todas las operaciones devuelven una nueva instancia.
 */
public final class Complex {

    public static final Complex ZERO = new Complex(0, 0);
    public static final Complex ONE = new Complex(1, 0);
    public static final Complex I = new Complex(0, 1);

    private final double re;
    private final double im;

    public Complex(double re, double im) {
        this.re = re;
        this.im = im;
    }

    public static Complex of(double re, double im) {
        return new Complex(re, im);
    }

    public static Complex real(double re) {
        return new Complex(re, 0);
    }

    /**
     * Construye un complejo a partir de su forma polar.
     * @param modulus módulo (radio)
     * @param argument argumento en radianes
     */
    public static Complex polar(double modulus, double argument) {
        return new Complex(modulus * Math.cos(argument), modulus * Math.sin(argument));
    }

    public double re() {
        return re;
    }

    public double im() {
        return im;
    }

    /**
     * @return el módulo |z|
     */
    public double abs() {
        return Math.hypot(re, im);
    }

    /**
     * @return el argumento principal en el intervalo (-π, π]
     */
    public double arg() {
        return Math.atan2(im, re);
    }

    public boolean isZero() {
        return re == 0 && im == 0;
    }

    public Complex conjugate() {
        return new Complex(re, -im);
    }

    public Complex negate() {
        return new Complex(-re, -im);
    }

    public Complex add(Complex o) {
        return new Complex(re + o.re, im + o.im);
    }

    public Complex subtract(Complex o) {
        return new Complex(re - o.re, im - o.im);
    }

    public Complex multiply(Complex o) {
        return new Complex(re * o.re - im * o.im, re * o.im + im * o.re);
    }

    public Complex multiply(double scalar) {
        return new Complex(re * scalar, im * scalar);
    }

    /**
     * @throws ArithmeticException si el divisor es cero
     */
    public Complex divide(Complex o) {
        double denominator = o.re * o.re + o.im * o.im;
        if (denominator == 0) throw new ArithmeticException("La división por 0 no está definida");
        return new Complex(
                (re * o.re + im * o.im) / denominator,
                (im * o.re - re * o.im) / denominator
        );
    }

    public Complex divide(double scalar) {
        if (scalar == 0) throw new ArithmeticException("La división por 0 no está definida");
        return new Complex(re / scalar, im / scalar);
    }

    public Complex reciprocal() {
        return ONE.divide(this);
    }

    public Complex exp() {
        double scale = Math.exp(re);
        return new Complex(scale * Math.cos(im), scale * Math.sin(im));
    }

    /**
     * Logaritmo natural (rama principal).
     * @throws ArithmeticException si el número es cero
     */
    public Complex log() {
        if (isZero()) throw new ArithmeticException("El logaritmo de 0 no está definido");
        return new Complex(Math.log(abs()), arg());
    }

    /**
     * Potencia entera por exponenciación binaria (exacta para exponentes enteros).
     */
    public Complex pow(int exponent) {
        if (exponent < 0) return reciprocal().pow(-exponent);

        Complex result = ONE;
        Complex base = this;
        int e = exponent;
        while (e > 0) {
            if ((e & 1) == 1) result = result.multiply(base);
            base = base.multiply(base);
            e >>= 1;
        }
        return result;
    }

    /**
     * Potencia compleja (rama principal): {@code exp(w · log z)}.
     */
    public Complex pow(Complex exponent) {
        if (isZero()) return exponent.isZero() ? ONE : ZERO;
        return exponent.multiply(log()).exp();
    }

    /**
     * Raíz cuadrada principal.
     */
    public Complex sqrt() {
        if (isZero()) return ZERO;
        return polar(Math.sqrt(abs()), arg() / 2);
    }

    /**
     * Las n raíces n-ésimas del número, ordenadas por argumento creciente.
     * @param n índice de la raíz (n ≥ 1)
     */
    public Complex[] roots(int n) {
        if (n < 1) throw new IllegalArgumentException("El índice de la raíz debe ser mayor o igual que 1");

        Complex[] roots = new Complex[n];
        if (isZero()) {
            java.util.Arrays.fill(roots, ZERO);
            return roots;
        }

        double modulus = Math.pow(abs(), 1.0 / n);
        double argument = arg();
        for (int k = 0; k < n; k++) {
            roots[k] = polar(modulus, (argument + 2 * Math.PI * k) / n);
        }
        return roots;
    }

    public Complex sin() {
        return new Complex(Math.sin(re) * Math.cosh(im), Math.cos(re) * Math.sinh(im));
    }

    public Complex cos() {
        return new Complex(Math.cos(re) * Math.cosh(im), -Math.sin(re) * Math.sinh(im));
    }

    public Complex tan() {
        return sin().divide(cos());
    }

    public Complex sinh() {
        return new Complex(Math.sinh(re) * Math.cos(im), Math.cosh(re) * Math.sin(im));
    }

    public Complex cosh() {
        return new Complex(Math.cosh(re) * Math.cos(im), Math.sinh(re) * Math.sin(im));
    }

    public Complex tanh() {
        return sinh().divide(cosh());
    }

    /**
     * Compara con una tolerancia absoluta sobre la distancia entre ambos números.
     */
    public boolean isCloseTo(Complex o, double epsilon) {
        return subtract(o).abs() <= epsilon;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Complex c
                && Double.compare(re, c.re) == 0
                && Double.compare(im, c.im) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * Double.hashCode(re) + Double.hashCode(im);
    }

    @Override
    public String toString() {
        return re + (im < 0 ? " - " : " + ") + Math.abs(im) + "i";
    }
}
