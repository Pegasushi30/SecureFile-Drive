package com.example.securedrive.service.util;

import java.util.ArrayList;
import java.util.List;


public class DeltaUtil {


    private static String[] splitLines(String data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        String normalized = data.replaceAll("\\r?\\n", "\n").trim();
        return normalized.split("\n");
    }



    public static String calculateDelta(String oldData, String newData) {
        String[] oldLines = splitLines(oldData);
        String[] newLines = splitLines(newData);

        List<String> delta = new ArrayList<>();
        int oldIndex = 0, newIndex = 0;
        int lineNumber = 0;

        while (oldIndex < oldLines.length || newIndex < newLines.length) {
            if (oldIndex >= oldLines.length) {
                delta.add("A:" + lineNumber + ":" + newLines[newIndex]);
                newIndex++;
                lineNumber++;
            } else if (newIndex >= newLines.length) {
                delta.add("R:" + lineNumber + ":" + oldLines[oldIndex]);
                oldIndex++;
            } else if (!oldLines[oldIndex].equals(newLines[newIndex])) {
                delta.add("A:" + lineNumber + ":" + newLines[newIndex]);
                delta.add("R:" + lineNumber + ":" + oldLines[oldIndex]);
                oldIndex++;
                newIndex++;
                lineNumber++;
            } else {
                oldIndex++;
                newIndex++;
                lineNumber++;
            }
        }
        return String.join("\n", delta);
    }


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
                continue;
            }

            String[] parts = deltaLine.split(":", 3);
            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid delta format " + deltaLine);
            }

            String command = parts[0];
            int lineNumber = Integer.parseInt(parts[1]);
            String content = parts[2];

            switch (command) {
                case "A": {
                    while (resultLines.size() < lineNumber && oldIndex < oldLines.length) {
                        resultLines.add(oldLines[oldIndex++]);
                    }
                    resultLines.add(content);
                    break;
                }
                case "R": {
                    while (resultLines.size() < lineNumber && oldIndex < oldLines.length) {
                        resultLines.add(oldLines[oldIndex++]);
                    }
                    if (oldIndex >= oldLines.length) {
                        throw new IllegalStateException("The line number being attempted to delete is invalid: " + lineNumber);
                    }
                    String removedLine = oldLines[oldIndex++];
                    if (!removedLine.equals(content)) {
                        throw new IllegalStateException("Delta and data mismatch: "
                                + "To be deleted: " + content + ", Deleted: " + removedLine);
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown delta command: " + command);
            }
            deltaIndex++;
        }
        while (oldIndex < oldLines.length) {
            resultLines.add(oldLines[oldIndex++]);
        }

        return String.join("\n", resultLines);
    }




}
