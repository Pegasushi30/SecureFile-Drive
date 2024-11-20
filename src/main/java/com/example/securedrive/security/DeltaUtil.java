package com.example.securedrive.security;

import java.util.ArrayList;
import java.util.List;

public class DeltaUtil {

    public static String calculateDelta(String oldData, String newData) {
        StringBuilder delta = new StringBuilder();
        String[] oldLines = oldData.split("\n");
        String[] newLines = newData.split("\n");

        int oldIndex = 0, newIndex = 0;

        while (oldIndex < oldLines.length || newIndex < newLines.length) {
            if (oldIndex >= oldLines.length) {
                delta.append("+").append(newLines[newIndex]).append("\n");
                newIndex++;
            } else if (newIndex >= newLines.length) {
                delta.append("-").append(oldLines[oldIndex]).append("\n");
                oldIndex++;
            } else if (!oldLines[oldIndex].equals(newLines[newIndex])) {
                delta.append("-").append(oldLines[oldIndex]).append("\n");
                delta.append("+").append(newLines[newIndex]).append("\n");
                oldIndex++;
                newIndex++;
            } else {
                delta.append(" ").append(oldLines[oldIndex]).append("\n");
                oldIndex++;
                newIndex++;
            }
        }

        return delta.toString();
    }

    public static String applyDelta(String oldData, String delta) {
        StringBuilder newData = new StringBuilder();
        String[] oldLines = oldData.split("\n");
        String[] deltaLines = delta.split("\n");

        List<String> resultLines = new ArrayList<>();
        int oldIndex = 0;

        for (String deltaLine : deltaLines) {
            if (deltaLine.startsWith("+")) {
                resultLines.add(deltaLine.substring(1));
            } else if (deltaLine.startsWith("-")) {
                oldIndex++;
            } else if (deltaLine.startsWith(" ")) {
                resultLines.add(oldLines[oldIndex]);
                oldIndex++;
            }
        }

        while (oldIndex < oldLines.length) {
            resultLines.add(oldLines[oldIndex]);
            oldIndex++;
        }

        for (String line : resultLines) {
            newData.append(line).append("\n");
        }

        return newData.toString().trim();
    }
}
