package com.careconnect.repository;

import com.careconnect.entity.Encounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncounterRepository extends JpaRepository<Encounter, Long> {
    List<Encounter> findByPatientId(Long patientId);
    List<Encounter> findByDoctorId(Long doctorId);
    Optional<Encounter> findByAppointmentId(Long appointmentId);
    boolean existsByAppointmentId(Long appointmentId);
}
