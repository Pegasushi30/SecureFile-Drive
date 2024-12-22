package com.example.securedrive;

import com.example.securedrive.util.BinaryDeltaUtil;
import com.example.securedrive.util.BinaryDeltaUtil.DeltaCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
public class SecuredriveApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecuredriveApplication.class, args);

		try {
			// 1) Original ve Modified dosyalarını yükle
			byte[] original = loadFile("src/main/resources/static/images/OriginalImages/SecureFile Drive_Introduction.pdf");
			byte[] modified = loadFile("src/main/resources/static/images/ModifiedImages/SecureFile Drive_Introduction.pdf");

			// 2) Delta hesapla (original -> modified)
			System.out.println("Calculating delta (original -> modified)...");
			List<DeltaCommand> deltaCommands = BinaryDeltaUtil.calculateDelta(original, modified);
			System.out.println("Delta command count (v1 -> v2): " + deltaCommands.size());

			// 3) Delta uygula => reconstructedV2
			byte[] reconstructedV2 = BinaryDeltaUtil.applyDelta(original, deltaCommands);

			// 4) Eşleşme kontrolü
			boolean matchV2 = java.util.Arrays.equals(modified, reconstructedV2);
			System.out.println("Reconstructed matches modified? " + matchV2);

			if (matchV2) {
				saveFile(reconstructedV2, "src/main/resources/static/images/ReconstructedImages/reconstructed_SecureFile Drive_Introduction_v2.pdf");
				System.out.println("Reconstructed v2 file saved!");
			} else {
				System.err.println("Reconstruction FAILED for v2!");
				return; // Hatalıysa devam etmeyelim
			}

			// 5) Başka bir versiyon dosyası (v3) yükle
			byte[] version3 = loadFile("src/main/resources/static/images/ModifiedImages2/SecureFile Drive_Introduction.pdf");

			// 6) v2 -> v3 delta hesapla
			System.out.println("Calculating delta (v2 -> v3)...");
			List<DeltaCommand> deltaV2ToV3 = BinaryDeltaUtil.calculateDelta(reconstructedV2, version3);
			System.out.println("Delta command count (v2 -> v3): " + deltaV2ToV3.size());

			// 7) Delta uygula => reconstructedV3
			byte[] reconstructedV3 = BinaryDeltaUtil.applyDelta(reconstructedV2, deltaV2ToV3);

			// 8) Eşleşme kontrolü
			boolean matchV3 = java.util.Arrays.equals(version3, reconstructedV3);
			System.out.println("Reconstructed matches version3? " + matchV3);

			if (matchV3) {
				saveFile(reconstructedV3, "src/main/resources/static/images/ReconstructedImages/reconstructed_SecureFile Drive_Introduction_v3.pdf");
				System.out.println("Reconstructed v3 file saved!");
			} else {
				System.err.println("Reconstruction FAILED for v3!");
			}

		} catch (IOException e) {
			System.err.println("File I/O error: " + e.getMessage());
		}
	}

	// Yardımcı metodlar
	private static byte[] loadFile(String filePath) throws IOException {
		System.out.println("Loading file: " + filePath);
		return Files.readAllBytes(Path.of(filePath));
	}

	private static void saveFile(byte[] data, String filePath) throws IOException {
		Path outputPath = Path.of(filePath);
		Files.createDirectories(outputPath.getParent());
		Files.write(outputPath, data);
	}
}
