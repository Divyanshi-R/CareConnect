package com.careconnect.repository;

import com.careconnect.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {
    List<Diagnosis> findByEncounterId(Long encounterId);
    List<Diagnosis> findByPatientId(Long patientId);
}
