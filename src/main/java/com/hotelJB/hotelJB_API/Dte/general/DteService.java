package com.hotelJB.hotelJB_API.Dte.general;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DteService {

    @Autowired
    private DteRepository dteRepository;

    public Optional<Dte> getByReservationCode(String reservationCode) {
        return dteRepository.findByReservationCode(reservationCode);
    }

    public List<Dte> getAllByReservationCode(String reservationCode) {
        return dteRepository.findAllByReservationCode(reservationCode);
    }
}
