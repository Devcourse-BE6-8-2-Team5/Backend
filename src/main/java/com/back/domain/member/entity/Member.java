package com.back.domain.member.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;

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

    @Column(nullable = false, unique = true)
    private String apiKey; //리프레시 토큰

    @Column(nullable = false)
    private boolean isAdmin;

    // 유저가 푼 퀴즈 기록을 저장하는 리스트 일단 엔티티 없어서 주석
   //@OneToMany(mappedBy ="member", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<memberQuizAnswer> memberQuizAnswers = new ArrayList<>(); //유저가 퀴즈를 푼 기록


    public boolean isAdmin() {
        return isAdmin;
    }
}
