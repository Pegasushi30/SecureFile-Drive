package com.example.securedrive;

import com.example.securedrive.service.util.BinaryDeltaUtil;
import com.example.securedrive.service.util.BinaryDeltaUtil.DeltaCommand;
import com.example.securedrive.service.util.DeltaUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class SecuredriveApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecuredriveApplication.class, args);

		try {
			// =======================
			// 2. İkili Delta Hesaplama (v1 -> v2)
			// =======================
			byte[] originalBinary = BinaryDeltaUtil.loadBinaryFile("src/main/resources/static/images/OriginalImages/SecureFile Drive_Introduction.pdf");
			byte[] modifiedBinary = BinaryDeltaUtil.loadBinaryFile("src/main/resources/static/images/ModifiedImages/SecureFile Drive_Introduction.pdf");

			System.out.println("Calculating binary delta (v1 -> v2)...");
			List<DeltaCommand> binaryDeltaCommands = BinaryDeltaUtil.calculateDelta(originalBinary, modifiedBinary);
			System.out.println("Binary delta computed. Command count: " + binaryDeltaCommands.size());
			String binaryDeltaPath = "src/main/resources/static/deltas/binary_delta_v1_v2.json";
			BinaryDeltaUtil.saveBinaryDeltaCommands(binaryDeltaCommands, binaryDeltaPath);
			System.out.println("Binary delta saved to " + binaryDeltaPath);

			// =======================
			// 3. İkili Delta Uygulama ve Doğrulama (v1 -> v2)
			// =======================
			byte[] reconstructedV2 = BinaryDeltaUtil.applyDelta(originalBinary, binaryDeltaCommands);
			boolean matchV2 = java.util.Arrays.equals(modifiedBinary, reconstructedV2);
			System.out.println("Reconstructed V2 matches modified? " + matchV2);

			if (matchV2) {
				String reconstructedV2Path = "src/main/resources/static/images/ReconstructedImages/reconstructed_SecureFile Drive_Introduction_v2.pdf";
				BinaryDeltaUtil.saveBinaryFile(reconstructedV2, reconstructedV2Path);
				System.out.println("Reconstructed V2 file saved to " + reconstructedV2Path);
			} else {
				System.err.println("Reconstruction FAILED for V2!");
				return; // Hatalıysa devam etmeyelim
			}

			// =======================
			// 4. Başka bir versiyon dosyası (v3) için Delta Hesaplama
			// =======================
			byte[] version3Binary =  BinaryDeltaUtil.loadBinaryFile("src/main/resources/static/images/ModifiedImages2/SecureFile Drive_Introduction.pdf");

			System.out.println("Calculating binary delta (v2 -> v3)...");
			List<DeltaCommand> binaryDeltaV2ToV3 = BinaryDeltaUtil.calculateDelta(reconstructedV2, version3Binary);
			System.out.println("Binary delta (v2 -> v3) computed. Command count: " + binaryDeltaV2ToV3.size());
			String binaryDeltaV2V3Path = "src/main/resources/static/deltas/binary_delta_v2_v3.json";
			BinaryDeltaUtil.saveBinaryDeltaCommands(binaryDeltaV2ToV3, binaryDeltaV2V3Path);
			System.out.println("Binary delta (v2 -> v3) saved to " + binaryDeltaV2V3Path);

			// =======================
			// 5. İkili Delta Uygulama ve Doğrulama (v2 -> v3)
			// =======================
			byte[] reconstructedV3 = BinaryDeltaUtil.applyDelta(reconstructedV2, binaryDeltaV2ToV3);
			boolean matchV3 = java.util.Arrays.equals(version3Binary, reconstructedV3);
			System.out.println("Reconstructed V3 matches version3? " + matchV3);

			if (matchV3) {
				String reconstructedV3Path = "src/main/resources/static/images/ReconstructedImages/reconstructed_SecureFile Drive_Introduction_v3.pdf";
				BinaryDeltaUtil.saveBinaryFile(reconstructedV3, reconstructedV3Path);
				System.out.println("Reconstructed V3 file saved to " + reconstructedV3Path);
			} else {
				System.err.println("Reconstruction FAILED for V3!");
			}

		} catch (IOException e) {
			System.err.println("File I/O or JSON processing error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
