package com.pdg.reservation.fixture;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import static com.pdg.reservation.fixture.MemberGenerator.generateEmailOfLength;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public final class TestDataSource {

    private TestDataSource() {
    }

    public static Stream<Arguments> invalidEmails() {
        return Stream.of(
                arguments(named("null", null)),
                arguments(named("빈 문자열", "")),
                arguments(named("공백", " ")),
                arguments(named("형식 오류", "plainaddress")),
                arguments(named("101자", generateEmailOfLength(101)))
        );
    }
}
