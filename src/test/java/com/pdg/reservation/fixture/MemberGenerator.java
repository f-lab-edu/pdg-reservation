package com.pdg.reservation.fixture;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MemberGenerator {

    private static final String[] SURNAMES = {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임"};
    private static final String[] GIVEN_SYLLABLES = {"서", "윤", "민", "준", "하", "지", "우", "현", "도", "연", "아", "수", "예", "진"};
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    // 서버의 비밀번호 규칙(허용 특수문자)과 같아야 한다. 어긋나면 가입 준비가 400으로 실패한다.
    private static final String SPECIALS = "@$!%*#?&";

    private MemberGenerator() {
    }

    public static String generateEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    // 지정한 길이의 유효한 형식 이메일을 만든다 (46~108자)
    public static String generateEmailOfLength(int length) {
        String local = "a".repeat(40);
        String domainLabel = "b".repeat(length - local.length() - ".com".length() - 1);
        return local + "@" + domainLabel + ".com";
    }

    public static String generateName() {
        return pick(SURNAMES) + pick(GIVEN_SYLLABLES) + pick(GIVEN_SYLLABLES);
    }

    // 지정한 길이의 이름을 만든다
    public static String generateNameOfLength(int length) {
        return "가".repeat(length);
    }

    // "nick" + 임의 6자 = 10자 (3~10자 규칙 안, 한 테스트 안에서 겹칠 확률은 무시할 수 있다)
    public static String generateNickName() {
        return "nick" + UUID.randomUUID().toString().substring(0, 6);
    }

    // 지정한 길이의 닉네임을 만든다
    public static String generateNickNameOfLength(int length) {
        return "가".repeat(length);
    }

    // 영문·숫자·특수문자를 각각 1개 이상 포함한 지정 길이의 비밀번호를 만든다 (3자 이상)
    public static String generatePasswordOfLength(int length) {
        StringBuilder password = new StringBuilder();
        password.append(pick(LETTERS)).append(pick(DIGITS)).append(pick(SPECIALS));
        for (int i = 3; i < length; i++) {
            password.append(pick(LETTERS + DIGITS));
        }
        return password.toString();
    }

    // 영문·숫자·특수문자를 각각 1개 이상 포함한 11자
    public static String generatePassword() {
        return generatePasswordOfLength(11);
    }

    public static String generatePhoneNumber() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return String.format("010-%04d-%04d", random.nextInt(10000), random.nextInt(10000));
    }

    private static String pick(String[] values) {
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    private static String pick(String characters) {
        return String.valueOf(characters.charAt(ThreadLocalRandom.current().nextInt(characters.length())));
    }
}
