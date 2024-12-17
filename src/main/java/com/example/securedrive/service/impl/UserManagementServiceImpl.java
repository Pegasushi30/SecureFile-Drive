package com.example.securedrive.service.impl;

import com.example.securedrive.model.User;
import com.example.securedrive.repository.UserRepository;
import com.example.securedrive.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;

    @Autowired
    public UserManagementServiceImpl(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}

