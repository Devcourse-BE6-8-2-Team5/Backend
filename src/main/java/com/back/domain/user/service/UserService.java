package com.back.domain.user.service;

import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User join(String name,String password, String email) {

        User user = User.builder()
                .name(name)
                .password(password)
                .email(email)
                .exp(0)
                .level(1)
                .isAdmin(false)
                .apiKey(UUID.randomUUID().toString())
                .build();

        return userRepository.save(user);

    }

    public Optional<User> findByName(String name) {
        return userRepository.findByName(name);
    }
}
