package com.bristotartur.cedupscore_api.controllers;

import com.bristotartur.cedupscore_api.dtos.request.ParticipantCSVDto;
import com.bristotartur.cedupscore_api.dtos.request.ParticipantRequestDto;
import com.bristotartur.cedupscore_api.dtos.response.ParticipantInactivationReport;
import com.bristotartur.cedupscore_api.dtos.response.ParticipantRegistrationReport;
import com.bristotartur.cedupscore_api.dtos.response.ParticipantResponseDto;
import com.bristotartur.cedupscore_api.enums.Gender;
import com.bristotartur.cedupscore_api.enums.ParticipantType;
import com.bristotartur.cedupscore_api.services.ParticipantCSVService;
import com.bristotartur.cedupscore_api.services.ParticipantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/participants")
@RequiredArgsConstructor
@Transactional
public class ParticipantController {

    private final ParticipantService participantService;
    private final ParticipantCSVService participantCSVService;

    @GetMapping
    public ResponseEntity<Page<ParticipantResponseDto>> listAllParticipants(@RequestParam(value = "name", required = false) String name,
                                                                            @RequestParam(value = "edition", required = false) Long editionId,
                                                                            @RequestParam(value = "team", required = false) Long teamId,
                                                                            @RequestParam(value = "gender", required = false) Gender gender,
                                                                            @RequestParam(value = "type", required = false) ParticipantType participantType,
                                                                            @RequestParam(value = "status", required = false) String status,
                                                                            @RequestParam(value = "order", required = false) String order,
                                                                            Pageable pageable) {
        var participants = participantService.findAllParticipants(name, editionId, teamId, gender, participantType, status, order, pageable);
        var dtos = participants.getContent()
                .stream()
                .map(participantService::createParticipantResponseDto)
                .toList();

        return ResponseEntity.ok().body(new PageImpl<>(dtos, pageable, participants.getTotalElements()));
    }

    @GetMapping(path = "/from-event/{eventId}")
    public ResponseEntity<Page<ParticipantResponseDto>> listParticipantsFromEvent(@PathVariable Long eventId,
                                                                                  Pageable pageable) {
        var participants = participantService.findParticipantsFromEvent(eventId, pageable);
        var dtos = participants.getContent()
                .stream()
                .map(participantService::createParticipantResponseDto)
                .toList();

        return ResponseEntity.ok().body(new PageImpl<>(dtos, pageable, participants.getTotalElements()));
    }

    @GetMapping(path = "/from-team/{teamId}/in-event/{eventId}")
    public ResponseEntity<Page<ParticipantResponseDto>> listParticipantsFromEventByTeam(@PathVariable Long teamId,
                                                                                        @PathVariable Long eventId,
                                                                                        Pageable pageable) {
        var participants = participantService.findParticipantsFromEventByTeam(teamId, eventId, pageable);
        var dtos = participants.getContent()
                .stream()
                .map(participantService::createParticipantResponseDto)
                .toList();

        return ResponseEntity.ok().body(new PageImpl<>(dtos, pageable, participants.getTotalElements()));
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<ParticipantResponseDto> findParticipantById(@PathVariable Long id) {
        var participant = participantService.findParticipantById(id);
        return ResponseEntity.ok().body(participantService.createParticipantResponseDto(participant));
    }

    @GetMapping(path = "/find")
    public ResponseEntity<ParticipantResponseDto> findParticipantByCpf(@RequestParam("cpf") String cpf) {
        var participant = participantService.findParticipantByCpf(cpf);
        return ResponseEntity.ok().body(participantService.createParticipantResponseDto(participant));
    }

    @PostMapping
    @PreAuthorize(
            "hasAnyAuthority('SCOPE_SUPER_ADMIN', 'SCOPE_EDITION_ADMIN')"
    )
    public ResponseEntity<ParticipantResponseDto> saveParticipant(@RequestBody @Valid ParticipantRequestDto requestDto) {
        var participant = participantService.saveParticipant(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(participantService.createParticipantResponseDto(participant));
    }

    @PostMapping(path = "upload/registration-csv", consumes = {"multipart/form-data"})
    @PreAuthorize(
            "hasAnyAuthority('SCOPE_SUPER_ADMIN', 'SCOPE_EDITION_ADMIN')"
    )
    public ResponseEntity<ParticipantRegistrationReport> uploadParticipantsRegistrationCSVFile(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty() || !file.getContentType().equals("text/csv")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
        return ResponseEntity.ok(participantCSVService.handleParticipantsRegistrationCSVFile(file));
    }

    @PostMapping(path = "upload/inactivation-csv", consumes = {"multipart/form-data"})
    @PreAuthorize(
            "hasAnyAuthority('SCOPE_SUPER_ADMIN', 'SCOPE_EDITION_ADMIN')"
    )
    public ResponseEntity<ParticipantInactivationReport> uploadParticipantsInactivationCSVFile(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty() || !file.getContentType().equals("text/csv")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
        return ResponseEntity.ok(participantCSVService.handleParticipantsInactivationCSVFile(file));
    }

    @PostMapping(path = "generate/csv")
    @PreAuthorize(
            "hasAnyAuthority('SCOPE_SUPER_ADMIN', 'SCOPE_EDITION_ADMIN')"
    )
    public ResponseEntity<byte[]> generateParticipantsCSV(@RequestParam("type") String type,
                                                          @RequestBody List<ParticipantCSVDto> dtos) {
        var csvBytes = participantCSVService.generateParticipantsCSV(type, dtos);
        var headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "participantes-com-problemas.csv");

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.CREATED);
    }

