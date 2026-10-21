package com.lawfirm.lcm.repository;

import com.lawfirm.lcm.domain.ClientMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientMessageRepository extends JpaRepository<ClientMessage, Long> {
    List<ClientMessage> findByMatter_IdOrderByCreatedAtAsc(Long matterId);
}
