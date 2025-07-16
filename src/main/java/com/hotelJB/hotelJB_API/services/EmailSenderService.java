package com.hotelJB.hotelJB_API.services;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;

@Service
public class EmailSenderService {

    @Value("${mailjet.api.key}")
    private String apiKey;

    @Value("${mailjet.api.secret}")
    private String apiSecret;

    @Value("${mailjet.sender.email}")
    private String senderEmail;

    @Value("${mailjet.sender.name}")
    private String senderName;

    public void sendMail(String to, String subject, String htmlBody) {
        sendMailWithMultipleAttachments(to, subject, htmlBody, null);
    }

    public void sendMailWithMultipleAttachments(
            String to,
            String subject,
            String htmlBody,
            Map<String, String> attachmentsBase64
    ) {
        try {
            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/json");

            String escapedHtmlBody = htmlBody
                    .replace("\"", "\\\"")
                    .replace("\n", "")
                    .replace("\r", "");

            String attachmentPart = "";
            if (attachmentsBase64 != null && !attachmentsBase64.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append(",\"Attachments\":[");

                int i = 0;
                for (Map.Entry<String, String> entry : attachmentsBase64.entrySet()) {
                    String filename = entry.getKey();
                    String content = entry.getValue();

                    // Detect content-type by file extension
                    String contentType = "application/octet-stream";
                    if (filename.endsWith(".pdf")) {
                        contentType = "application/pdf";
                    } else if (filename.endsWith(".json")) {
                        contentType = "application/json";
                    }

                    sb.append(String.format("""
                        {
                          "ContentType": "%s",
                          "Filename": "%s",
                          "Base64Content": "%s"
                        }
                        """, contentType, filename, content));

                    if (++i < attachmentsBase64.size()) {
                        sb.append(",");
                    }
                }
                sb.append("]");
                attachmentPart = sb.toString();
            }

            String jsonBody = String.format("""
                {
                  "Messages": [
                    {
                      "From": {
                        "Email": "%s",
                        "Name": "%s"
                      },
                      "To": [
                        {
                          "Email": "%s",
                          "Name": "%s"
                        }
                      ],
                      "Subject": "%s",
                      "TextPart": "%s",
                      "HTMLPart": "%s"
                      %s
                    }
                  ]
                }
                """,
                    senderEmail,
                    senderName,
                    to,
                    "Destinatario",
                    subject,
                    "Este es un mensaje alternativo en texto plano.",
                    escapedHtmlBody,
                    attachmentPart
            );

            RequestBody body = RequestBody.create(mediaType, jsonBody);

            String credentials = apiKey + ":" + apiSecret;
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

            Request request = new Request.Builder()
                    .url("https://api.mailjet.com/v3.1/send")
                    .post(body)
                    .addHeader("Authorization", basicAuth)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();

                if (!response.isSuccessful()) {
                    System.out.println("❌ Error al enviar correo Mailjet:");
                    System.out.println("HTTP Status: " + response.code());
                    System.out.println("Response Body: " + responseBody);
                    throw new RuntimeException("Mailjet API error: " + responseBody);
                }

                System.out.println("✅ Correo enviado correctamente vía Mailjet a: " + to);
                System.out.println("Response Body: " + responseBody);
            }
        } catch (Exception e) {
            System.out.println("❌ Excepción enviando correo Mailjet:");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
