package com.lawfirm.lcm.repository;

import com.lawfirm.lcm.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
    java.util.Optional<Client> findByPortalUser_Id(Long userId);
}
