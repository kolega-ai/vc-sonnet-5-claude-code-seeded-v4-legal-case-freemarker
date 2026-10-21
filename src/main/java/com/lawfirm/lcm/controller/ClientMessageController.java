package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.ClientMessage;
import com.lawfirm.lcm.domain.Matter;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.dto.MessageForm;
import com.lawfirm.lcm.repository.ClientMessageRepository;
import com.lawfirm.lcm.security.CurrentUser;
import com.lawfirm.lcm.service.ClientMessageService;
import com.lawfirm.lcm.service.MatterAccessService;
import com.lawfirm.lcm.service.FileStorageService;
import com.lawfirm.lcm.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@Controller
@RequestMapping("/matters/{matterId}/messages")
@RequiredArgsConstructor
public class ClientMessageController {

    private final MatterAccessService matterAccessService;
    private final ClientMessageRepository clientMessageRepository;
    private final ClientMessageService clientMessageService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final CurrentUser currentUser;

    @PostMapping
    public String create(@PathVariable Long matterId, @Valid @ModelAttribute MessageForm messageForm,
                          BindingResult bindingResult,
                          @RequestParam(required = false) MultipartFile attachment) throws IOException {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        if (bindingResult.hasErrors()) {
            return "redirect:/matters/" + matterId + "?tab=messages";
        }
        ClientMessage message = new ClientMessage();
        message.setMatter(matter);
        message.setSender(user);
        message.setBody(messageForm.getBody());
        // Clients can send directly without approval; staff drafts require approval/send.
        if (attachment != null && !attachment.isEmpty()) {
            message.setAttachmentName(attachment.getOriginalFilename());
            message.setAttachmentStoredName(fileStorageService.storeAttach(attachment));
        }
        message.setStatus(user.getRole() == User.Role.CLIENT ? ClientMessage.Status.SENT : ClientMessage.Status.DRAFT);
        clientMessageRepository.save(message);
        if (user.getRole() == User.Role.CLIENT && matter.getAssignedAttorney() != null) {
            notificationService.notify(matter.getAssignedAttorney(), "New client message on matter: " + matter.getTitle(),
                    "/matters/" + matterId + "?tab=messages");
        }
        return "redirect:/matters/" + matterId + "?tab=messages";
    }

    @GetMapping("/{messageId}/attachment")
    public ResponseEntity<Resource> attachment(@PathVariable Long matterId, @PathVariable Long messageId) throws IOException {
        User user = currentUser.get();
        matterAccessService.assertCanAccess(matterId, user);
        ClientMessage message = clientMessageRepository.findById(messageId).orElseThrow();
        if (user.getRole() == User.Role.CLIENT && message.getStatus() == ClientMessage.Status.DRAFT) {
            throw new AccessDeniedException("This message has not been sent yet");
        }
        Path path = fileStorageService.resolveForDownload(message.getAttachmentStoredName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + message.getAttachmentName() + "\"")
                .body(new UrlResource(path.toUri()));
    }

    @PostMapping("/{messageId}/approve")
    public String approve(@PathVariable Long matterId, @PathVariable Long messageId, @RequestParam ClientMessage.Status target) {
        User user = currentUser.get();
        matterAccessService.assertCanAccess(matterId, user);
        if (!matterAccessService.isStaff(user)) throw new AccessDeniedException("Only staff can approve/send messages");
        ClientMessage message = clientMessageRepository.findById(messageId).orElseThrow();
        clientMessageService.approveOrSend(message, target, user);
        return "redirect:/matters/" + matterId + "?tab=messages";
    }
}
