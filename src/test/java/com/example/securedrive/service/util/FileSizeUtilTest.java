package com.example.securedrive.service.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FileSizeUtilTest {

    @Test
    void testFormatSize() {
        // Test for bytes
        assertEquals("500 B", FileSizeUtil.formatSize(500), "Bytes formatting failed");

        // Test for kilobytes
        assertEquals("1.00 KB", FileSizeUtil.formatSize(1024), "Kilobytes formatting failed");
        assertEquals("1.95 KB", FileSizeUtil.formatSize(2000), "Kilobytes formatting failed");

        // Test for megabytes
        assertEquals("1.00 MB", FileSizeUtil.formatSize(1024 * 1024), "Megabytes formatting failed");
        assertEquals("2.34 MB", FileSizeUtil.formatSize(2457600), "Megabytes formatting failed");

        // Test for gigabytes
        assertEquals("1.00 GB", FileSizeUtil.formatSize(1024L * 1024 * 1024), "Gigabytes formatting failed");
        assertEquals("1.16 GB", FileSizeUtil.formatSize(1250000000), "Gigabytes formatting failed");


        // Edge cases
        assertEquals("0 B", FileSizeUtil.formatSize(0), "Zero bytes formatting failed");
        assertEquals("1023 B", FileSizeUtil.formatSize(1023), "Edge case for bytes failed");
        assertEquals("1023.00 KB", FileSizeUtil.formatSize(1023L * 1024), "Edge case for kilobytes failed");
    }
}
