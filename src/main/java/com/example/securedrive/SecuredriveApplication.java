package com.example.securedrive;

import com.example.securedrive.security.DeltaUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SecuredriveApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecuredriveApplication.class, args);
		String v1 = "public class Example {\r\n" +
				"    public static void main(String[] args) {\r\n" +
				"        System.out.println(\"Hello, World!\");\r\n" +
				"        int number = 10;\r\n" +
				"        System.out.println(\"Number: \" + number);\r\n" +
				"    }\r\n" +
				"}";

		String v2 = "public class Example {\n" +
				"    public static void main(String[] args) {\n" +
				"        System.out.println(\"Hello, SecureDrive!\");\n" +
				"        int number = 20;\n" +
				"        System.out.println(\"Updated Number: \" + number);\n" +
				"    }\n" +
				"}";

		String v3 = "public class Example {\n" +
				"    public static void main(String[] args) {\n" +
				"        System.out.println(\"Hello, Drive!\");\n" +
				"        int number = 50;\n" +
				"        System.out.println(\"Updated Number: \" + number);\n" +
				"    }\n" +
				"}";

		String v4 = "public class Example {\n" +
				"    public static void main(String[] args) {\n" +
				"        System.out.println(\"Hello, Example!\");\n" +
				"        int number = 100;\n" +
				"        System.out.println(\"Number: \" + number);\n" +
				"    }\n" +
				"}";

		// Delta hesaplamaları
		String deltaV2 = DeltaUtil.calculateDelta(v1, v2);
		String deltaV3 = DeltaUtil.calculateDelta(v2, v3);
		String deltaV4 = DeltaUtil.calculateDelta(v3, v4);

		// Delta'ların sıralı uygulanması
		String appliedV2 = DeltaUtil.applyDelta(v1, deltaV2);
		String appliedV3 = DeltaUtil.applyDelta(appliedV2, deltaV3);
		String appliedV4 = DeltaUtil.applyDelta(appliedV3, deltaV4);

		// Sonuçların doğruluğu kontrol edilir
		System.out.println("Uygulanan v4:\n" + appliedV4);
		System.out.println("v4 eşleşiyor mu? " + v4.equals(appliedV4));
	}
}
