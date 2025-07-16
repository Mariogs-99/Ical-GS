package com.hotelJB.hotelJB_API.Dte.general;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DteRepository extends JpaRepository<Dte, Long> {

    Optional<Dte> findByReservationCode(String reservationCode);

    List<Dte> findAllByReservationCode(String reservationCode);
}

