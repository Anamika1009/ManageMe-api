package com.manage.manageme.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ProfileDTO {

    private Long id;

    // Sends as fullName, but accepts fullname/full_name from older clients
    @JsonProperty("fullName")
    @JsonAlias({"fullname", "full_name"})
    private String fullName;

    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String profileImageUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}