package com.hotelJB.hotelJB_API.twilio;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppService {

    @Value("${twilio.whatsappNumber}") // el número de Twilio sandbox, por ejemplo +14155238886
    private String fromNumber;

    public void sendWhatsAppMessage(String to, String messageBody) {
        // to debe venir en formato internacional, por ejemplo +50370123456
        if (!to.startsWith("+")) {
            throw new IllegalArgumentException("El número debe comenzar con + e incluir el código de país.");
        }

        Message message = Message.creator(
                new PhoneNumber("whatsapp:" + to),
                new PhoneNumber("whatsapp:" + fromNumber),
                messageBody
        ).create();

        System.out.println("WhatsApp enviado: SID = " + message.getSid());
    }
}
