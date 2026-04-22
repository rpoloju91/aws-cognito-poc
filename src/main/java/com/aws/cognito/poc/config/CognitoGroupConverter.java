package com.aws.cognito.poc.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
public class CognitoGroupConverter implements Converter<Jwt, AbstractAuthenticationToken> {


    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        log.debug("Token signature verified successfully. Beginning claim extraction.");


// 1. Extract Client ID and Set Tenant Context
        String clientId = jwt.getClaimAsString("client_id");
        if (clientId != null && !clientId.trim().isEmpty()) {
            log.info("Authenticated request for Client ID: {}. Setting TenantContext.", clientId);
// TenantContext.setTenantId(clientId);
        } else {
            log.warn("Verified token is missing the 'client_id' claim. TenantContext will NOT be set.");
        }


// 2. Extract Authorities
        List<SimpleGrantedAuthority> authorities = extractAuthorities(jwt);


        log.debug("Successfully converted JWT to AuthenticationToken with authorities: {}", authorities);
        return new JwtAuthenticationToken(jwt, authorities);
    }


    private List<SimpleGrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> cognitoGroups = jwt.getClaimAsStringList("cognito:groups");


        if (cognitoGroups == null || cognitoGroups.isEmpty()) {
            log.debug("No 'cognito:groups' found in token. Assigning empty authorities.");
            return new ArrayList<>();
        }


        List<SimpleGrantedAuthority> mappedAuthorities = cognitoGroups.stream()
                .map(group -> new SimpleGrantedAuthority("ROLE_" + group.toUpperCase()))
                .collect(Collectors.toList());


        log.debug("Mapped Cognito Groups {} to Spring Roles {}", cognitoGroups, mappedAuthorities);
        return mappedAuthorities;
    }
}