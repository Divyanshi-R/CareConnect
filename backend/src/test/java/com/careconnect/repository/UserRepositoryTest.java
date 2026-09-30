package com.careconnect.repository;

import com.careconnect.dto.UserResponse;
import com.careconnect.entity.Role;
import com.careconnect.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully persist and find a user by email")
    void shouldSaveAndFindUserByEmail() {
        User user = new User("doctor.smith@careconnect.com", "$2a$10$hashedPasswordValue", Role.DOCTOR, true);
        User savedUser = userRepository.saveAndFlush(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();

        Optional<User> found = userRepository.findByEmail("doctor.smith@careconnect.com");
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(Role.DOCTOR);
        assertThat(found.get().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("existsByEmail should return true for existing user and false for non-existent email")
    void shouldCheckIfEmailExists() {
        User user = new User("patient.jane@careconnect.com", "$2a$10$hashedPasswordValue", Role.PATIENT, true);
        userRepository.saveAndFlush(user);

        assertThat(userRepository.existsByEmail("patient.jane@careconnect.com")).isTrue();
        assertThat(userRepository.existsByEmail("unknown@careconnect.com")).isFalse();
    }

    @Test
    @DisplayName("findByRole should return all users matching the requested role")
    void shouldFindUsersByRole() {
        userRepository.save(new User("admin@careconnect.com", "$2a$10$hash1", Role.ADMIN, true));
        userRepository.save(new User("doc1@careconnect.com", "$2a$10$hash2", Role.DOCTOR, true));
        userRepository.save(new User("doc2@careconnect.com", "$2a$10$hash3", Role.DOCTOR, true));
        userRepository.save(new User("patient1@careconnect.com", "$2a$10$hash4", Role.PATIENT, true));
        userRepository.flush();

        List<User> doctors = userRepository.findByRole(Role.DOCTOR);
        assertThat(doctors).hasSize(2);
        assertThat(doctors).extracting(User::getEmail)
                .containsExactlyInAnyOrder("doc1@careconnect.com", "doc2@careconnect.com");

        long adminCount = userRepository.countByRole(Role.ADMIN);
        assertThat(adminCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw DataIntegrityViolationException when inserting duplicate email")
    void shouldEnforceUniqueEmailConstraint() {
        User user1 = new User("unique@careconnect.com", "$2a$10$hash1", Role.PATIENT, true);
        userRepository.saveAndFlush(user1);

        User duplicateUser = new User("unique@careconnect.com", "$2a$10$hash2", Role.DOCTOR, true);

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicateUser))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("UserResponse DTO should map entity fields without exposing password")
    void shouldMapToUserResponseDtoSafely() {
        User user = new User("admin@careconnect.com", "$2a$10$sensitivePasswordHash", Role.ADMIN, true);
        User savedUser = userRepository.saveAndFlush(user);

        UserResponse dto = UserResponse.fromEntity(savedUser);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(savedUser.getId());
        assertThat(dto.email()).isEqualTo("admin@careconnect.com");
        assertThat(dto.role()).isEqualTo(Role.ADMIN);
        assertThat(dto.enabled()).isTrue();
        assertThat(dto.createdAt()).isNotNull();
        assertThat(dto.updatedAt()).isNotNull();
    }
}
