package com.roomledger.group;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomledger.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupIntegrationTest extends IntegrationTest {

    private JsonNode createGroup(String token, String name) throws Exception {
        return readJson(postJson("/api/groups", Map.of("name", name), token)
                .andExpect(status().isCreated()));
    }

    private JsonNode joinGroup(String token, String joinCode) throws Exception {
        return readJson(postJson("/api/groups/join", Map.of("joinCode", joinCode), token)
                .andExpect(status().isOk()));
    }

    @Test
    void creatorBecomesAdminAndOnlyMember() throws Exception {
        String ana = registerUser("Ana");

        JsonNode group = createGroup(ana, "Apartament 12");

        assertThat(group.get("name").asText()).isEqualTo("Apartament 12");
        assertThat(group.get("myRole").asText()).isEqualTo("ADMIN");
        assertThat(group.get("joinCode").asText()).hasSize(8);
        assertThat(group.get("members")).hasSize(1);
    }

    @Test
    void joiningWithCodeAddsMember() throws Exception {
        String ana = registerUser("Ana");
        String bogdan = registerUser("Bogdan");
        JsonNode group = createGroup(ana, "Apartament");

        // lowercase + spaces on purpose: codes are normalized before lookup
        JsonNode joined = joinGroup(bogdan, "  " + group.get("joinCode").asText().toLowerCase() + " ");

        assertThat(joined.get("myRole").asText()).isEqualTo("MEMBER");
        assertThat(joined.get("members")).hasSize(2);
    }

    @Test
    void joiningTwiceIsIdempotent() throws Exception {
        String ana = registerUser("Ana");
        String bogdan = registerUser("Bogdan");
        String code = createGroup(ana, "Apartament").get("joinCode").asText();

        joinGroup(bogdan, code);
        JsonNode secondJoin = joinGroup(bogdan, code);

        assertThat(secondJoin.get("members")).hasSize(2);
    }

    @Test
    void joiningWithUnknownCodeReturns404() throws Exception {
        String ana = registerUser("Ana");

        postJson("/api/groups/join", Map.of("joinCode", "ZZZZZZZZ"), ana)
                .andExpect(status().isNotFound());
    }

    @Test
    void myGroupsListsOnlyGroupsIAmMemberOf() throws Exception {
        String ana = registerUser("Ana");
        String bogdan = registerUser("Bogdan");
        createGroup(ana, "Grupul Anei");
        createGroup(bogdan, "Grupul lui Bogdan");

        mockMvc.perform(get("/api/groups").header("Authorization", bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Grupul Anei"))
                .andExpect(jsonPath("$[0].myRole").value("ADMIN"));
    }

    @Test
    void nonMemberGets404ForExistingGroup() throws Exception {
        String ana = registerUser("Ana");
        String outsider = registerUser("Outsider");
        String groupId = createGroup(ana, "Apartament").get("id").asText();

        mockMvc.perform(get("/api/groups/" + groupId).header("Authorization", bearer(outsider)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownGroupIdReturns404() throws Exception {
        String ana = registerUser("Ana");

        mockMvc.perform(get("/api/groups/" + UUID.randomUUID()).header("Authorization", bearer(ana)))
                .andExpect(status().isNotFound());
    }

    @Test
    void memberCannotRegenerateJoinCode() throws Exception {
        String ana = registerUser("Ana");
        String bogdan = registerUser("Bogdan");
        JsonNode group = createGroup(ana, "Apartament");
        joinGroup(bogdan, group.get("joinCode").asText());

        mockMvc.perform(post("/api/groups/" + group.get("id").asText() + "/join-code")
                        .header("Authorization", bearer(bogdan)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRegeneratesJoinCodeAndOldCodeStopsWorking() throws Exception {
        String ana = registerUser("Ana");
        String bogdan = registerUser("Bogdan");
        JsonNode group = createGroup(ana, "Apartament");
        String oldCode = group.get("joinCode").asText();

        JsonNode updated = readJson(mockMvc.perform(post("/api/groups/" + group.get("id").asText() + "/join-code")
                        .header("Authorization", bearer(ana)))
                .andExpect(status().isOk()));

        assertThat(updated.get("joinCode").asText()).isNotEqualTo(oldCode);
        postJson("/api/groups/join", Map.of("joinCode", oldCode), bogdan)
                .andExpect(status().isNotFound());
    }

    @Test
    void createGroupWithBlankNameReturns400() throws Exception {
        String ana = registerUser("Ana");

        postJson("/api/groups", Map.of("name", "  "), ana)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }
}
