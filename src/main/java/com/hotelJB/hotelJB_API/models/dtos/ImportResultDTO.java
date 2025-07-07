package com.hotelJB.hotelJB_API.models.dtos;

import java.util.ArrayList;
import java.util.List;

public class ImportResultDTO {

    public static class ImportedReservationInfo {
        public String uid;
        public String roomName;
        public String dates;
    }

    public static class RejectedReservationInfo {
        public String uid;
        public String roomName;
        public String reason;
    }

    private List<ImportedReservationInfo> importedReservations = new ArrayList<>();
    private List<RejectedReservationInfo> rejectedReservations = new ArrayList<>();

    public void addImported(String uid, String roomName, String dates) {
        ImportedReservationInfo info = new ImportedReservationInfo();
        info.uid = uid;
        info.roomName = roomName;
        info.dates = dates;
        importedReservations.add(info);
    }

    public void addRejected(String uid, String roomName, String reason) {
        RejectedReservationInfo info = new RejectedReservationInfo();
        info.uid = uid;
        info.roomName = roomName;
        info.reason = reason;
        rejectedReservations.add(info);
    }

    public List<ImportedReservationInfo> getImportedReservations() {
        return importedReservations;
    }

    public List<RejectedReservationInfo> getRejectedReservations() {
        return rejectedReservations;
    }
}
