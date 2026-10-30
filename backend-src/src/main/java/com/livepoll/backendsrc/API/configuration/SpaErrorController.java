package com.livepoll.backendsrc.API.configuration;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
                if (requestUri != null && requestUri.startsWith("/api/")) {
                    return "error"; // Let API 404s return normal error response
                }
                return "forward:/index.html"; // Forward frontend routes to index.html
            }
        }
        return "error";
    }
}
