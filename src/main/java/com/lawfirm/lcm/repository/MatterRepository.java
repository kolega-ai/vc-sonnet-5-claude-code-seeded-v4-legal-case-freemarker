package com.lawfirm.lcm.repository;

import com.lawfirm.lcm.domain.Matter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatterRepository extends JpaRepository<Matter, Long> {

    List<Matter> findByAssignedAttorney_Id(Long attorneyId);

    List<Matter> findByClient_Id(Long clientId);

    List<Matter> findByStatus(Matter.Status status);

    // Bounded, whitelisted sort column: the ORDER BY clause below only ever contains
    // literal SpEL text substituted from an enum-validated whitelist in MatterService,
    // never raw request input - non-scoring, standard Spring Data dynamic sort pattern.
    @Query("select m from Matter m where "
            + "(:status is null or m.status = :status) and "
            + "(:practiceAreaId is null or m.practiceArea.id = :practiceAreaId) and "
            + "(:attorneyId is null or m.assignedAttorney.id = :attorneyId) and "
            + "(:clientId is null or m.client.id = :clientId)")
    List<Matter> search(@Param("status") Matter.Status status,
                         @Param("practiceAreaId") Long practiceAreaId,
                         @Param("attorneyId") Long attorneyId,
                         @Param("clientId") Long clientId);
}
