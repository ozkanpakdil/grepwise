package io.github.ozkanpakdil.grepwise.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Error controller to provide a deterministic JSON response for errors.
 * The SPA (frontend) is served from classpath:/static via SpaRedirectConfig,
 * which forwards "/" and non-API routes to /index.html.
 */
@RestController
public class HomeController implements ErrorController {

    @RequestMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> root() {
        org.springframework.core.io.Resource index = new org.springframework.core.io.ClassPathResource("static/index.html");
        if (!index.exists()) {
            return org.springframework.http.ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(null);
        }
        return org.springframework.http.ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(index);
    }

    @RequestMapping(path = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> login() {
        org.springframework.core.io.Resource index = new org.springframework.core.io.ClassPathResource("static/index.html");
        if (!index.exists()) {
            return org.springframework.http.ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(null);
        }
        return org.springframework.http.ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(index);
    }

    @RequestMapping(path = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> handleError(HttpServletRequest request) {
        Object statusAttr = request.getAttribute("jakarta.servlet.error.status_code");
        int statusCode = statusAttr instanceof Integer ? (Integer) statusAttr : 500;
        Object message = request.getAttribute("jakarta.servlet.error.message");
        Object uri = request.getAttribute("jakarta.servlet.error.request_uri");

        Map<String, Object> body = new HashMap<>();
        body.put("status", statusCode);
        body.put("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
        body.put("message", message != null ? message : "Unexpected error");
        body.put("path", uri != null ? uri : "/error");
        return body;
    }
}
