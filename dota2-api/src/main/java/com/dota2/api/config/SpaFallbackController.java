package com.dota2.api.config;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * SPA fallback: forward all non-API 404 errors to index.html
 * so Vue Router can handle client-side routing.
 */
@Controller
public class SpaFallbackController implements ErrorController {

    @GetMapping("/")
    public String root() {
        return "forward:/index.html";
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        // Only forward 404s (not 500, 403, etc.)
        if (status != null && Integer.parseInt(status.toString()) == 404) {
            // Check it's not an API path
            if (uri == null || (!uri.toString().startsWith("/user/") && !"/error".equals(uri.toString()))) {
                return "forward:/index.html";
            }
        }
        // For other errors or API errors, let Spring handle normally
        return null;
    }
}
