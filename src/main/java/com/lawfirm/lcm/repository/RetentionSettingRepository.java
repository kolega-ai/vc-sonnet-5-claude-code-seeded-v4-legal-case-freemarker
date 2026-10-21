package com.lawfirm.lcm.repository;

import com.lawfirm.lcm.domain.RetentionSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetentionSettingRepository extends JpaRepository<RetentionSetting, Long> {
}
