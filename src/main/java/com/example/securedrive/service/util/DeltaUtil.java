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
        List<String> resultLines = new ArrayList<>(List.of(oldLines));

        String[] deltaLines = splitLines(delta);

        String previousCommand = null;
        int previousLineNumber = -1;

        for (String deltaLine : deltaLines) {
            if (deltaLine.isBlank()) {
                continue; // Boş satırları atla
            }

            // "KOMUT:SATIR_NO:ICERIK"
            String[] parts = deltaLine.split(":", 3);
            if (parts.length < 3) {
                throw new IllegalArgumentException("Geçersiz delta formatı: " + deltaLine);
            }

            String command = parts[0];            // "A" ya da "R"
            int lineNumber = Integer.parseInt(parts[1]);
            String content = parts[2];

            switch (command) {
                case "A": { // Satır Ekleme
                    // Eklenecek konum valid mi?
                    if (lineNumber > resultLines.size()) {
                        throw new IllegalStateException("Eklenecek satır numarası geçersiz: " + lineNumber);
                    }

                    // Belirtilen konuma yeni satır ekle
                    resultLines.add(lineNumber, content);

                    // Bir sonraki komut "R" olursa, bunun "A" komutundan sonra geldiğini bilmemiz lazım
                    previousCommand = "A";
                    previousLineNumber = lineNumber;
                    break;
                }
                case "R": { // Satır Silme
                    // Normalde "R" lineNumber'dan siler.
                    // Ancak bir önceki komut "A" VE aynı lineNumber ise,
                    // eski satır kaydığı için lineNumber+1'den silmemiz lazım.
                    int removeIndex = lineNumber;
                    if ("A".equals(previousCommand) && lineNumber == previousLineNumber) {
                        removeIndex = lineNumber + 1;
                    }

                    if (removeIndex >= resultLines.size()) {
                        throw new IllegalStateException("Silinmeye çalışılan satır numarası geçersiz: " + removeIndex);
                    }

                    // Sil ve içerik kontrolü yap
                    String removedLine = resultLines.remove(removeIndex);
                    if (!removedLine.equals(content)) {
                        throw new IllegalStateException("Delta ve veri uyumsuz: "
                                + "Silinmesi gereken: " + content + ", Silinen: " + removedLine);
                    }

                    // Komuttan sonra "previousCommand" güncelle
                    previousCommand = "R";
                    previousLineNumber = lineNumber;
                    break;
                }
                default:
                    throw new IllegalArgumentException("Bilinmeyen delta komutu: " + command);
            }
        }

        return String.join("\n", resultLines);
    }



}
