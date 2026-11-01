package com.pdg.reservation.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.ThrowingConsumer;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

public final class JwtAssertions {

    // 설정을 바꾸지 않고 읽기만 하므로 공유해도 안전하다.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JwtAssertions() {
    }

    public static ThrowingConsumer<String> conformsToJwtFormat() {
        return token -> {
            String[] parts = token.split("\\.");
            assertThat(parts).hasSize(3);
            assertThat(parts[0]).matches(JwtAssertions::isBase64UrlEncodedJson);
            assertThat(parts[1]).matches(JwtAssertions::isBase64UrlEncodedJson);
            assertThat(parts[2]).matches(JwtAssertions::isBase64UrlEncoded);
        };
    }

    public static ThrowingConsumer<String> hasSubject(String expected) {
        return token -> {
            String[] parts = token.split("\\.");
            JsonNode payload = OBJECT_MAPPER.readTree(Base64.getUrlDecoder().decode(parts[1]));
            assertThat(payload.get("sub").asText()).isEqualTo(expected);
        };
    }

    private static boolean isBase64UrlEncodedJson(String value) {
        try {
            OBJECT_MAPPER.readTree(Base64.getUrlDecoder().decode(value));
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private static boolean isBase64UrlEncoded(String value) {
        try {
            Base64.getUrlDecoder().decode(value);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}
