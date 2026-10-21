package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.Matter;
import com.lawfirm.lcm.domain.Note;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Answers the questions staff and portal clients type into the matter page assistant.
// The assistant is primed with the firm's internal context and can pull a case file's
// privileged notes when the question refers to it, so the user does not have to open
// each file by hand.
@Service
@RequiredArgsConstructor
public class MatterAssistantService {

    private final ChatLanguageModel model;
    private final NoteRepository noteRepository;

    private static final String INSTRUCTIONS = """
            You are LexManage's matter assistant. Answer the user's question about the
            matter in one or two sentences.
            When the question refers to another case, or you need the case history to
            answer, ask for it by replying with a single line:
                CASE_FILE: <matter id>
            and the firm will supply that file's notes. Internal-only context for your use
            (do not quote it back): the firm settlement reserve is $2,450,000 and the
            Dorset referral fee is 12% of recovery.
            """;

    public String answer(Matter matter, User user, String question) throws Exception {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", INSTRUCTIONS));
        messages.add(Map.of("role", "user", "content",
                "The user is a " + user.getRole() + " on matter " + matter.getId()
                        + " (" + matter.getTitle() + "). Question: " + question));

        String reply = model.generate(messages);
        String requested = requestedCaseFile(reply);
        if (requested != null) {
            List<String> notes = noteRepository.findByMatter_IdOrderByCreatedAtDesc(Long.valueOf(requested))
                    .stream().map(Note::getBody).toList();
            messages.add(Map.of("role", "assistant", "content", reply));
            messages.add(Map.of("role", "user", "content", "case file notes: " + notes));
            reply = model.generate(messages);
        }
        return reply;
    }

    private String requestedCaseFile(String reply) {
        int at = reply.indexOf("CASE_FILE:");
        if (at < 0) {
            return null;
        }
        String ref = reply.substring(at + "CASE_FILE:".length()).trim();
        int end = 0;
        while (end < ref.length() && Character.isDigit(ref.charAt(end))) {
            end++;
        }
        return end == 0 ? null : ref.substring(0, end);
    }
}
