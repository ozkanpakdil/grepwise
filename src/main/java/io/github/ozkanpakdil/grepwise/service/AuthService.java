package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.grpc.*;
import io.github.ozkanpakdil.grepwise.model.Role;
import io.github.ozkanpakdil.grepwise.model.User;
import io.github.ozkanpakdil.grepwise.repository.RoleRepository;
import io.github.ozkanpakdil.grepwise.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service implementation for handling authentication operations.
 */
@Service
public class AuthService extends AuthServiceGrpc.AuthServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenService tokenService;
    private final AuditLogService auditLogService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, 
                      TokenService tokenService, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenService = tokenService;
        this.auditLogService = auditLogService;
        this.passwordEncoder = new BCryptPasswordEncoder();
        
        // Initialize default roles
        roleRepository.initializeDefaultRoles();
        
        // Create a default admin user if no users exist
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@grepwise.io");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setFirstName("Admin");
            admin.setLastName("User");
            
            // Add ADMIN role
            Role adminRole = roleRepository.findByName("ADMIN");
            if (adminRole != null) {
                admin.addRole(adminRole);
            }
            
            userRepository.save(admin);
        }
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<AuthResponse> responseObserver) {
        logger.info("Registration attempt for username: {}", request.getUsername());
        
        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            logger.warn("Registration failed: Username already exists: {}", request.getUsername());
            
            // Log the failed registration attempt
            auditLogService.createAuthAuditLog(
                "REGISTER", 
                "FAILURE", 
                request.getUsername(), 
                "Registration failed: Username already exists"
            );
            
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Username already exists")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Registration failed: Email already exists: {}", request.getEmail());
            
            // Log the failed registration attempt
            auditLogService.createAuthAuditLog(
                "REGISTER", 
                "FAILURE", 
                request.getUsername(), 
                "Registration failed: Email already exists"
            );
            
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Email already exists")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        
        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        
        // Add USER role
        Role userRole = roleRepository.findByName("USER");
        if (userRole != null) {
            user.addRole(userRole);
        }
        
        user = userRepository.save(user);
        
        // Generate tokens
        String accessToken = tokenService.generateToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);
        
        // Log the successful registration
        logger.info("User registered successfully: {}", user.getUsername());
        auditLogService.createAuthAuditLog(
            "REGISTER", 
            "SUCCESS", 
            user.getUsername(), 
            "User registered successfully"
        );
        
        // Build response
        AuthResponse response = AuthResponse.newBuilder()
                .setSuccess(true)
                .setMessage("User registered successfully")
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setExpiresAt(tokenService.getExpirationDateFromToken(accessToken).getTime())
                .setUser(convertToGrpcUser(user))
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void login(LoginRequest request, StreamObserver<AuthResponse> responseObserver) {
        logger.info("Login attempt for username: {}", request.getUsername());
        
        // Find user by username
        User user = userRepository.findByUsername(request.getUsername());
        
        // Check if user exists and password matches
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Login failed: Invalid username or password for: {}", request.getUsername());
            
            // Log the failed login attempt
            auditLogService.createAuthAuditLog(
                "LOGIN", 
                "FAILURE", 
                request.getUsername(), 
                "Login failed: Invalid username or password"
            );
            
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Invalid username or password")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        
        // Generate tokens
        String accessToken = tokenService.generateToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);
        
        // Log the successful login
        logger.info("User logged in successfully: {}", user.getUsername());
        auditLogService.createAuthAuditLog(
            "LOGIN", 
            "SUCCESS", 
            user.getUsername(), 
            "Login successful"
        );
        
        // Build response
        AuthResponse response = AuthResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Login successful")
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setExpiresAt(tokenService.getExpirationDateFromToken(accessToken).getTime())
                .setUser(convertToGrpcUser(user))
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        String token = request.getToken();
        
        // Extract username from token for logging (might be null if token is invalid)
        String username = null;
        try {
            username = tokenService.getUsernameFromToken(token);
            logger.debug("Token validation attempt for user: {}", username);
        } catch (Exception e) {
            logger.debug("Token validation attempt with potentially invalid token");
        }
        
        // Validate token
        if (!tokenService.validateToken(token)) {
            logger.debug("Token validation failed: Invalid token");
            
            // Log the failed token validation (only if we could extract a username)
            if (username != null) {
                auditLogService.createAuthAuditLog(
                    "TOKEN_VALIDATE", 
                    "FAILURE", 
                    username, 
                    "Token validation failed: Invalid token"
                );
            }
            
            responseObserver.onNext(ValidateTokenResponse.newBuilder()
                    .setValid(false)
                    .build());
            responseObserver.onCompleted();
            return;
        }
        
        // Get user ID and roles from token
        String userId = tokenService.getUserIdFromToken(token);
        List<String> roles = tokenService.getRolesFromToken(token);
        long expiresAt = tokenService.getExpirationDateFromToken(token).getTime();
        
        // Log successful token validation (using debug level since this happens frequently)
        logger.debug("Token validated successfully for user: {}", username);
        
        // We don't log successful token validations to avoid excessive audit logs
        // since this operation happens frequently
        
        // Build response
        ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                .setValid(true)
                .setUserId(userId)
                .addAllRoles(roles)
                .setExpiresAt(expiresAt)
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void refreshToken(RefreshTokenRequest request, StreamObserver<AuthResponse> responseObserver) {
        String refreshToken = request.getRefreshToken();
        
        // Extract username from token for logging (might be null if token is invalid)
        String username = null;
        try {
            username = tokenService.getUsernameFromToken(refreshToken);
            logger.info("Token refresh attempt for user: {}", username);
        } catch (Exception e) {
            logger.info("Token refresh attempt with invalid token");
        }
        
        // Validate refresh token
        if (!tokenService.validateToken(refreshToken)) {
            logger.warn("Token refresh failed: Invalid refresh token");
            
            // Log the failed token refresh
            auditLogService.createAuthAuditLog(
                "TOKEN_REFRESH", 
                "FAILURE", 
                username != null ? username : "unknown", 
                "Token refresh failed: Invalid refresh token"
            );
            
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Invalid refresh token")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        
        // Get user ID from token
        String userId = tokenService.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId);
        
        if (user == null) {
            logger.warn("Token refresh failed: User not found for ID: {}", userId);
            
            // Log the failed token refresh
            auditLogService.createAuthAuditLog(
                "TOKEN_REFRESH", 
                "FAILURE", 
                username != null ? username : "unknown", 
                "Token refresh failed: User not found"
            );
            
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("User not found")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        
        // Generate new access token
        String accessToken = tokenService.generateToken(user);
        
        // Log the successful token refresh
        logger.info("Token refreshed successfully for user: {}", user.getUsername());
        auditLogService.createAuthAuditLog(
            "TOKEN_REFRESH", 
            "SUCCESS", 
            user.getUsername(), 
            "Token refreshed successfully"
        );
        
        // Build response
        AuthResponse response = AuthResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Token refreshed successfully")
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setExpiresAt(tokenService.getExpirationDateFromToken(accessToken).getTime())
                .setUser(convertToGrpcUser(user))
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        logger.info("Logout request received");
        
        // In a real implementation, we would invalidate the tokens
        // For now, we just return a success response and log the event
        
        // Log the logout event
        // Since we don't have user information in the logout request,
        // we'll log it with an "unknown" username
        auditLogService.createAuthAuditLog(
            "LOGOUT", 
            "SUCCESS", 
            "unknown", 
            "User logged out successfully"
        );
        
        LogoutResponse response = LogoutResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Logout successful")
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getCurrentUser(GetCurrentUserRequest request, StreamObserver<io.github.ozkanpakdil.grepwise.grpc.User> responseObserver) {
        String token = request.getAccessToken();
        
        // Validate token
        if (!tokenService.validateToken(token)) {
            // Return an empty user if token is invalid
            responseObserver.onNext(io.github.ozkanpakdil.grepwise.grpc.User.newBuilder().build());
            responseObserver.onCompleted();
            return;
        }
        
        // Get user ID from token
        String userId = tokenService.getUserIdFromToken(token);
        User user = userRepository.findById(userId);
        
        if (user == null) {
            // Return an empty user if user not found
            responseObserver.onNext(io.github.ozkanpakdil.grepwise.grpc.User.newBuilder().build());
            responseObserver.onCompleted();
            return;
        }
        
        // Build response
        io.github.ozkanpakdil.grepwise.grpc.User response = convertToGrpcUser(user);
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateUser(UpdateUserRequest request, StreamObserver<io.github.ozkanpakdil.grepwise.grpc.User> responseObserver) {
        String token = request.getAccessToken();
        
        // Validate token
        if (!tokenService.validateToken(token)) {
            // Return an empty user if token is invalid
            responseObserver.onNext(io.github.ozkanpakdil.grepwise.grpc.User.newBuilder().build());
            responseObserver.onCompleted();
            return;
        }
        
        // Get user ID from token
        String userId = tokenService.getUserIdFromToken(token);
        User user = userRepository.findById(userId);
        
        if (user == null) {
            // Return an empty user if user not found
            responseObserver.onNext(io.github.ozkanpakdil.grepwise.grpc.User.newBuilder().build());
            responseObserver.onCompleted();
            return;
        }
        
        // Update user information
        if (!request.getEmail().isEmpty()) {
            user.setEmail(request.getEmail());
        }
        
        if (!request.getFirstName().isEmpty()) {
            user.setFirstName(request.getFirstName());
        }
        
        if (!request.getLastName().isEmpty()) {
            user.setLastName(request.getLastName());
        }
        
        user = userRepository.save(user);
        
        // Build response
        io.github.ozkanpakdil.grepwise.grpc.User response = convertToGrpcUser(user);
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void changePassword(ChangePasswordRequest request, StreamObserver<ChangePasswordResponse> responseObserver) {
        String token = request.getAccessToken();
        
        // Validate token
        if (!tokenService.validateToken(token)) {
            responseObserver.onNext(ChangePasswordResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Invalid token")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        
        // Get user ID from token
        String userId = tokenService.getUserIdFromToken(token);
        User user = userRepository.findById(userId);
        
        if (user == null) {
            responseObserver.onNext(ChangePasswordResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("User not found")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        
        // Check if current password matches
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            responseObserver.onNext(ChangePasswordResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Current password is incorrect")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Build response
        ChangePasswordResponse response = ChangePasswordResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Password changed successfully")
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Convert a model User to a gRPC User.
     *
     * @param modelUser The model User to convert
     * @return The converted gRPC User
     */
    private io.github.ozkanpakdil.grepwise.grpc.User convertToGrpcUser(User modelUser) {
        return io.github.ozkanpakdil.grepwise.grpc.User.newBuilder()
                .setId(modelUser.getId())
                .setUsername(modelUser.getUsername())
                .setEmail(modelUser.getEmail())
                .setFirstName(modelUser.getFirstName())
                .setLastName(modelUser.getLastName())
                .addAllRoles(modelUser.getRoleNames())
                .setCreatedAt(modelUser.getCreatedAt())
                .setUpdatedAt(modelUser.getUpdatedAt())
                .setEnabled(modelUser.isEnabled())
                .build();
    }
}