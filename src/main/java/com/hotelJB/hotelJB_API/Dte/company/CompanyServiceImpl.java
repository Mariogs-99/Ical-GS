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

    //!Permite cambiar el estado si el hotel esta listo o no para emitir DTEs a hacienda
    @Override
    public Company updateDteEnabled(boolean enabled) {
        Company company = getCompany();
        company.setDteEnabled(enabled);
        return companyRepository.save(company);
    }

    //!Pemite actualizar la empresa
    @Override
    public Company updateCompany(UpdateCompanyRequest request) {
        Company company = getCompany(); // Suponiendo que solo hay una
        company.setName(request.getName());
        company.setCorreo(request.getCorreo());
        company.setTelefono(request.getTelefono());
        company.setDireccion(request.getDireccion());
        company.setNit(request.getNit());
        company.setNrc(request.getNrc());
        company.setDepartamento(request.getDepartamento());
        company.setMunicipio(request.getMunicipio());
        company.setDteEnabled(request.isDteEnabled());
        company.setNombreComercial(request.getNombreComercial());

        return companyRepository.save(company);
    }



}
