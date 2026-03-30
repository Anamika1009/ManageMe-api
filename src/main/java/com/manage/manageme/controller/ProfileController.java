package com.manage.manageme.controller;

import com.manage.manageme.dto.AuthDTO;
import com.manage.manageme.dto.ProfileDTO;
import com.manage.manageme.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor

public class ProfileController {
    private final ProfileService profileService;
    @PostMapping("/register")
    public ResponseEntity<ProfileDTO> registerProfile(
        @RequestBody ProfileDTO profileDTO){

        ProfileDTO registeredProfile = profileService.registerProfile(profileDTO);
        // we will implement the logic to register a profile here, for example, we will check if the email is already registered, if not, then we will save the profile to the database and return the profile DTO
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredProfile);
    }
    @GetMapping("/activate")
    public ResponseEntity<String> activateProfile(@RequestParam("token") String activationToken){
        boolean isActivated = profileService.activateProfile(activationToken);
        if(isActivated){
            return ResponseEntity.ok("Profile activated successfully");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid activation token");
        }
    }
    @PostMapping("/login")
    public ResponseEntity<Map < String, Object >> login(@RequestBody AuthDTO authDTO){
        try {
            if (!profileService.isAccountActive(authDTO.getEmail())) {

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "Message", "Account is not activated. Please activate your account first."
                ));
            }
            Map <String, Object> response = profileService.authenticateAndGenerateToken(authDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "Message", e.getMessage()
            ));
        }

    }

    @GetMapping("/test")
    public String test(){
        return "Test successful";
   }
}
