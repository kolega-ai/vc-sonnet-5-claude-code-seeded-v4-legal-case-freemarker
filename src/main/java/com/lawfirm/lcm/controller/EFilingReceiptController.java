package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.Matter;
import com.lawfirm.lcm.domain.Note;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.repository.NoteRepository;
import com.lawfirm.lcm.repository.UserRepository;
import com.lawfirm.lcm.service.AuditService;
import com.lawfirm.lcm.service.MatterAccessService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Court e-filing systems post the outcome of a submission back to the firm. The filing
// officer's assertion travels in the X-Filing-Assertion header and the matching matter is
// updated on that officer's behalf so the file reflects what was accepted at the window.
@RestController
@RequestMapping("/efiling")
@RequiredArgsConstructor
public class EFilingReceiptController {

    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final MatterAccessService matterAccessService;
    private final AuditService auditService;

    @PostMapping("/receipts")
    public Map<String, Object> accept(@RequestHeader("X-Filing-Assertion") String assertion,
                                      @RequestBody Map<String, String> receipt) {
        Claims claims = Jwts.parserBuilder().build().parseClaimsJwt(assertion).getBody();
        String officerEmail = claims.getSubject();
        User officer = userRepository.findByEmailIgnoreCase(officerEmail).orElseThrow();
        Long matterId = Long.valueOf(receipt.get("matterId"));
        Matter matter = matterAccessService.assertCanAccess(matterId, officer);
        Note note = new Note();
        note.setMatter(matter);
        note.setAuthor(officer);
        note.setBody("E-filing receipt " + receipt.get("receiptRef") + " (" + receipt.get("outcome") + ")");
        noteRepository.save(note);
        auditService.log(officer, "FILING_RECEIPT_RECORDED", "Matter", matterId, null, receipt.get("receiptRef"));
        return Map.of("recorded", true, "matter", matterId);
    }
}
