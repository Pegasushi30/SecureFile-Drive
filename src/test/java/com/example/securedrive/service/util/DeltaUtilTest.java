package com.example.securedrive.service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeltaUtilTest {

    @Test
    void testCalculateDelta_AddLines() {
        // Given
        String oldData = "Line1\nLine2";
        String newData = "Line1\nLine2\nLine3";

        // When
        String delta = DeltaUtil.calculateDelta(oldData, newData);

        // Then
        String expectedDelta = "A:2:Line3";
        assertEquals(expectedDelta, delta);
    }

    @Test
    void testCalculateDelta_RemoveLines() {
        // Given
        String oldData = "Line1\nLine2\nLine3";
        String newData = "Line1\nLine2";

        // When
        String delta = DeltaUtil.calculateDelta(oldData, newData);

        // Then
        String expectedDelta = "R:2:Line3";
        assertEquals(expectedDelta, delta);
    }

    @Test
    void testCalculateDelta_ChangeLines() {
        // Given
        String oldData = "Line1\nLine2\nLine3";
        String newData = "Line1\nLineX\nLine3";

        // When
        String delta = DeltaUtil.calculateDelta(oldData, newData);

        // Then
        // Artık "Önce ekle, sonra sil" (A -> R) yaklaşımını bekliyoruz:
        String expectedDelta = "A:1:LineX\nR:1:Line2";
        assertEquals(expectedDelta, delta);
    }


    @Test
    void testCalculateDelta_ComplexScenario() {
        // Given
        String oldData = "Line1\nLine2\nLine3\nLine4";
        String newData = "Line0\nLine1\nLineX\nLine4\nLine5";

        // When
        String delta = DeltaUtil.calculateDelta(oldData, newData);

        // Debugging: Log the generated delta
        System.out.println("Generated Delta:\n" + delta);

        // Expected Delta
        String expectedDelta = "A:0:Line0\nR:0:Line1\nA:1:Line1\nR:1:Line2\nA:2:LineX\nR:2:Line3\nA:4:Line5";

        // Then
        assertEquals(expectedDelta, delta, "The generated delta does not match the expected delta.");
    }




    @Test
    void testApplyDelta_AddLines() {
        // Given
        String oldData = "Line1\nLine2";
        String delta = "A:2:Line3";

        // When
        String newData = DeltaUtil.applyDelta(oldData, delta);

        // Then
        String expectedNewData = "Line1\nLine2\nLine3";
        assertEquals(expectedNewData, newData);
    }

    @Test
    void testApplyDelta_RemoveLines() {
        // Given
        String oldData = "Line1\nLine2\nLine3";
        String delta = "R:2:Line3";

        // When
        String newData = DeltaUtil.applyDelta(oldData, delta);

        // Then
        String expectedNewData = "Line1\nLine2";
        assertEquals(expectedNewData, newData);
    }

    @Test
    void testApplyDelta_ChangeLines() {
        // Given
        String oldData = "Line1\nLine2\nLine3";
        String delta = "R:1:Line2\nA:1:LineX";

        // When
        String newData = DeltaUtil.applyDelta(oldData, delta);

        // Then
        String expectedNewData = "Line1\nLineX\nLine3";
        assertEquals(expectedNewData, newData);
    }

    @Test
    void testApplyDelta_ComplexScenario() {
        // Given
        String oldData = "Line1\nLine2\nLine3\nLine4";
        String delta = "A:0:Line0\nR:2:Line2\nA:2:LineX\nR:3:Line3\nA:4:Line5";

        // When
        String newData = DeltaUtil.applyDelta(oldData, delta);

        // Then
        String expectedNewData = "Line0\nLine1\nLineX\nLine4\nLine5";
        assertEquals(expectedNewData, newData);
    }

    @Test
    void testApplyDelta_InvalidDelta() {
        // Given
        String oldData = "Line1\nLine2\nLine3";
        String delta = "R:5:LineX"; // Invalid line number

        // Expect an exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                DeltaUtil.applyDelta(oldData, delta));

        assertTrue(exception.getMessage().contains("Silinmeye çalışılan satır numarası geçersiz"));
    }

    @Test
    void testApplyDelta_InvalidFormat() {
        // Given
        String oldData = "Line1\nLine2\nLine3";
        String delta = "INVALID:1:LineX"; // Invalid command

        // Expect an exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                DeltaUtil.applyDelta(oldData, delta));

        assertTrue(exception.getMessage().contains("Bilinmeyen delta komutu"));
    }
}

