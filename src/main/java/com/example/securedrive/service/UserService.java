package com.example.securedrive.service;

import com.example.securedrive.model.User;
import com.example.securedrive.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;


    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    public void saveUser(User user) {
        userRepository.save(user);
    }
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}

