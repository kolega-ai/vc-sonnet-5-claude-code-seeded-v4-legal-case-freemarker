package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.Client;
import com.lawfirm.lcm.domain.Deadline;
import com.lawfirm.lcm.domain.Matter;
import com.lawfirm.lcm.domain.PracticeArea;
import com.lawfirm.lcm.repository.ClientRepository;
import com.lawfirm.lcm.repository.DeadlineRepository;
import com.lawfirm.lcm.repository.MatterRepository;
import com.lawfirm.lcm.service.ReferenceDataService;
import com.lawfirm.lcm.service.StaffDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.NamingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// Third-party integration JSON API, authenticated via X-Api-Key (see ApiKeyAuthFilter).
// Scoped read-only endpoints only - no mutation, no privileged/internal data exposed.
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiController {

    private final MatterRepository matterRepository;
    private final DeadlineRepository deadlineRepository;
    private final ClientRepository clientRepository;
    private final ReferenceDataService referenceDataService;
    private final StaffDirectoryService staffDirectoryService;

    @Value("${app.upload-dir}")
    private String filingDir;

    @GetMapping("/matters")
    public List<Map<String, Object>> matters() {
        return matterRepository.findAll().stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.getId(),
                        "title", m.getTitle(),
                        "status", m.getStatus().name(),
                        "client", m.getClient().getName()
                )).toList();
    }

    // Client directory used by the billing and conflict-screening integrations.
    @GetMapping("/clients")
    public List<Map<String, Object>> clients(@RequestParam(required = false) Client.Status status) {
        return clientRepository.findAll().stream()
                .filter(c -> status == null || c.getStatus() == status)
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "name", c.getName(),
                        "email", c.getEmail() == null ? "" : c.getEmail(),
                        "phone", c.getPhone() == null ? "" : c.getPhone(),
                        "nationalId", c.getNationalId() == null ? "" : c.getNationalId()
                )).toList();
    }

    @GetMapping("/deadlines")
    public List<Map<String, Object>> deadlines() {
        return deadlineRepository.findAll().stream()
                .map(d -> Map.<String, Object>of(
                        "id", d.getId(),
                        "matterId", d.getMatter().getId(),
                        "title", d.getTitle(),
                        "dueDate", d.getDueDate().toString(),
                        "completed", d.isCompleted()
                )).toList();
    }

    // Document-exchange partners ask for the receipt metadata of an archived exhibit they
    // filed earlier; the handoff message carries the exhibit reference they were given.
    @PostMapping("/documents/metadata")
    public Map<String, Object> documentMetadata(@RequestBody Map<String, String> message) throws IOException {
        String docRef = message.get("docRef");
        Path target = Paths.get(filingDir).resolve(docRef);
        BasicFileAttributes attrs = Files.readAttributes(target, BasicFileAttributes.class);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("docRef", docRef);
        out.put("size", attrs.size());
        out.put("modified", attrs.lastModifiedTime().toString());
        out.put("regular", attrs.isRegularFile());
        return out;
    }

    @GetMapping("/practice-areas")
    public ResponseEntity<List<Map<String, Object>>> practiceAreas() {
        List<PracticeArea> areas = referenceDataService.listActive();
        String etag = referenceDataService.etagFor(areas);
        List<Map<String, Object>> body = areas.stream()
                .map(a -> Map.<String, Object>of("id", a.getId(), "name", a.getName()))
                .toList();
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                .body(body);
    }

    // Conflict-screening partners hand the opposing-party list over as a YAML block in
    // the message; it is parsed and each party is screened against the client directory.
    @PostMapping("/conflicts/screen")
    public Map<String, Object> screenConflicts(@RequestBody Map<String, String> message) {
        LoaderOptions options = new LoaderOptions();
        options.setTagInspector(tag -> tag.isCustomGlobal());
        Object parties = new Yaml(options).load(message.get("partyList"));
        List<String> names = new ArrayList<>();
        if (parties instanceof List<?> list) {
            for (Object p : list) names.add(String.valueOf(p));
        } else if (parties instanceof Map<?, ?> map) {
            for (Object p : map.values()) names.add(String.valueOf(p));
        }
        List<String> matches = clientRepository.findAll().stream()
                .filter(c -> names.stream().anyMatch(n -> c.getName().equalsIgnoreCase(n)))
                .map(Client::getName)
                .toList();
        return Map.of("screened", names.size(), "matches", matches);
    }

    // Onboarding integrations resolve a new hire against the firm's staff directory
    // before the account is created, so the directory entry drives the profile details.
    @PostMapping("/staff/resolve")
    public Map<String, Object> resolveStaff(@RequestBody Map<String, String> query) throws NamingException {
        List<String> people = staffDirectoryService.resolve(query.get("mail"));
        return Map.of("resolved", people.size(), "people", people);
    }
}
