package com.bytecodes.ms_accounts.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IbanUtilTest {

    private final IbanUtil ibanUtil = new IbanUtil();

    @Test
    void generate_spanish_iban_has_expected_format() {
        String iban = ibanUtil.generateSpanishIban();

        assertEquals(24, iban.length());
        assertTrue(iban.startsWith("ES"));
        assertTrue(iban.substring(2).matches("\\d{22}"));
    }

    @Test
    void generate_spanish_iban_checksum_is_valid() {
        String iban = ibanUtil.generateSpanishIban();
        assertEquals(1, ibanModulo97(iban));
    }

    @Test
    void generate_spanish_iban_is_not_constant() {
        Set<String> ibans = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            ibans.add(ibanUtil.generateSpanishIban());
        }
        assertTrue(ibans.size() > 1);
    }

    @Test
    void convert_country_to_numeric_private_method_ok() throws Exception {
        Method method = IbanUtil.class.getDeclaredMethod("convertCountryToNumeric", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(ibanUtil, "ES");
        assertEquals("1428", result);
    }

    @Test
    void generate_random_number_private_method_returns_digits() throws Exception {
        Method method = IbanUtil.class.getDeclaredMethod("generateRandomNumber", int.class);
        method.setAccessible(true);

        String random20 = (String) method.invoke(ibanUtil, 20);
        String random10 = (String) method.invoke(ibanUtil, 10);

        assertEquals(20, random20.length());
        assertTrue(random20.matches("\\d{20}"));
        assertEquals(10, random10.length());
        assertTrue(random10.matches("\\d{10}"));
        assertNotEquals(random20.substring(0, 10), random10);
    }

    private int ibanModulo97(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        String numeric = toNumericIban(rearranged);
        return new BigInteger(numeric).mod(BigInteger.valueOf(97)).intValue();
    }

    private String toNumericIban(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            } else {
                sb.append((int) Character.toUpperCase(c) - 55);
            }
        }
        return sb.toString();
    }
}

