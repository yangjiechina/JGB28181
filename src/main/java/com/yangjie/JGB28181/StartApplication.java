package com.yangjie.JGB28181;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class StartApplication 
{
	public static void main( String[] args )
	{
		SpringApplication.run(StartApplication.class, args);
	}
}
