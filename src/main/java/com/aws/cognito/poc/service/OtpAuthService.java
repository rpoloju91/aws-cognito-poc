package com.aws.cognito.poc.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class OtpAuthService {

    private static final Logger log = LoggerFactory.getLogger(OtpAuthService.class);
    private final JavaMailSender mailSender;
    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${cognito.user-pool-id}")
    private String userPoolId;

    @Value("${cognito.client-id}")
    private String clientId;

    // Cache to store OTPs with a 5-minute expiration
    private final Cache<String, String> otpCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private final SecureRandom secureRandom = new SecureRandom();

    public OtpAuthService(JavaMailSender mailSender, CognitoIdentityProviderClient cognitoClient) {
        this.mailSender = mailSender;
        this.cognitoClient = cognitoClient;
    }

    public void generateAndSendOtp(String email) {
        // 1. Generate 6 digit OTP
        String otp = String.format("%06d", secureRandom.nextInt(1000000));

        // 2. Store OTP in cache
        otpCache.put(email, otp);

        // 3. Send via SES
        sendEmail(email, otp);
        log.info("OTP generated and sent to email: {}", email);
    }

    public AdminInitiateAuthResponse verifyOtpAndLogin(String email, String otp) {
        // 1. Verify OTP
        String cachedOtp = otpCache.getIfPresent(email);
        if (cachedOtp == null || !cachedOtp.equals(otp)) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        // 2. OTP is valid. Invalidate it from cache.
        otpCache.invalidate(email);

        // 3. Set the OTP as the temporary password in Cognito to allow login
        // If the user doesn't exist, we auto-register them as a Customer.
        try {
            AdminSetUserPasswordRequest setPasswordRequest = AdminSetUserPasswordRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .password(otp)
                    .permanent(true) // Set as permanent so it doesn't force a password change
                    .build();
            cognitoClient.adminSetUserPassword(setPasswordRequest);
        } catch (UserNotFoundException e) {
            log.info("Customer {} not found in Cognito. Auto-registering...", email);
            autoRegisterCustomer(email, otp);
        }

        // 4. Initiate Admin Auth to get the JWT tokens
        Map<String, String> authParams = new HashMap<>();
        authParams.put("USERNAME", email);
        authParams.put("PASSWORD", otp);

        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                .userPoolId(userPoolId)
                .clientId(clientId)
                .authParameters(authParams)
                .build();

        return cognitoClient.adminInitiateAuth(authRequest);
    }

    private void sendEmail(String toEmail, String otp) {
        String subject = "Your Login OTP";
        String bodyHtml = "<html><body><h3>Your Login OTP</h3><p>Your One-Time Password (OTP) for login is: <b>" + otp + "</b></p><p>This OTP will expire in 5 minutes.</p></body></html>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(bodyHtml, true); // true indicates HTML
            
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private void autoRegisterCustomer(String email, String otp) {
        // 1. Create the user
        AdminCreateUserRequest createRequest = AdminCreateUserRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .userAttributes(
                        AttributeType.builder().name("email").value(email).build(),
                        AttributeType.builder().name("email_verified").value("true").build()
                )
                .messageAction(MessageActionType.SUPPRESS)
                .build();
        cognitoClient.adminCreateUser(createRequest);

        // 2. Assign the CUSTOMERUSER group so they have the correct roles in SecurityConfig
        try {
            AdminAddUserToGroupRequest groupRequest = AdminAddUserToGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .groupName("CUSTOMERUSER")
                    .build();
            cognitoClient.adminAddUserToGroup(groupRequest);
        } catch (Exception e) {
            log.warn("Failed to add user {} to CUSTOMERUSER group. The group might not exist in Cognito yet.", email);
        }

        // 3. Set their password to the OTP
        AdminSetUserPasswordRequest setPasswordRequest = AdminSetUserPasswordRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .password(otp)
                .permanent(true)
                .build();
        cognitoClient.adminSetUserPassword(setPasswordRequest);
    }
}
