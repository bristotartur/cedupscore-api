package com.bristotartur.cedupscore_api.repositories.people;

import com.bristotartur.cedupscore_api.domain.people.Team;
import com.bristotartur.cedupscore_api.enums.TeamLogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByName(String name);

    Optional<Team> findByLogo(TeamLogo logo);

}
