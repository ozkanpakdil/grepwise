package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.grpc.*;
import io.github.ozkanpakdil.grepwise.model.Role;
import io.github.ozkanpakdil.grepwise.model.User;
import io.github.ozkanpakdil.grepwise.repository.RoleRepository;
import io.github.ozkanpakdil.grepwise.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for handling authentication operations.
 */
@Service
public class AuthService extends AuthServiceGrpc.AuthServiceImplBase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenService = tokenService;
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
        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Username already exists")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
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
        // Find user by username
        User user = userRepository.findByUsername(request.getUsername());
        
        // Check if user exists and password matches
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
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
        
        // Validate token
        if (!tokenService.validateToken(token)) {
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
        
        // Validate refresh token
        if (!tokenService.validateToken(refreshToken)) {
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
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("User not found")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        
        // Generate new access token
        String accessToken = tokenService.generateToken(user);
        
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
        // In a real implementation, we would invalidate the tokens
        // For now, we just return a success response
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