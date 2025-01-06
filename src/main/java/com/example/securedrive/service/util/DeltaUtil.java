package com.example.securedrive.service.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Delta hesaplama ve uygulama işlemleri için yardımcı sınıf.
 */
public class DeltaUtil {

    /**
     * Veriyi normalize eder ve satırlara böler.
     *
     * @param data Metin
     * @return Satır dizisi
     */
    private static String[] splitLines(String data) {
        if (data == null) {
            throw new IllegalArgumentException("Veri null olamaz.");
        }
        // Tüm satır sonu karakterlerini normalize et ve baş/sondaki boşlukları temizle
        String normalized = data.replaceAll("\\r?\\n", "\n").trim();
        return normalized.split("\n");
    }


    /**
     * İki metin arasındaki delta'yı hesaplar.
     *
     * @param oldData Eski veri
     * @param newData Yeni veri
     * @return Delta çıktısı
     */
    public static String calculateDelta(String oldData, String newData) {
        String[] oldLines = splitLines(oldData);
        String[] newLines = splitLines(newData);

        List<String> delta = new ArrayList<>();
        int oldIndex = 0, newIndex = 0;
        int lineNumber = 0;

        while (oldIndex < oldLines.length || newIndex < newLines.length) {
            if (oldIndex >= oldLines.length) {
                // Eski veri bitti, geriye kalan tüm newLines ekleniyor
                delta.add("A:" + lineNumber + ":" + newLines[newIndex]);
                newIndex++;
                lineNumber++;
            } else if (newIndex >= newLines.length) {
                // Yeni veri bitti, geriye kalan tüm oldLines siliniyor
                delta.add("R:" + lineNumber + ":" + oldLines[oldIndex]);
                oldIndex++;
                // lineNumber aynı kalır (silme işleminde)
            } else if (!oldLines[oldIndex].equals(newLines[newIndex])) {
                // Satırlar farklı, önce ekle, sonra sil
                delta.add("A:" + lineNumber + ":" + newLines[newIndex]);
                delta.add("R:" + lineNumber + ":" + oldLines[oldIndex]);
                oldIndex++;
                newIndex++;
                lineNumber++;
            } else {
                // Satırlar aynı, ilerle
                oldIndex++;
                newIndex++;
                lineNumber++;
            }
        }
        return String.join("\n", delta);
    }

    /**
     * Delta uygulayarak yeni veriyi oluşturur.
     *
     * @param oldData Eski veri
     * @param delta   Delta verisi
     * @return Yeni veri
     */
    public static String applyDelta(String oldData, String delta) {
        String[] oldLines = splitLines(oldData);
        String[] deltaLines = splitLines(delta);

        List<String> resultLines = new ArrayList<>();
        int oldIndex = 0;
        int deltaIndex = 0;

        while (deltaIndex < deltaLines.length) {
            String deltaLine = deltaLines[deltaIndex];
            if (deltaLine.isBlank()) {
                deltaIndex++;
                continue; // Boş satırları atla
            }

            String[] parts = deltaLine.split(":", 3);
            if (parts.length < 3) {
                throw new IllegalArgumentException("Geçersiz delta formatı: " + deltaLine);
            }

            String command = parts[0];
            int lineNumber = Integer.parseInt(parts[1]);
            String content = parts[2];

            switch (command) {
                case "A": { // Satır Ekleme
                    // Eklenecek konuma kadar eski satırları ekle
                    while (resultLines.size() < lineNumber && oldIndex < oldLines.length) {
                        resultLines.add(oldLines[oldIndex++]);
                    }
                    // Yeni satırı ekle
                    resultLines.add(content);
                    break;
                }
                case "R": { // Satır Silme
                    // Silinecek konuma kadar eski satırları ekle
                    while (resultLines.size() < lineNumber && oldIndex < oldLines.length) {
                        resultLines.add(oldLines[oldIndex++]);
                    }
                    // Eski satırı atla (sil)
                    if (oldIndex >= oldLines.length) {
                        throw new IllegalStateException("Silinmeye çalışılan satır numarası geçersiz: " + lineNumber);
                    }
                    String removedLine = oldLines[oldIndex++];
                    if (!removedLine.equals(content)) {
                        throw new IllegalStateException("Delta ve veri uyumsuz: "
                                + "Silinmesi gereken: " + content + ", Silinen: " + removedLine);
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("Bilinmeyen delta komutu: " + command);
            }
            deltaIndex++;
        }

        // Geriye kalan eski satırları ekle
        while (oldIndex < oldLines.length) {
            resultLines.add(oldLines[oldIndex++]);
        }

        return String.join("\n", resultLines);
    }




}
