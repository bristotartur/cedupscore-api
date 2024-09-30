package com.bristotartur.cedupscore_api.domain;

import com.bristotartur.cedupscore_api.dtos.request.ParticipantRequestDto;
import com.bristotartur.cedupscore_api.enums.Gender;
import com.bristotartur.cedupscore_api.enums.ParticipantType;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "TB_PARTICIPANT")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String cpf;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ParticipantType type;

    @Column(nullable = false)
    private Boolean isActive;

    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<EditionRegistration> editionRegistrations = new HashSet<>();

    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<EventRegistration> eventRegistrations = new HashSet<>();

    @Override
    public String toString() {
        return "Participant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", cpf='" + cpf + '\'' +
                ", gender=" + gender +
                ", type=" + type +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participant that = (Participant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public boolean compareTo(ParticipantRequestDto dto) {
        if (dto == null) return false;
        return Objects.equals(name, dto.getName()) &&
                Objects.equals(cpf, dto.getCpf()) &&
                Objects.equals(gender, dto.getGender()) &&
                Objects.equals(type, dto.getType());
    }

}
