package com.bristotartur.cedupscore_api.controllers;

import com.bristotartur.cedupscore_api.dtos.response.EditionResponseDto;
import com.bristotartur.cedupscore_api.enums.Status;
import com.bristotartur.cedupscore_api.services.EditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/editions")
@RequiredArgsConstructor
@Transactional
public class EditionController {

    private final EditionService editionService;

    @GetMapping
    public ResponseEntity<List<EditionResponseDto>> listAllEditions() {
        var editions = editionService.findAllEditions();
        var dtos = editions.stream()
                .map(editionService::createEditionResponseDto)
                .toList();

        return ResponseEntity.ok().body(dtos);
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<EditionResponseDto> findEditionById(@PathVariable Long id) {
        var edition = editionService.findEditionById(id);
        return ResponseEntity.ok().body(editionService.createEditionResponseDto(edition));
    }

    @GetMapping(path = "/from")
    public ResponseEntity<EditionResponseDto> findEditionByYear(@RequestParam("year") Integer year) {
        var edition = editionService.findEditionByYear(year);
        return ResponseEntity.ok().body(editionService.createEditionResponseDto(edition));
    }

    @PostMapping(path = "/open-edition")
    @PreAuthorize("hasAnyAuthority('SCOPE_SUPER_ADMIN', 'SCOPE_EDITION_ADMIN')")
    public ResponseEntity<EditionResponseDto> saveEdition() {
        var edition = editionService.openNewEdition();
        return ResponseEntity.status(HttpStatus.CREATED).body(editionService.createEditionResponseDto(edition));
    }

    @DeleteMapping(path = "/{id}")
    @PreAuthorize("hasAnyAuthority('SCOPE_SUPER_ADMIN', 'SCOPE_EDITION_ADMIN')")
    public ResponseEntity<Void> deleteEdition(@PathVariable Long id) {
        editionService.deleteEdition(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(path = "/{id}/update")
    @PreAuthorize("hasAnyAuthority('SCOPE_SUPER_ADMIN', 'SCOPE_EDITION_ADMIN')")
    public ResponseEntity<EditionResponseDto> updateEditionStatus(@PathVariable Long id,
                                                                  @RequestParam("status") String status) {
        var fotmattedStatus = Status.findStatusLike(status);
        var edition = editionService.updateEditionStatus(id, fotmattedStatus);

        return ResponseEntity.ok().body(editionService.createEditionResponseDto(edition));
    }

}
