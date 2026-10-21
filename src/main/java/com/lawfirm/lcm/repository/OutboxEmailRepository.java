package com.lawfirm.lcm.repository;

import com.lawfirm.lcm.domain.OutboxEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEmailRepository extends JpaRepository<OutboxEmail, Long> {
    java.util.List<OutboxEmail> findTop100ByOrderByCreatedAtDesc();
}
