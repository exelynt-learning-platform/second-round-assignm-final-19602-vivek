package com.ecommerce.security;

import java.io.Serializable;

public record SecurityUserPrincipal(Long userId, String email) implements Serializable {
}
