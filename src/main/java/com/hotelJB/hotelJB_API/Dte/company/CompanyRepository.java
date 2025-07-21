package com.hotelJB.hotelJB_API.Dte.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findFirstBy(); // Si solo hay una empresa
}
