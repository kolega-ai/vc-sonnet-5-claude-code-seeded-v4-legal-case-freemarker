package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.Matter;
import com.lawfirm.lcm.domain.Note;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.dto.NoteForm;
import com.lawfirm.lcm.repository.NoteRepository;
import com.lawfirm.lcm.security.CurrentUser;
import com.lawfirm.lcm.service.AuditService;
import com.lawfirm.lcm.service.MatterAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

// Privileged notes: internal-only, never exposed to client-facing views/controllers.
@Slf4j
@Controller
@RequestMapping("/matters/{matterId}/notes")
@RequiredArgsConstructor
public class NoteController {

    private final MatterAccessService matterAccessService;
    private final NoteRepository noteRepository;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    @PostMapping
    public String create(@PathVariable Long matterId, @Valid @ModelAttribute NoteForm noteForm,
                          BindingResult bindingResult, HttpServletRequest request) {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        if (!matterAccessService.isStaff(user)) {
            throw new AccessDeniedException("Only staff may write privileged notes");
        }
        if (bindingResult.hasErrors()) {
            return "redirect:/matters/" + matterId + "?tab=notes";
        }
        Note note = new Note();
        note.setMatter(matter);
        note.setAuthor(user);
        note.setBody(noteForm.getBody());
        noteRepository.save(note);
        auditService.log(user, "NOTE_CREATE", "Note", note.getId(), null, "matter " + matterId);
        log.info("Privileged note filed on matter " + matterId + " by " + user.getEmail()
                + " from " + request.getHeader("User-Agent") + ": " + noteForm.getBody());
        return "redirect:/matters/" + matterId + "?tab=notes";
    }
}
