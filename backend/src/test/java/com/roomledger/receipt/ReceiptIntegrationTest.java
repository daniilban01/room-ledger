package com.roomledger.receipt;

import com.roomledger.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs with the "dev-fake" profile (see IntegrationTest), so the parser returns a canned receipt. */
class ReceiptIntegrationTest extends IntegrationTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};

    private ResultActions upload(String groupId, String token, byte[] bytes) throws Exception {
        var image = new MockMultipartFile("image", "bon.jpg", "image/jpeg", bytes);
        return mockMvc.perform(multipart("/api/groups/" + groupId + "/receipts/parse")
                .file(image)
                .header("Authorization", bearer(token)));
    }

    @Test
    void returnsADraftWithoutSavingAnything() throws Exception {
        String ana = registerUser("Ana");
        String groupId = createGroup(ana).get("id").asText();

        upload(groupId, ana, JPEG)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Magazin Demo"))
                .andExpect(jsonPath("$.items.length()").value(4))
                .andExpect(jsonPath("$.itemsTotal").value(37.30))
                .andExpect(jsonPath("$.warnings.length()").value(0));

        getWithToken("/api/groups/" + groupId + "/expenses", ana).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsFilesThatAreNotImagesWhateverTheyClaim() throws Exception {
        String ana = registerUser("Ana");
        String groupId = createGroup(ana).get("id").asText();

        // Named .jpg and sent as image/jpeg, but the bytes are plain text.
        upload(groupId, ana, "not an image".getBytes()).andExpect(status().isBadRequest());
    }

    @Test
    void nonMemberGets404() throws Exception {
        String ana = registerUser("Ana");
        String groupId = createGroup(ana).get("id").asText();

        upload(groupId, registerUser("Outsider"), JPEG).andExpect(status().isNotFound());
    }

    @Test
    void eleventhScanInAnHourIsRateLimited() throws Exception {
        String ana = registerUser("Ana");
        String groupId = createGroup(ana).get("id").asText();
        for (int i = 0; i < 10; i++) {
            upload(groupId, ana, JPEG).andExpect(status().isOk());
        }

        upload(groupId, ana, JPEG)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
