package io.fortalis.fortalisauth.it;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

final class AuthLogoutIntegrationTest extends BaseIntegrationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void logout_revokesRefreshToken_andRefreshFailsAfter() throws Exception {
        String email = "logout+" + UUID.randomUUID() + "@itest.local";
        String regBody = """
                    { "email":"%s", "password":"Str0ngPass!", "displayName":"Out" }
                """.formatted(email);

        MvcResult reg = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(regBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken", not(emptyString())))
                .andReturn();

        JsonNode regJson = MAPPER.readTree(reg.getResponse().getContentAsString());
        String refresh = regJson.get("refreshToken").asText();

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(("{ \"refreshToken\":\"%s\" }").formatted(refresh)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(("{ \"refreshToken\":\"%s\" }").formatted(refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_withUnknownToken_isIdempotent200() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{ \"refreshToken\":\"bogus.token.value\" }"))
                .andExpect(status().isOk());
    }
}