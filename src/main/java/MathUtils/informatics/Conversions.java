package MathUtils.informatics;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Conversions {

    public static String decimalToBinary(int decimal) {

        ArrayList<Integer> lst = new ArrayList<>();

        while (decimal != 0) {
            lst.add(decimal % 2);
            decimal /= 2;
        }

        java.util.Collections.reverse(lst);
        return lst.stream().map(Object::toString)
                .collect(Collectors.joining());

    }

    public static int binaryToDecimal(String binary) {

        int sum = 0;

        for (int i = 0; i < binary.length(); i++) {
            sum += Integer.parseInt(String.valueOf(binary.charAt(i))) * (int) Math.pow(2, binary.length() - 1 - i);
        }

        return sum;
    }

}
