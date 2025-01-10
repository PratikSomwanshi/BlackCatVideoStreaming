package com.wanda.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanda.dto.CustomUserDetails;
import com.wanda.dto.RedisUserDTO;
import com.wanda.entity.Users;
import com.wanda.service.CustomUserDetailsService;
import com.wanda.service.JWTService;
import com.wanda.utils.exceptions.CustomException;
import com.wanda.utils.exceptions.enums.ErrorCode;
import com.wanda.utils.exceptions.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Configuration
public class JWTFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper; // To convert objects to JSON

    private final String USER_CACHE_KEY = "user:";

    private RedisTemplate<String, Object> redisTemplate;

    public JWTFilter(
            JWTService jwtService,
            CustomUserDetailsService customUserDetailsService,
            ObjectMapper objectMapper,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    private static final String[] EXCLUDED_PATHS = {"/ws/**", "/api/v1/login", "/register", "/user", "/google/login", "/api/v1/video", "/payment/webhook"};
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String requestPath = request.getRequestURI();
        for (String pattern : EXCLUDED_PATHS) {
            if (pathMatcher.match(pattern, requestPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {


        String path = request.getRequestURI();


        // Extract Authorization header
        String bearer = request.getHeader("Authorization");




        try {
            // Extract email from token
            String email = jwtService.extractEmail(bearer);

            System.out.println("email extracted: " + email);
            // Skip if the user is already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                var cachedUser = (RedisUserDTO) redisTemplate.opsForValue().get(USER_CACHE_KEY + email);

                if(cachedUser == null) {
                    System.out.println("from database");
                    var user = customUserDetailsService.loadUserByUsername(email);
                    cachedUser = new RedisUserDTO( user.getEmailUsername(),user.getUsername(), user.getIsPremiumUser());
                    redisTemplate.opsForValue().set(USER_CACHE_KEY + email, cachedUser, Duration.ofMinutes(60));
                }else{
                    System.out.println("from redis");
                }


                if (path.startsWith("/api/v1/video/hls/")) {
                    if (!cachedUser.getIsPremium()) {
                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Access restricted to premium users.", ErrorCode.USER_NOT_PREMIUM);
                        return;
                    }
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        cachedUser,
                        null,
                        null
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