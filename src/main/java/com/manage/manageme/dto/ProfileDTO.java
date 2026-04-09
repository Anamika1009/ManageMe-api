package com.manage.manageme.dto;
// DTO package is used to transfer data between the client and the server,
// it is a simple Java class that contains only fields and getters and setters,
// it does not contain any business logic,
// it is used to encapsulate the data that we want to transfer between the client and the server,
// and it is also used to validate the data that we receive from the client before we process it in the service layer,
// for example, we can use DTOs to validate the email format, password strength, etc.
// before we save the profile to the database.
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {
    private Long id;
    private String fullName;
    private String email;
    private String password;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
