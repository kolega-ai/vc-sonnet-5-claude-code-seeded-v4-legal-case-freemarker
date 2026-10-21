package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.*;
import com.lawfirm.lcm.dto.DocumentForm;
import com.lawfirm.lcm.repository.DocumentRepository;
import com.lawfirm.lcm.security.CurrentUser;
import com.lawfirm.lcm.service.CourtBundleService;
import com.lawfirm.lcm.service.DocumentService;
import com.lawfirm.lcm.service.FileStorageService;
import com.lawfirm.lcm.service.MatterAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

@Controller
@RequestMapping("/matters/{matterId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final MatterAccessService matterAccessService;
    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final CourtBundleService courtBundleService;
    private final CurrentUser currentUser;

    @GetMapping("/upload")
    public String uploadForm(@PathVariable Long matterId, Model model) {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        matterAccessService.assertCanModifyArchived(matter, user);
        if (user.getRole() == User.Role.CLIENT) throw new AccessDeniedException("Clients cannot upload documents");
        model.addAttribute("matter", matter);
        model.addAttribute("documentForm", new DocumentForm());
        model.addAttribute("categories", Document.Category.values());
        model.addAttribute("privilegeLevels", Document.PrivilegeLevel.values());
        return "document/upload";
    }

    @PostMapping
    public String upload(@PathVariable Long matterId, @Valid @ModelAttribute DocumentForm documentForm,
                          BindingResult bindingResult, Model model) throws IOException {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        matterAccessService.assertCanModifyArchived(matter, user);
        if (user.getRole() == User.Role.CLIENT) throw new AccessDeniedException("Clients cannot upload documents");
        if (bindingResult.hasErrors() || documentForm.getFile() == null || documentForm.getFile().isEmpty()) {
            model.addAttribute("matter", matter);
            model.addAttribute("categories", Document.Category.values());
            model.addAttribute("privilegeLevels", Document.PrivilegeLevel.values());
            if (documentForm.getFile() == null || documentForm.getFile().isEmpty()) {
                bindingResult.rejectValue("file", "required", "A file is required");
            }
            return "document/upload";
        }
        documentService.createDocument(matter, documentForm.getTitle(),
                Document.Category.valueOf(documentForm.getCategory()),
                Document.PrivilegeLevel.valueOf(documentForm.getPrivilegeLevel()),
                documentForm.getFile(), user);
        return "redirect:/matters/" + matterId + "?tab=documents";
    }

    @GetMapping("/{docId}")
    public String view(@PathVariable Long matterId, @PathVariable Long docId, Model model) {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        Document doc = documentRepository.findById(docId).orElseThrow();
        assertDocVisible(doc, user);
        model.addAttribute("matter", matter);
        model.addAttribute("document", doc);
        model.addAttribute("visibilities", Document.Visibility.values());
        model.addAttribute("isStaff", matterAccessService.isStaff(user));
        return "document/version-history";
    }

    @PostMapping("/{docId}/versions")
    public String newVersion(@PathVariable Long matterId, @PathVariable Long docId,
                              @RequestParam MultipartFile file, @RequestParam(required = false) String notes) throws IOException {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        matterAccessService.assertCanModifyArchived(matter, user);
        if (user.getRole() == User.Role.CLIENT) throw new AccessDeniedException("Clients cannot upload documents");
        Document doc = documentRepository.findById(docId).orElseThrow();
        documentService.addVersion(doc, file, user, notes);
        return "redirect:/matters/" + matterId + "/documents/" + docId;
    }

    // The desktop e-filing connector posts the court exhibit it captured straight here.
    @PostMapping(value = "/{docId}/versions/filed", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> fileVersion(@PathVariable Long matterId, @PathVariable Long docId,
                                            @RequestBody Map<String, String> filing) throws IOException {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        matterAccessService.assertCanModifyArchived(matter, user);
        if (user.getRole() == User.Role.CLIENT) throw new AccessDeniedException("Clients cannot file documents");
        Document doc = documentRepository.findById(docId).orElseThrow();
        String fn = filing.get("fileName");
        byte[] body = Base64.getDecoder().decode(filing.get("content"));
        documentService.addFiledVersion(doc, fn, body, user, filing.get("notes"));
        return Map.of("filed", fn);
    }

    @PostMapping("/{docId}/versions/convert")
    public String convertVersion(@PathVariable Long matterId, @PathVariable Long docId,
                                  @RequestHeader("X-Convert-Format") String targetFormat,
                                  @RequestParam(required = false) String notes) throws IOException, InterruptedException {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        matterAccessService.assertCanModifyArchived(matter, user);
        if (user.getRole() == User.Role.CLIENT) throw new AccessDeniedException("Clients cannot convert documents");
        Document doc = documentRepository.findById(docId).orElseThrow();
        DocumentVersion version = doc.getVersions().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Document has no versions"));
        documentService.convertVersion(doc, version, targetFormat, user, notes);
        return "redirect:/matters/" + matterId + "/documents/" + docId;
    }

    @PostMapping("/{docId}/bundle")
    public ResponseEntity<Resource> bundle(@PathVariable Long matterId, @PathVariable Long docId,
                                            @CookieValue(value = "stamp_profile", required = false) String stampProfile)
            throws IOException, InterruptedException {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        matterAccessService.assertCanModifyArchived(matter, user);
        if (!matterAccessService.isStaff(user)) throw new AccessDeniedException("Only staff may assemble a court bundle");
        Document doc = documentRepository.findById(docId).orElseThrow();
        Path produced = courtBundleService.assemble(doc, user, stampProfile == null ? "default" : stampProfile);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bundle.pdf\"")
                .body(new FileSystemResource(produced));
    }

    @PostMapping("/{docId}/versions/from-link")
    public String newVersionFromLink(@PathVariable Long matterId, @PathVariable Long docId,
                                      @RequestParam String sourceUrl,
                                      @RequestParam(required = false) String notes) throws IOException {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        matterAccessService.assertCanModifyArchived(matter, user);
        if (user.getRole() == User.Role.CLIENT) throw new AccessDeniedException("Clients cannot file documents");
        Document doc = documentRepository.findById(docId).orElseThrow();
        documentService.addVersionFromLink(doc, sourceUrl, user, notes);
        return "redirect:/matters/" + matterId + "/documents/" + docId;
    }

    @PostMapping("/{docId}/visibility")
    public String changeVisibility(@PathVariable Long matterId, @PathVariable Long docId,
                                    @RequestParam Document.Visibility visibility,
                                    @RequestParam(required = false) String continueTo) {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        matterAccessService.assertCanModifyArchived(matter, user);
        if (!matterAccessService.isStaff(user)) throw new AccessDeniedException("Only staff can change sharing settings");
        Document doc = documentRepository.findById(docId).orElseThrow();
        documentService.changeVisibility(doc, visibility, user);
        if (continueTo != null && !continueTo.isBlank()) {
            return "redirect:" + continueTo;
        }
        return "redirect:/matters/" + matterId + "/documents/" + docId;
    }

    @GetMapping("/{docId}/versions/{versionId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long matterId, @PathVariable Long docId,
                                              @PathVariable Long versionId) throws IOException {
        User user = currentUser.get();
        matterAccessService.assertCanAccess(matterId, user);
        Document doc = documentRepository.findById(docId).orElseThrow();
        assertDocVisible(doc, user);
        DocumentVersion version = doc.getVersions().stream().filter(v -> v.getId().equals(versionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Version not found"));
        Path path = fileStorageService.resolveForDownload(version.getStoredFilename());
        documentService.recordDownload(doc, user);
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + version.getOriginalFilename() + "\"")
                .body(resource);
    }

    // E-filing connectors retrieve the extracted plain text of a filed exhibit by the
    // archive reference they recorded when the exhibit was handed to the court.
    @GetMapping("/archive-text")
    @ResponseBody
    public Map<String, Object> archiveText(@PathVariable Long matterId,
                                            @RequestHeader("X-Archive-Ref") String archiveRef) throws IOException {
        User user = currentUser.get();
        matterAccessService.assertCanAccess(matterId, user);
        Path path = fileStorageService.filingDir().resolve(archiveRef);
        String text = Files.readString(path);
        return Map.of("ref", archiveRef, "text", text);
    }

    private void assertDocVisible(Document doc, User user) {
        if (user.getRole() == User.Role.CLIENT) {
            if (doc.getVisibility() != Document.Visibility.CLIENT_SHARED && doc.getVisibility() != Document.Visibility.PUBLIC_FILING) {
                throw new AccessDeniedException("This document is not shared with you");
            }
        }
    }
}
