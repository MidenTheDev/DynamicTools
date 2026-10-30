package us.to.midensthings.dynamicTools.util;

public class RomanNumerals {
    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] NUMERALS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    /***
     * @return the number in roman numerals, e.g. 4 -> "IV". Numbers below 1 are returned as plain digits.
     */
    public static String toRoman(int number) {
        if (number < 1) {
            return String.valueOf(number);
        }
        StringBuilder roman = new StringBuilder();
        for (int i = 0; i < VALUES.length; i++) {
            while (number >= VALUES[i]) {
                roman.append(NUMERALS[i]);
                number -= VALUES[i];
            }
        }
        return roman.toString();
    }
}
