package com.gout.service;

import com.gout.dto.request.LoginRequest;
import com.gout.dto.request.RegisterRequest;
import com.gout.dto.response.TokenResponse;

public interface AuthService {

    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(String refreshToken);
}
