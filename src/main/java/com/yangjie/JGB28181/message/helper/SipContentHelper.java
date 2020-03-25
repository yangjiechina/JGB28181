 package com.yangjie.JGB28181.message.helper;

public class SipContentHelper {

	public static String generateCatalogContent(String deviceId,String sn){
        StringBuffer content = new StringBuffer(200);
        content.append("<?xml version=\"1.0\"?>\r\n");
        content.append("<Query>\r\n");
        content.append("<CmdType>Catalog</CmdType>\r\n");
        content.append("<SN>"+sn+"</SN>\r\n");
        content.append("<DeviceID>"+deviceId+"</DeviceID>\r\n");
        content.append("</Query>");
        return content.toString();
	}
	public static String generateRealTimeMeidaStreamInviteContent(String sessionId,String ip,int port,boolean isTcp,boolean isActive,String sessionName,String ssrc){
        StringBuffer content = new StringBuffer(200);
         content.append("v=0\r\n");
         content.append("o="+sessionId+" 0 0 IN IP4 "+ip+"\r\n");
         content.append("s="+sessionName+"\r\n");
         content.append("c=IN IP4 "+ip+"\r\n");
         content.append("t=0 0\r\n");
         content.append("m=video "+port+" "+(isTcp?"TCP/":"")+"RTP/AVP 96 98 97\r\n");
         content.append("a=sendrecv\r\n");
         content.append("a=rtpmap:96 PS/90000\r\n");
         content.append("a=rtpmap:98 H264/90000\r\n");
         content.append("a=rtpmap:97 MPEG4/90000\r\n");
         if(isTcp){
             content.append("a=setup:"+(isActive?"active\r\n":"passive\r\n"));
             content.append("a=connection:new\r\n");
         }
         content.append("y="+ssrc+"\r\n");
        return content.toString();
	}
}
