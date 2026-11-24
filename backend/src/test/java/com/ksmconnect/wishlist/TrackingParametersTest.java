package com.ksmconnect.wishlist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingParametersTest {

    static String empikFacebookLink() throws IOException {
        try (InputStream stream = TrackingParametersTest.class.getResourceAsStream("/wishlist/empik-facebook-link.txt")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).strip();
        }
    }

    @Test
    void stripsAdvertisingParametersFromRealShopLink() throws IOException {
        String link = empikFacebookLink();
        assertThat(link).hasSize(2055);
        assertThat(TrackingParameters.strip(link)).isEqualTo(
                "https://www.empik.com/zakreslacze-stabilo-boss-original-pastel-6-sztuk-stabilo,p1242414260,szkolne-i-papiernicze-p"
                        + "?mpShopId=0&destinationUrl=product%2Fp1242414260&media_type=image");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "https://shop.pl/p/1|https://shop.pl/p/1",
            "https://shop.pl/p/1?color=red&size=M|https://shop.pl/p/1?color=red&size=M",
            "https://shop.pl/p/1?utm_source=fb&color=red|https://shop.pl/p/1?color=red",
            "https://shop.pl/p/1?fbclid=abc&gclid=x&UTM_MEDIUM=y|https://shop.pl/p/1",
            "https://shop.pl/p/1?color=red&utm_source=fb#opinie|https://shop.pl/p/1?color=red#opinie",
            "https://shop.pl/p/1?utm_source=fb#opinie|https://shop.pl/p/1#opinie",
            "https://shop.pl/p/1?a=1&&b=2|https://shop.pl/p/1?a=1&b=2",
            "https://shop.pl/p/1?%ZZbad=1&utm_id=2|https://shop.pl/p/1?%ZZbad=1"
    })
    void keepsProductParametersAndRemovesOnlyTracking(String input, String expected) {
        assertThat(TrackingParameters.strip(input)).isEqualTo(expected);
    }
}
