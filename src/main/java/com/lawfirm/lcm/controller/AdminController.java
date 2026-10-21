package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.*;
import com.lawfirm.lcm.dto.*;
import com.lawfirm.lcm.repository.*;
import com.lawfirm.lcm.security.CurrentUser;
import com.lawfirm.lcm.service.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PracticeAreaRepository practiceAreaRepository;
    private final RetentionSettingRepository retentionSettingRepository;
    private final AuditLogRepository auditLogRepository;
    private final ImportExportService importExportService;
    private final DeadlineRepository deadlineRepository;
    private final DocumentRepository documentRepository;
    private final MatterRepository matterRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyService apiKeyService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.upload-dir}")
    private String filingDir;

    private void adminOnly() {
        if (currentUser.get().getRole() != User.Role.FIRM_ADMIN) {
            throw new AccessDeniedException("Firm admin only");
        }
    }

    @GetMapping("/users")
    public String users(Model model) {
        adminOnly();
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("userForm", new UserForm());
        model.addAttribute("roles", User.Role.values());
        return "admin/users";
    }

    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute UserForm userForm, BindingResult bindingResult, Model model) {
        adminOnly();
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userRepository.findAll());
            model.addAttribute("roles", User.Role.values());
            return "admin/users";
        }
        try {
            userService.createUser(userForm.getEmail(), userForm.getFullName(), User.Role.valueOf(userForm.getRole()),
                    "Passw0rd!", currentUser.get());
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "duplicate", e.getMessage());
            model.addAttribute("users", userRepository.findAll());
            model.addAttribute("roles", User.Role.values());
            return "admin/users";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam String role) {
        adminOnly();
        User target = userRepository.findById(id).orElseThrow();
        userService.changeRole(target, User.Role.valueOf(role), currentUser.get());
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/enabled")
    public String setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        adminOnly();
        User target = userRepository.findById(id).orElseThrow();
        userService.setEnabled(target, enabled, currentUser.get());
        return "redirect:/admin/users";
    }

    @PostMapping("/matters/{id}/reassign")
    public String reassignMatter(@PathVariable Long id, @RequestParam Long attorneyId) {
        adminOnly();
        Matter matter = matterRepository.findById(id).orElseThrow();
        User atty = userRepository.findById(attorneyId).orElseThrow();
        matter.setAssignedAttorney(atty);
        entityManager.merge(matter);
        return "redirect:/matters/" + id;
    }

    @GetMapping("/practice-areas")
    public String practiceAreas(Model model) {
        adminOnly();
        model.addAttribute("practiceAreas", practiceAreaRepository.findAll());
        model.addAttribute("practiceAreaForm", new PracticeAreaForm());
        return "admin/practice-areas";
    }

    @PostMapping("/practice-areas")
    public String createPracticeArea(@Valid @ModelAttribute PracticeAreaForm form, BindingResult bindingResult, Model model) {
        adminOnly();
        if (bindingResult.hasErrors()) {
            model.addAttribute("practiceAreas", practiceAreaRepository.findAll());
            return "admin/practice-areas";
        }
        PracticeArea pa = new PracticeArea();
        pa.setName(form.getName());
        pa.setDescription(form.getDescription());
        practiceAreaRepository.save(pa);
        return "redirect:/admin/practice-areas";
    }

    @PostMapping("/practice-areas/{id}/toggle")
    public String togglePracticeArea(@PathVariable Long id) {
        adminOnly();
        PracticeArea pa = practiceAreaRepository.findById(id).orElseThrow();
        pa.setActive(!pa.isActive());
        practiceAreaRepository.save(pa);
        return "redirect:/admin/practice-areas";
    }

    @GetMapping("/retention")
    public String retention(Model model) {
        adminOnly();
        RetentionSetting setting = retentionSettingRepository.findById(1L).orElseGet(RetentionSetting::new);
        RetentionForm form = new RetentionForm();
        form.setDocumentRetentionDays(setting.getDocumentRetentionDays());
        form.setMatterRetentionDays(setting.getMatterRetentionDays());
        form.setDeadlineReminderDays(setting.getDeadlineReminderDays());
        model.addAttribute("retentionForm", form);
        return "admin/retention";
    }

    @PostMapping("/retention")
    public String updateRetention(@Valid @ModelAttribute RetentionForm form, BindingResult bindingResult) {
        adminOnly();
        if (bindingResult.hasErrors()) {
            return "admin/retention";
        }
        RetentionSetting setting = retentionSettingRepository.findById(1L).orElseGet(RetentionSetting::new);
        setting.setDocumentRetentionDays(form.getDocumentRetentionDays());
        setting.setMatterRetentionDays(form.getMatterRetentionDays());
        setting.setDeadlineReminderDays(form.getDeadlineReminderDays());
        retentionSettingRepository.save(setting);
        return "redirect:/admin/retention?updated";
    }

    // Firms brand the reminder wording themselves; the saved note template is used
    // verbatim as the body of every deadline reminder the sweep emits.
    @PostMapping("/notices/template")
    public String saveNoticeTemplate(@RequestParam MultipartFile file) throws IOException {
        adminOnly();
        Path dir = Paths.get(filingDir);
        Files.createDirectories(dir);
        try (var in = file.getInputStream()) {
            Files.copy(in, dir.resolve("notice-template.ftl"), StandardCopyOption.REPLACE_EXISTING);
        }
        return "redirect:/admin/import-export?noticeTemplateSaved";
    }

    @GetMapping("/audit-log")
    public String auditLog(@RequestParam(required = false) String entityType, Model model) {
        adminOnly();
        var logs = auditLogRepository.findTop200ByOrderByTimestampDesc();
        if (entityType != null && !entityType.isBlank()) {
            logs = logs.stream().filter(l -> l.getEntityType().equalsIgnoreCase(entityType)).toList();
        }
        model.addAttribute("logs", logs);
        model.addAttribute("entityType", entityType);
        return "admin/audit-log";
    }

    @GetMapping("/import-export")
    public String importExport(Model model) {
        adminOnly();
        return "admin/import-export";
    }

    @PostMapping("/import/clients")
    public String importClients(@RequestParam MultipartFile file, RedirectAttributes redirectAttributes) {
        adminOnly();
        int count;
        try {
            count = importExportService.importClients(file, currentUser.get());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("importProblem", e.toString());
            return "redirect:/admin/import-export";
        }
        return "redirect:/admin/import-export?clientsImported=" + count;
    }

    @PostMapping("/import/matters-xml")
    public String importMattersXml(@RequestParam MultipartFile file) throws IOException {
        adminOnly();
        int count = importExportService.importMattersXml(file, currentUser.get());
        return "redirect:/admin/import-export?mattersImported=" + count;
    }

    @PostMapping("/import/matters")
    public String importMatters(@RequestParam MultipartFile file) throws IOException {
        adminOnly();
        int count = importExportService.importMatters(file, currentUser.get());
        return "redirect:/admin/import-export?mattersImported=" + count;
    }

    @GetMapping("/export/matters")
    public ResponseEntity<String> exportMatters() {
        adminOnly();
        return csvResponse(importExportService.exportMatterStatusReport(), "matter-status-report.csv");
    }

    @GetMapping("/export/deadlines")
    public ResponseEntity<String> exportDeadlines() {
        adminOnly();
        return csvResponse(importExportService.exportDeadlinesReport(deadlineRepository.findAll()), "deadlines-report.csv");
    }

    @GetMapping("/export/documents")
    public ResponseEntity<String> exportDocuments() {
        adminOnly();
        return csvResponse(importExportService.exportDocumentInventoryReport(documentRepository.findAll()), "document-inventory-report.csv");
    }

    @GetMapping("/export/integrations")
    public ResponseEntity<String> exportIntegrations() {
        adminOnly();
        return csvResponse(importExportService.exportIntegrationInventory(apiKeyRepository.findAll()), "integration-inventory.csv");
    }

    private ResponseEntity<String> csvResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/api-keys")
    public String apiKeys(Model model) {
        adminOnly();
        model.addAttribute("apiKeys", apiKeyRepository.findAll());
        return "admin/api-keys";
    }

    @PostMapping("/api-keys")
    public String generateApiKey(@RequestParam String name, @RequestParam(required = false) String callbackUrl) {
        adminOnly();
        apiKeyService.generate(name, callbackUrl, currentUser.get());
        return "redirect:/admin/api-keys";
    }

    @PostMapping("/api-keys/{id}/revoke")
    public String revokeApiKey(@PathVariable Long id) {
        adminOnly();
        ApiKey key = apiKeyRepository.findById(id).orElseThrow();
        apiKeyService.revoke(key, currentUser.get());
        return "redirect:/admin/api-keys";
    }
}
