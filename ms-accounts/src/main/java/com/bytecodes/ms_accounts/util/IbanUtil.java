package com.bytecodes.ms_accounts.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public final class IbanUtil {

    private static final String COUNTRY_CODE = "ES";
    private final SecureRandom random = new SecureRandom();


    public String generateSpanishIban() {

        // Generar 20 dígitos aleatorios (CCC ficticio)
        String ccc = generateRandomNumber(20);

        // Convertir país a números (E=14, S=28)
        String countryNumeric = convertCountryToNumeric(COUNTRY_CODE);

        // IBAN temporal para calcular dígitos de control
        String ibanTemp = ccc + countryNumeric + "00";

        BigInteger number = new BigInteger(ibanTemp);
        int remainder = number.mod(BigInteger.valueOf(97)).intValue();
        int controlDigits = 98 - remainder;

        String formattedControl = String.format("%02d", controlDigits);

        return COUNTRY_CODE + formattedControl + ccc;
    }

    private String generateRandomNumber(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String convertCountryToNumeric(String country) {
        StringBuilder sb = new StringBuilder();
        for (char c : country.toCharArray()) {
            sb.append((int) c - 55); // A=10, B=11...
        }
        return sb.toString();
    }
}

