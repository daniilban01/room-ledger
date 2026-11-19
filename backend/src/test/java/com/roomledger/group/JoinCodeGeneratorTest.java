package com.roomledger.group;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JoinCodeGeneratorTest {

    private final JoinCodeGenerator generator = new JoinCodeGenerator();

    @Test
    void generatesCodesOfFixedLengthFromTheAllowedAlphabet() {
        for (int i = 0; i < 1_000; i++) {
            String code = generator.generate();

            assertThat(code).hasSize(JoinCodeGenerator.LENGTH);
            assertThat(code.chars()).allMatch(c -> JoinCodeGenerator.ALPHABET.indexOf(c) >= 0);
        }
    }

    @Test
    void neverUsesAmbiguousCharacters() {
        assertThat(JoinCodeGenerator.ALPHABET).doesNotContain("0", "O", "1", "I", "L");
    }

    @Test
    void codesAreRandom() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            codes.add(generator.generate());
        }
        assertThat(codes).hasSize(1_000);
    }
}
