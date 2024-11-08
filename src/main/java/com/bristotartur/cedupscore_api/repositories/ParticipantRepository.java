package com.bristotartur.cedupscore_api.repositories;

import com.bristotartur.cedupscore_api.domain.Event;
import com.bristotartur.cedupscore_api.domain.Participant;
import com.bristotartur.cedupscore_api.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long>, JpaSpecificationExecutor<Participant> {

    @Query("""
        SELECT er.participant FROM EventRegistration er
        WHERE
            er.team = :team AND er.event = :event
    """)
    List<Participant> findByTeamAndEvent(@Param("team") Team team, @Param("event") Event event);

    @Query("SELECT p FROM Participant p WHERE p.cpf = :cpf")
    Optional<Participant> findByCpf(@Param("cpf") String cpf);

    List<Participant> findByCpfIn(Set<String> cpfs);

}
