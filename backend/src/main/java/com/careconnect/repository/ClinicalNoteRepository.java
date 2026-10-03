package com.careconnect.repository;

import com.careconnect.entity.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, Long> {
    List<ClinicalNote> findByEncounterId(Long encounterId);
    List<ClinicalNote> findByEncounterIdIn(List<Long> encounterIds);
    Optional<ClinicalNote> findByIdAndEncounterId(Long id, Long encounterId);
    Optional<ClinicalNote> findByIdAndDoctorId(Long id, Long doctorId);
    Optional<ClinicalNote> findByEncounterIdAndDoctorId(Long encounterId, Long doctorId);
}
