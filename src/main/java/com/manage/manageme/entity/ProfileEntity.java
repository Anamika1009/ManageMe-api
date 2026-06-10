package com.manage.manageme.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity     // this annotation tells Spring that this class is an entity and will be mapped to a table in the database
@Table(name = "tbl_profile")  // this annotation tells Spring that the table name in the database will be "tbl_profile", if we don't specify this annotation, then the table name will be "profile_entity" by default
@Data   // this annotation generates getters and setters for all fields, toString, equals and hashCode methods
@NoArgsConstructor  // this annotation generates a no-argument constructor
@AllArgsConstructor // this annotation generates a constructor with all arguments
@Builder  // this annotation generates a builder pattern for the class, which allows us to create objects in a more readable way,
// for example: ProfileEntity profile = ProfileEntity.builder().fullname("John Doe").build();
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // this annotation tells Spring that the id field will be generated automatically by the database, and the strategy is IDENTITY, which means that the database will generate a unique value for the id field
    private Long id;
    @Column(name = "fullname")
    private String fullName;
    @Column(unique = true)
    private String email;
    private String password;
    private String profileImageUrl;
    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    private Boolean isActive;
    private String activationToken;     // this field will be used to store the activation token for the profile, which will be used to activate the profile
    // the activation token will be generated when the profile is created and will be sent to the user's email address, the user will then click on the activation link in the email to activate their profile
    // if profile is activated, then activationToken will be null
    // if profile is not activated, then activationToken will be a random string

    @PrePersist  // this method will be called before the entity is persisted to the database
    // persist means save the entity to the database
    public void prePersist() {
        if (this.isActive == null){
            isActive = false;
            // if the profile is not active, then we will generate an activation token for the profile
        }
    }

}