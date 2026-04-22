package com.aws.cognito.poc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;

import java.util.ArrayList;
import java.util.List;

@Service
public class CognitoAdminService {

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${cognito.user-pool-id}")
    private String userPoolId;

    public CognitoAdminService(CognitoIdentityProviderClient cognitoClient) {
        this.cognitoClient = cognitoClient;
    }

    public AdminCreateUserResponse createClientUser(String email, String name) {
        List<AttributeType> userAttributes = new ArrayList<>();
        userAttributes.add(AttributeType.builder().name("email").value(email).build());
        userAttributes.add(AttributeType.builder().name("email_verified").value("true").build());
        if (name != null && !name.isEmpty()) {
            userAttributes.add(AttributeType.builder().name("name").value(name).build());
        }

        AdminCreateUserRequest request = AdminCreateUserRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .userAttributes(userAttributes)
                .messageAction(MessageActionType.SUPPRESS) // Suppress email as we'll send our own OTP for login
                .build();

        return cognitoClient.adminCreateUser(request);
    }
}
