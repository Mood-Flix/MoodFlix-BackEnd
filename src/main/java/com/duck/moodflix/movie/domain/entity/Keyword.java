package com.duck.moodflix.movie.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "keywords",
        indexes = {
                @Index(name = "idx_keywords_name", columnList = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;
}
