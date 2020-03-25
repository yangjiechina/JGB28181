package com.yangjie.JGB28181.common.utils;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * 检测端口是否占用工具类
 * Android、javaWeb均可用
 * @author yangjie 
 * 2020年3月17日
 */
public class PortUtils {

    private static final int PORT_MAX = 65535;

    public static boolean isUsing(boolean isTcp,int port){
    	return isTcp?isTcpPortUsing(port):isUdpPortUsing(port);
    }
    public static boolean isTcpPortUsing(int port) {
        try {
            Socket socket = new Socket();
            //建立一个Socket连接
            SocketAddress socketAddress = new InetSocketAddress(port);
            socket.bind(socketAddress);
            socket.close();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean isUdpPortUsing(int port) {
        try {
            DatagramSocket datagramSocket = new DatagramSocket(null);
            SocketAddress socketAddress = new InetSocketAddress(port);
            datagramSocket.bind(socketAddress);

            datagramSocket.close();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static int findAvailablePortRange(int startPort, boolean isTcp) {
        return findAvailablePortRange(startPort, PORT_MAX, isTcp);
    }

    public static int findAvailablePortRange(int startPort, int endPort, boolean isTcp) {
        int resultPort = -1;
        for (int i = startPort; i <= endPort; i++) {
            boolean status = isTcp ? isTcpPortUsing(i) : isUdpPortUsing(i);
            if (!status) {
                resultPort = i;
                break;
            }
        }
        return resultPort;
    }
}
