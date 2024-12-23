package com.wanda.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanda.dto.CustomUserDetails;
import com.wanda.service.CustomUserDetailsService;
import com.wanda.service.JWTService;
import com.wanda.utils.exceptions.CustomException;
import com.wanda.utils.exceptions.enums.ErrorCode;
import com.wanda.utils.exceptions.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class JWTFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper; // To convert objects to JSON

    public JWTFilter(
            JWTService jwtService,
            CustomUserDetailsService customUserDetailsService,
            ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip filtering for login and register endpoints
        System.out.println(path);
        if (path.equals("/api/v1/login") || path.equals("/register") || path.equals("/user") || path.equals("/google/login") || path.equals("/api/v1/video")) {
            filterChain.doFilter(request, response);
            return;
        }



        // Extract Authorization header
        String bearer = request.getHeader("Authorization");




        try {
            // Extract email from token
            String email = jwtService.extractEmail(bearer);

            // Skip if the user is already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                var user = customUserDetailsService.loadUserByUsername(email);

                if (path.startsWith("/api/v1/video/hls/")) {
                    if (!user.getIsPremiumUser()) {
                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Access restricted to premium users.", ErrorCode.USER_NOT_PREMIUM);
                        return;
                    }
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (CustomException e) {
            // Handle token errors
            logger.info("exception code " + e.getCode());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage(), e.getCode());
            return;
        }

        // Proceed with the filter chain if no errors
        filterChain.doFilter(request, response);
    }

    /**
     * Helper method to send an error response in the `ErrorResponse` format.
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String explanation, ErrorCode code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");

        logger.debug("code " + code);

        ErrorResponse errorResponse = new ErrorResponse(
                "Authentication failed",
                explanation,
                code
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}