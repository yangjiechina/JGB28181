package com.yangjie.JGB28181.common.utils;

public class BitUtils {

	public static  int byte2ToInt(byte b1,byte b2){
		int temp1 = b1&0xff ;
		int temp2 = b2&0xff ;
		return (temp1<< 8) + temp2;
	}
	public static  int byte4ToInt(byte b1,byte b2,byte b3,byte b4){

		int temp1 = b1&0xff ;
		int temp2 = b2&0xff ;
		int temp3 = b3&0xff ;
		int temp4 = b4&0xff ;
		return (temp1 << 24) + (temp2<< 16)+(temp3<< 8)+temp4;
	}
	
	
	public static void writeBits(Integer cache,int value,int start,int length){
		int intValue = cache.intValue();
	}
}
