package com.dota2.common.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Calendar;

public class JwtUtils {

    public static final String HMAC256_KEY = "dota2-analyze-secret-key-2024";

    private static final long TOKEN_EXPIRE_HOURS = 24;

    public static String createToken(Long userId, String username) {
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.HOUR, (int) TOKEN_EXPIRE_HOURS);

        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("userName", username)
                .withExpiresAt(instance.getTime())
                .sign(Algorithm.HMAC256(HMAC256_KEY));
    }

    public static DecodedJWT verifyToken(String token) {
        return JWT.require(Algorithm.HMAC256(HMAC256_KEY))
                .build()
                .verify(token);
    }

    public static Long getUserId(String token) {
        return verifyToken(token).getClaim("userId").asLong();
    }

    public static String getUserName(String token) {
        return verifyToken(token).getClaim("userName").asString();
    }
}
