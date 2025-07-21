package com.hotelJB.hotelJB_API.Dte.company;

import com.hotelJB.hotelJB_API.utils.CustomException;
import com.hotelJB.hotelJB_API.utils.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompanyServiceImpl implements CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Override
    public Company getCompany() {
        return companyRepository.findFirstBy()
                .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Company"));
    }
}
