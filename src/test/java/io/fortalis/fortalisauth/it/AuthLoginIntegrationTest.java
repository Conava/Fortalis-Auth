package io.fortalis.fortalisauth.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class AuthLoginIntegrationTest extends BaseIntegrationTest {

    @Test
    void loginStart_withoutMfa_returnsTokens() throws Exception {
        String email = "login+" + UUID.randomUUID() + "@itest.local";
        String password = "Str0ngPass!";
        String reg = """
                    { "email":"%s", "password":"%s", "displayName":"Lin" }
                """.formatted(email, password);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(reg))
                .andExpect(status().isOk());

        String login = """
                    { "emailOrUsername":"%s", "password":"%s" }
                """.formatted(email, password);

        mockMvc.perform(post("/auth/login/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.mfaEnabled", is(false)));
    }

    @Test
    void login_wrongPassword_unauthorized() throws Exception {
        String email = "badpwd+" + UUID.randomUUID() + "@itest.local";
        String password = "Str0ngPass!";
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(("{ \"email\":\"%s\", \"password\":\"%s\", \"displayName\":\"ABC\" }").formatted(email, password)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(("{ \"emailOrUsername\":\"%s\", \"password\":\"wrong!\" }").formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type", is("https://auth.fortalis.game/errors/invalid_credentials")))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.detail", is("Bad credentials")));
    }

    @Test
    void login_unknownUser_unauthorized() throws Exception {
        mockMvc.perform(post("/auth/login/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{ \"emailOrUsername\":\"nobody@example.com\", \"password\":\"irrelevant\" }"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type", is("https://auth.fortalis.game/errors/invalid_credentials")))
                .andExpect(jsonPath("$.status", is(401)));
    }
}
