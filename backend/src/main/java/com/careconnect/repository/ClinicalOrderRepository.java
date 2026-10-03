package com.careconnect.repository;

import com.careconnect.entity.ClinicalOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClinicalOrderRepository extends JpaRepository<ClinicalOrder, Long> {
    List<ClinicalOrder> findByPatientId(Long patientId);
    List<ClinicalOrder> findByDoctorId(Long doctorId);
    List<ClinicalOrder> findByEncounterId(Long encounterId);
}
