package com.back.domain.member.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.PRIVATE)
    @EqualsAndHashCode.Include
    private long id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @Min(0)
    private int exp; //경험치

    @Min(1)
    private int level; //레벨

    @Column(nullable = false)
    private boolean isAdmin;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false, unique = true)
    private String apiKey; //리프레시 토큰



    // 유저가 푼 퀴즈 기록을 저장하는 리스트 일단 엔티티 없어서 주석
   //@OneToMany(mappedBy ="member", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<memberQuizAnswer> memberQuizAnswers = new ArrayList<>(); //유저가 퀴즈를 푼 기록

    public Member(long id, String email, String name) {
        setId(id);
        this.email = email;
        setName(name);
    }

    public Member(long id, String email, String name, String role) {
        setId(id);
        this.email = email;
        setName(name);
        this.role = role;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getAuthoritiesAsStringList()
                .stream()
                .map(auth -> new SimpleGrantedAuthority("ROLE_" + auth))
                .toList();
    }

    private List<String> getAuthoritiesAsStringList() {
        List<String> authorities = new ArrayList<>();
        // 관리자면 ROLE_ADMIN, 아니면 ROLE_USER
        if (isAdmin()) {
            authorities.add("ADMIN");
        } else {
            authorities.add("USER");
        }
        return authorities;
    }

    public boolean isAdmin() {
        return isAdmin;
    }
}
