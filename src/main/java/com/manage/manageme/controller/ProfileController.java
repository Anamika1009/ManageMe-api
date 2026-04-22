package com.manage.manageme.controller;

import com.manage.manageme.dto.AuthDTO;
import com.manage.manageme.dto.ProfileDTO;
import com.manage.manageme.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/register")
    public ResponseEntity<ProfileDTO> registerProfile(@RequestBody ProfileDTO profileDTO) {
        ProfileDTO registeredProfile = profileService.registerProfile(profileDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredProfile);
    }

    @GetMapping("/activate")
    public ResponseEntity<String> activateProfile(@RequestParam("token") String activationToken) {
        boolean isActivated = profileService.activateProfile(activationToken);
        if (isActivated) {
            return ResponseEntity.ok("Profile activated successfully");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid activation token");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthDTO authDTO) {
        try {
            if (!profileService.isAccountActive(authDTO.getEmail())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "message", "Account is not activated. Please activate your account first."
                ));
            }

            // Ensure this map includes token + user (ProfileDTO with fullName)
            Map<String, Object> response = profileService.authenticateAndGenerateToken(authDTO);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileDTO profileDTO) {
        try {
            ProfileDTO updated = profileService.updateProfile(profileDTO);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/update-password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> passwordData) {
        try {
            profileService.updatePassword(
                    passwordData.get("currentPassword"),
                    passwordData.get("newPassword")
            );
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileDTO> getPublicProfile(Authentication authentication) {
        // Use authenticated user identity instead of null
        String email = authentication != null ? authentication.getName() : null;
        ProfileDTO profileDTO = profileService.getPublicProfile(email);
        return ResponseEntity.ok(profileDTO);
    }

    @GetMapping("/test")
    public String test() {
        return "Test successful";
    }
}