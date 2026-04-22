package com.aws.cognito.poc.utils;

import org.springframework.security.oauth2.jwt.Jwt;


import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class JwtUtils {


    public static Set<String> getGroups(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        if (groups == null) return Collections.emptySet();
        return groups.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    }


    public static boolean hasGroup(Jwt jwt, String group) {
        return getGroups(jwt).contains(group);
    }
}