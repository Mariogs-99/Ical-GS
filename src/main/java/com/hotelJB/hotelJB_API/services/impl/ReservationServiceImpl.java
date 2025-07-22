package com.hotelJB.hotelJB_API.services.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelJB.hotelJB_API.Dte.DteAuthService;
import com.hotelJB.hotelJB_API.Dte.DteBuilderService;
import com.hotelJB.hotelJB_API.Dte.DteSignerService;
import com.hotelJB.hotelJB_API.Dte.DteTransmitterService;
import com.hotelJB.hotelJB_API.Dte.dto.DteBuilderResult;
import com.hotelJB.hotelJB_API.Dte.dto.DteRequestDTO;
import com.hotelJB.hotelJB_API.Dte.dto.DteResponse;
import com.hotelJB.hotelJB_API.Dte.general.Dte;
import com.hotelJB.hotelJB_API.Dte.general.DteRepository;
import com.hotelJB.hotelJB_API.models.dtos.ReservationDTO;
import com.hotelJB.hotelJB_API.models.dtos.ReservationRoomDTO;
import com.hotelJB.hotelJB_API.models.entities.Reservation;
import com.hotelJB.hotelJB_API.models.entities.ReservationRoom;
import com.hotelJB.hotelJB_API.models.entities.Room;
import com.hotelJB.hotelJB_API.models.responses.*;
import com.hotelJB.hotelJB_API.repositories.ReservationRepository;
import com.hotelJB.hotelJB_API.repositories.ReservationRoomRepository;
import com.hotelJB.hotelJB_API.repositories.RoomRepository;
import com.hotelJB.hotelJB_API.services.EmailSenderService;
import com.hotelJB.hotelJB_API.services.ReservationRoomService;
import com.hotelJB.hotelJB_API.services.ReservationService;
import com.hotelJB.hotelJB_API.utils.CustomException;
import com.hotelJB.hotelJB_API.utils.ErrorType;
import com.hotelJB.hotelJB_API.websocket.WebSocketNotificationService;
import com.hotelJB.hotelJB_API.wompi.WompiService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JREmptyDataSource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReservationServiceImpl implements ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRoomRepository reservationRoomRepository;

    @Autowired
    private ReservationRoomService reservationRoomService;

    @Autowired
    private EmailSenderService gmailSenderService;

    @Autowired
    private EmailSenderService emailSenderService;


    @Autowired
    private WebSocketNotificationService webSocketNotificationService;

    @Autowired
    private WompiService wompiService;

    @Autowired
    private DteBuilderService dteBuilderService;

    @Autowired
    private DteSignerService dteSignerService;

    @Autowired
    private DteAuthService dteAuthService;

    @Autowired
    private DteTransmitterService dteTransmitterService;

    @Autowired
    private DteRepository dteRepository;



    @Override
    public ReservationResponse save(ReservationDTO data) throws Exception {
        int totalReserved = data.getRooms().stream()
                .mapToInt(ReservationRoomDTO::getQuantity)
                .sum();

        Reservation reservation = new Reservation(
                data.getInitDate(),
                data.getFinishDate(),
                data.getCantPeople(),
                data.getName(),
                data.getEmail(),
                data.getPhone(),
                data.getPayment(),
                null,
                totalReserved
        );

        String initialStatus = data.getInitDate().isAfter(LocalDate.now()) ? "FUTURA" : "ACTIVA";
        reservation.setStatus(initialStatus);
        reservation.setRoomNumber(data.getRoomNumber());

        if (!data.getRooms().isEmpty()) {
            int firstRoomId = data.getRooms().get(0).getRoomId();
            Room room = roomRepository.findById(firstRoomId)
                    .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Room"));
            reservation.setRoom(room);
        }

        // Guardar la reserva para obtener el ID
        reservationRepository.save(reservation);

        // Generar reservationCode
        String wompiReference = "Reserva-" + reservation.getReservationId();
        reservation.setReservationCode(wompiReference);

        // Generar número de control DTE
        String numeroControl = "DTE-" + String.format("%014d", reservation.getReservationId());
        reservation.setDteControlNumber(numeroControl);

        // Guardar cambios
        reservationRepository.save(reservation);

        System.out.println("Referencia Wompi generada: " + wompiReference);

        // Notificar por websocket
        webSocketNotificationService.notifyNewReservation(reservation);

        // Guardar habitaciones reservadas
        reservationRoomService.saveRoomsForReservation(reservation.getReservationId(), data.getRooms());

        List<ReservationRoomResponse> roomResponses = data.getRooms().stream().map(roomDTO -> {
            Room r = roomRepository.findById(roomDTO.getRoomId())
                    .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Room"));
            ReservationRoomResponse resp = new ReservationRoomResponse();
            resp.setRoomId(r.getRoomId());
            resp.setRoomName(r.getNameEs());
            resp.setAssignedRoomNumber(roomDTO.getAssignedRoomNumber());
            resp.setQuantity(roomDTO.getQuantity());
            return resp;
        }).collect(Collectors.toList());

        // ------------------------------------------------
        // FLUJO DTE
        // ------------------------------------------------

        // Builder adaptado
        DteBuilderResult result = dteBuilderService.buildDte(reservation, data.getRooms());
        DteRequestDTO dteRequest = result.getDteRequest();
        Map<String, Object> jasperParams = result.getJasperParams();



        ObjectMapper mapper = new ObjectMapper();
        String dteJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dteRequest);

        System.out.println("====================================");
        System.out.println("DTE GENERADO:");
        System.out.println(dteJson);
        System.out.println("====================================");

        try {
            // 2. Firmar el DTE
            String dteFirmado = dteSignerService.firmar(dteJson);
            System.out.println("DTE FIRMADO:");
            System.out.println(dteFirmado);

            // 3. Obtener token de MH
            String token = dteAuthService.obtenerToken();

            // Enviar DTE
            DteResponse dteResponse = dteTransmitterService.enviarDte(dteRequest, token);

            jasperParams.put("selloRecibido", dteResponse.getSelloRecibido() != null ? dteResponse.getSelloRecibido() : "");

            System.out.println("RESPUESTA HACIENDA:");
            System.out.println(dteResponse);

            if (dteResponse.isExitoso()) {
                System.out.println("✅ DTE RECIBIDO CORRECTAMENTE");
            } else {
                System.out.println("❌ DTE RECHAZADO: " + dteResponse.getMensaje());
                System.out.println("OBSERVACIONES: " + dteResponse.getObservaciones());
            }

            // Guardar DTE en base de datos
            Dte dteEntity = new Dte();
            dteEntity.setReservationCode(reservation.getReservationCode());
            dteEntity.setNumeroControl(dteRequest.getIdentificacion().getNumeroControl());
            dteEntity.setCodigoGeneracion(dteRequest.getIdentificacion().getCodigoGeneracion());
            dteEntity.setTipoDte(dteRequest.getIdentificacion().getTipoDte());
            dteEntity.setEstado(dteResponse.getEstado());
            dteEntity.setFechaGeneracion(java.time.LocalDateTime.now());
            dteEntity.setDteJson(dteJson);
            dteEntity.setRespuestaHaciendaJson(
                    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(dteResponse)
            );

            dteRepository.save(dteEntity);

            System.out.println("DTE guardado en base de datos.");
            System.out.println("RESPUESTA HACIENDA:");
            System.out.println(dteResponse);

            if (dteResponse.isExitoso()) {
                System.out.println("✅ DTE RECIBIDO CORRECTAMENTE");
                // Podrías guardar códigoGeneracion en reserva si quieres:
                // reservation.setCodigoGeneracion(dteResponse.getCodigoGeneracion());
                // reservationRepository.save(reservation);
            } else {
                System.out.println("❌ DTE RECHAZADO: " + dteResponse.getMensaje());
                System.out.println("OBSERVACIONES: " + dteResponse.getObservaciones());
            }

        } catch (Exception e) {
            System.out.println("⚠️ No se pudo procesar el DTE con Hacienda:");
            e.printStackTrace();
            // Aquí puedes notificar al admin o guardar log, etc.
        }

        try {
            // Compilar plantilla
            JasperReport jasperReport = JasperCompileManager.compileReport(
                    "src/main/resources/reports/DTEFactura.jrxml"
            );

            // Llenar el reporte
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    jasperParams,
                    new JREmptyDataSource()
            );

            // Carpeta PDF en raíz del proyecto
            String pdfDirectory = "PDF";
            String pdfFileName = "DTEFactura_" + reservation.getReservationId() + ".pdf";

            File folder = new File(pdfDirectory);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String pdfPath = pdfDirectory + File.separator + pdfFileName;

            // Exportar a archivo PDF
            JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);

            System.out.println("✅ PDF DTE generado en: " + pdfPath);

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("❌ Error generando el PDF DTE.");
        }




        // ------------------------------------------------
        // ENVÍO DE CORREO
        // ------------------------------------------------

        String htmlBody = String.format("""
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <style>
    body {
      font-family: 'Segoe UI', 'Helvetica Neue', sans-serif;
      background-color: #f3f3f3;
      padding: 30px 15px;
      color: #333;
    }
    .container {
      background-color: #ffffff;
      padding: 40px;
      max-width: 700px;
      margin: auto;
      border-radius: 16px;
      box-shadow: 0 6px 20px rgba(0, 0, 0, 0.05);
    }
    .logo {
      text-align: center;
      margin-bottom: 25px;
    }
    .logo img {
      height: 70px;
    }
    h2 {
      color: #2E7D32;
      font-size: 1.8rem;
      text-align: center;
      margin-bottom: 30px;
    }
    .section-title {
      font-size: 1.1rem;
      color: #4E342E;
      margin-bottom: 10px;
      font-weight: bold;
    }
    .info-box {
      background-color: #FAFAFA;
      border: 1px solid #E0E0E0;
      padding: 20px;
      border-radius: 10px;
      font-size: 0.95rem;
      margin-bottom: 20px;
    }
    .info-box p {
      margin: 10px 0;
    }
    .highlight {
      color: #2E7D32;
      font-weight: 600;
    }
    .reservation-code {
      text-align: center;
      font-size: 1.2rem;
      color: #1B5E20;
      font-weight: bold;
      margin-top: 30px;
    }
    .footer {
      margin-top: 40px;
      text-align: center;
      font-size: 0.85rem;
      color: #777;
    }
    .contact-box {
      margin-top: 40px;
      font-size: 0.95rem;
      text-align: center;
      border-top: 1px solid #ddd;
      padding-top: 30px;
      color: #444;
    }
    .contact-box p {
      margin: 6px 0;
    }
    .contact-logo {
      font-size: 1.4rem;
      color: #2E7D32;
      font-weight: bold;
    }
    .social-icons {
      margin-top: 10px;
    }
    .social-icons a {
      margin: 0 6px;
      text-decoration: none;
      font-weight: bold;
      color: #555;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="logo">
      <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQLo8t9NH1j1eo_tGo70lM2OcYKY4mhwhntvA&s" alt="Hotel Jardines de las Marías" />
    </div>
    <h2>¡Gracias por su reserva, %s!</h2>
    <div class="section-title">Resumen de la reserva</div>
    <div class="info-box">
      <p><span class="highlight">Fecha de entrada:</span> %s</p>
      <p><span class="highlight">Fecha de salida:</span> %s</p>
      <p><span class="highlight">Cantidad de personas:</span> %d</p>
      <p><span class="highlight">Cantidad de habitaciones:</span> %d</p>
    </div>
    <div class="reservation-code">Código de Reserva: %s</div>
    <div class="footer">
      Este es un mensaje automático. Si necesita asistencia, puede contactarnos:
    </div>
    <div class="contact-box">
      <div class="contact-logo">Hotel Jardines de las Marías</div>
      <p>📞 2562-8891</p>
      <p>📱 7890-5449</p>
      <p>✉️ jardindelasmariashotel@gmail.com</p>
      <p>📍 2 Avenida sur #23, Barrio el Calvario, Suchitoto</p>
      <div class="social-icons">
        <a href="https://www.facebook.com/hoteljardindelasmarias" target="_blank">Facebook</a> |
        <a href="https://www.instagram.com/hoteljardindelasmarias/" target="_blank">Instagram</a>
      </div>
    </div>
  </div>
</body>
</html>
""",
                reservation.getName(),
                reservation.getInitDate(),
                reservation.getFinishDate(),
                reservation.getCantPeople(),
                reservation.getQuantityReserved(),
                reservation.getReservationCode()
        );


        try {
            String pdfPath = "PDF" + File.separator + "DTEFactura_" + reservation.getReservationId() + ".pdf";

            byte[] pdfBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(pdfPath));
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

            // Codificar JSON
            String base64Json = Base64.getEncoder()
                    .encodeToString(dteJson.getBytes(StandardCharsets.UTF_8));

            // Armar mapa de adjuntos
            Map<String, String> attachments = new HashMap<>();
            attachments.put("DTEFactura_" + reservation.getReservationId() + ".pdf", base64Pdf);
            attachments.put("DTEFactura_" + reservation.getReservationId() + ".json", base64Json);

            // Enviar correo con PDF + JSON
            emailSenderService.sendMailWithMultipleAttachments(
                    reservation.getEmail(),
                    "Confirmación de Reserva - Hotel Jardines de las Marías",
                    htmlBody,
                    attachments
            );