    @PostMapping(path = "/{id}/register-in-edition/{editionId}")
    @PreAuthorize(
            "hasAnyAuthority('SCOPE_SUPER_ADMIN', 'SCOPE_EDITION_ADMIN')"
    )
    public ResponseEntity<ParticipantResponseDto> registerInEdition(@PathVariable Long id,
                                                                    @PathVariable Long editionId,
                                                                    @RequestParam("team") Long teamId) {
        var participant = participantService.findParticipantById(id);
        var registeredParticipant = participantService.registerParticipantInEdition(participant, editionId, teamId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(participantService.createParticipantResponseDto(registeredParticipant));
    }

    @PostMapping(path = "/{id}/register-in-event/{eventId}")
    @PreAuthorize(
            "hasAnyAuthority('SCOPE_SUPER_ADMIN', 'SCOPE_EDITION_ADMIN')"
    )
    public ResponseEntity<ParticipantResponseDto> registerInEvent(@PathVariable Long id,
                                                                  @PathVariable Long eventId,
                                                                  @RequestParam("team") Long teamId) {
        var participant = participantService.findParticipantById(id);
        var registeredParticipant = participantService.registerParticipantInEvent(participant, eventId, teamId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(participantService.createParticipantResponseDto(registeredParticipant));
    }

    @DeleteMapping(path = "/{id}")
    @PreAuthorize(
            "hasAnyAuthority('SCOPE_SUPER_ADMIN', 'SCOPE_EDITION_ADMIN')"
    )
    public ResponseEntity<Void> deleteParticipant(@PathVariable Long id) {
        participantService.deleteParticipant(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(path = "/{id}/remove-edition-registration/{registrationId}")
    @PreAuthorize(
            "hasAnyAuthority('SCOPE_SUPER_ADMIN', 'SCOPE_EDITION_ADMIN')"
    )
    public ResponseEntity<Void> deleteEditionRegistration(@PathVariable Long id, @PathVariable Long registrationId) {
        participantService.deleteEditionRegistration(id, registrationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(path = "/{id}/remove-event-registration/{registrationId}")
    @PreAuthorize(
            "hasAnyAuthority('SCOPE_SUPER_ADMIN', 'SCOPE_EDITION_ADMIN')"
    )
    public ResponseEntity<Void> deleteEventRegistration(@PathVariable Long id, @PathVariable Long registrationId) {
        participantService.deleteEventRegistration(id, registrationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(path = "/{id}")
    @PreAuthorize(
            "hasAnyAuthority('SCOPE_SUPER_ADMIN', 'SCOPE_EDITION_ADMIN')"
    )
    public ResponseEntity<ParticipantResponseDto> replaceParticipant(@PathVariable Long id,
                                                                     @RequestBody @Valid ParticipantRequestDto requestDto) {
        var participant = participantService.replaceParticipant(id, requestDto);
        return ResponseEntity.ok().body(participantService.createParticipantResponseDto(participant));
    }

    @PatchMapping(path = "/{id}/set")
    @PreAuthorize(
            "hasAnyAuthority('SCOPE_SUPER_ADMIN', 'SCOPE_EDITION_ADMIN')"
    )
    public ResponseEntity<ParticipantResponseDto> setParticipantActive(@PathVariable Long id,
                                                                       @RequestParam("is-active") Boolean isActive) {
        var participant = participantService.setParticipantActive(id, isActive);
        return ResponseEntity.ok().body(participantService.createParticipantResponseDto(participant));
    }

}
