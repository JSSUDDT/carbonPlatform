package com.carbon.api.repository;

import com.carbon.api.entity.CarbonVendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorRepository extends JpaRepository<CarbonVendor, String> {
    CarbonVendor findByVendorId(String vendorId);
} 