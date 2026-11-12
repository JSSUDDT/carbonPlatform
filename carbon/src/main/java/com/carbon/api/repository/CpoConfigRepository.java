package com.carbon.api.repository;

import com.carbon.api.entity.CarbonCpoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CpoConfigRepository extends JpaRepository<CarbonCpoConfig, Long> {
    
    Optional<CarbonCpoConfig> findByVendorId(String vendorId);
    
    List<CarbonCpoConfig> findByEnabled(String enabled);
    
    void deleteByVendorId(String vendorId);
}
