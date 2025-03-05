package com.gym.service;

import com.gym.dto.ChangePasswordRequest;
import com.gym.dto.ForgotPasswordRequest;
import com.gym.dto.LoginRequest;
import com.gym.dto.ResetPasswordRequest;
import com.gym.vo.LoginResponse;

import javax.servlet.http.HttpServletRequest;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    LoginResponse loginWithGoogle(String googleIdToken);
    public void changePassword(ChangePasswordRequest request);
}

