package com.hotelJB.hotelJB_API.Dte;

import com.hotelJB.hotelJB_API.Dte.conf.DteCorrelativoService;
import com.hotelJB.hotelJB_API.Dte.dto.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DteBuilderService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private DteCorrelativoService correlativoService;

    public DteRequestDTO buildDte(Reservation reservation, List<ReservationRoomDTO> rooms) {

        DteRequestDTO dte = new DteRequestDTO();

        // ============================
        // IDENTIFICACION
        // ============================
        IdentificacionDTO identificacion = new IdentificacionDTO();
        identificacion.setVersion(1);
        identificacion.setAmbiente("00");
        identificacion.setTipoDte("01");

        String numeroControl = generarNumeroControl();
        identificacion.setNumeroControl(numeroControl);
        identificacion.setCodigoGeneracion(UUID.randomUUID().toString().toUpperCase());
        identificacion.setTipoModelo(1);
        identificacion.setTipoOperacion(1);
        identificacion.setTipoMoneda("USD");
        identificacion.setFecEmi(reservation.getInitDate().toString());
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

        // ============================
        // EMISOR
        // ============================
        EmisorDTO emisor = new EmisorDTO();
        emisor.setNit("06140308061025");
        emisor.setNrc("1748340");
        emisor.setNombre(cleanText("INFO LOGIC"));
        emisor.setCodActividad("62020");
        emisor.setDescActividad(cleanText("CONSULTORÍAS Y GESTIÓN DE SERVICIOS INFORMÁTICOS"));
        emisor.setNombreComercial(cleanText("INFORMATICA Y LOGISTICA, SOCIEDAD ANONIMA DE CAPITAL VARIABLE"));
        emisor.setTipoEstablecimiento("01");

        DireccionDTO dirEmisor = new DireccionDTO();
        dirEmisor.setDepartamento("06");
        dirEmisor.setMunicipio("14");
        dirEmisor.setComplemento(cleanText("Av. Arturo numero 3"));
        emisor.setDireccion(dirEmisor);

        emisor.setTelefono("73181827");
        emisor.setCorreo("escobar.mario@globalsolutionslt.com");
        emisor.setCodEstableMH("S006");
        emisor.setCodEstable("S006");
        emisor.setCodPuntoVentaMH("M201");
        emisor.setCodPuntoVenta("M201");

        dte.setEmisor(emisor);

        // ============================
        // RECEPTOR
        // ============================
        ReceptorDTO receptor = new ReceptorDTO();
        receptor.setTipoDocumento("13");
        receptor.setNumDocumento("00425287-9");

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

        if (reservation.getPhone() != null &&
                reservation.getPhone().trim().matches("\\d{8,}")) {
            receptor.setTelefono(reservation.getPhone().trim());
        } else {
            receptor.setTelefono(null);
        }

        if (reservation.getEmail() != null && !reservation.getEmail().trim().isEmpty()) {
            receptor.setCorreo(reservation.getEmail().trim());
        } else {
            receptor.setCorreo(null);
        }

        receptor.setCodActividad(null);
        receptor.setDescActividad(null);
        receptor.setNrc(null);

        dte.setReceptor(receptor);

        // ============================
        // CUERPO DOCUMENTO Y CALCULOS
        // ============================

        long nights = Math.max(1, reservation.getFinishDate().toEpochDay() - reservation.getInitDate().toEpochDay());

        BigDecimal totalBruto = BigDecimal.ZERO;
        int totalRoomsQty = 0;

        for (ReservationRoomDTO roomDto : rooms) {
            Room room = roomRepository.findById(roomDto.getRoomId())
                    .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Room"));

            BigDecimal price = BigDecimal.valueOf(room.getPrice());
            int totalNights = (int) nights;
            int cantidad = roomDto.getQuantity() * totalNights;

            BigDecimal subtotalRoom = price.multiply(BigDecimal.valueOf(cantidad));
            subtotalRoom = round2(subtotalRoom);

            totalBruto = totalBruto.add(subtotalRoom);
            totalRoomsQty += cantidad;
        }

        // CINEMARK LOGIC
        BigDecimal totalIva = round2(
                totalBruto.multiply(new BigDecimal("13"))
                        .divide(new BigDecimal("113"), 2, RoundingMode.HALF_UP)
        );

        BigDecimal ventaGravadaTotal = totalBruto;
        BigDecimal totalPagar = totalBruto;

        BigDecimal precioUnitBruto = round2(
                totalBruto.divide(BigDecimal.valueOf(totalRoomsQty), 2, RoundingMode.HALF_UP)
        );

        BigDecimal ivaUnit = round2(
                precioUnitBruto.multiply(new BigDecimal("13"))
                        .divide(new BigDecimal("113"), 2, RoundingMode.HALF_UP)
        );

        // Crear único ítem
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
        item.setVentaGravada(ventaGravadaTotal.doubleValue());
        item.setTributos(null);
        item.setPsv(0.0);
        item.setNoGravado(0.0);
        item.setIvaItem(ivaUnit.multiply(BigDecimal.valueOf(totalRoomsQty)).doubleValue());
        items.add(item);

        dte.setCuerpoDocumento(items);

        // ============================
        // RESUMEN
        // ============================
        ResumenDTO resumen = new ResumenDTO();
        resumen.setTotalNoSuj(0.0);
        resumen.setTotalExenta(0.0);
        resumen.setTotalGravada(ventaGravadaTotal.doubleValue());
        resumen.setSubTotalVentas(ventaGravadaTotal.doubleValue());
        resumen.setDescuNoSuj(0.0);
        resumen.setDescuExenta(0.0);
        resumen.setDescuGravada(0.0);
        resumen.setPorcentajeDescuento(0.0);
        resumen.setTotalDescu(0.0);
        resumen.setTributos(null);
        resumen.setSubTotal(ventaGravadaTotal.doubleValue());
        resumen.setIvaRete1(0.0);
        resumen.setReteRenta(0.0);
        resumen.setMontoTotalOperacion(ventaGravadaTotal.doubleValue());
        resumen.setTotalNoGravado(0.0);
        resumen.setTotalPagar(totalPagar.doubleValue());
        resumen.setTotalLetras(cleanText(convertNumberToLetras(totalPagar.doubleValue())));
        resumen.setTotalIva(totalIva.doubleValue());
        resumen.setSaldoFavor(0.0);
        resumen.setCondicionOperacion(1);

        List<PagoDTO> pagos = new ArrayList<>();
        PagoDTO pago = new PagoDTO();
        pago.setCodigo("03");
        pago.setMontoPago(totalPagar.doubleValue());
        pago.setReferencia(null);
        pago.setPlazo(null);
        pago.setPeriodo(null);
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

        return dte;
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
