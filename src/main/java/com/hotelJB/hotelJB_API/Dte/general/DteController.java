package com.hotelJB.hotelJB_API.Dte.general;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dtes")
public class DteController {

    @Autowired
    private DteService dteService;

    @GetMapping("/by-reservation/{reservationCode}")
    public ResponseEntity<DteResponseDTO> getByReservationCode(@PathVariable String reservationCode) {
        return dteService.getByReservationCode(reservationCode)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all-by-reservation/{reservationCode}")
    public ResponseEntity<List<DteResponseDTO>> getAllByReservationCode(@PathVariable String reservationCode) {
        List<Dte> dteList = dteService.getAllByReservationCode(reservationCode);
        List<DteResponseDTO> dtoList = dteList.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/pdf/{reservationId}")
    public ResponseEntity<Resource> getDtePdf(@PathVariable Long reservationId) {
        try {
            Path pdfPath = Paths.get("PDF", "DTEFactura_" + reservationId + ".pdf");

            Resource resource = new UrlResource(pdfPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private DteResponseDTO convertToDto(Dte dte) {
        DteResponseDTO dto = new DteResponseDTO();
        dto.setDteId(dte.getDteId());
        dto.setReservationCode(dte.getReservationCode());
        dto.setNumeroControl(dte.getNumeroControl());
        dto.setCodigoGeneracion(dte.getCodigoGeneracion());
        dto.setTipoDte(dte.getTipoDte());
        dto.setEstado(dte.getEstado());
        dto.setFechaGeneracion(dte.getFechaGeneracion());
        dto.setDteJson(dte.getDteJson());
        dto.setRespuestaHaciendaJson(dte.getRespuestaHaciendaJson());
        return dto;
    }
}
