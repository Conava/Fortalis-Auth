package io.fortalis.fortalisauth.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fortalis.fortalisauth.it.support.TotpTestUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class MfaTotpIntegrationTest extends BaseIntegrationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void enableTotp_thenLoginWithMfa_flow() throws Exception {
        String email = "mfa+" + UUID.randomUUID() + "@itest.local";
        String password = "Str0ngPass!";
        // register
        MvcResult reg = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(("{ \"email\":\"%s\", \"password\":\"%s\", \"displayName\":\"MfaUser\" }").formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode regJson = MAPPER.readTree(reg.getResponse().getContentAsString());
        String access = regJson.get("accessToken").asText();
        String refresh = regJson.get("refreshToken").asText();

        // setup TOTP (needs auth)
        MvcResult setup = mockMvc.perform(post("/auth/mfa/totp/setup")
                        .header("Authorization", "Bearer " + access)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secretBase32", not(emptyString())))
                .andReturn();
        String secret = MAPPER.readTree(setup.getResponse().getContentAsString()).get("secretBase32").asText();
        String code = TotpTestUtil.currentCodeFromBase32Secret(secret);

        // enable
        mockMvc.perform(post("/auth/mfa/totp/enable")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("code", code))
                .andExpect(status().isOk());

        // logout
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(("{ \"refreshToken\":\"%s\" }").formatted(refresh)))
                .andExpect(status().isOk());

        // login/start should now require MFA and return ticket
        MvcResult start = mockMvc.perform(post("/auth/login/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(("{ \"emailOrUsername\":\"%s\", \"password\":\"%s\" }").formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginTicket", not(emptyString())))
                .andExpect(jsonPath("$.allowedFactors", hasItem("TOTP")))
                .andReturn();
        String ticket = MAPPER.readTree(start.getResponse().getContentAsString()).get("loginTicket").asText();

        // complete with current TOTP
        String code2 = TotpTestUtil.currentCodeFromBase32Secret(secret);
        mockMvc.perform(post("/auth/login/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(("""
                                    { "loginTicket":"%s", "factor":"TOTP", "code":"%s" }
                                """).formatted(ticket, code2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.mfaEnabled", is(true)));
    }

    @Test
    void enableTotp_withWrongCode_fails400() throws Exception {
        String email = "mfabad+" + UUID.randomUUID() + "@itest.local";
        String password = "Str0ngPass!";

        MvcResult reg = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(("{ \"email\":\"%s\", \"password\":\"%s\", \"displayName\":\"BadMfa\" }").formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        String access = MAPPER.readTree(reg.getResponse().getContentAsString()).get("accessToken").asText();

        MvcResult setup = mockMvc.perform(post("/auth/mfa/totp/setup")
                        .header("Authorization", "Bearer " + access)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(post("/auth/mfa/totp/enable")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("code", "000000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("https://auth.fortalis.game/errors/totp_invalid")))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void loginComplete_withWrongTotp_fails401() throws Exception {
        String email = "mfawrong+" + UUID.randomUUID() + "@itest.local";
        String password = "Str0ngPass!";

        // Register and enable MFA properly
        MvcResult reg = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(("{ \"email\":\"%s\", \"password\":\"%s\", \"displayName\":\"Mfa\" }").formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        String access = MAPPER.readTree(reg.getResponse().getContentAsString()).get("accessToken").asText();

        MvcResult setup = mockMvc.perform(post("/auth/mfa/totp/setup")
                        .header("Authorization", "Bearer " + access)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String secret = MAPPER.readTree(setup.getResponse().getContentAsString()).get("secretBase32").asText();
        String good = TotpTestUtil.currentCodeFromBase32Secret(secret);

        mockMvc.perform(post("/auth/mfa/totp/enable")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("code", good))
                .andExpect(status().isOk());

        // Start login to get ticket
        MvcResult start = mockMvc.perform(post("/auth/login/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(("{ \"emailOrUsername\":\"%s\", \"password\":\"%s\" }").formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginTicket", not(emptyString())))
                .andReturn();
        String ticket = MAPPER.readTree(start.getResponse().getContentAsString()).get("loginTicket").asText();

        // Complete with wrong code
        mockMvc.perform(post("/auth/login/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                    { "loginTicket":"%s", "factor":"TOTP", "code":"123456" }
                                """.formatted(ticket)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type", is("https://auth.fortalis.game/errors/mfa_invalid")))
                .andExpect(jsonPath("$.status", is(401)));
    }
}
