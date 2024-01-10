package com.example.tcp;

import com.google.gson.JsonObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class TcpApplication {

	private static final String[] TEST_DEVICE_ID = {
			"VDR-1000", "VDR-10001"
			,"test-0001", "test-0002", "test-0003", "test-0004", "test-0005", "test-0006", "test-0007",
			"test-0008", "test-0009", "test-0010", "test-0011", "test-0012", "test-0013", "test-0014", "test-0015", "test-0016",
			"test-0017", "test-0018", "test-0019", "test-0020"
	};

	private static final byte[] FIRST_RESPONSE = {0x10,0x00,0x00,0x00,0x07,0x00,0x00,0x20,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
	private static final byte[] SECOND_RESPONSE = {0x10,0x00,0x00,0x00,0x23,0x00,0x00,0x20,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};

	public static void main(String[] args) throws IOException, InterruptedException {
		SpringApplication.run(TcpApplication.class, args);

		ServerSocket serverSocket = null;
		Socket socket;
		InputStream inputStream;
		OutputStream outputStream;

		long lastPacketTime = System.currentTimeMillis();


		try {
			// 서버 소켓 생성
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 9999 포트에서 대기 중...");

			// 클라이언트의 연결을 기다림
			while (true) {
				socket = serverSocket.accept();
				System.out.println("클라이언트가 연결되었습니다.");

				// 첫번째 패킷인지 확인
				boolean isFirstPacket = true;

				while (true) {
					try {
						System.out.println("데이터를 받습니다.");

						// 클라이언트로부터 데이터를 읽기 위한 InputStream 생성
						inputStream = socket.getInputStream();
						// 클라이언트로부터 데이터를 전송하기 위한 OutputStream 생성
						outputStream = socket.getOutputStream();

						// 클라이언트로부터 데이터를 읽어오기
						byte[] test = new byte[1600];

						int length = inputStream.read(test);

						if (length > 1000) {
							processMessage(test);
						}

						long currentTime = System.currentTimeMillis();
						long elapsedTime = currentTime - lastPacketTime;

						if (elapsedTime < 1000) {
							// Wait until 1 second has passed since the last packet
							Thread.sleep(1000 - elapsedTime);
						}

						lastPacketTime = System.currentTimeMillis();

						// 두번째 패킷 응답
						if (!isFirstPacket) {
							outputStream.write(SECOND_RESPONSE);
							outputStream.flush();
							continue;
						}

						// 프로토콜 첫번째 패킷 응답 코드
						outputStream.write(FIRST_RESPONSE);
						outputStream.flush();

						System.out.println("응답 데이터를 클라이언트에 전송했습니다.");

						isFirstPacket = false;

					} catch (SocketException e){
						System.out.println(" 연결 종료 " );
						System.out.println(" 소켓 연결 대기 중");
						break;
					}
                }
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
        } finally {
			serverSocket.close();
			System.out.println("통신이 종료");
		}
	}

	private static void urlConnection(String deviceId, String ecgSampleRate, String ecgGraphData, String ppgSamplerate, String ppgGraphData,
								String respirationSampleRate, String respirationGraph, short spo2Data, short respData, short tempData,
								short nibpSys, short nibpDia, short nibpMean, short hrdata) throws IOException {
		String apiUrl = "http://localhost:8071/device/setWearableVitalSign";

		short battery = 0;

		URL url = new URL(apiUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("deviceId",deviceId);
		jsonObject.addProperty("ecgSampleRate",ecgSampleRate);
		jsonObject.addProperty("ecgGraphData",ecgGraphData);
		jsonObject.addProperty("ppgSampleRate",ppgSamplerate);
		jsonObject.addProperty("ppgGraphData",ppgGraphData);
		jsonObject.addProperty("respirationSampleRate",respirationSampleRate);
		jsonObject.addProperty("respirationGraph",respirationGraph);
		jsonObject.addProperty("spo2Data",spo2Data);
		jsonObject.addProperty("respData",respData);
		jsonObject.addProperty("tempData",tempData);
		jsonObject.addProperty("nibpSys",nibpSys);
		jsonObject.addProperty("nibpDia",nibpDia);
		jsonObject.addProperty("nibpMean",nibpMean);
		jsonObject.addProperty("hrdata",hrdata);
		jsonObject.addProperty("battery",battery);

		System.out.println(jsonObject);

		OutputStream os = conn.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
		writer.write(jsonObject.toString());
		writer.flush();
		writer.close();
		os.close();

		conn.getContent();

	}

	public static void processMessage(byte[] receivedData ) throws IOException {
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
		Map<String,Boolean> isData = printFlagData(flag);

		// ecgSampleRate
		short ecgSampleRate = readShortFromBytes(receivedData,56);
		System.out.println("ecgSampleRate: " + ecgSampleRate);

		// ecgGraphData
		short[] ecgGraphData = new short[250];
		int ecgRange = 63;

		for (int i = 0; i < 250; i++) {
			ecgGraphData[i] = readShortFromBytes(receivedData,ecgRange);
			ecgRange += 2;
		}

		System.out.println("ecgGraphData: " + Arrays.toString(ecgGraphData));

		// ppgSampleRate
		short ppgSampleRate = readShortFromBytes(receivedData,563);
		System.out.println("ppgSampleRate: " + ppgSampleRate);

		// ppgSampleRate
		short[] ppgGraphData = new short[250];
		int ppgRange = 570;

		for (int i = 0; i < 250; i++) {
			ppgGraphData[i] = readShortFromBytes(receivedData,ppgRange);
			ppgRange += 2;
		}

		System.out.println("ppgGraphData: " + Arrays.toString(ppgGraphData));

		// rrgSampleRate
		short rrgSampleRate = readShortFromBytes(receivedData,1070);
		System.out.println("rrgSampleRate: " + rrgSampleRate);

		// rrgGraphData
		short[] rrgGraphData = new short[250];
		int rrgRange = 1077;

		for (int i = 0; i < 250; i++) {
			rrgGraphData[i] = readShortFromBytes(receivedData,rrgRange);
			rrgRange += 2;
		}

		System.out.println("rrgGraphData: " + Arrays.toString(rrgGraphData));

		int dataLength = 1577;

		short spo2 = 0;
		if (isData.get("SPO2")){
			spo2 = readShortFromBytes(receivedData,dataLength);
			System.out.println("spo2: " + spo2);
			dataLength += 2;
		}

		short resp = 0;
		if (isData.get("RESP")){
			resp = readShortFromBytes(receivedData,dataLength);
			System.out.println("resp: " + resp);
			dataLength += 2;
		}

		short temp = 0;
		if (isData.get("TEMP")){
			temp = readShortFromBytes(receivedData,dataLength);
			System.out.println("temp: " + temp);
			dataLength += 2;
		}

		short nibpSys = 0;
		short nibpDia = 0;
		short nibpMean = 0;

		if (isData.get("NIBP")) {
			nibpSys = readShortFromBytes(receivedData, dataLength);
			System.out.println("nibpSys: " + nibpSys);
			dataLength += 2;

			nibpDia = readShortFromBytes(receivedData, dataLength);
			System.out.println("nibpDia: " + nibpDia);
			dataLength += 2;

			nibpMean = readShortFromBytes(receivedData, dataLength);
			System.out.println("nibpMean: " + nibpMean);
			dataLength += 2;
		}

		short hr = 0;
		if (isData.get("HR")){
			hr = readShortFromBytes(receivedData,dataLength);
			System.out.println("hr: " + hr);
		}

		//실제 사용 api
		urlConnection(deviceId, String.valueOf(ecgSampleRate) ,Arrays.toString(ecgGraphData),String.valueOf(ppgSampleRate),Arrays.toString(ppgGraphData)
				,String.valueOf(rrgSampleRate),Arrays.toString(rrgGraphData),spo2,resp,temp,nibpSys,nibpDia,nibpMean,hr);

		// test api -> 디바이스 Id 임의로 늘려놓은 데이터
//		if (length > 1500){
//				//서버로 데이터 보내기
//				for (int i = 0; i < DEVICE_ID.length; i++) {
//					urlConnection(DEVICE_ID[i], String.valueOf(ecgSampleRate) ,Arrays.toString(ecgGraphData),String.valueOf(ppgSampleRate),Arrays.toString(ppgGraphData)
//					,String.valueOf(rrgSampleRate),Arrays.toString(rrgGraphData),spo2,resp,temp,nibpSys,nibpDia,nibpMean,hr);
//				}
//
//				for (int i = 0; i < DEVICE_ID.length; i++) {
//					urlConnection(DEVICE_ID[i], String.valueOf(ecgSampleRate) ,Arrays.toString(ecgGraphData),String.valueOf(ppgSampleRate),Arrays.toString(ppgGraphData)
//					,String.valueOf(rrgSampleRate),Arrays.toString(rrgGraphData),spo2,resp,temp,nibpSys,nibpDia,nibpMean,hr);
//			}
//		}
	}
	private static short readShortFromBytes(byte[] bytes, int offset) {
//		return (short) ((bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8));
		return ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
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

	private static Map<String,Boolean> printFlagData(int flag) {

		Map<String,Boolean> isData = new HashMap<>();

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
			isData.put("SPO2", true);
			System.out.println("SPO2 Data Present");
		} else {
			isData.put("SPO2", false);
		}

		if ((flag & 0x00020000) != 0) {
			isData.put("RESP", true);
			System.out.println("RESP Data Present");
		} else {
			isData.put("RESP", false);
		}

		if ((flag & 0x00040000) != 0) {
			isData.put("TEMP", true);
			System.out.println("TEMP Data Present");
		} else {
			isData.put("TEMP", false);
		}

		if ((flag & 0x00080000) != 0) {
			isData.put("NIBP", true);
			System.out.println("NIBP Data Present");
		} else {
			isData.put("NIBP", false);
		}

		if ((flag & 0x00100000) != 0) {
			isData.put("HR", true);
			System.out.println("HR Data Present");
		} else {
			isData.put("HR", false);
		}

		return isData;
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