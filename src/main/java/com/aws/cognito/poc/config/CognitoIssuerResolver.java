package com.aws.cognito.poc.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class CognitoIssuerResolver implements AuthenticationManagerResolver<HttpServletRequest> {


    private final Map<String, AuthenticationManager> managers = new ConcurrentHashMap<>();
    private final CognitoGroupConverter converter = new CognitoGroupConverter();
    private final Set<String> trustedIssuers;
    private final ObjectMapper mapper = new ObjectMapper();


    public CognitoIssuerResolver(List<String> issuers) {
        this.trustedIssuers = new HashSet<>(issuers);
        log.info("CognitoIssuerResolver initialized with {} trusted issuers", issuers.size());
    }


    @Override
    public AuthenticationManager resolve(HttpServletRequest request) {
        log.debug("Resolving AuthenticationManager for request: {}", request.getRequestURI());


        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            log.warn("Authentication failed: Missing or malformed Authorization header");
            throw new JwtException("Missing or invalid Authorization header");
        }


        String token = authHeader.substring(7);
        try {
            String[] chunks = token.split("\\.");
            if (chunks.length < 2) {
                log.warn("Authentication failed: JWT structure is invalid (missing payload)");
                throw new JwtException("Invalid JWT structure");
            }


            String payloadJson = new String(Base64.getUrlDecoder().decode(chunks[1]));
            Map<String, Object> payload = mapper.readValue(payloadJson, Map.class);


            String issuer = (String) payload.get("iss");


            if (issuer == null || !trustedIssuers.contains(issuer)) {
                throw new JwtException("Untrusted or missing issuer: " + issuer);
            }


// Route to the correct manager
            return managers.computeIfAbsent(issuer, key -> {
                log.info("First time seeing issuer [{}]. Creating and caching new AuthenticationManager...", key);
                return createManager(key);
            });


        } catch (JwtException e) {
// Re-throw our specific exceptions without wrapping them
            throw e;
        } catch (Exception e) {
            log.error("Critical error while parsing unverified JWT payload", e);
            throw new JwtException("Token parsing failed", e);
        }
    }


    private AuthenticationManager createManager(String issuer) {
        log.debug("Fetching JWKS from Cognito for issuer: {}", issuer);
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(converter);
        return provider::authenticate;
    }
}