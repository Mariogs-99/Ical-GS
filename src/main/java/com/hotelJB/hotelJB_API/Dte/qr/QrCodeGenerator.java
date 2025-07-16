package com.hotelJB.hotelJB_API.Dte.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class QrCodeGenerator {

    /**
     * Genera un QR en base64 a partir de la URL indicada.
     * @param url url a codificar (ej. URL de consulta DTE)
     * @param width ancho del QR
     * @param height alto del QR
     * @return string Base64 PNG image
     */
    public static String generateQrBase64(String url, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height);

            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int color = (bitMatrix.get(x, y)) ? 0xFF000000 : 0xFFFFFFFF;
                    bufferedImage.setRGB(x, y, color);
                }
            }

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "PNG", pngOutputStream);

            byte[] pngData = pngOutputStream.toByteArray();
            return Base64.getEncoder().encodeToString(pngData);

        } catch (WriterException | java.io.IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
