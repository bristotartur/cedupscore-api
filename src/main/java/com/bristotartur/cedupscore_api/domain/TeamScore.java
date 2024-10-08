package com.bristotartur.cedupscore_api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "TB_TEAM_SCORE")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class TeamScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer score;


    @Column(nullable = false)
    private Integer tasksWon;

    @Column(nullable = false)
    private Integer sportsWon;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "edition_id", nullable = false)
    private Edition edition;

    @Override
    public String toString() {
        return "TeamScore{" +
                "id=" + id +
                ", score=" + score +
                ", tasksWon=" + tasksWon +
                ", sportsWon=" + sportsWon +
                ", team=" + team +
                ", edition=" + edition +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamScore teamScore = (TeamScore) o;
        return Objects.equals(id, teamScore.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

}
