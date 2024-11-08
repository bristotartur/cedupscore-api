package com.bristotartur.cedupscore_api.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "TB_EDITION_REGISTRATION")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EditionRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "edition_id", nullable = false)
    private Edition edition;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Override
    public String toString() {
        return "EditionRegistration{" +
                "id=" + id +
                ", participant=" + participant +
                ", team=" + team +
                ", edition=" + edition +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EditionRegistration that = (EditionRegistration) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

}
