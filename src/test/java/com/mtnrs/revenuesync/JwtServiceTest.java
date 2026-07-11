package com.mtnrs.revenuesync;

import com.mtnrs.revenuesync.domain.User;
import com.mtnrs.revenuesync.domain.enums.UserRole;
import com.mtnrs.revenuesync.repository.MerchantRepository;
import com.mtnrs.revenuesync.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secret", "my-secret-key-that-is-long-enough-for-hmac-256-testing-purposes-12345");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);

        testUser = User.of("Test Buyer", "buyer@example.com", "encoded-password", UserRole.USER);
        ReflectionTestUtils.setField(testUser, "id", 1L);
    }

    @Test
    void testGenerateTokenContainsCorrectClaims() {
        when(merchantRepository.existsByUserId(1L)).thenReturn(true);

        String token = jwtService.generateToken(testUser);

        assertNotNull(token);
        assertEquals("buyer@example.com", jwtService.extractEmail(token));
        assertEquals(1L, jwtService.extractUserId(token));
        assertEquals("USER", jwtService.extractRole(token));
        assertTrue(jwtService.extractHasMerchants(token));
    }

    @Test
    void testExtractEmailFromValidToken() {
        when(merchantRepository.existsByUserId(1L)).thenReturn(false);

        String token = jwtService.generateToken(testUser);
        String email = jwtService.extractEmail(token);

        assertEquals("buyer@example.com", email);
    }

    @Test
    void testIsTokenValidForCorrectUser() {
        when(merchantRepository.existsByUserId(1L)).thenReturn(false);

        String token = jwtService.generateToken(testUser);
        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertTrue(isValid);
    }

    @Test
    void testHasMerchantsClaimWhenMerchantExists() {
        when(merchantRepository.existsByUserId(1L)).thenReturn(true);

        String token = jwtService.generateToken(testUser);
        Boolean hasMerchants = jwtService.extractHasMerchants(token);

        assertTrue(hasMerchants);
    }
}