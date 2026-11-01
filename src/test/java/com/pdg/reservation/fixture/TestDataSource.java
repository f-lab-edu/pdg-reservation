package com.pdg.reservation.fixture;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import static com.pdg.reservation.fixture.MemberGenerator.generateEmailOfLength;
import static com.pdg.reservation.fixture.MemberGenerator.generateNameOfLength;
import static com.pdg.reservation.fixture.MemberGenerator.generatePasswordOfLength;
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
                arguments(named("최대 길이 초과 (101자)", generateEmailOfLength(101)))
        );
    }

    public static Stream<Arguments> invalidNames() {
        return Stream.of(
                arguments(named("null", null)),
                arguments(named("빈 문자열", "")),
                arguments(named("공백", " ")),
                arguments(named("공백만 3자", "   ")),
                arguments(named("최소 길이 미만 (2자)", generateNameOfLength(2))),
                arguments(named("최대 길이 초과 (11자)", generateNameOfLength(11)))
        );
    }

    public static Stream<Arguments> invalidPasswords() {
        return Stream.of(
                arguments(named("null", null)),
                arguments(named("빈 문자열", "")),
                arguments(named("공백", " ")),
                arguments(named("최소 길이 미만 (7자)", generatePasswordOfLength(7))),
                arguments(named("최대 길이 초과 (21자)", generatePasswordOfLength(21))),
                arguments(named("영문 없음", "12345678!@")),
                arguments(named("숫자 없음", "Abcdefgh!@")),
                arguments(named("특수문자 없음", "Abcdefgh12")),
                arguments(named("한글 포함", "Abcd123!한글가")),
                arguments(named("허용되지 않는 특수문자", "Abcd123^xyz")),
                arguments(named("중간에 공백", "Abcd 123!xyz"))
        );
    }
}
