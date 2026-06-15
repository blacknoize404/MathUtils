package MathUtils.informatics;

import org.junit.jupiter.api.Test;

class ConversionsTest {

    @Test
    void decimalToBinaryZero() {
        String result = Conversions.decimalToBinary(0);
        assert "".equals(result) : "Expected empty string but got " + result;
    }

    @Test
    void decimalToBinaryOne() {
        String result = Conversions.decimalToBinary(1);
        assert "1".equals(result) : "Expected '1' but got " + result;
    }

    @Test
    void decimalToBinaryTwo() {
        String result = Conversions.decimalToBinary(2);
        assert "10".equals(result) : "Expected '10' but got " + result;
    }

    @Test
    void decimalToBinaryTen() {
        String result = Conversions.decimalToBinary(10);
        assert "1010".equals(result) : "Expected '1010' but got " + result;
    }

    @Test
    void binaryToDecimalZero() {
        int result = Conversions.binaryToDecimal("0");
        assert result == 0 : "Expected 0 but got " + result;
    }

    @Test
    void binaryToDecimalOne() {
        int result = Conversions.binaryToDecimal("1");
        assert result == 1 : "Expected 1 but got " + result;
    }

    @Test
    void binaryToDecimal10() {
        int result = Conversions.binaryToDecimal("10");
        assert result == 2 : "Expected 2 but got " + result;
    }

    @Test
    void binaryToDecimal1010() {
        int result = Conversions.binaryToDecimal("1010");
        assert result == 10 : "Expected 10 but got " + result;
    }

    @Test
    void binaryToDecimal1111() {
        int result = Conversions.binaryToDecimal("1111");
        assert result == 15 : "Expected 15 but got " + result;
    }

    @Test
    void roundTrip() {
        for (int i = 0; i < 20; i++) {
            String binary = Conversions.decimalToBinary(i);
            int back = Conversions.binaryToDecimal(binary);
            assert back == i : "Round trip failed for " + i + ": got " + back;
        }
    }
}