//            emailSenderService.sendMailWithAttachment(
//                    "escobar.mario@globalsolutionslt.com", // ← correo de prueba fijo
//                    "Confirmación de Reserva - Hotel Jardines de las Marías",
//                    htmlBody,
//                    base64Pdf,
//                    "DTEFactura_" + reservation.getReservationId() + ".pdf"
//            );


            System.out.println("✅ Correo enviado con PDF adjunto.");

        } catch (Exception e) {
            System.out.println("❌ Error enviando correo con PDF adjunto:");
            e.printStackTrace();
        }


        RoomShortResponse roomShortResponse = null;
        if (reservation.getRoom() != null) {
            roomShortResponse = new RoomShortResponse(
                    reservation.getRoom().getRoomId(),
                    reservation.getRoom().getNameEs()
            );
        }

        return new ReservationResponse(
                reservation.getReservationId(),
                reservation.getReservationCode(),
                reservation.getInitDate(),
                reservation.getFinishDate(),
                reservation.getCantPeople(),
                reservation.getName(),
                reservation.getEmail(),
                reservation.getPhone(),
                reservation.getPayment(),
                reservation.getQuantityReserved(),
                reservation.getCreationDate(),
                reservation.getStatus(),
                roomResponses,
                roomShortResponse,
                reservation.getRoomNumber(),
                reservation.getDteControlNumber(),
                null
        );
    }




    @Override
    @Transactional
    public void update(ReservationDTO data, int reservationId) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Reservation"));

        reservation.setInitDate(data.getInitDate());
        reservation.setFinishDate(data.getFinishDate());
        reservation.setCantPeople(data.getCantPeople());
        reservation.setName(data.getName());
        reservation.setEmail(data.getEmail());
        reservation.setPhone(data.getPhone());
        reservation.setPayment(data.getPayment());
        reservation.setRoomNumber(data.getRoomNumber());
        reservation.setStatus(data.getStatus());

        reservationRepository.save(reservation);

        reservationRoomService.deleteByReservationId(reservationId);
        reservationRoomService.saveRoomsForReservation(reservationId, data.getRooms());
    }

    @Override
    public void delete(int reservationId) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Reservation"));
        reservationRepository.delete(reservation);
    }

    @Override
    public List<Reservation> getAll() {
        return reservationRepository.findAll();
    }

    @Override
    public Optional<Reservation> findById(int reservationId) {
        return reservationRepository.findById(reservationId);
    }

    @Override
    public List<Map<String, LocalDate>> getFullyBookedDatesForHotel() {
        List<Object[]> reservedDates = reservationRepository.findAllReservedDates();
        if (reservedDates.isEmpty()) return Collections.emptyList();

        long totalRooms = roomRepository.count();
        Map<LocalDate, Integer> reservationCountByDate = new HashMap<>();

        for (Object[] dates : reservedDates) {
            LocalDate startDate = (LocalDate) dates[0];
            LocalDate endDate = (LocalDate) dates[1];
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                reservationCountByDate.put(current, reservationCountByDate.getOrDefault(current, 0) + 1);
                current = current.plusDays(1);
            }
        }

        List<Map<String, LocalDate>> fullyBookedDates = new ArrayList<>();
        for (Map.Entry<LocalDate, Integer> entry : reservationCountByDate.entrySet()) {
            if (entry.getValue() >= totalRooms) {
                Map<String, LocalDate> dateMap = new HashMap<>();
                dateMap.put("fullyBookedDate", entry.getKey());
                fullyBookedDates.add(dateMap);
            }
        }

        return fullyBookedDates;
    }

    @Override
    public List<ReservationResponse> getAllResponses() {
        return reservationRepository.findAll().stream()
                .map(res -> {
                    List<ReservationRoom> reservationRooms = reservationRoomRepository.findByReservation_ReservationId(res.getReservationId());

                    List<ReservationRoomResponse> roomResponses = reservationRooms.stream().map(rr -> {
                        Room r = rr.getRoom();
                        ReservationRoomResponse resp = new ReservationRoomResponse();
                        resp.setRoomId(r.getRoomId());
                        resp.setRoomName(r.getNameEs());
                        resp.setAssignedRoomNumber(rr.getAssignedRoomNumber());
                        resp.setQuantity(rr.getQuantity());
                        return resp;
                    }).collect(Collectors.toList());

                    RoomShortResponse roomShortResponse = null;
                    if (res.getRoom() != null) {
                        roomShortResponse = new RoomShortResponse(
                                res.getRoom().getRoomId(),
                                res.getRoom().getNameEs()
                        );
                    }


                    return new ReservationResponse(
                            res.getReservationId(),
                            res.getReservationCode(),
                            res.getInitDate(),
                            res.getFinishDate(),
                            res.getCantPeople(),
                            res.getName(),
                            res.getEmail(),
                            res.getPhone(),
                            res.getPayment(),
                            res.getQuantityReserved(),
                            res.getCreationDate(),
                            res.getStatus(),
                            roomResponses,
                            roomShortResponse,
                            res.getRoomNumber(),
                            res.getDteControlNumber(),
                            null
                    );
                }).collect(Collectors.toList());
    }

    @Override
    public List<RoomResponse> getAvailableRooms(LocalDate initDate, LocalDate finishDate, int cantPeople) {
        List<Room> allRooms = roomRepository.findAll();
        List<RoomResponse> availableRooms = new ArrayList<>();

        for (Room room : allRooms) {
            int reserved = reservationRoomRepository.countReservedQuantityForRoomInRange(
                    room.getRoomId(), initDate, finishDate
            );

            int disponibles = room.getQuantity() - reserved;

            CategoryRoomResponse categoryResponse = null;
            if (room.getCategoryRoom() != null) {
                var cat = room.getCategoryRoom();
                categoryResponse = new CategoryRoomResponse(
                        cat.getCategoryRoomId(),
                        cat.getNameCategoryEs(),
                        cat.getDescriptionEs(),
                        cat.getRoomSize(),
                        cat.getBedInfo(),
                        null,
                        Boolean.TRUE.equals(cat.getHasTv()),
                        Boolean.TRUE.equals(cat.getHasAc()),
                        Boolean.TRUE.equals(cat.getHasPrivateBathroom())
                );
            }

            availableRooms.add(new RoomResponse(
                    room.getRoomId(),
                    room.getNameEs(),
                    room.getNameEn(),
                    room.getMaxCapacity(),
                    room.getDescriptionEs(),
                    room.getDescriptionEn(),
                    room.getPrice(),
                    room.getSizeBed(),
                    room.getQuantity(),
                    room.getImg() != null ? room.getImg().getPath() : null,
                    disponibles,
                    categoryResponse,
                    disponibles > 0
            ));
        }

        return availableRooms;
    }

    @Override
    public void assignRoomNumber(int reservationId, String roomNumber) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Reservation"));

        boolean roomInUse = reservationRepository.findActiveByRoomNumber(roomNumber).isPresent();
        if (roomInUse) {
            throw new CustomException(ErrorType.NOT_AVAILABLE, "Esa habitación ya está asignada a otra reserva activa.");
        }

        reservation.setRoomNumber(roomNumber);
        reservationRepository.save(reservation);
    }

    @Override
    public Reservation getActiveReservationByRoomNumber(String roomNumber) throws Exception {
        return reservationRepository.findActiveByRoomNumber(roomNumber)
                .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Reserva activa no encontrada."));
    }

    @Override
    public boolean isRoomNumberInUse(String roomNumber) {
        return reservationRepository.findActiveByRoomNumber(roomNumber).isPresent();
    }

    @Override
    public ReservationResponse getByRoomNumber(String roomNumber) throws Exception {
        Reservation reservation = reservationRepository.findTopByRoomNumber(roomNumber)
                .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Reservation"));

        List<ReservationRoom> reservationRooms = reservationRoomRepository.findByReservation_ReservationId(reservation.getReservationId());

        List<ReservationRoomResponse> roomResponses = reservationRooms.stream().map(rr -> {
            Room r = rr.getRoom();
            ReservationRoomResponse resp = new ReservationRoomResponse();
            resp.setRoomId(r.getRoomId());
            resp.setRoomName(r.getNameEs());
            resp.setAssignedRoomNumber(rr.getAssignedRoomNumber());
            resp.setQuantity(rr.getQuantity());
            return resp;
        }).collect(Collectors.toList());

        RoomShortResponse roomShortResponse = null;
        if (reservation.getRoom() != null) {
            roomShortResponse = new RoomShortResponse(
                    reservation.getRoom().getRoomId(),
                    reservation.getRoom().getNameEs()
            );
        }

        return new ReservationResponse(
                reservation.getReservationId(),
                reservation.getReservationCode(),
                reservation.getInitDate(),
                reservation.getFinishDate(),
                reservation.getCantPeople(),
                reservation.getName(),
                reservation.getEmail(),
                reservation.getPhone(),
                reservation.getPayment(),
                reservation.getQuantityReserved(),
                reservation.getCreationDate(),
                reservation.getStatus(),
                roomResponses,
                roomShortResponse,
                reservation.getRoomNumber(),
                reservation.getDteControlNumber(),
                null
        );
    }

    @Override
    public void assignRoomNumbers(int reservationId, List<ReservationRoomDTO> assignments) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Reservation"));

        List<ReservationRoom> reservationRooms = reservationRoomRepository.findByReservation_ReservationId(reservationId);

        for (ReservationRoomDTO dto : assignments) {
            String assignedNumber = dto.getAssignedRoomNumber();

            if (assignedNumber != null && !assignedNumber.trim().isEmpty()) {
                boolean alreadyAssigned = reservationRoomRepository
                        .existsByAssignedRoomNumberAndOtherReservation(assignedNumber.trim(), reservationId);

                if (alreadyAssigned) {
                    throw new CustomException(ErrorType.NOT_AVAILABLE,
                            "La habitación número '" + assignedNumber + "' ya está asignada a otra reserva.");
                }
            }

            for (ReservationRoom rr : reservationRooms) {
                if (rr.getRoom().getRoomId() == dto.getRoomId()) {
                    rr.setAssignedRoomNumber(assignedNumber);
                    rr.setQuantity(dto.getQuantity());
                }
            }
        }

        reservationRoomRepository.saveAll(reservationRooms);
    }


    @Override
    public void saveEntity(Reservation reservation) {
        reservationRepository.save(reservation);
    }


    //?Buscar por medio de reserva
    @Override
    public ReservationResponse getByReservationCode(String reservationCode) throws Exception {
        Reservation reservation = reservationRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Reservation"));

        List<ReservationRoom> reservationRooms =
                reservationRoomRepository.findByReservation_ReservationId(reservation.getReservationId());

        List<ReservationRoomResponse> roomResponses = reservationRooms.stream().map(rr -> {
            Room r = rr.getRoom();
            ReservationRoomResponse resp = new ReservationRoomResponse();
            resp.setRoomId(r.getRoomId());
            resp.setRoomName(r.getNameEs());
            resp.setAssignedRoomNumber(rr.getAssignedRoomNumber());
            resp.setQuantity(rr.getQuantity());
            return resp;
        }).collect(Collectors.toList());

        RoomShortResponse roomShortResponse = null;
        if (reservation.getRoom() != null) {
            roomShortResponse = new RoomShortResponse(
                    reservation.getRoom().getRoomId(),
                    reservation.getRoom().getNameEs()
            );
        }

        return new ReservationResponse(
                reservation.getReservationId(),
                reservation.getReservationCode(),
                reservation.getInitDate(),
                reservation.getFinishDate(),
                reservation.getCantPeople(),
                reservation.getName(),
                reservation.getEmail(),
                reservation.getPhone(),
                reservation.getPayment(),
                reservation.getQuantityReserved(),
                reservation.getCreationDate(),
                reservation.getStatus(),
                roomResponses,
                roomShortResponse,
                reservation.getRoomNumber(),
                reservation.getDteControlNumber(),
                null
        );
    }

    //!Metodo save con status

        @Override
    public ReservationResponse saveWithStatus(ReservationDTO data, String status) throws Exception {
        int totalReserved = data.getRooms().stream()
                .mapToInt(ReservationRoomDTO::getQuantity)
                .sum();

        Reservation reservation = new Reservation(
                data.getInitDate(),
                data.getFinishDate(),
                data.getCantPeople(),
                data.getName(),
                data.getEmail(),
                data.getPhone(),
                data.getPayment(),
                null,
                totalReserved
        );

        reservation.setStatus(status);
        reservation.setRoomNumber(data.getRoomNumber());

        if (!data.getRooms().isEmpty()) {
            int firstRoomId = data.getRooms().get(0).getRoomId();
            Room room = roomRepository.findById(firstRoomId)
                    .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Room"));
            reservation.setRoom(room);
        }

        // Guardar reserva inicial para obtener el ID
        reservationRepository.save(reservation);

        // Generar el reservationCode tipo "Reserva-123"
        String wompiReference = "Reserva-" + reservation.getReservationId();
        reservation.setReservationCode(wompiReference);
        reservationRepository.save(reservation);

        System.out.println("Referencia Wompi generada: " + wompiReference);

        webSocketNotificationService.notifyNewReservation(reservation);

        reservationRoomService.saveRoomsForReservation(reservation.getReservationId(), data.getRooms());

        List<ReservationRoomResponse> roomResponses = data.getRooms().stream().map(roomDTO -> {
            Room r = roomRepository.findById(roomDTO.getRoomId())
                    .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Room"));
            ReservationRoomResponse resp = new ReservationRoomResponse();
            resp.setRoomId(r.getRoomId());
            resp.setRoomName(r.getNameEs());
            resp.setAssignedRoomNumber(roomDTO.getAssignedRoomNumber());
            resp.setQuantity(roomDTO.getQuantity());
            return resp;
        }).collect(Collectors.toList());

        //? Generar HTML del correo
            String htmlBody = String.format("""
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <style>
    body {
      font-family: 'Helvetica Neue', 'Segoe UI', sans-serif;
      background-color: #f9f9f9;
      margin: 0;
      padding: 40px 20px;
      color: #333333;
    }
    .container {
      max-width: 650px;
      background-color: #ffffff;
      margin: auto;
      padding: 40px;
      border-radius: 12px;
      box-shadow: 0 4px 15px rgba(0,0,0,0.1);
    }
    .logo {
      text-align: center;
      margin-bottom: 30px;
    }
    .logo img {
      height: 60px;
    }
    h2 {
      font-size: 1.6rem;
      color: #2E7D32;
      text-align: center;
      margin-bottom: 30px;
      font-weight: 600;
    }
    .info-section {
      border-top: 1px solid #eee;
      border-bottom: 1px solid #eee;
      padding: 20px 0;
      margin-bottom: 30px;
      font-size: 0.95rem;
    }
    .info-row {
      margin: 10px 0;
    }
    .label {
      font-weight: 600;
      color: #555;
      display: inline-block;
      width: 160px;
    }
    .reservation-code {
      text-align: center;
      font-size: 1.2rem;
      color: #2E7D32;
      font-weight: 600;
      margin-top: 20px;
    }
    .footer {
      text-align: center;
      font-size: 0.85rem;
      color: #777;
      margin-top: 40px;
    }
    .contact {
      margin-top: 10px;
      color: #555;
      line-height: 1.6;
    }
    .social-icons {
      margin-top: 15px;
    }
    .social-icons a {
      margin: 0 8px;
      text-decoration: none;
      color: #2E7D32;
      font-weight: 600;
      font-size: 0.9rem;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="logo">
      <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQLo8t9NH1j1eo_tGo70lM2OcYKY4mhwhntvA&s" alt="Hotel Jardines de las Marías" />
    </div>
    <h2>¡Gracias por su reserva, %s!</h2>
    <div class="info-section">
      <div class="info-row"><span class="label">Fecha de entrada:</span> %s</div>
      <div class="info-row"><span class="label">Fecha de salida:</span> %s</div>
      <div class="info-row"><span class="label">Cantidad de personas:</span> %d</div>
      <div class="info-row"><span class="label">Cantidad de habitaciones:</span> %d</div>
    </div>
    <div class="reservation-code">Código de Reserva: %s</div>
    <div class="footer">
      Este es un mensaje automático. Para más información o asistencia:
      <div class="contact">
        Hotel Jardines de las Marías<br/>
        📞 2562-8891 | 📱 7890-5449<br/>
        ✉️ jardindelasmariashotel@gmail.com<br/>
        📍 2 Avenida sur #23, Barrio el Calvario, Suchitoto
      </div>
      <div class="social-icons">
        <a href="https://www.facebook.com/hoteljardindelasmarias" target="_blank">Facebook</a>
        <a href="https://www.instagram.com/hoteljardindelasmarias/" target="_blank">Instagram</a>
      </div>
    </div>
  </div>
</body>
</html>
""",
                    reservation.getName(),
                    reservation.getInitDate(),
                    reservation.getFinishDate(),
                    reservation.getCantPeople(),
                    reservation.getQuantityReserved(),
                    reservation.getReservationCode()
            );


            // Enviar correo si lo necesitas:
        // emailSenderService.sendMail(
        //         reservation.getEmail(),
        //         "Confirmación de Reserva - Hotel Jardines de las Marías",
        //         htmlBody
        // );

            RoomShortResponse roomShortResponse = null;
            if (reservation.getRoom() != null) {
                roomShortResponse = new RoomShortResponse(
                        reservation.getRoom().getRoomId(),
                        reservation.getRoom().getNameEs()
                );
            }

        return new ReservationResponse(
                reservation.getReservationId(),
                reservation.getReservationCode(),
                reservation.getInitDate(),
                reservation.getFinishDate(),
                reservation.getCantPeople(),
                reservation.getName(),
                reservation.getEmail(),
                reservation.getPhone(),
                reservation.getPayment(),
                reservation.getQuantityReserved(),
                reservation.getCreationDate(),
                reservation.getStatus(),
                roomResponses,
                roomShortResponse,
                reservation.getRoomNumber(),
                reservation.getDteControlNumber(),
                null
        );
    }


    //! Enviar correos por medio de webhook

    @Override
    public String buildReservationEmailBody(Reservation reservation) {
        return String.format("""
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <style>
    body {
      font-family: 'Segoe UI', 'Helvetica Neue', sans-serif;
      background-color: #f3f3f3;
      padding: 30px 15px;
      color: #333;
    }
    .container {
      background-color: #ffffff;
      padding: 40px;
      max-width: 700px;
      margin: auto;
      border-radius: 16px;
      box-shadow: 0 6px 20px rgba(0, 0, 0, 0.05);
    }
    .logo {
      text-align: center;
      margin-bottom: 25px;
    }
    .logo img {
      height: 70px;
    }
    h2 {
      color: #2E7D32;
      font-size: 1.8rem;
      text-align: center;
      margin-bottom: 30px;
    }
    .section-title {
      font-size: 1.1rem;
      color: #4E342E;
      margin-bottom: 10px;
      font-weight: bold;
    }
    .info-box {
      background-color: #FAFAFA;
      border: 1px solid #E0E0E0;
      padding: 20px;
      border-radius: 10px;
      font-size: 0.95rem;
      margin-bottom: 20px;
    }
    .info-box p {
      margin: 10px 0;
    }
    .highlight {
      color: #2E7D32;
      font-weight: 600;
    }
    .reservation-code {
      text-align: center;
      font-size: 1.2rem;
      color: #1B5E20;
      font-weight: bold;
      margin-top: 30px;
    }
    .footer {
      margin-top: 40px;
      text-align: center;
      font-size: 0.85rem;
      color: #777;
    }
    .contact-box {
      margin-top: 40px;
      font-size: 0.95rem;
      text-align: center;
      border-top: 1px solid #ddd;
      padding-top: 30px;
      color: #444;
    }
    .contact-box p {
      margin: 6px 0;
    }
    .contact-logo {
      font-size: 1.4rem;
      color: #2E7D32;
      font-weight: bold;
    }
    .icon {
      margin-right: 6px;
    }
    .social-icons {
      margin-top: 10px;
    }
    .social-icons a {
      margin: 0 6px;
      text-decoration: none;
      font-weight: bold;
      color: #555;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="logo">
      <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQLo8t9NH1j1eo_tGo70lM2OcYKY4mhwhntvA&s" alt="Hotel Jardines de las Marías" />
    </div>
    <h2>¡Gracias por su reserva, %s!</h2>
    <div class="section-title">Resumen de la reserva</div>
    <div class="info-box">
      <p><span class="highlight">Fecha de entrada:</span> %s</p>
      <p><span class="highlight">Fecha de salida:</span> %s</p>
      <p><span class="highlight">Cantidad de personas:</span> %d</p>
      <p><span class="highlight">Cantidad de habitaciones:</span> %d</p>
    </div>
    <div class="reservation-code">Código de Reserva: %s</div>
    <div class="footer">
      Este es un mensaje automático. Si necesita asistencia, puede contactarnos:
    </div>
    <div class="contact-box">
      <div class="contact-logo">Hotel Jardines de las Marías</div>
      <p>📞 2562-8891</p>
      <p>📱 7890-5449</p>
      <p>✉️ jardindelasmariashotel@gmail.com</p>
      <p>📍 2 Avenida sur #23, Barrio el Calvario, Suchitoto</p>
      <div class="social-icons">
        <a href="https://www.facebook.com/hoteljardindelasmarias" target="_blank">Facebook</a> |
        <a href="https://www.instagram.com/hoteljardindelasmarias/" target="_blank">Instagram</a>
      </div>
    </div>
  </div>
</body>
</html>
""",
                reservation.getName(),
                reservation.getInitDate(),
                reservation.getFinishDate(),
                reservation.getCantPeople(),
                reservation.getQuantityReserved(),
                reservation.getReservationCode()
        );
    }


    @Override
    public ReservationDTO generateAndSendDte(int reservationId) {
        Reservation reservation = null;

        try {
            reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new RuntimeException("Reserva no encontrada."));

            List<ReservationRoomDTO> roomDTOs = reservationRoomRepository
                    .findByReservation_ReservationId(reservationId)
                    .stream()
                    .map(rr -> new ReservationRoomDTO(
                            rr.getRoom().getRoomId(),
                            rr.getQuantity(),
                            rr.getAssignedRoomNumber()
                    ))
                    .collect(Collectors.toList());

            // Build DTE
            DteBuilderResult result = dteBuilderService.buildDte(reservation, roomDTOs);
            DteRequestDTO dteRequest = result.getDteRequest();
            Map<String, Object> jasperParams = result.getJasperParams();

            ObjectMapper mapper = new ObjectMapper();
            String dteJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dteRequest);

            // Firmar
            String dteFirmado = dteSignerService.firmar(dteJson);

            // Token
            String token = dteAuthService.obtenerToken();

            // Transmitir
            DteResponse dteResponse = dteTransmitterService.enviarDte(dteRequest, token);

            // Guardar DTE
            Dte dteEntity = new Dte();
            dteEntity.setReservationCode(reservation.getReservationCode());
            dteEntity.setNumeroControl(dteRequest.getIdentificacion().getNumeroControl());
            dteEntity.setCodigoGeneracion(dteRequest.getIdentificacion().getCodigoGeneracion());
            dteEntity.setTipoDte(dteRequest.getIdentificacion().getTipoDte());
            dteEntity.setEstado(dteResponse.getEstado());
            dteEntity.setFechaGeneracion(java.time.LocalDateTime.now());
            dteEntity.setDteJson(dteJson);
            dteEntity.setRespuestaHaciendaJson(
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dteResponse)
            );
            dteRepository.save(dteEntity);

            // Generar PDF
            JasperReport jasperReport = JasperCompileManager.compileReport(
                    "src/main/resources/reports/DTEFactura.jrxml"
            );
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    jasperParams,
                    new JREmptyDataSource()
            );

            String pdfDirectory = "PDF";
            String pdfFileName = "DTEFactura_" + reservationId + ".pdf";

            File folder = new File(pdfDirectory);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String pdfPath = pdfDirectory + File.separator + pdfFileName;
            JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);

            // Preparar adjuntos
            byte[] pdfBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(pdfPath));
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

            String base64Json = Base64.getEncoder()
                    .encodeToString(dteJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            Map<String, String> attachments = new HashMap<>();
            attachments.put(pdfFileName, base64Pdf);
            attachments.put("DTEFactura_" + reservationId + ".json", base64Json);

            // Enviar correo
            String htmlBody = buildReservationEmailBody(reservation);
            emailSenderService.sendMailWithMultipleAttachments(
                    reservation.getEmail(),
                    "Confirmación de Reserva - Hotel Jardines de las Marías",
                    htmlBody,
                    attachments
            );

            System.out.println("✅ DTE generado y correo enviado con PDF y JSON adjuntos.");

        } catch (Exception e) {
            System.out.println("❌ Error generando o enviando DTE:");
            e.printStackTrace();
        }

        // Asegúrate de que reservation no sea null antes de construir el DTO
        if (reservation == null) return null;

        ReservationDTO dto = new ReservationDTO();
        dto.setReservationCode(reservation.getReservationCode());
        dto.setName(reservation.getName());
        dto.setEmail(reservation.getEmail());
        dto.setPhone(reservation.getPhone());
        dto.setInitDate(reservation.getInitDate());
        dto.setFinishDate(reservation.getFinishDate());
        dto.setCantPeople(reservation.getCantPeople());
        dto.setStatus(reservation.getStatus());

        List<ReservationRoomDTO> roomDTOs = reservationRoomRepository
                .findByReservation_ReservationId(reservation.getReservationId())
                .stream()
                .map(rr -> new ReservationRoomDTO(
                        rr.getRoom().getRoomId(),
                        rr.getQuantity(),
                        rr.getAssignedRoomNumber()
                ))
                .collect(Collectors.toList());

        dto.setRooms(roomDTOs);

        return dto;
    }



    @Override
    public Reservation findEntityById(Integer id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorType.ENTITY_NOT_FOUND, "Reserva no encontrada"));
    }


    @Override
    public ReservationDTO toDto(Reservation reservation) {
        ReservationDTO dto = new ReservationDTO();
        dto.setReservationCode(reservation.getReservationCode());
        dto.setInitDate(reservation.getInitDate());
        dto.setFinishDate(reservation.getFinishDate());
        dto.setCantPeople(reservation.getCantPeople());
        dto.setName(reservation.getName());
        dto.setEmail(reservation.getEmail());
        dto.setPhone(reservation.getPhone());
        dto.setStatus(reservation.getStatus());
        dto.setPayment((float) reservation.getPayment());

        // Puedes omitir wantsDte si ya no lo usas por reserva

        dto.setRooms(
                reservationRoomRepository
                        .findByReservation_ReservationId(reservation.getReservationId())
                        .stream()
                        .map(rr -> new ReservationRoomDTO(
                                rr.getRoom().getRoomId(),
                                rr.getQuantity(),
                                rr.getAssignedRoomNumber()
                        ))
                        .collect(Collectors.toList())
        );

        return dto;
    }


    @Override
    public String buildReservationEmailBody(ReservationDTO dto) {
        return String.format("""
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <style>
    body {
      font-family: 'Helvetica Neue', 'Segoe UI', sans-serif;
      background-color: #f9f9f9;
      margin: 0;
      padding: 40px 20px;
      color: #333333;
    }
    .container {
      max-width: 650px;
      background-color: #ffffff;
      margin: auto;
      padding: 40px;
      border-radius: 12px;
      box-shadow: 0 4px 15px rgba(0,0,0,0.1);
    }
    .logo {
      text-align: center;
      margin-bottom: 30px;
    }
    .logo img {
      height: 60px;
    }
    h2 {
      font-size: 1.6rem;
      color: #2E7D32;
      text-align: center;
      margin-bottom: 30px;
      font-weight: 600;
    }
    .info-section {
      border-top: 1px solid #eee;
      border-bottom: 1px solid #eee;
      padding: 20px 0;
      margin-bottom: 30px;
      font-size: 0.95rem;
    }
    .info-row {
      margin: 10px 0;
    }
    .label {
      font-weight: 600;
      color: #555;
      display: inline-block;
      width: 160px;
    }
    .reservation-code {
      text-align: center;
      font-size: 1.2rem;
      color: #2E7D32;
      font-weight: 600;
      margin-top: 20px;
    }
    .footer {
      text-align: center;
      font-size: 0.85rem;
      color: #777;
      margin-top: 40px;
    }
    .contact {
      margin-top: 10px;
      color: #555;
      line-height: 1.6;
    }
    .social-icons {
      margin-top: 15px;
    }
    .social-icons a {
      margin: 0 8px;
      text-decoration: none;
      color: #2E7D32;
      font-weight: 600;
      font-size: 0.9rem;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="logo">
      <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQLo8t9NH1j1eo_tGo70lM2OcYKY4mhwhntvA&s" alt="Hotel Jardines de las Marías" />
    </div>
    <h2>¡Gracias por su reserva, %s!</h2>
    <div class="info-section">
      <div class="info-row"><span class="label">Fecha de entrada:</span> %s</div>
      <div class="info-row"><span class="label">Fecha de salida:</span> %s</div>
      <div class="info-row"><span class="label">Cantidad de personas:</span> %d</div>
      <div class="info-row"><span class="label">Cantidad de habitaciones:</span> %d</div>
    </div>
    <div class="reservation-code">Código de Reserva: %s</div>
    <div class="footer">
      Este es un mensaje automático. Para más información o asistencia:
      <div class="contact">
        Hotel Jardines de las Marías<br/>
        📞 2562-8891 | 📱 7890-5449<br/>
        ✉️ jardindelasmariashotel@gmail.com<br/>
        📍 2 Avenida sur #23, Barrio el Calvario, Suchitoto
      </div>
      <div class="social-icons">
        <a href="https://www.facebook.com/hoteljardindelasmarias" target="_blank">Facebook</a>
        <a href="https://www.instagram.com/hoteljardindelasmarias/" target="_blank">Instagram</a>
      </div>
    </div>
  </div>
</body>
</html>
""",
                dto.getName(),
                dto.getInitDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                dto.getFinishDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                dto.getCantPeople(),
                dto.getRooms().size(),
                dto.getReservationCode()
        );
    }






}
