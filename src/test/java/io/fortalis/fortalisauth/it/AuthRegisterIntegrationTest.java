package io.fortalis.fortalisauth.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class AuthRegisterIntegrationTest extends BaseIntegrationTest {

    @Test
    void register_success_returnsTokensAndNoMfa() throws Exception {
        String email = "user+" + UUID.randomUUID() + "@itest.local";
        String body = """
            { "email":"%s", "password":"Str0ngPass!", "displayName":"Tester" }
        """.formatted(email);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", not(emptyString())))
            .andExpect(jsonPath("$.refreshToken", not(emptyString())))
            .andExpect(jsonPath("$.displayName", is("Tester")))
            .andExpect(jsonPath("$.mfaEnabled", is(false)));
    }

    @Test
    void register_duplicateEmail_fails400() throws Exception {
        String email = "dupe+" + UUID.randomUUID() + "@itest.local";
        String payload = """
            { "email":"%s", "password":"Str0ngPass!", "displayName":"ABC" }
        """.formatted(email).replace("\"A\"", "\"Alpha\"");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type", is("https://auth.fortalis.game/errors/email_taken")))
            .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void register_invalidEmail_fails400() throws Exception {
        String body = """
            { "email":"not-an-email", "password":"Str0ngPass!", "displayName":"Xavier" }
        """;
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_weakPassword_fails400() throws Exception {
        String body = """
            { "email":"%s", "password":"short", "displayName":"Zoe" }
        """.formatted("pw+" + UUID.randomUUID() + "@itest.local");
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }
}
