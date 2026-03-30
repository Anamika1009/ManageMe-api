package com.manage.manageme.service;

import com.manage.manageme.dto.AuthDTO;
import com.manage.manageme.dto.ProfileDTO;
import com.manage.manageme.entity.ProfileEntity;
import com.manage.manageme.repository.ProfileRepository;
import com.manage.manageme.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    @Value("${app.activation.url}")
    private String activationURL;

    // Register new profile
    // This method takes a ProfileDTO object as input, converts it to a ProfileEntity, generates an activation token, saves the profile in the database, and sends an activation email to the user.
    // This is taken by the user when they want to create a new profile. The method will handle the registration process, including saving the profile and sending the activation email.

    public ProfileDTO registerProfile(ProfileDTO profileDTO) {

        ProfileEntity newProfile = toEntity(profileDTO);

        // Generate activation token
        newProfile.setActivationToken(UUID.randomUUID().toString());

        // Save profile in database
        newProfile = profileRepository.save(newProfile);

        // Send activation email
        String activationLink = activationURL+
                "/api/v1.0/activate?token="
                        + newProfile.getActivationToken();

        String subject = "Activate your Money Management profile";

        String body = "Click on the following link to activate your profile:\n"
                + activationLink;

        emailService.sendEmail(newProfile.getEmail(), subject, body);

        return toDTO(newProfile);
    }

    // Convert DTO → Entity
    // This method takes a ProfileDTO object and converts it into a ProfileEntity object.
    // This method is used when we want to save a profile to the database. It takes the data from the DTO, encodes the password for security, and creates an entity that can be persisted in the database.
    public ProfileEntity toEntity(ProfileDTO profileDTO) {

        return ProfileEntity.builder()
                .id(profileDTO.getId())
                .FullName(profileDTO.getFullname())
                .email(profileDTO.getEmail())
                .password(passwordEncoder.encode(profileDTO.getPassword())) // This will encode the password before saving,
                // encode means to convert the password into a secure format that cannot be easily reversed,
                // ensuring that even if the database is compromised, the actual passwords remain protected.
                .profileImageUrl(profileDTO.getProfileImageeUrl())
                .createdAt(profileDTO.getCreatedAt())
                .updatedAt(profileDTO.getUpdatedAt())
                .build();
    }

    // This method is used when we want to return profile data to the client.
    // It takes the data from the entity, which is the format used for database storage,
    // and converts it into a DTO format that is suitable for transferring data to the client,
    // ensures the sensitive information not included in dto.

    public ProfileDTO toDTO(ProfileEntity profileEntity) {

        return ProfileDTO.builder()
                .id(profileEntity.getId())
                .fullname(profileEntity.getFullName())
                .email(profileEntity.getEmail())
                .profileImageeUrl(profileEntity.getProfileImageUrl())
                .createdAt(profileEntity.getCreatedAt())
                .updatedAt(profileEntity.getUpdatedAt())
                .build();
    }

    // Activate profile using activation token
    // This method takes an activation token as input,
    // finds the corresponding profile in the database,
    // and if found, clears the activation token to activate the profile.

    public boolean activateProfile(String activationToken) {
        return profileRepository.findByActivationToken(activationToken)
                .map(profile -> {
                    profile.setActivationToken(null); // Ye token ko clear karega
                    profile.setIsActive(true);        // 👉 YAHI LINE MISSING THI! Ye database mein 0 se 1 karega
                    profileRepository.save(profile);  // Database mein update save ho jayega
                    return true;
                })
                .orElse(false);
    }

    // Check if the account is active before allowing login
    public boolean isAccountActive(String email) {
        return profileRepository.findByEmail(email)
                .map(ProfileEntity::getIsActive) // Returns true if isActive is 1 in DB
                .orElse(false); // Returns false if user is not found
    }

    public ProfileEntity getCurrentProfile(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return profileRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Profile not found for email: " + authentication.getName()));

    }

    public ProfileDTO getPublicProfile(String email) {
        ProfileEntity currentUser = null;
        if (email == null) {
            currentUser = getCurrentProfile();
        } else {
            currentUser = profileRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Profile not found for email: " + email));
        }
        return ProfileDTO.builder()
                .id(currentUser.getId())
                .fullname(currentUser.getFullName())
                .email(currentUser.getEmail())
                .profileImageeUrl(currentUser.getProfileImageUrl())
                .createdAt(currentUser.getCreatedAt())
                .updatedAt(currentUser.getUpdatedAt())
                .build();
    }
// Authenticate user and generate JWT token
// This method takes an AuthDTO object containing the user's email and password,basically, authenticates the user, and if successful, generates a JWT token for the user.
// this method is responsible for handling the login process.
// It will verify the user's credentials against the stored data in the database,
// and if the authentication is successful, it will generate a JWT token that can be used for subsequent authenticated requests to the server.
// The method will return a map containing the token and any relevant user information that may be needed by the client after a successful login.


    public Map<String, Object> authenticateAndGenerateToken(AuthDTO authDTO) {
        try{
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authDTO.getEmail(), authDTO.getPassword()));
            // If authentication is successful, generate JWT token
            String token = jwtUtil.generateToken(authDTO.getEmail());
            return Map.of(
                    "token",token,
                    "user", getPublicProfile(authDTO.getEmail())
            );
        }catch(Exception e){
            throw new RuntimeException("Invalid email or password");

        }

    }
}