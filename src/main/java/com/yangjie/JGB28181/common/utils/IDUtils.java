package com.yangjie.JGB28181.common.utils;

import java.util.UUID;

public class IDUtils {
	public static String id() {
		return UUID.randomUUID().toString().replace("-", "").toUpperCase();
	}

	public static String idLow() {
		return UUID.randomUUID().toString().replace("-", "").toLowerCase();
	}
	
}
