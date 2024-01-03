package com.example.tcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@SpringBootApplication
public class TcpApplication {

	private static final String[] DEVICE_ID = {
			"VDR-1000", "VDR-10001", "test-0001", "test-0002", "test-0003", "test-0004", "test-0005", "test-0006", "test-0007",
			"test-0008", "test-0009", "test-0010", "test-0011", "test-0012", "test-0013", "test-0014", "test-0015", "test-0016",
			"test-0017", "test-0018", "test-0019", "test-0020"
	};

	public static void main(String[] args) throws IOException {
		SpringApplication.run(TcpApplication.class, args);

		ServerSocket serverSocket = null;
		Socket socket = null;
		InputStream inputStream = null;
		OutputStream outputStream = null;
		int a ;

		try {
			// 서버 소켓 생성
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 9999 포트에서 대기 중...");

			// 클라이언트의 연결을 기다림
			while (true) {
				socket = serverSocket.accept();
				System.out.println("클라이언트가 연결되었습니다.");
				int count = 0;

				while (true) {

					System.out.println("데이터를 받습니다.");

					// 클라이언트로부터 데이터를 읽기 위한 InputStream 생성
					inputStream = socket.getInputStream();

					// 클라이언트로부터 데이터를 전송하기 위한 OutputStream 생성
					outputStream = socket.getOutputStream();

					// 클라이언트로부터 데이터를 읽어오기
					byte[] test = new byte[1600];
					int length = inputStream.read(test);

//                    if (length > 100){
//                        //서버로 데이터 보내기
//                        for (int i = 0; i < DEVICE_ID.length; i++) {
//                            urlConnection(DEVICE_ID[i], test);
//                        }
//                    }

					System.out.println(Arrays.toString(test));
//                    processMessage(test);

					if (count >= 1) {
						byte[] secondBytes = {0x10,0x00,0x00,0x00,0x23,0x00,0x00,0x20,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
						outputStream.write(secondBytes);
						outputStream.flush();
						continue;
					}

					// 프로토콜 응답 코드
					byte[] vitalBytes = {0x10,0x00,0x00,0x00,0x07,0x00,0x00,0x20,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
					outputStream.write(vitalBytes);
					outputStream.flush();

					System.out.println("응답 데이터를 클라이언트에 전송했습니다.");

//					if (count == 1) {
//						break;
//					}

					count++;
				}
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			serverSocket.close();
			System.out.println("통신이 종료");
		}
	}

	private static void urlConnection(String id, byte[] data) throws IOException {
		String apiUrl = "http://localhost:8071/device/setWearableVitalSign?deviceId="+id;

		URL url = new URL(apiUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);

		OutputStream os = conn.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
		writer.write(Arrays.toString(data));
		writer.flush();
		writer.close();
		os.close();

		conn.getContent();
	}

	private static byte[] hexStringToByteArray(String hexString) {
		int len = hexString.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
					+ Character.digit(hexString.charAt(i + 1), 16));
		}
		return data;
	}

	public static void processMessage(byte[] receivedData ) {
		int length = readIntFromBytes(receivedData, 0);
		System.out.println("Length: " + length);

		// 2. OP Code
		int opCode = readIntFromBytes(receivedData, 4);
		System.out.println("OP Code: " + opCode);
		printOpCode(opCode);

		// 3. Request ID
		long requestId = readLongFromBytes(receivedData, 8);
		System.out.println("Request ID: " + requestId);

		// 4. Device ID
		String deviceId = readStringFromBytes(receivedData, 16, 16);
		System.out.println("Device ID: " + deviceId);

		// 5. Sequence
		int sequence = readIntFromBytes(receivedData, 32);
		System.out.println("Sequence: " + sequence);

		// 6. Patient ID
		int patientId = readIntFromBytes(receivedData, 36);
		System.out.println("Patient ID: " + patientId);

		// 7. Interval
		int interval = readIntFromBytes(receivedData, 40);
		System.out.println("Interval: " + interval);

		// 8. Data (연, 월, 일)
		int data = readIntFromBytes(receivedData, 44);
		System.out.println("Data: " + data);

		// 9. Time (시, 분, 초)
		int time = readIntFromBytes(receivedData, 48);
		System.out.println("Time: " + time);

		// 10. Flag
		int flag = readIntFromBytes(receivedData, 52);
		System.out.println("Flag: " + flag);

		// Flag에 해당하는 데이터 출력
		printFlagData(flag);

		short ecgSampleRate = readShortFromBytes(receivedData,56);
		System.out.println("ecgSampleRate: " + ecgSampleRate);

		short ppgSampleRate = readShortFromBytes(receivedData,563);
		System.out.println("ecgSampleRate: " + ppgSampleRate);

		short rrgSampleRate = readShortFromBytes(receivedData,1070);
		System.out.println("ecgSampleRate: " + rrgSampleRate);

		short spo2 = readShortFromBytes(receivedData,1577);
		System.out.println("spo2: " + spo2);

		short resp = readShortFromBytes(receivedData,1579);
		System.out.println("resp: " + resp);

		short hr = readShortFromBytes(receivedData,1581);
		System.out.println("hr: " + hr);

//        short nibpSys = readShortFromBytes(receivedData,1583);
//        System.out.println("nibpSys: " + nibpSys);
//
//        short nibpDia = readShortFromBytes(receivedData,1585);
//        System.out.println("nibpDia: " + nibpDia);
//
//        short nibpMean = readShortFromBytes(receivedData,1587);
//        System.out.println("nibpMean: " + nibpMean);
//
//        short hr = readShortFromBytes(receivedData,1589);
//        System.out.println("hr: " + hr);
	}
	private static short readShortFromBytes(byte[] bytes, int offset) {
		return (short) ((bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8));
	}
	private static int readIntFromBytes(byte[] bytes, int offset) {
		return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
	}

	private static long readLongFromBytes(byte[] bytes, int offset) {
		return ByteBuffer.wrap(bytes, offset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
	}

	private static String readStringFromBytes(byte[] bytes, int offset, int length) {
		byte[] strBytes = new byte[length];
		System.arraycopy(bytes, offset, strBytes, 0, length);
		return new String(strBytes, StandardCharsets.UTF_8).trim();
	}

	private static void printFlagData(int flag) {
		// Flag에 해당하는 데이터 출력
		if ((flag & 0x00000001) != 0) {
			System.out.println("ECG Data Present");
		}
		if ((flag & 0x00004000) != 0) {
			System.out.println("PPG Data Present");
		}
		if ((flag & 0x00008000) != 0) {
			System.out.println("RRG Data Present");
		}
		if ((flag & 0x00010000) != 0) {
			System.out.println("SPO2 Data Present");
		}
		if ((flag & 0x00020000) != 0) {
			System.out.println("RESP Data Present");
		}
		if ((flag & 0x00040000) != 0) {
			System.out.println("TEMP Data Present");
		}
		if ((flag & 0x00080000) != 0) {
			System.out.println("NIBP Data Present");
		}
		if ((flag & 0x00100000) != 0) {
			System.out.println("HR Data Present");
		}
	}
	private static void printOpCode(int opCode){
		if ((opCode & 0x20000006) != 0) {
			System.out.println("ECG Data Present");
		}
		if ((opCode & 0x00004000) != 0) {
			System.out.println("PPG Data Present");
		}
	}
}