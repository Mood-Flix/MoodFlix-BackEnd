package com.duck.moodflix.movie.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "persons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long tmdbId; // TMDb의 인물 고유 ID

    @Column(nullable = false)
    private String name;

    private String profileUrl;

    @Builder
    public Person(Long tmdbId, String name, String profileUrl) {
        this.tmdbId = tmdbId;
        this.name = name;
        this.profileUrl = profileUrl;
    }
}