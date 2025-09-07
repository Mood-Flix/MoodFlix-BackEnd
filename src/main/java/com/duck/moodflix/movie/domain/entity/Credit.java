package com.duck.moodflix.movie.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "credits",
        indexes = {
                @Index(name = "ix_credits_movie_id", columnList = "movie_id"),
                @Index(name = "ix_credits_person_id", columnList = "person_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_credits_movie_person_role",
                        columnNames = {"movie_id", "person_id", "job", "character_name"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Credit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false)
    private String department; // "Acting"(출연), "Directing"(연출) 등 부서

    private String job; // "Director"(감독), "Actor"(배우) 등 직책

    @Column(name = "character_name")
    private String characterName; // 배역 이름 (배우인 경우)

    @Builder
    public Credit(Movie movie, Person person, String department, String job, String characterName) {
        this.movie = movie;
        this.person = person;
        this.department = department;
        this.job = job;
        this.characterName = characterName;
    }
}