package com.example.securedrive.security;

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
                // Yeni satır ekleniyor
                delta.add("A:" + lineNumber + ":" + newLines[newIndex]);
                newIndex++;
                lineNumber++;
            } else if (newIndex >= newLines.length) {
                // Eski satır kaldırılıyor
                delta.add("R:" + lineNumber + ":" + oldLines[oldIndex]);
                oldIndex++;
                // lineNumber aynı kalır çünkü bir satır silindi
            } else if (!oldLines[oldIndex].equals(newLines[newIndex])) {
                // Satırlar farklı, önce sil, sonra ekle
                delta.add("R:" + lineNumber + ":" + oldLines[oldIndex]);
                delta.add("A:" + lineNumber + ":" + newLines[newIndex]);
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

        List<String> resultLines = new ArrayList<>(List.of(oldLines));

        int currentIndex = 0; // Şu anki işlem yapılacak satır indeksi

        for (String deltaLine : deltaLines) {
            if (deltaLine.isEmpty()) {
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
                case "R": // Satır Silme
                    if (currentIndex != lineNumber) {
                        currentIndex = lineNumber; // Satır numarasına git
                    }
                    if (currentIndex >= resultLines.size()) {
                        throw new IllegalStateException("Silinmeye çalışılan satır numarası geçersiz: " + currentIndex);
                    }
                    String removedLine = resultLines.remove(currentIndex);
                    if (!removedLine.equals(content)) {
                        throw new IllegalStateException("Delta ve veri uyumsuz: Silinmesi gereken: "
                                + content + ", Silinen: " + removedLine);
                    }
                    break;

                case "A": // Satır Ekleme
                    if (currentIndex != lineNumber) {
                        currentIndex = lineNumber; // Satır numarasına git
                    }
                    resultLines.add(currentIndex, content);
                    currentIndex++; // Eklenen satır sonrası indeksi artır
                    break;

                default:
                    throw new IllegalArgumentException("Bilinmeyen delta komutu: " + command);
            }
        }

        // Tüm satır sonu karakterlerini normalize ederek geri dön
        return String.join("\n", resultLines);
    }


}
