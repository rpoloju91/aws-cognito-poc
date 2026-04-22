package com.aws.cognito.poc.controller;

import com.aws.cognito.poc.service.OtpAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

import java.util.Map;

@RestController
@RequestMapping("/public/auth")
public class PublicAuthController {

    private final OtpAuthService otpAuthService;

    public PublicAuthController(OtpAuthService otpAuthService) {
        this.otpAuthService = otpAuthService;
    }

    @PostMapping("/send-otp")
    public Map<String, String> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        otpAuthService.generateAndSendOtp(email);
        return Map.of("status", "success", "message", "OTP sent successfully to " + email);
    }

    @PostMapping("/verify-otp")
    public Map<String, Object> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        if (email == null || otp == null) {
            throw new IllegalArgumentException("Email and OTP are required");
        }
        
        AdminInitiateAuthResponse authResponse = otpAuthService.verifyOtpAndLogin(email, otp);
        AuthenticationResultType result = authResponse.authenticationResult();
        
        return Map.of(
                "status", "success",
                "accessToken", result.accessToken(),
                "idToken", result.idToken(),
                "refreshToken", result.refreshToken(),
                "expiresIn", result.expiresIn()
        );
    }
}
