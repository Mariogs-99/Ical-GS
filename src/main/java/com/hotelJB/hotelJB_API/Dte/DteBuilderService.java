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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DteBuilderService {

    @Autowired
    private RoomRepository roomRepository;

    public DteRequestDTO buildDte(Reservation reservation, List<ReservationRoomDTO> rooms, String numeroControl) {
        DteRequestDTO dte = new DteRequestDTO();

        // Identificacion
        IdentificacionDTO identificacion = new IdentificacionDTO();
        identificacion.setVersion("1");
        identificacion.setAmbiente("00");
        identificacion.setTipoDte("11"); // Consumidor Final
        identificacion.setNumeroControl(numeroControl);
        identificacion.setCodigoGeneracion(UUID.randomUUID().toString());
        dte.setIdentificacion(identificacion);

        // Emisor
        EmisorDTO emisor = new EmisorDTO();
        emisor.setNit("06140308061025");
        emisor.setNrc("123456-7");
        emisor.setNombre("Hotel Jardines de las Marias");
        dte.setEmisor(emisor);

        // Receptor (Cliente)
        ReceptorDTO receptor = new ReceptorDTO();
        receptor.setTipoDocumento("36"); // DUI
        receptor.setNumDocumento("00000000-0"); // genérico si no tienes DUI real
        receptor.setNombre(reservation.getName());
        dte.setReceptor(receptor);

        // CuerpoDocumento
        List<CuerpoDocumentoDTO> items = new ArrayList<>();
        double total = 0.0;
        int itemIndex = 1;

        long nights = ChronoUnit.DAYS.between(
                reservation.getInitDate(),
                reservation.getFinishDate()
        );

        for (ReservationRoomDTO roomDto : rooms) {
            Room room = roomRepository.findById(roomDto.getRoomId())
                    .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Room"));

            double price = round(room.getPrice());
            int totalNights = (int) nights;
            int quantity = roomDto.getQuantity() * totalNights;
            double monto = round(price * quantity);

            CuerpoDocumentoDTO item = new CuerpoDocumentoDTO();
            item.setNumItem(String.valueOf(itemIndex++));
            item.setDescripcion(room.getNameEs());
            item.setCantidad(quantity);
            item.setPrecioUni(price);
            item.setVentaGravada(monto);

            items.add(item);
            total += monto;
        }


        dte.setCuerpoDocumento(items);

        // Resumen
        double totalRounded = round(total);
        double iva = round(total * 0.13);
        double totalPagar = round(total + iva);

        ResumenDTO resumen = new ResumenDTO();
        resumen.setTotalGravada(totalRounded);
        resumen.setSubTotalVentas(totalRounded);
        resumen.setIva(iva);
        resumen.setTotalPagar(totalPagar);
        dte.setResumen(resumen);


        return dte;
    }

    /**
     * Método auxiliar para redondear a 2 decimales.
     */
    private double round(double value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
