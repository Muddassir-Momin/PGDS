package com.pgds.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwt = new JwtService("test-secret-test-secret-test-secret-1234", 5);

    @Test
    void roundTripKeepsSubjectAndRole() {
        var claims = jwt.parse(jwt.generateToken("admin", "SUPER_ADMIN"));
        assertEquals("admin", claims.getSubject());
        assertEquals("SUPER_ADMIN", claims.get("role", String.class));
    }

    @Test
    void tamperedTokenIsRejected() {
        String t = jwt.generateToken("admin", "SUPER_ADMIN");
        assertThrows(JwtException.class, () -> jwt.parse(t.substring(0, t.length() - 2) + "xx"));
    }
}
