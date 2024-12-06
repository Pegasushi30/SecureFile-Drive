package com.example.securedrive.security;

import java.util.ArrayList;
import java.util.List;

public class DeltaUtil {

    /**
     * İki metin arasındaki delta'yı hesaplar.
     * @param oldData Eski veri
     * @param newData Yeni veri
     * @return Delta çıktısı
     */
    public static String calculateDelta(String oldData, String newData) {
        StringBuilder delta = new StringBuilder();
        String[] oldLines = oldData.split("\n");
        String[] newLines = newData.split("\n");

        int oldIndex = 0, newIndex = 0;

        while (oldIndex < oldLines.length || newIndex < newLines.length) {
            if (oldIndex >= oldLines.length) {
                delta.append("ADD:").append(newLines[newIndex]).append("\n");
                newIndex++;
            } else if (newIndex >= newLines.length) {
                delta.append("REMOVE:").append(oldLines[oldIndex]).append("\n");
                oldIndex++;
            } else if (!oldLines[oldIndex].equals(newLines[newIndex])) {
                delta.append("REMOVE:").append(oldLines[oldIndex]).append("\n");
                delta.append("ADD:").append(newLines[newIndex]).append("\n");
                oldIndex++;
                newIndex++;
            } else {
                delta.append("UNCHANGED:").append(oldLines[oldIndex]).append("\n");
                oldIndex++;
                newIndex++;
            }
        }

        return delta.toString();
    }

    /**
     * Delta uygulayarak yeni veriyi oluşturur.
     * @param oldData Eski veri
     * @param delta Delta verisi
     * @return Yeni veri
     */
    public static String applyDelta(String oldData, String delta) {
        String[] oldLines = oldData.split("\n");
        String[] deltaLines = delta.split("\n");

        List<String> resultLines = new ArrayList<>();
        int oldIndex = 0;

        for (String deltaLine : deltaLines) {
            if (deltaLine.startsWith("ADD:")) {
                resultLines.add(deltaLine.substring(4));
            } else if (deltaLine.startsWith("REMOVE:")) {
                oldIndex++;
            } else if (deltaLine.startsWith("UNCHANGED:")) {
                resultLines.add(oldLines[oldIndex]);
                oldIndex++;
            }
        }

        while (oldIndex < oldLines.length) {
            resultLines.add(oldLines[oldIndex]);
            oldIndex++;
        }

        return String.join("\n", resultLines);
    }

    /**
     * Delta doğrulaması için bir hash üretir.
     * @param delta Delta içeriği
     * @return Hash değeri
     */
    public static String calculateDeltaHash(String delta) {
        return Integer.toHexString(delta.hashCode());
    }
}
