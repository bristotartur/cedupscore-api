package com.bristotartur.cedupscore_api.services;

import com.bristotartur.cedupscore_api.domain.people.Team;
import com.bristotartur.cedupscore_api.domain.scores.TeamScore;
import com.bristotartur.cedupscore_api.dtos.request.TeamRequestDto;
import com.bristotartur.cedupscore_api.dtos.response.TeamResponseDto;
import com.bristotartur.cedupscore_api.enums.Status;
import com.bristotartur.cedupscore_api.exceptions.ConflictException;
import com.bristotartur.cedupscore_api.exceptions.NotFoundException;
import com.bristotartur.cedupscore_api.exceptions.UnprocessableEntityException;
import com.bristotartur.cedupscore_api.mappers.TeamMapper;
import com.bristotartur.cedupscore_api.repositories.people.TeamRepository;
import com.bristotartur.cedupscore_api.repositories.scores.TeamScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final TeamScoreRepository teamScoreRepository;

    public List<Team> findAllTeams() {
        return teamRepository.findAll();
    }

    public List<Team> findAllActiveTeams() {

        return teamRepository.findAll()
                .stream()
                .filter(Team::getIsActive)
                .toList();
    }

    public Team findTeamById(Long id) {

        return teamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipe não encontrada."));
    }

    public TeamResponseDto createTeamResponseDto(Team team) {
        return teamMapper.toTeamResponseDto(team);
    }

    public Team saveTeam(TeamRequestDto dto) {

        teamRepository.findByName(dto.name()).ifPresent(team -> {
            throw new ConflictException("O nome '%s' já está em uso.".formatted(team.getName()));
        });
        teamRepository.findByLogo(dto.logo()).ifPresent(team -> {
            throw new ConflictException("A logo '%s' já está em uso.".formatted(team.getLogo()));
        });
        return teamRepository.save(teamMapper.toNewTeam(dto));
    }

    public void deleteTeam(Long id) {
        var team = this.findTeamById(id);

        if (!teamScoreRepository.findAllByTeam(team).isEmpty()) {
            throw new UnprocessableEntityException("A equipe não pode ser removida");
        }
        teamRepository.delete(team);
    }

    public Team replaceTeam(Long id, TeamRequestDto dto) {
        var team = this.findTeamById(id);

        if (!team.getName().equals(dto.name())) {
            teamRepository.findByName(dto.name()).ifPresent(t -> {
                throw new ConflictException("O nome '%s' já está em uso.".formatted(t.getName()));
            });
        }
        if (!team.getLogo().equals(dto.logo())) {
            teamRepository.findByLogo(dto.logo()).ifPresent(t -> {
                throw new ConflictException("A logo '%s' já está em uso.".formatted(t.getLogo()));
            });
        }
        return teamRepository.save(teamMapper.toExistingTeam(id, dto, team.getIsActive()));
    }

    public Team setTeamActive(Long id, boolean isActive) {
        var team = this.findTeamById(id);

        if (isActive == team.getIsActive()) return team;

        if (!isActive) {
            teamScoreRepository.findAllByTeam(team)
                    .stream()
                    .map(TeamScore::getEdition)
                    .filter(edition -> edition.getStatus().equals(Status.IN_PROGRESS))
                    .findFirst()
                    .ifPresent(edition -> {
                        throw new ConflictException("A equipe não pode ser desativada");
                    });
        }
        team.setIsActive(isActive);
        return teamRepository.save(team);
    }

}
