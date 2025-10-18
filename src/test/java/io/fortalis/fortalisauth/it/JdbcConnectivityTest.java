package io.fortalis.fortalisauth.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class JdbcConnectivityTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void canQueryDatabase() {
        Integer one = jdbcTemplate.queryForObject("select 1", Integer.class);
        assertEquals(1, one);
    }

    @Test
    void usesFortalisAuthDatabase() {
        String db = jdbcTemplate.queryForObject("select current_database()", String.class);
        assertEquals("fortalis_auth", db);
    }

    @Test
    void genRandomUuidFunctionExists() {
        Boolean ok = jdbcTemplate.queryForObject("select gen_random_uuid() is not null", Boolean.class);
        assertEquals(Boolean.TRUE, ok);
    }
}
