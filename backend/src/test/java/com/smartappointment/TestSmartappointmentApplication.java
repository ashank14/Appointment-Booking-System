package com.smartappointment;

import org.springframework.boot.SpringApplication;

public class TestSmartappointmentApplication {

	public static void main(String[] args) {
		SpringApplication.from(SmartappointmentApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
