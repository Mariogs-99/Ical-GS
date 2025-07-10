package com.hotelJB.hotelJB_API.Dte;

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

    // Simulador de correlativo secuencial
    private static long correlativo = 1;

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

        // CAMPOS OBLIGATORIOS AUNQUE NULL
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
        // CUERPO DOCUMENTO
        // ============================
        List<CuerpoDocumentoDTO> items = new ArrayList<>();
        double total = 0.0;
        int itemIndex = 1;

        long nights = Math.max(1, reservation.getFinishDate().toEpochDay() - reservation.getInitDate().toEpochDay());

        for (ReservationRoomDTO roomDto : rooms) {
            Room room = roomRepository.findById(roomDto.getRoomId())
                    .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Room"));

            double price = round(room.getPrice());
            int totalNights = (int) nights;
            int quantity = roomDto.getQuantity() * totalNights;
            double monto = round(price * quantity);
            double ivaItem = round(monto * 0.13);

            CuerpoDocumentoDTO item = new CuerpoDocumentoDTO();
            item.setNumItem(itemIndex++);
            item.setTipoItem(1);
            item.setNumeroDocumento(null);
            item.setCantidad((double) quantity);
            item.setCodigo("2-333");
            item.setCodTributo(null);
            item.setUniMedida(59);
            item.setDescripcion(cleanText(room.getNameEs()) + " - " + totalNights + " noches");
            item.setPrecioUni(price);
            item.setMontoDescu(0.0);
            item.setVentaNoSuj(0.0);
            item.setVentaExenta(0.0);
            item.setVentaGravada(monto);
            item.setTributos(null);
            item.setPsv(monto);
            item.setNoGravado(0.0);
            item.setIvaItem(ivaItem);
            items.add(item);
            total += monto;
        }
        dte.setCuerpoDocumento(items);

        // ============================
        // RESUMEN
        // ============================
        double totalRounded = round(total);
        double iva = round(total * 0.13);
        double totalPagar = round(total + iva);

        ResumenDTO resumen = new ResumenDTO();
        resumen.setTotalNoSuj(0.0);
        resumen.setTotalExenta(0.0);
        resumen.setTotalGravada(totalRounded);
        resumen.setSubTotalVentas(totalRounded);
        resumen.setDescuNoSuj(0.0);
        resumen.setDescuExenta(0.0);
        resumen.setDescuGravada(0.0);
        resumen.setPorcentajeDescuento(0.0);
        resumen.setTotalDescu(0.0);
        resumen.setTributos(null);
        resumen.setSubTotal(totalRounded);
        resumen.setIvaRete1(0.0);
        resumen.setReteRenta(0.0);
        resumen.setMontoTotalOperacion(totalPagar);
        resumen.setTotalNoGravado(0.0);
        resumen.setTotalPagar(totalPagar);
        resumen.setTotalLetras(cleanText(convertNumberToLetras(totalPagar)));
        resumen.setTotalIva(iva);
        resumen.setSaldoFavor(0.0);
        resumen.setCondicionOperacion(1);

        List<PagoDTO> pagos = new ArrayList<>();
        PagoDTO pago = new PagoDTO();
        pago.setCodigo("03");
        pago.setMontoPago(totalPagar);
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

    /**
     * Genera número de control con correlativo de 12 dígitos
     */
    private synchronized String generarNumeroControl() {
        String establecimiento = "S006";
        String puntoVenta = "M201";
        String correlativoStr = String.format("%012d", correlativo++);
        return String.format("DTE-01-%s%s-%s", establecimiento, puntoVenta, correlativoStr);
    }

    private double round(double value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String convertNumberToLetras(double value) {
        long parteEntera = (long) value;
        int centavos = (int) Math.round((value - parteEntera) * 100);
        return String.format("%d dólares con %02d/100", parteEntera, centavos);
    }

    private String cleanText(String text) {
        if (text == null) return null;
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }
}
