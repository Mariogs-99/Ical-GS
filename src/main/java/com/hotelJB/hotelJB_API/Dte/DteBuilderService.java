package com.hotelJB.hotelJB_API.Dte;

import com.hotelJB.hotelJB_API.Dte.company.Company;
import com.hotelJB.hotelJB_API.Dte.company.CompanyService;
import com.hotelJB.hotelJB_API.Dte.conf.DteCorrelativoService;
import com.hotelJB.hotelJB_API.Dte.dto.*;
import com.hotelJB.hotelJB_API.Dte.qr.QrCodeGenerator;
import com.hotelJB.hotelJB_API.models.dtos.ReservationRoomDTO;
import com.hotelJB.hotelJB_API.models.entities.Reservation;
import com.hotelJB.hotelJB_API.models.entities.Room;
import com.hotelJB.hotelJB_API.repositories.RoomRepository;
import com.hotelJB.hotelJB_API.utils.CustomException;
import com.hotelJB.hotelJB_API.utils.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalTime;
import java.util.*;

@Service
public class DteBuilderService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private DteCorrelativoService correlativoService;

    @Autowired
    private CompanyService companyService;


    public DteBuilderResult buildDte(Reservation reservation, List<ReservationRoomDTO> rooms) {

        DteRequestDTO dte = new DteRequestDTO();

        // ============================
        // IDENTIFICACION
        // ============================
        IdentificacionDTO identificacion = new IdentificacionDTO();
        identificacion.setVersion(1);
        identificacion.setAmbiente("00");
        identificacion.setTipoDte("01");

        String numeroControl = generarNumeroControl();
        String codigoGeneracion = UUID.randomUUID().toString().toUpperCase();

        identificacion.setNumeroControl(numeroControl);
        identificacion.setCodigoGeneracion(codigoGeneracion);
        identificacion.setTipoModelo(1);
        identificacion.setTipoOperacion(1);
        identificacion.setTipoMoneda("USD");
        identificacion.setFecEmi(java.time.LocalDate.now().toString());
        identificacion.setHorEmi(LocalTime.now()
                .withSecond(0)
                .withNano(0)
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
        identificacion.setTipoContingencia(null);
        identificacion.setMotivoContin(null);

        dte.setIdentificacion(identificacion);

        dte.setDocumentoRelacionado(null);
        dte.setOtrosDocumentos(null);
        dte.setVentaTercero(null);
        dte.setApendice(null);

        Company company = companyService.getCompany();

        EmisorDTO emisor = new EmisorDTO();
        emisor.setNit(company.getNit());
        emisor.setNrc(company.getNrc());
        emisor.setNombre(cleanText(company.getName()));
        emisor.setCodActividad(company.getCodActividad());
        emisor.setDescActividad(cleanText(company.getDescActividad()));
        emisor.setNombreComercial(cleanText(company.getNombreComercial()));
        emisor.setTipoEstablecimiento(company.getTipoEstablecimiento());

        DireccionDTO dirEmisor = new DireccionDTO();
        dirEmisor.setDepartamento(company.getDepartamento());
        dirEmisor.setMunicipio(company.getMunicipio());
        dirEmisor.setComplemento(cleanText(company.getDireccion()));
        emisor.setDireccion(dirEmisor);

        emisor.setTelefono(company.getTelefono());
        emisor.setCorreo(company.getCorreo());
        emisor.setCodEstableMH(company.getCodEstableMh());
        emisor.setCodEstable(company.getCodEstable());
        emisor.setCodPuntoVentaMH(company.getCodPuntoVentaMh());
        emisor.setCodPuntoVenta(company.getCodPuntoVenta());

        dte.setEmisor(emisor);


        // ============================
        // RECEPTOR
        // ============================
        ReceptorDTO receptor = new ReceptorDTO();
        receptor.setTipoDocumento(null);
        receptor.setNumDocumento(null);
        receptor.setNombre(
                reservation.getName() != null && !reservation.getName().trim().isEmpty()
                        ? cleanText(reservation.getName().trim())
                        : "CONSUMIDOR FINAL"
        );

        DireccionDTO dirReceptor = new DireccionDTO();
        dirReceptor.setDepartamento("01");
        dirReceptor.setMunicipio("03");
        dirReceptor.setComplemento("SIN DIRECCION");
        receptor.setDireccion(dirReceptor);

        receptor.setTelefono(
                reservation.getPhone() != null && reservation.getPhone().matches("\\d{8,}")
                        ? reservation.getPhone()
                        : null
        );

        receptor.setCorreo(
                reservation.getEmail() != null && !reservation.getEmail().trim().isEmpty()
                        ? reservation.getEmail().trim()
                        : null
        );

        receptor.setCodActividad(null);
        receptor.setDescActividad(null);
        receptor.setNrc(null);

        dte.setReceptor(receptor);

        // ============================
        // CUERPO DOCUMENTO Y CALCULOS
        // ============================

        long nights = Math.max(1, reservation.getFinishDate().toEpochDay() - reservation.getInitDate().toEpochDay());

        BigDecimal totalNeto = BigDecimal.ZERO;
        int totalRoomsQty = 0;

        for (ReservationRoomDTO roomDto : rooms) {
            Room room = roomRepository.findById(roomDto.getRoomId())
                    .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Room"));

            BigDecimal price = BigDecimal.valueOf(room.getPrice());
            int cantidad = roomDto.getQuantity() * (int) nights;

            BigDecimal subtotalRoom = price.multiply(BigDecimal.valueOf(cantidad));
            subtotalRoom = round2(subtotalRoom);

            totalNeto = totalNeto.add(subtotalRoom);
            totalRoomsQty += cantidad;
        }

        // IVA sobre neto
        BigDecimal iva = round2(totalNeto.multiply(new BigDecimal("0.13")));

        // Total Bruto (neto + iva)
        BigDecimal totalBruto = totalNeto.add(iva);

        // Tributo (5%) sobre neto
        BigDecimal tributo = round2(totalNeto.multiply(new BigDecimal("0.05")));

        // Total a pagar
        BigDecimal totalPagar = totalBruto.add(tributo);

        // Precio unitario neto
        BigDecimal precioUnitNeto = totalRoomsQty > 0
                ? round2(totalNeto.divide(BigDecimal.valueOf(totalRoomsQty), 2, RoundingMode.HALF_UP))
                : BigDecimal.ZERO;

        // IVA unitario
        BigDecimal ivaUnit = round2(precioUnitNeto.multiply(new BigDecimal("0.13")));

        // Precio unitario bruto (neto + iva)
        BigDecimal precioUnitBruto = precioUnitNeto.add(ivaUnit);

        // IVA total del ítem
        BigDecimal ivaTotalItem = ivaUnit.multiply(BigDecimal.valueOf(totalRoomsQty));

        // ============================
        // CUERPO DOCUMENTO
        // ============================
        List<CuerpoDocumentoDTO> items = new ArrayList<>();
        CuerpoDocumentoDTO item = new CuerpoDocumentoDTO();
        item.setNumItem(1);
        item.setTipoItem(1);
        item.setNumeroDocumento(null);
        item.setCantidad((double) totalRoomsQty);
        item.setCodigo("HAB001");
        item.setCodTributo(null);
        item.setUniMedida(3);
        item.setDescripcion(cleanText("Habitación(es) reservadas - " + nights + " noches"));
        item.setPrecioUni(precioUnitBruto.doubleValue());
        item.setMontoDescu(0.0);
        item.setVentaNoSuj(0.0);
        item.setVentaExenta(0.0);
        item.setVentaGravada(totalBruto.doubleValue());
        item.setTributos(Collections.singletonList("59"));
        item.setPsv(0.0);
        item.setNoGravado(0.0);
        item.setIvaItem(ivaTotalItem.doubleValue());
        items.add(item);

        dte.setCuerpoDocumento(items);

        // ============================
        // RESUMEN
        // ============================
        ResumenDTO resumen = new ResumenDTO();
        resumen.setTotalNoSuj(0.0);
        resumen.setTotalExenta(0.0);
        resumen.setTotalGravada(totalBruto.doubleValue());
        resumen.setSubTotalVentas(totalBruto.doubleValue());
        resumen.setDescuNoSuj(0.0);
        resumen.setDescuExenta(0.0);
        resumen.setDescuGravada(0.0);
        resumen.setPorcentajeDescuento(0.0);
        resumen.setTotalDescu(0.0);

        resumen.setTributos(Collections.singletonList(
                new TributoDTO("59", "Turismo: por alojamiento (5%)", tributo.doubleValue())
        ));

        resumen.setSubTotal(totalBruto.doubleValue());
        resumen.setIvaRete1(0.0);
        resumen.setReteRenta(0.0);
        resumen.setMontoTotalOperacion(totalPagar.doubleValue());
        resumen.setTotalNoGravado(0.0);
        resumen.setTotalPagar(totalPagar.doubleValue());
        resumen.setTotalLetras(cleanText(convertNumberToLetras(totalPagar.doubleValue())));
        resumen.setTotalIva(iva.doubleValue());
        resumen.setSaldoFavor(0.0);
        resumen.setCondicionOperacion(1);

        List<PagoDTO> pagos = new ArrayList<>();
        PagoDTO pago = new PagoDTO();
        pago.setCodigo("09");
        pago.setMontoPago(totalPagar.doubleValue());
        pagos.add(pago);
        resumen.setPagos(pagos);
        resumen.setNumPagoElectronico(null);

        dte.setResumen(resumen);

        // ============================
        // EXTENSION
        // ============================
        ExtensionDTO extension = new ExtensionDTO();
        extension.setNombEntrega(null);
        extension.setDocuEntrega(null);
        extension.setNombRecibe(null);
        extension.setDocuRecibe(null);
        extension.setPlacaVehiculo(null);
        extension.setObservaciones(cleanText("INFO LOGIC - SIN OBSERVACIONES"));

        dte.setExtension(extension);

        // ============================
        // PARAMS JASPER
        // ============================
        Map<String, Object> params = new HashMap<>();
        params.put("emisorNombre", emisor.getNombre());
        params.put("emisorNit", emisor.getNit());
        params.put("emisorNrc", emisor.getNrc());
        params.put("emisorDireccion", emisor.getDireccion().getComplemento());
        params.put("emisorTelefono", emisor.getTelefono());
        params.put("emisorCorreo", emisor.getCorreo());

        params.put("clienteNombre", receptor.getNombre());
        params.put("clienteDocumento", receptor.getNumDocumento());
        params.put("clienteTelefono", receptor.getTelefono() != null ? receptor.getTelefono() : "");
        params.put("clienteCorreo", receptor.getCorreo() != null ? receptor.getCorreo() : "");
        params.put("fechaEmision", identificacion.getFecEmi());
        params.put("horaEmision", identificacion.getHorEmi());

        params.put("descripcionItem", item.getDescripcion());
        params.put("cantidadItem", String.valueOf(item.getCantidad().intValue()));

        params.put("precioItem", precioUnitBruto.toPlainString());
        params.put("subtotalItem", totalBruto.toPlainString());

        params.put("precioUnitarioNeto", precioUnitNeto.toPlainString());
        params.put("ivaUnitario", ivaUnit.toPlainString());
        params.put("precioUnitarioBruto", precioUnitBruto.toPlainString());

        params.put("totalNeto", totalNeto.toPlainString());
        params.put("totalIva", iva.toPlainString());
        params.put("totalTributo", tributo.toPlainString());
        params.put("totalGravado", totalNeto.toPlainString());
        params.put("subTotalVentas", totalNeto.toPlainString());
        params.put("totalPagar", totalPagar.toPlainString());

        params.put("codigoGeneracion", identificacion.getCodigoGeneracion());
        params.put("numerodeControl", identificacion.getNumeroControl());
        params.put("observaciones", extension.getObservaciones());
        params.put("logoImage", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQLo8t9NH1j1eo_tGo70lM2OcYKY4mhwhntvA&s");

        // -----------------------------------
        // Generar link del QR para consulta en Hacienda
        // -----------------------------------
        String ambiente = identificacion.getAmbiente();
        String codGen = identificacion.getCodigoGeneracion();
        String fechaEmi = identificacion.getFecEmi();

        String qrUrl = String.format(
                "https://admin.factura.gob.sv/consultaPublica?ambiente=%s&codGen=%s&fechaEmi=%s",
                ambiente,
                codGen,
                fechaEmi
        );

        // Generar QR en base64 (150x150 píxeles)
        String qrBase64 = QrCodeGenerator.generateQrBase64(qrUrl, 150, 150);

        if (qrBase64 != null) {
            // Convertir Base64 a bytes
            byte[] qrBytes = java.util.Base64.getDecoder().decode(qrBase64);

            // Convertir a InputStream
            java.io.InputStream qrStream = new java.io.ByteArrayInputStream(qrBytes);

            // Pasar el InputStream al parámetro Jasper
            params.put("qrImageBase64", qrStream);
        } else {
            params.put("qrImageBase64", null);
        }






        return new DteBuilderResult(dte, params);
    }

    private String generarNumeroControl() {
        long correlativo = correlativoService.nextCorrelativo("01", "S006", "M201");
        String correlativoStr = String.format("%015d", correlativo);
        return String.format("DTE-01-S006M201-%s", correlativoStr);
    }

    private BigDecimal round2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String convertNumberToLetras(double value) {
        long parteEntera = (long) value;
        int centavos = (int) Math.round((value - parteEntera) * 100);
        return String.format("%d dolares con %02d/100", parteEntera, centavos);
    }

    private String cleanText(String text) {
        if (text == null) return null;
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }
}
