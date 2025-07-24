package com.back.domain.user.dto;


import com.back.domain.user.entity.User;
import lombok.Getter;

@Getter
public class UserDto {
    private Long id;
    private String name;
    private String email;

    public UserDto(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
    }
}
