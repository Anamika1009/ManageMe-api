package com.manage.manageme.repository;

import com.manage.manageme.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {
    // this interface will be used to perform CRUD operations on the ProfileEntity, and it will also provide some additional methods for querying the database, such as findByEmail, findByActivationToken, etc.
    Optional<ProfileEntity> findByEmail(String email);
    // this method will be used to find a profile by email, and it will return an Optional<ProfileEntity>,
    // which means that it can return a profile, or it can return null if the profile is not found
    Optional<ProfileEntity> findByActivationToken(String activationToken);
    // this method will be used to find a profile by activation token, and it will return
    // an Optional<ProfileEntity>, which means that it can return a profile, or it can return null if the profile is not found

}
