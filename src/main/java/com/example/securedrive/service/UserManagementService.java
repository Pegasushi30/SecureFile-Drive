// com/example/securedrive/service/UserManagementService.java
package com.example.securedrive.service;

import com.example.securedrive.model.User;

import java.util.Optional;
import java.util.Set;

public interface UserManagementService {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Set<User> getContactsForUser(String username);
}
