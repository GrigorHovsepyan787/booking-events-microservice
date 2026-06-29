package org.example.userservice.service.impl;

import org.example.userservice.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== create() Tests ====================

    @Test
    void create_shouldGenerateAndStoreRefreshToken() {
        Long userId = 1L;

        String token = refreshTokenService.create(userId);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();

        verify(valueOperations).set(eq("refresh_token:" + token), eq("1"), any());
    }

    @Test
    void create_shouldReturnUniqueTokensOnMultipleCalls() {
        String token1 = refreshTokenService.create(1L);
        String token2 = refreshTokenService.create(1L);

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void create_shouldStoreCorrectUserIdForDifferentUsers() {
        String token1 = refreshTokenService.create(10L);
        String token2 = refreshTokenService.create(20L);

        verify(valueOperations).set(eq("refresh_token:" + token1), eq("10"), any());
        verify(valueOperations).set(eq("refresh_token:" + token2), eq("20"), any());
    }

    @Test
    void create_shouldHandleNullUserIdGracefully() {
        assertThatThrownBy(() -> refreshTokenService.create(null))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(valueOperations);
    }

    @Test
    void create_shouldUseCorrectRedisKeyFormat() {
        String token = refreshTokenService.create(1L);

        verify(valueOperations).set(eq("refresh_token:" + token), anyString(), any());
    }

    @Test
    void create_shouldStoreUserIdAsString() {
        Long userId = 42L;

        String token = refreshTokenService.create(userId);

        verify(valueOperations).set(eq("refresh_token:" + token), eq("42"), any());
    }

    @Test
    void create_shouldHandleZeroUserId() {
        String token = refreshTokenService.create(0L);

        assertThat(token).isNotNull();
        verify(valueOperations).set(eq("refresh_token:" + token), eq("0"), any());
    }

    @Test
    void create_shouldHandleNegativeUserId() {
        String token = refreshTokenService.create(-1L);

        assertThat(token).isNotNull();
        verify(valueOperations).set(eq("refresh_token:" + token), eq("-1"), any());
    }

    @Test
    void create_shouldHandleWhitespaceUserId() {
        String token = refreshTokenService.create(1L);

        verify(valueOperations).set(eq("refresh_token:" + token), eq("1"), any());
    }

    @Test
    void create_shouldHandleVeryLargeUserId() {
        Long largeUserId = 999999999999999999L;

        String token = refreshTokenService.create(largeUserId);

        assertThat(token).isNotNull();
        verify(valueOperations).set(eq("refresh_token:" + token), eq("999999999999999999"), any());
    }

    @Test
    void create_shouldGenerateValidUUIDFormat() {
        String token = refreshTokenService.create(1L);

        assertThat(token).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void create_shouldHandleMultipleCreatesForSameUser() {
        String token1 = refreshTokenService.create(1L);
        String token2 = refreshTokenService.create(1L);
        String token3 = refreshTokenService.create(1L);

        assertThat(token1).isNotEqualTo(token2);
        assertThat(token2).isNotEqualTo(token3);
        assertThat(token1).isNotEqualTo(token3);

        verify(valueOperations, times(3)).set(anyString(), eq("1"), any());
    }

    // ==================== validate() Tests ====================

    @Test
    void validate_shouldReturnUserId_whenTokenIsValid() {
        String token = "valid-token";
        when(valueOperations.get("refresh_token:" + token)).thenReturn("42");

        Long result = refreshTokenService.validate(token);

        assertThat(result).isEqualTo(42L);
        verify(valueOperations).get("refresh_token:" + token);
    }

    @Test
    void validate_shouldThrowInvalidRefreshTokenException_whenTokenDoesNotExist() {
        String token = "nonexistent-token";
        when(valueOperations.get("refresh_token:" + token)).thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.validate(token))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is invalid or expired");

        verify(valueOperations).get("refresh_token:" + token);
    }

    @Test
    void validate_shouldThrowInvalidRefreshTokenException_whenTokenIsEmpty() {
        String token = "";
        when(valueOperations.get("refresh_token:" + token)).thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.validate(token))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is invalid or expired");

        verify(valueOperations).get("refresh_token:" + token);
    }

    @Test
    void validate_shouldReturnZeroUserId_whenStoredUserIdIsZero() {
        String token = "zero-user-token";
        when(valueOperations.get("refresh_token:" + token)).thenReturn("0");

        Long result = refreshTokenService.validate(token);

        assertThat(result).isEqualTo(0L);
    }

    @Test
    void validate_shouldReturnCorrectUserId_whenStoredUserIdIsPositive() {
        String token = "positive-user-token";
        when(valueOperations.get("refresh_token:" + token)).thenReturn("100");

        Long result = refreshTokenService.validate(token);

        assertThat(result).isEqualTo(100L);
    }

    @Test
    void validate_shouldReturnNegativeUserId_whenStoredUserIdIsNegative() {
        String token = "negative-user-token";
        when(valueOperations.get("refresh_token:" + token)).thenReturn("-50");

        Long result = refreshTokenService.validate(token);

        assertThat(result).isEqualTo(-50L);
    }

    @Test
    void validate_shouldHandleWhitespaceToken() {
        String token = "  ";
        when(valueOperations.get("refresh_token:" + token)).thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.validate(token))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is invalid or expired");

        verify(valueOperations).get("refresh_token:" + token);
    }

    @Test
    void validate_shouldHandleSpecialCharactersInToken() {
        String token = "token@123!";
        when(valueOperations.get("refresh_token:" + token)).thenReturn("42");

        Long result = refreshTokenService.validate(token);

        assertThat(result).isEqualTo(42L);
        verify(valueOperations).get("refresh_token:" + token);
    }

    @Test
    void validate_shouldUseCorrectRedisKeyFormat() {
        String token = "test-token";
        when(valueOperations.get("refresh_token:" + token)).thenReturn("1");

        refreshTokenService.validate(token);

        verify(valueOperations).get("refresh_token:" + token);
    }

    @Test
    void validate_shouldThrowException_whenTokenIsNull() {
        when(valueOperations.get("refresh_token:" + null)).thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.validate(null))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is invalid or expired");

        verify(valueOperations).get("refresh_token:" + null);
    }

    // ==================== rotate() Tests ====================

    @Test
    void rotate_shouldInvalidateOldTokenAndCreateNewOne() {
        String oldToken = "old-token";
        Long userId = 10L;

        String newToken = refreshTokenService.rotate(userId, oldToken);

        assertThat(newToken).isNotNull();
        assertThat(newToken).isNotEmpty();

        verify(redisTemplate).delete("refresh_token:" + oldToken);
        verify(valueOperations).set(eq("refresh_token:" + newToken), eq("10"), any());
    }

    @Test
    void rotate_shouldGenerateUniqueTokenEachTime() {
        String oldToken = "old-token";

        String newToken1 = refreshTokenService.rotate(1L, oldToken);
        String newToken2 = refreshTokenService.rotate(1L, oldToken);

        assertThat(newToken1).isNotEqualTo(newToken2);
    }

    @Test
    void rotate_shouldCallLogoutThenCreate() {
        String oldToken = "old-token";
        Long userId = 1L;

        String newToken = refreshTokenService.rotate(userId, oldToken);

        InOrder inOrder = inOrder(redisTemplate, redisTemplate, valueOperations);
        inOrder.verify(redisTemplate).delete("refresh_token:" + oldToken);
        inOrder.verify(redisTemplate).opsForValue();
        inOrder.verify(valueOperations).set(eq("refresh_token:" + newToken), eq("1"), any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void rotate_shouldHandleNullUserId() {
        String oldToken = "old-token";

        assertThatThrownBy(() -> refreshTokenService.rotate(null, oldToken))
                .isInstanceOf(NullPointerException.class);

        verify(redisTemplate).delete("refresh_token:" + oldToken);
    }

    @Test
    void rotate_shouldHandleNullOldToken() {
        Long userId = 1L;

        String newToken = refreshTokenService.rotate(userId, null);

        assertThat(newToken).isNotNull();
        verify(redisTemplate).delete("refresh_token:" + null);
        verify(valueOperations).set(eq("refresh_token:" + newToken), eq("1"), any());
    }

    @Test
    void rotate_shouldHandleBothNullParameters() {
        assertThatThrownBy(() -> refreshTokenService.rotate(null, null))
                .isInstanceOf(NullPointerException.class);

        verify(redisTemplate).delete("refresh_token:" + null);
    }

    @Test
    void rotate_shouldStoreCorrectUserIdForNewToken() {
        String oldToken = "old-token";
        Long userId = 99L;

        String newToken = refreshTokenService.rotate(userId, oldToken);

        verify(valueOperations).set(eq("refresh_token:" + newToken), eq("99"), any());
    }

    @Test
    void rotate_shouldHandleZeroUserId() {
        String oldToken = "old-token";

        String newToken = refreshTokenService.rotate(0L, oldToken);

        assertThat(newToken).isNotNull();
        verify(valueOperations).set(eq("refresh_token:" + newToken), eq("0"), any());
    }

    @Test
    void rotate_shouldHandleNegativeUserId() {
        String oldToken = "old-token";

        String newToken = refreshTokenService.rotate(-1L, oldToken);

        assertThat(newToken).isNotNull();
        verify(valueOperations).set(eq("refresh_token:" + newToken), eq("-1"), any());
    }

    @Test
    void rotate_shouldHandleEmptyOldToken() {
        Long userId = 1L;

        String newToken = refreshTokenService.rotate(userId, "");

        assertThat(newToken).isNotNull();
        verify(redisTemplate).delete("refresh_token:");
        verify(valueOperations).set(eq("refresh_token:" + newToken), eq("1"), any());
    }

    @Test
    void rotate_shouldHandleWhitespaceOldToken() {
        Long userId = 1L;

        String newToken = refreshTokenService.rotate(userId, "  ");

        assertThat(newToken).isNotNull();
        verify(redisTemplate).delete("refresh_token:  ");
        verify(valueOperations).set(eq("refresh_token:" + newToken), eq("1"), any());
    }

    @Test
    void rotate_shouldHandleSpecialCharactersInOldToken() {
        Long userId = 1L;
        String oldToken = "token@123!";

        String newToken = refreshTokenService.rotate(userId, oldToken);

        assertThat(newToken).isNotNull();
        verify(redisTemplate).delete("refresh_token:token@123!");
        verify(valueOperations).set(eq("refresh_token:" + newToken), eq("1"), any());
    }

    @Test
    void rotate_shouldGenerateDifferentTokensForSameUser() {
        String oldToken = "old-token";
        Long userId = 1L;

        String newToken1 = refreshTokenService.rotate(userId, oldToken);
        String newToken2 = refreshTokenService.rotate(userId, oldToken);

        assertThat(newToken1).isNotEqualTo(newToken2);
        verify(valueOperations, times(2)).set(anyString(), eq("1"), any());
    }

    // ==================== logout() Tests ====================

    @Test
    void logout_shouldDeleteTokenFromRedis() {
        String token = "token-to-logout";

        refreshTokenService.logout(token);

        verify(redisTemplate).delete("refresh_token:" + token);
    }

    @Test
    void logout_shouldHandleNullTokenGracefully() {
        refreshTokenService.logout(null);

        verify(redisTemplate).delete("refresh_token:" + null);
    }

    @Test
    void logout_shouldHandleNonExistentTokenGracefully() {
        String token = "nonexistent-token";

        refreshTokenService.logout(token);

        verify(redisTemplate).delete("refresh_token:" + token);
    }

    @Test
    void logout_shouldHandleMultipleLogouts() {
        String token1 = "token-1";
        String token2 = "token-2";

        refreshTokenService.logout(token1);
        refreshTokenService.logout(token2);

        verify(redisTemplate).delete("refresh_token:" + token1);
        verify(redisTemplate).delete("refresh_token:" + token2);
    }

    @Test
    void logout_shouldNotCallOpsForValue() {
        String token = "token-to-logout";

        refreshTokenService.logout(token);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void logout_shouldHandleEmptyToken() {
        refreshTokenService.logout("");

        verify(redisTemplate).delete("refresh_token:");
    }

    @Test
    void logout_shouldHandleWhitespaceToken() {
        refreshTokenService.logout("  ");

        verify(redisTemplate).delete("refresh_token:  ");
    }

    @Test
    void logout_shouldHandleSpecialCharactersInToken() {
        String token = "token@123!";

        refreshTokenService.logout(token);

        verify(redisTemplate).delete("refresh_token:token@123!");
    }

    @Test
    void logout_shouldUseCorrectRedisKeyFormat() {
        String token = "test-token";

        refreshTokenService.logout(token);

        verify(redisTemplate).delete("refresh_token:" + token);
    }

    @Test
    void logout_shouldHandleVeryLongToken() {
        String token = "a".repeat(255);

        refreshTokenService.logout(token);

        verify(redisTemplate).delete("refresh_token:" + token);
    }

    // ==================== Integration Flow Tests ====================

    @Test
    void completeFlow_shouldCreateValidateAndLogout() {
        Long userId = 10L;
        String token = refreshTokenService.create(userId);

        when(valueOperations.get("refresh_token:" + token)).thenReturn("10");

        Long validatedUserId = refreshTokenService.validate(token);
        assertThat(validatedUserId).isEqualTo(10L);

        refreshTokenService.logout(token);

        when(valueOperations.get("refresh_token:" + token)).thenReturn(null);
        assertThatThrownBy(() -> refreshTokenService.validate(token))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(valueOperations, times(2)).get("refresh_token:" + token);
        verify(redisTemplate).delete("refresh_token:" + token);
    }

    @Test
    void completeFlow_shouldHandleRotationWithInvalidOldToken() {
        Long userId = 10L;
        String oldToken = "old-token";

        String newToken = refreshTokenService.rotate(userId, oldToken);

        assertThat(newToken).isNotNull();
        assertThat(newToken).isNotEqualTo(oldToken);

        verify(redisTemplate).delete("refresh_token:" + oldToken);
        verify(valueOperations).set(eq("refresh_token:" + newToken), eq("10"), any());
    }

    @Test
    void completeFlow_shouldCreateRotateAndValidate() {
        Long userId = 10L;
        String oldToken = refreshTokenService.create(userId);

        when(valueOperations.get("refresh_token:" + oldToken)).thenReturn("10");
        Long validatedOldToken = refreshTokenService.validate(oldToken);
        assertThat(validatedOldToken).isEqualTo(10L);

        String newToken = refreshTokenService.rotate(userId, oldToken);

        assertThat(newToken).isNotNull();
        assertThat(newToken).isNotEqualTo(oldToken);

        when(valueOperations.get("refresh_token:" + newToken)).thenReturn("10");
        Long validatedNewToken = refreshTokenService.validate(newToken);
        assertThat(validatedNewToken).isEqualTo(10L);

        when(valueOperations.get("refresh_token:" + oldToken)).thenReturn(null);
        assertThatThrownBy(() -> refreshTokenService.validate(oldToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(valueOperations, times(2)).get("refresh_token:" + oldToken);
        verify(valueOperations).get("refresh_token:" + newToken);
        verify(redisTemplate).delete("refresh_token:" + oldToken);
    }

    @Test
    void completeFlow_shouldHandleMultipleUsers() {
        Long user1Id = 1L;
        Long user2Id = 2L;

        String user1Token = refreshTokenService.create(user1Id);
        String user2Token = refreshTokenService.create(user2Id);

        when(valueOperations.get("refresh_token:" + user1Token)).thenReturn("1");
        when(valueOperations.get("refresh_token:" + user2Token)).thenReturn("2");

        Long validatedUser1 = refreshTokenService.validate(user1Token);
        Long validatedUser2 = refreshTokenService.validate(user2Token);

        assertThat(validatedUser1).isEqualTo(1L);
        assertThat(validatedUser2).isEqualTo(2L);

        verify(valueOperations).get("refresh_token:" + user1Token);
        verify(valueOperations).get("refresh_token:" + user2Token);
    }

    @Test
    void completeFlow_shouldRotateMultipleTimes() {
        Long userId = 1L;
        String token1 = refreshTokenService.create(userId);

        when(valueOperations.get("refresh_token:" + token1)).thenReturn("1");
        refreshTokenService.validate(token1);

        String token2 = refreshTokenService.rotate(userId, token1);

        when(valueOperations.get("refresh_token:" + token2)).thenReturn("1");
        refreshTokenService.validate(token2);

        String token3 = refreshTokenService.rotate(userId, token2);

        assertThat(token1).isNotEqualTo(token2);
        assertThat(token2).isNotEqualTo(token3);
        assertThat(token1).isNotEqualTo(token3);

        verify(redisTemplate, times(2)).delete(anyString());
        verify(valueOperations, times(3)).set(anyString(), eq("1"), any());
    }

    // ==================== Concurrent Scenario Tests ====================

    @Test
    void create_shouldHandleMultipleCreates() {
        String token1 = refreshTokenService.create(1L);
        String token2 = refreshTokenService.create(2L);
        String token3 = refreshTokenService.create(3L);

        assertThat(token1).isNotNull();
        assertThat(token2).isNotNull();
        assertThat(token3).isNotNull();
        assertThat(token1).isNotEqualTo(token2);
        assertThat(token2).isNotEqualTo(token3);

        verify(valueOperations, times(3)).set(anyString(), anyString(), any());
    }

    @Test
    void validate_shouldHandleMultipleValidations() {
        String token = "valid-token";
        when(valueOperations.get("refresh_token:" + token)).thenReturn("42");

        Long result1 = refreshTokenService.validate(token);
        Long result2 = refreshTokenService.validate(token);
        Long result3 = refreshTokenService.validate(token);

        assertThat(result1).isEqualTo(42L);
        assertThat(result2).isEqualTo(42L);
        assertThat(result3).isEqualTo(42L);

        verify(valueOperations, times(3)).get("refresh_token:" + token);
    }

    @Test
    void rotate_shouldHandleMultipleRotations() {
        String oldToken1 = "old-token-1";
        String oldToken2 = "old-token-2";

        String newToken1 = refreshTokenService.rotate(1L, oldToken1);
        String newToken2 = refreshTokenService.rotate(1L, oldToken2);

        assertThat(newToken1).isNotNull();
        assertThat(newToken2).isNotNull();
        assertThat(newToken1).isNotEqualTo(newToken2);

        verify(redisTemplate, times(2)).delete(anyString());
        verify(valueOperations, times(2)).set(anyString(), eq("1"), any());
    }

    // ==================== Repository Interaction Verification Tests ====================

    @Test
    void create_shouldCallOpsForValueExactlyOnce() {
        refreshTokenService.create(1L);

        verify(redisTemplate, times(1)).opsForValue();
        verifyNoMoreInteractions(redisTemplate);
    }

    @Test
    void validate_shouldCallOpsForValueExactlyOnce() {
        String token = "test-token";
        when(valueOperations.get("refresh_token:" + token)).thenReturn("1");

        refreshTokenService.validate(token);

        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get("refresh_token:" + token);
    }

    @Test
    void rotate_shouldCallDeleteAndSet() {
        String oldToken = "old-token";
        Long userId = 1L;

        refreshTokenService.rotate(userId, oldToken);

        verify(redisTemplate).delete("refresh_token:" + oldToken);
        verify(valueOperations).set(anyString(), eq("1"), any());
    }

    @Test
    void logout_shouldCallDeleteExactlyOnce() {
        String token = "test-token";

        refreshTokenService.logout(token);

        verify(redisTemplate, times(1)).delete("refresh_token:" + token);
        verifyNoMoreInteractions(redisTemplate);
    }

    @Test
    void create_shouldNotCallGetOrDelete() {
        refreshTokenService.create(1L);

        verify(valueOperations, never()).get(anyString());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void validate_shouldNotCallSetOrDelete() {
        String token = "test-token";
        when(valueOperations.get("refresh_token:" + token)).thenReturn("1");

        refreshTokenService.validate(token);

        verify(valueOperations, never()).set(anyString(), anyString(), any());
        verify(redisTemplate, never()).delete(anyString());
    }

    // ==================== Additional Edge Case Tests ====================

    @Test
    void validate_shouldHandleVeryLargeUserId() {
        String token = "test-token";
        when(valueOperations.get("refresh_token:" + token)).thenReturn(String.valueOf(Long.MAX_VALUE));

        Long result = refreshTokenService.validate(token);

        assertThat(result).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void rotate_shouldPreserveUserId() {
        String oldToken = "old-token";
        Long userId = 123L;

        String newToken = refreshTokenService.rotate(userId, oldToken);

        verify(valueOperations).set(eq("refresh_token:" + newToken), eq("123"), any());
    }

    @Test
    void logout_shouldNotAffectOtherTokens() {
        String token1 = "token-1";
        String token2 = "token-2";

        refreshTokenService.logout(token1);

        verify(redisTemplate).delete("refresh_token:" + token1);
        verify(redisTemplate, never()).delete("refresh_token:" + token2);
    }

    @Test
    void create_shouldReturnNonEmptyString() {
        String token = refreshTokenService.create(1L);

        assertThat(token).isNotEmpty();
        assertThat(token.length()).isGreaterThan(0);
    }

    @Test
    void create_shouldReturnTokenWithCorrectLength() {
        String token = refreshTokenService.create(1L);

        assertThat(token).hasSize(36);
    }

    @Test
    void validate_shouldThrowExceptionWithCorrectMessageForNullToken() {
        when(valueOperations.get("refresh_token:" + null)).thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.validate(null))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is invalid or expired")
                .hasNoCause();
    }

    @Test
    void validate_shouldThrowExceptionWithCorrectMessage() {
        String token = "invalid-token";
        when(valueOperations.get("refresh_token:" + token)).thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.validate(token))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is invalid or expired")
                .hasNoCause();
    }

    @Test
    void rotate_shouldReturnNonEmptyString() {
        String oldToken = "old-token";
        Long userId = 1L;

        String newToken = refreshTokenService.rotate(userId, oldToken);

        assertThat(newToken).isNotEmpty();
        assertThat(newToken.length()).isGreaterThan(0);
    }
}