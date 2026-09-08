package com.example.projectCollab.repository;

import com.example.projectCollab.entity.Role;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByStudentId(String studentId);

    // ✅ NEW: Find users by roles and status
    List<User> findByRoleInAndStatus(List<Role> roles, UserStatus status);
}