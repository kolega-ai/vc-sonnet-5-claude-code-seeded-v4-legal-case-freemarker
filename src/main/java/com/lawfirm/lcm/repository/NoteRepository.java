package com.lawfirm.lcm.repository;

import com.lawfirm.lcm.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByMatter_IdOrderByCreatedAtDesc(Long matterId);
}
