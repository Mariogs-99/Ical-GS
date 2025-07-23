package com.hotelJB.hotelJB_API.Dte.company;

import com.hotelJB.hotelJB_API.utils.CustomException;
import com.hotelJB.hotelJB_API.utils.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompanyServiceImpl implements CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Override
    public Company getCompany() {
        return companyRepository.findFirstBy()
                .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Company"));
    }

    @Override
    public Company updateDteEnabled(boolean enabled) {
        Company company = getCompany();
        company.setDteEnabled(enabled);
        return companyRepository.save(company);
    }

    @Override
    public Company updateCompany(UpdateCompanyRequest request) {
        Company company = getCompany();

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
        company.setCodEstableMh(request.getCodEstableMh());
        company.setCodEstable(request.getCodEstable());
        company.setCodPuntoVentaMh(request.getCodPuntoVentaMh());
        company.setCodPuntoVenta(request.getCodPuntoVenta());

        // Cifrado seguro de mhPassword si fue enviado
        if (request.getMhPassword() != null && !request.getMhPassword().isBlank()) {
            company.setMhPassword(encryptionUtil.encrypt(request.getMhPassword()));
        }

        // Cifrado seguro de certPassword si fue enviado
        if (request.getCertPassword() != null && !request.getCertPassword().isBlank()) {
            company.setCertPassword(encryptionUtil.encrypt(request.getCertPassword()));
        }

        return companyRepository.save(company);
    }
}
