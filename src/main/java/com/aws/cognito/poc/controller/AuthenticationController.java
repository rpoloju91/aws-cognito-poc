package com.aws.cognito.poc.controller;

import com.aws.cognito.poc.utils.JwtUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.Map;
import java.util.Set;


import com.aws.cognito.poc.service.CognitoAdminService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api")
public class AuthenticationController {

    private final CognitoAdminService cognitoAdminService;

    public AuthenticationController(CognitoAdminService cognitoAdminService) {
        this.cognitoAdminService = cognitoAdminService;
    }


    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('CLIENTADMIN')")
    public Map<String, Object> getAdminAccessInfo(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("user", jwt.getClaim("username"),
                "roles", JwtUtils.getGroups(jwt),
                "claims", jwt.getClaims());
    }


    @GetMapping("/customer")
    public Map<String, Object> getCustomerAccessInfo(@AuthenticationPrincipal Jwt jwt) {
        Set<String> groups = JwtUtils.getGroups(jwt);
        return Map.of("claims", jwt.getClaims(), "roles", groups);
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasAnyRole('CLIENTADMIN')")
    public Map<String, String> createClientUser(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String name = request.get("name");
        cognitoAdminService.createClientUser(email, name);
        return Map.of("status", "success", "message", "User created successfully");
    }

}