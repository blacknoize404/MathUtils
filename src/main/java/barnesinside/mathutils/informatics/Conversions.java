package barnesinside.mathutils.informatics;

public class Conversions {

    /**
     * Convierte un entero a su representación binaria. Los negativos conservan el signo ("-101").
     * @param decimal valor a convertir
     * @return la cadena binaria ("0" para el cero)
     */
    public static String decimalToBinary(int decimal) {
        return (decimal < 0 ? "-" : "") + Long.toBinaryString(Math.abs((long) decimal));
    }

    /**
     * Convierte una cadena binaria (con signo opcional) a entero.
     * @param binary cadena compuesta por 0 y 1
     * @return el valor decimal
     * @throws NumberFormatException si la cadena no es binaria o no cabe en un int
     */
    public static int binaryToDecimal(String binary) {
        return Integer.parseInt(binary, 2);
    }

}
