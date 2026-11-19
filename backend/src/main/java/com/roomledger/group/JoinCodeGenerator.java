package com.roomledger.group;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class JoinCodeGenerator {

    // No 0/O or 1/I/L, so a code read aloud or typed from a phone is not misread.
    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 8;

    // SecureRandom, not Random: join codes grant access to a group, so they must not be predictable.
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
