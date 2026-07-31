package com.dota2.api.filter;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.dota2.common.utils.JwtUtils;
import com.dota2.common.utils.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("ssoToken");
        if (token == null || token.isEmpty()) {
            writeUnauthorized(response, "未提供认证Token");
            return false;
        }

        try {
            JwtUtils.verifyToken(token);
            return true;
        } catch (JWTVerificationException e) {
            log.warn("Token验证失败: {}", e.getMessage());
            writeUnauthorized(response, "Token无效或已过期");
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Result<?> result = Result.fail(message);
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(result));
    }
}
