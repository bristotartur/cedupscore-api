package com.bristotartur.cedupscore_api.services;

import com.bristotartur.cedupscore_api.domain.EventRegistration;
import com.bristotartur.cedupscore_api.domain.Participant;
import com.bristotartur.cedupscore_api.domain.Team;
import com.bristotartur.cedupscore_api.dtos.request.EventRegistrationRequestDto;
import com.bristotartur.cedupscore_api.dtos.request.ParticipantFilterDto;
import com.bristotartur.cedupscore_api.dtos.request.ParticipantRequestDto;
import com.bristotartur.cedupscore_api.dtos.response.ParticipantResponseDto;
import com.bristotartur.cedupscore_api.enums.Status;
import com.bristotartur.cedupscore_api.exceptions.NotFoundException;
import com.bristotartur.cedupscore_api.exceptions.UnprocessableEntityException;
import com.bristotartur.cedupscore_api.mappers.ParticipantMapper;
import com.bristotartur.cedupscore_api.mappers.RegistrationMapper;
import com.bristotartur.cedupscore_api.repositories.EditionRegistrationRepository;
import com.bristotartur.cedupscore_api.repositories.EventRegistrationRepository;
import com.bristotartur.cedupscore_api.repositories.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.bristotartur.cedupscore_api.repositories.ParticipantSpecifications.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final EditionRegistrationRepository editionRegistrationRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final ParticipantMapper participantMapper;
    private final RegistrationMapper registrationMapper;
    private final TeamService teamService;
    private final EditionService editionService;
    private final EventService eventService;
    private final ParticipantValidationService participantValidator;

    public Page<Participant> findAllParticipants(ParticipantFilterDto filter, Pageable pageable) {
        var order = (filter.order() != null) ? filter.order() : "";
        var sort = switch (order) {
            case "a-z" -> Sort.by("name").ascending();
            case "z-a" -> Sort.by("name").descending();

            default -> Sort.by("id").descending();
        };
        var spec = Specification.where(hasName(filter.name())
                .and(fromEdition(filter.edition()))
                .and(fromEvent(filter.event(), filter.edition()))
                .and(notFromEvent(filter.notInEvent(), filter.edition()))
                .and(fromTeam(filter.team(), filter.edition()))
                .and(hasGender(filter.gender()))
                .and(hasType(filter.type()))
                .and(hasStatus(filter.status()))
        );
        return participantRepository.findAll(spec, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort));
    }

    public Page<Participant> findAllParticipants(ParticipantFilterDto filter, List<Long> excludeIds, Pageable pageable) {
        var order = (filter.order() != null) ? filter.order() : "";
        var sort = switch (order) {
            case "a-z" -> Sort.by("name").ascending();
            case "z-a" -> Sort.by("name").descending();

            default -> Sort.by("id").descending();
        };
        var spec = Specification.where(hasName(filter.name())
                .and(fromEdition(filter.edition()))
                .and(fromEvent(filter.event(), filter.edition()))
                .and(notFromEvent(filter.notInEvent(), filter.edition()))
                .and(fromTeam(filter.team(), filter.edition()))
                .and(hasGender(filter.gender()))
                .and(hasType(filter.type()))
                .and(hasStatus(filter.status()))
                .and(withoutIds(excludeIds))
        );
        return participantRepository.findAll(spec, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort));
    }

    public Participant findParticipantById(Long id) {

        return participantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Participante não encontrado."));
    }

    public Participant findParticipantByCpf(String cpf) {
        participantValidator.validateCpf(cpf);
        return participantRepository.findByCpf(cpf)
                .orElseThrow(() -> new NotFoundException("Participante não encontrado."));
    }

    public ParticipantResponseDto createParticipantResponseDto(Participant participant, Boolean hasCpf) {
        var registrations = participant.getEditionRegistrations()
                .stream()
                .map(registration -> {
                    var teamDto = teamService.createTeamResponseDto(registration.getTeam());
                    return registrationMapper.toEditionRegistrationResponseDto(registration, teamDto);
                }).toList();

        return participantMapper.toParticipantResponseDto(participant, registrations, hasCpf);
    }

    public ParticipantResponseDto createParticipantResponseDto(Participant participant, EventRegistration eventRegistration, Boolean hasCpf) {
        var eventRegistrationDto = registrationMapper.toEventRegistrationResponseDto(
                eventRegistration,
                teamService.createTeamResponseDto(eventRegistration.getTeam())
        );
        var registrations = participant.getEditionRegistrations()
                .stream()
                .map(registration -> {
                    var teamDto = teamService.createTeamResponseDto(registration.getTeam());
                    return registrationMapper.toEditionRegistrationResponseDto(registration, teamDto);
                }).toList();

        return participantMapper.toParticipantResponseDto(participant, registrations, eventRegistrationDto, hasCpf);
    }

    public Participant saveParticipant(ParticipantRequestDto dto) {
        participantValidator.validateCpf(dto.cpf());

        participantRepository.findByCpf(dto.cpf()).stream()
                .findFirst()
                .ifPresent(p -> {
                    throw new UnprocessableEntityException("O CPF fornecido já está em uso.");
                });
        var participant = participantMapper.toNewParticipant(dto);
        participant.setName(participant.getName().toUpperCase(Locale.ROOT));

        var currentEdition = editionService.findByStatusDifferentThen(Status.ENDED, Status.CANCELED)
                .stream()
                .findFirst()
                .orElseThrow(() -> new UnprocessableEntityException(
                        "No momento nenhum participante pode ser inscrito, pois não há nenhuma edição agendada."
                ));
        var savedParticipant = participantRepository.save(participant);
        return this.registerParticipantInEdition(savedParticipant, currentEdition.getId(), dto.teamId());
    }

    public Participant registerParticipantInEdition(Participant participant, Long editionId, Long teamId) {
        var edition = editionService.findEditionById(editionId);
        var team = teamService.findTeamById(teamId);

        participantValidator.validateParticipantAndTeamActive(participant, team);
        var registrationOptional = participantValidator.validateParticipantForEdition(participant, edition);

        if (registrationOptional.isPresent()) {
            var existingRegistration = registrationOptional.get();

            participant.getEditionRegistrations().remove(existingRegistration);
            editionRegistrationRepository.delete(existingRegistration);
        }
        var registration = editionRegistrationRepository.save(
                registrationMapper.toNewEditionRegistration(participant, edition, team)
        );
        participant.getEditionRegistrations().add(registration);
        return participant;
    }

    public Participant registerParticipantInEvent(Participant participant, Long eventId, Long teamId) {
        var event = eventService.findEventById(eventId);
        var team = teamService.findTeamById(teamId);

        participantValidator.validateParticipantAndTeamActive(participant, team);
        participantValidator.validateParticipantTeamForEvent(participant, team, event);

        var registeredParticipants = participantRepository.findByTeamAndEvent(team, event).size();
        participantValidator.validateParticipantForEvent(participant, event, registeredParticipants);

        var registration = eventRegistrationRepository.save(
                registrationMapper.toNewEventRegistration(participant, event, team)
        );
        participant.getEventRegistrations().add(registration);
        return participant;
    }

    public List<Participant> registerAllParticipantsInEvent(List<EventRegistrationRequestDto> dtos, Long eventId) {
        var event = eventService.findEventById(eventId);
        var teamToParticipants = this.createTeamToParticipantsMap(dtos);

        var registrations = new HashSet<EventRegistration>();
        var teamToRegistrationCounts = event.getRegistrations()
                .stream()
                .collect(Collectors.groupingBy(
                        EventRegistration::getTeam, Collectors.counting())
                );
        teamToParticipants.forEach((team, participants) -> {
            var registeredParticipantsCount = new AtomicInteger(teamToRegistrationCounts.getOrDefault(team, 0L).intValue());

            participants.forEach(participant -> {
                participantValidator.validateParticipantAndTeamActive(participant, team);
                participantValidator.validateParticipantTeamForEvent(participant, team, event);
                participantValidator.validateParticipantForEvent(participant, event, registeredParticipantsCount.get());

                registeredParticipantsCount.incrementAndGet();
                registrations.add(registrationMapper.toNewEventRegistration(participant, event, team));
            });
        });
        return eventRegistrationRepository.saveAll(registrations)
                .stream()
                .map(EventRegistration::getParticipant).toList();
    }

    private Map<Team, List<Participant>> createTeamToParticipantsMap(List<EventRegistrationRequestDto> dtos) {
        var participantIds = dtos.stream()
                .map(EventRegistrationRequestDto::participantId).collect(Collectors.toSet());
        var teamsIds = dtos.stream()
                .map(EventRegistrationRequestDto::teamId).collect(Collectors.toSet());

        var participants = participantRepository.findAllById(participantIds);
        var teams = teamService.findAllTeamsById(teamsIds);

        var idToParticipants = participants.stream()
                .collect(Collectors.toMap(Participant::getId, Function.identity()));
        var idToTeams = teams.stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));

        return dtos.stream()
                .collect(Collectors.groupingBy(
                        dto -> this.getTeamById(idToTeams, dto.teamId()),
                        Collectors.mapping(
                                dto -> this.getParticipantById(idToParticipants, dto.participantId()),
                                Collectors.toList()
                        )
                ));
    }

    private Team getTeamById(Map<Long, Team> idToTeams, Long teamId) {

        return Optional.ofNullable(idToTeams.get(teamId))
                .orElseThrow(() -> new NotFoundException("Equipe não encontrada."));
    }

    private Participant getParticipantById(Map<Long, Participant> idToParticipants, Long participantId) {

        return Optional.ofNullable(idToParticipants.get(participantId))
                .orElseThrow(() -> new NotFoundException("Participante não encontrado."));
    }

    public void deleteParticipant(Long id) {
        var participant = this.findParticipantById(id);
        var registrations = participant.getEditionRegistrations();

        if (registrations.size() >= 2) {
            throw new UnprocessableEntityException("O participante não pode ser removido.");
        }
        var edition = registrations.iterator().next().getEdition();

        if (!edition.getStatus().equals(Status.SCHEDULED)) {
            throw new UnprocessableEntityException("O participante não pode ser removido.");
        }
        participantRepository.delete(participant);
    }

    public void deleteEditionRegistration(Long id, Long registrationId) {
        var participant = this.findParticipantById(id);
        var registration = editionRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Inscrição não encontrada."));

        participantValidator.validateEditionRegistrationToRemove(participant, registration);

        participant.getEditionRegistrations().remove(registration);
        editionRegistrationRepository.delete(registration);
    }

    public void deleteEventRegistration(Long id, Long registrationId) {
        var participant = this.findParticipantById(id);
        var registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Inscrição não encontrada."));

        participantValidator.validateEventRegistrationToRemove(participant, registration);

        participant.getEventRegistrations().remove(registration);
        eventRegistrationRepository.delete(registration);
    }

    public void deleteAllEventRegistrationsById(Long eventId, List<Long> registrationsIds) {
        var event = eventService.findEventById(eventId);
        var eventStatus = event.getStatus();

        if (!eventStatus.equals(Status.SCHEDULED)) {
            throw new UnprocessableEntityException("Nenhum participante pode ser desinscrito, pois o evento não está mais agendado.");
        }
        eventRegistrationRepository.deleteAllById(registrationsIds);
    }

    public Participant replaceParticipant(Long id, ParticipantRequestDto dto) {
        var participant = this.findParticipantById(id);
        var isActive = participant.getIsActive();

        participantValidator.validateCpf(dto.cpf());
        participantRepository.findByCpf(dto.cpf()).stream()
                .findFirst()
                .ifPresent(p -> {
                    if (!p.equals(participant)) {
                        throw new UnprocessableEntityException("O CPF fornecido já está em uso.");
                    }
                });
        var newParticipant = participantMapper.toExistingParticipant(id, dto, isActive);
        newParticipant.setName(newParticipant.getName().toUpperCase(Locale.ROOT));

        return participantRepository.save(newParticipant);
    }

    public Participant setParticipantStatus(Long id, Boolean status) {
        var participant = this.findParticipantById(id);

        if (status == participant.getIsActive()) return participant;

        this.participantValidator.validateParticipantToChangeStatus(participant);
        participant.setIsActive(status);

        return participantRepository.save(participant);
    }

}
