package com.example.tcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

class TcpApplicationTests {

	@Test
	void contextLoads() throws InterruptedException {
		long start = System.currentTimeMillis();
		System.out.println("start " + start);

		Thread.sleep(5000);

		long end = System.currentTimeMillis();
		System.out.println("end " + end);

		System.out.println((end-start)/1000 + "초" );
	}

}
