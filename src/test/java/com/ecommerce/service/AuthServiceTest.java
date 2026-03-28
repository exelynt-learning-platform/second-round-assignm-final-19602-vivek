package com.ecommerce.service;

import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.UserDTO;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_succeeds_and_returns_token() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test");
        req.setEmail("a@b.com");
        req.setPassword("secret1");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("hash");
        User saved = User.builder().id(1L).name("Test").email("a@b.com").password("hash").role(Role.USER).build();
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtTokenProvider.createToken(1L, "a@b.com", Role.USER)).thenReturn("jwt-token");
        when(userMapper.toDto(saved)).thenReturn(UserDTO.builder().id(1L).email("a@b.com").role(Role.USER).build());

        var res = authService.register(req);

        assertThat(res.getToken()).isEqualTo("jwt-token");
        assertThat(res.getType()).isEqualTo("Bearer");
        assertThat(res.getUser().getEmail()).isEqualTo("a@b.com");
        verify(passwordEncoder).encode("secret1");
    }

    @Test
    void register_fails_when_email_exists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("a@b.com");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void login_succeeds() {
        LoginRequest req = new LoginRequest();
        req.setEmail("a@b.com");
        req.setPassword("secret1");
        User user = User.builder().id(2L).email("a@b.com").password("hash").role(Role.USER).build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createToken(2L, "a@b.com", Role.USER)).thenReturn("jwt");
        when(userMapper.toDto(user)).thenReturn(UserDTO.builder().id(2L).email("a@b.com").build());

        var res = authService.login(req);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertThat(res.getToken()).isEqualTo("jwt");
    }
}
