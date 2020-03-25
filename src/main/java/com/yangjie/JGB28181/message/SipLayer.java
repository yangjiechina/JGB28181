package com.yangjie.JGB28181.message;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.bean.DeviceChannel;
import com.yangjie.JGB28181.bean.Host;
import com.yangjie.JGB28181.bean.PushStreamDevice;
import com.yangjie.JGB28181.common.constants.DeviceConstants;
import com.yangjie.JGB28181.common.utils.IDUtils;
import com.yangjie.JGB28181.common.utils.PortUtils;
import com.yangjie.JGB28181.common.utils.RedisUtil;
import com.yangjie.JGB28181.media.session.PushStreamDeviceManager;
import com.yangjie.JGB28181.message.helper.DigestServerAuthenticationHelper;
import com.yangjie.JGB28181.message.helper.SipContentHelper;
import com.yangjie.JGB28181.message.session.MessageManager;

import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Expires;

public class SipLayer implements SipListener{

	private Logger logger = LoggerFactory.getLogger(getClass());
	private SipStackImpl mSipStack;

	private AddressFactory mAddressFactory;

	private HeaderFactory mHeaderFactory;

	private MessageFactory mMessageFactory;

	private SipProvider mTCPSipProvider;
	private SipProvider mUDPSipProvider;

	private long mCseq = 1;
	private long mSN = 1;

	private String mLocalIp;
	private int mLocalPort;
	private String mSipId;
	private String mSipRealm;
	private String mPassword;
	private String mSsrcRealm;
	private String mStreamMediaIp;

	private DigestServerAuthenticationHelper mDigestServerAuthenticationHelper;
	public static final  String UDP = "UDP";
	public static final String TCP = "TCP";

	private static final String MESSAGE_CATALOG = "Catalog";
	private static final String MESSAGE_DEVICE_INFO = "DeviceInfo";
	private static final String MESSAGE_BROADCAST = "Broadcast";
	private static final String MESSAGE_DEVICE_STATUS = "DeviceStatus";
	private static final String MESSAGE_KEEP_ALIVE = "Keepalive";
	private static final String MESSAGE_MOBILE_POSITION = "MobilePosition";
	private static final String MESSAGE_MOBILE_POSITION_INTERVAL = "Interval";

	private static final String ELEMENT_DEVICE_ID = "DeviceID";
	private static final String ELEMENT_DEVICE_LIST = "DeviceList";
	private static final String ELEMENT_NAME = "Name";
	private static final String ELEMENT_STATUS = "Status";

	private static final int STREAM_MEDIA_START_PORT = 20000;
	private static final int STREAM_MEDIA_END_PORT = 21000;
	private  int mStreamPort = STREAM_MEDIA_START_PORT;


	private static final int SSRC_TYPE_PLAY = 0;
	private static final int SSRC_TYPE_HISTORY = 1;
	private static final int SSRC_MIN_VALUE = 0;
	private static final int SSRC_MAX_VALUE = 9999;
	private int mSsrc;

	public static final String SESSION_NAME_PLAY = "Play";
	public static final String SESSION_NAME_DOWNLOAD = "Download";
	public static final String SESSION_NAME_PLAY_BACK = "Playback";



	private MessageManager mMessageManager = MessageManager.getInstance();
	private PushStreamDeviceManager mPushStreamDeviceManager = PushStreamDeviceManager.getInstance();

	public SipLayer(String sipId,String sipRealm,String password,String localIp,int localPort,String streamMediaIp){
		this.mSipId = sipId;
		this.mLocalIp= localIp;
		this.mLocalPort = localPort;
		this.mSipRealm = sipRealm;
		this.mPassword = password;
		this.mSsrcRealm = mSipId.substring(3,8);
		this.mStreamMediaIp = streamMediaIp;
	}
	public boolean startServer(){
		return initSip();
	}
	@SuppressWarnings("deprecation")
	private boolean initSip() {
		try {
			SipFactory sipFactory = SipFactory.getInstance();
			Properties properties = new Properties();
			properties.setProperty("javax.sip.STACK_NAME", "GB28181_SIP");
			properties.setProperty("javax.sip.IP_ADDRESS", mLocalIp);
			mSipStack = (SipStackImpl) sipFactory.createSipStack(properties);

			mHeaderFactory = sipFactory.createHeaderFactory();
			mAddressFactory = sipFactory.createAddressFactory();
			mMessageFactory = sipFactory.createMessageFactory();
			mDigestServerAuthenticationHelper = new DigestServerAuthenticationHelper();
			//同时监听UDP和TCP
			try {
				ListeningPoint tcpListeningPoint = mSipStack.createListeningPoint(mLocalIp, mLocalPort,"TCP");

				ListeningPoint udpListeningPoint = mSipStack.createListeningPoint(mLocalIp, mLocalPort,"UDP");

				mTCPSipProvider = mSipStack.createSipProvider(tcpListeningPoint);
				mTCPSipProvider.addSipListener(this);

				mUDPSipProvider = mSipStack.createSipProvider(udpListeningPoint);
				mUDPSipProvider.addSipListener(this);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	/**
	 * 终端发送的Request信令
	 */
	public void processRequest(RequestEvent evt) {
		Request request = evt.getRequest();
		String method = request.getMethod();
		logger.info("processRequest >>> {}",request);
		try{
			if(method.equalsIgnoreCase(Request.REGISTER)){
				processRegister(evt);
			}else if(method.equalsIgnoreCase(Request.MESSAGE)){
				processMessage(evt);
			}else if(method.equalsIgnoreCase(Request.BYE)){
				processBye(evt);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}


	private void processBye(RequestEvent evt) throws Exception{
		ServerTransaction serverTransaction = evt.getServerTransaction();
		Dialog dialog = serverTransaction != null ? serverTransaction.getDialog() : null;

		Request request = evt.getRequest();
		CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
		close(mPushStreamDeviceManager.removeByCallId(callIdHeader.getCallId()));
		
		if(serverTransaction == null || dialog == null){
			serverTransaction = (isTCP(request)?mTCPSipProvider:mUDPSipProvider).getNewServerTransaction(request);
		}
		Response response = mMessageFactory.createResponse(Response.OK,request);
		serverTransaction.sendResponse(response);

		

	}
	private void processMessage(RequestEvent evt) throws Exception{
		Request request = evt.getRequest();
		SAXReader reader = new SAXReader();
		//reader.setEncoding("GB2312");
		Document xml = reader.read(new ByteArrayInputStream(request.getRawContent()));
		Element rootElement = xml.getRootElement();
		String cmd = rootElement.element("CmdType").getStringValue();
		Element deviceIdElement = rootElement.element(ELEMENT_DEVICE_ID);
		if(deviceIdElement == null){
			return;
		}
		String deviceId = deviceIdElement.getText().toString();
		Response response= null;

		//心跳，
		//如果redis中设备在线，更新。
		//不在线回复400
		if(MESSAGE_KEEP_ALIVE.equals(cmd)){
			if(RedisUtil.checkExist(deviceId)){
				RedisUtil.expire(deviceId, RedisUtil.EXPIRE);
			}else {
				response = mMessageFactory.createResponse(Response.BAD_REQUEST,request);
			}
		}
		
		//目录响应，保存到redis
		else if(MESSAGE_CATALOG.equals(cmd)){

			Element deviceListElement = rootElement.element(ELEMENT_DEVICE_LIST);
			if(deviceListElement == null){
				return;
			}
			Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
			if(deviceListIterator != null){
				String deviceStr = RedisUtil.get(deviceId);
				if(StringUtils.isEmpty(deviceStr)){
					return ;
				}
				Device device = JSONObject.parseObject(deviceStr,Device.class);
				Map<String, DeviceChannel> channelMap = device.getChannelMap();
				if(channelMap == null){
					channelMap = new HashMap<String, DeviceChannel>(5);
					device.setChannelMap(channelMap);
				}
				//遍历DeviceList
				while (deviceListIterator.hasNext()) {
					Element itemDevice = deviceListIterator.next();

					Element channelDeviceElement = itemDevice.element(ELEMENT_DEVICE_ID);

					if(channelDeviceElement == null){
						continue;
					}
					String channelDeviceId = channelDeviceElement.getText().toString();
					Element channdelNameElement = itemDevice.element(ELEMENT_NAME);
					String channelName = channdelNameElement != null ? channdelNameElement.getText().toString():"";
					Element statusElement = itemDevice.element(ELEMENT_STATUS);
					String status = statusElement != null?statusElement.getText().toString():"ON";

					DeviceChannel deviceChannel = channelMap.containsKey(channelDeviceId)?channelMap.get(channelDeviceId):new DeviceChannel();
					deviceChannel.setName(channelName);
					deviceChannel.setDeviceId(channelDeviceId);
					deviceChannel.setStatus(status.equals("ON")?DeviceConstants.ON_LINE:DeviceConstants.OFF_LINE);

					channelMap.put(channelDeviceId, deviceChannel);
				}
				//更新Redis
				RedisUtil.set(deviceId, JSONObject.toJSONString(device));
			}
		}
		if(response == null){
			response = mMessageFactory.createResponse(Response.OK,request);
		}
		sendResponse(response, getServerTransaction(evt));

	}
	private ServerTransaction getServerTransaction(RequestEvent evt) throws Exception{
		ServerTransaction serverTransaction = evt.getServerTransaction();
		if(serverTransaction == null){
			Request request = evt.getRequest();
			serverTransaction = (isTCP(request)?mTCPSipProvider:mUDPSipProvider).getNewServerTransaction(request);
		}
		return serverTransaction;

	}
	private void processRegister(RequestEvent evt) throws Exception{
		Request request = evt.getRequest();
		ServerTransaction serverTransaction = evt.getServerTransaction();
		boolean isTcp = isTCP(request);
		if(serverTransaction == null){
			serverTransaction = (isTCP(request)?mTCPSipProvider:mUDPSipProvider).getNewServerTransaction(request);
		}
		Response response = null;
		boolean passwordCorrect = false;
		boolean isRegisterSuceess = false;
		Device device = null;
		Header header = request.getHeader(AuthorizationHeader.NAME);
		//携带授权头
		//校验密码是否正确
		if(header != null){
			passwordCorrect = mDigestServerAuthenticationHelper.doAuthenticatePlainTextPassword(request,mPassword);
			if(!passwordCorrect){
				logger.info("密码错误");
			}
		}

		//未携带授权头或者密码错误 回复401
		if(header == null || !passwordCorrect){
			response = mMessageFactory.createResponse(Response.UNAUTHORIZED, request);
			mDigestServerAuthenticationHelper.generateChallenge(mHeaderFactory, response, mSipRealm);
		}
		//携带授权头并且密码正确
		else if (header != null && passwordCorrect){
			response = mMessageFactory.createResponse(Response.OK, request);
			//添加date头
			response.addHeader(mHeaderFactory.createDateHeader(Calendar.getInstance(Locale.ENGLISH)));
			ExpiresHeader expiresHeader = (ExpiresHeader) request.getHeader(Expires.NAME);
			//添加Contact头
			response.addHeader(request.getHeader(ContactHeader.NAME));
			//添加Expires头
			response.addHeader(request.getExpires());
			//注销成功
			if(expiresHeader != null && expiresHeader.getExpires() == 0){

			}
			//注册成功
			else{
				isRegisterSuceess = true;
				//1.获取到通信地址等信息，保存到Redis
				FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
				ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
				Host host = getHost(viaHeader);
				String deviceId = getDeviceId(fromHeader);
				device =new Device();
				device.setDeviceId(deviceId);
				device.setHost(host);
				device.setProtocol(isTcp?TCP:UDP);

			}
		}
		sendResponse(response,serverTransaction);
		//注册成功
		//1.保存到redis
		//2.下发catelog查询目录
		if(isRegisterSuceess && device != null){
			String callId = IDUtils.id();
			String fromTag = IDUtils.id();
			RedisUtil.set(device.getDeviceId(), RedisUtil.EXPIRE, JSONObject.toJSONString(device));
			sendCatalog(device, callId, fromTag, mCseq, String.valueOf(mSN));
		}
	}

	private void sendCatalog(Device device,String callId,String fromTag,long cseq,String sn) throws Exception{
		Host host = device.getHost();
		String deviceId = device.getDeviceId();
		Request request = createRequest(deviceId,host.getAddress(),host.getWanIp(),host.getWanPort(),device.getProtocol(),
				mSipId,mSipRealm,fromTag,
				deviceId,mSipRealm,null,
				callId,cseq,Request.MESSAGE);
		String catalogContent = SipContentHelper.generateCatalogContent(deviceId, sn);
		ContentTypeHeader contentTypeHeader = mHeaderFactory.createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(catalogContent, contentTypeHeader);
		request.addHeader(contentTypeHeader);

		sendRequest(request);

	}

	public void sendInvite(Device device,String sessionName,String callId,String channelId,int port,String ssrc,
			boolean isTcp) throws Exception{

		String fromTag = IDUtils.id();
		Host host = device.getHost();
		String realm = channelId.substring(0,8);
		Request request = createRequest(channelId,host.getAddress(),host.getWanIp(),host.getWanPort(),device.getProtocol(),
				mSipId,mSipRealm,fromTag,
				channelId,realm,null,
				callId,20,Request.INVITE);
		//添加Concat头
		Address concatAddress = mAddressFactory.createAddress(mAddressFactory.createSipURI(mSipId, mLocalIp.concat(":").concat(String.valueOf(mLocalPort))));
		request.addHeader(mHeaderFactory.createContactHeader(concatAddress));
		//添加消息体
		String content = SipContentHelper.generateRealTimeMeidaStreamInviteContent(mSipId,mStreamMediaIp,port,isTcp,false,sessionName,ssrc);
		ContentTypeHeader contentTypeHeader = mHeaderFactory.createContentTypeHeader("Application", "SDP");
		request.setContent(content, contentTypeHeader);
		//request.addHeader(contentTypeHeader);
		sendRequest(request);
	}
	private Request createRequest(String deviceId,String address,String targetIp,int targetPort,String protocol,
			String fromUserInfo, String fromHostPort, String fromTag,
			String toUserInfo, String toHostPort, String toTag,
			String callId,
			long cseqNo, String method) throws ParseException, InvalidArgumentException {
		//请求行
		SipURI requestLine = mAddressFactory.createSipURI(deviceId, address);
		//Via头
		ArrayList viaHeaderList = new ArrayList();
		ViaHeader viaHeader = mHeaderFactory.createViaHeader(targetIp, targetPort, protocol, null);
		viaHeader.setRPort();
		viaHeaderList.add(viaHeader);

		//To头
		SipURI toAddress = mAddressFactory.createSipURI(toUserInfo, toHostPort);
		Address toNameAddress = mAddressFactory.createAddress(toAddress);
		ToHeader toHeader = mHeaderFactory.createToHeader(toNameAddress, toTag);

		//From头
		SipURI from = mAddressFactory.createSipURI(fromUserInfo, fromHostPort);
		Address fromNameAddress = mAddressFactory.createAddress(from);
		FromHeader fromHeader = mHeaderFactory.createFromHeader(fromNameAddress, fromTag);

		//callId
		CallIdHeader callIdHeader = protocol.equals(TCP)?mTCPSipProvider.getNewCallId():mUDPSipProvider.getNewCallId();;
		callIdHeader.setCallId(callId);

		//Cseq
		CSeqHeader cSeqHeader = mHeaderFactory.createCSeqHeader(cseqNo, method);

		//Max_forward
		MaxForwardsHeader maxForwardsHeader = mHeaderFactory.createMaxForwardsHeader(70);

		return mMessageFactory.createRequest(requestLine, method, callIdHeader, cSeqHeader, fromHeader, toHeader,
				viaHeaderList, maxForwardsHeader);

	}
	private boolean isTCP(Request request){
		return isTCP((ViaHeader)request.getHeader(ViaHeader.NAME));
	}
	private boolean isTCP(Response response){
		return isTCP((ViaHeader)response.getHeader(ViaHeader.NAME));
	}
	private boolean isTCP(ViaHeader viaHeader){
		String protocol = viaHeader.getProtocol();
		return protocol.equals("TCP");
	}
	private String getDeviceId(FromHeader fromHeader){
		AddressImpl address = (AddressImpl) fromHeader.getAddress();
		SipUri uri = (SipUri) address.getURI();
		String user = uri.getUser();
		return user;
	}
	private Host getHost(ViaHeader viaHeader){
		String received = viaHeader.getReceived();
		int rPort = viaHeader.getRPort();
		//本地模拟设备 received 为空 rPort 为 -1
		//解析本地地址替代
		if(StringUtils.isEmpty(received) || rPort == -1){
			received = viaHeader.getHost();
			rPort = viaHeader.getPort();
		}
		Host host =new Host();
		host.setWanIp(received);
		host.setWanPort(rPort);
		host.setAddress(received.concat(":").concat(String.valueOf(rPort)));
		return host; 
	}
	private void sendResponse(Response response,ServerTransaction serverTransaction) throws Exception{
		logger.info("sendResponse >>> {}",response);
		serverTransaction.sendResponse(response);
	}
	private void sendRequest(Request request) throws SipException{
		logger.info("sendRequest >>> {}",request);
		ClientTransaction clientTransaction = (isTCP(request)?mTCPSipProvider:mUDPSipProvider).getNewClientTransaction(request);
		clientTransaction.sendRequest();
	}

	/**
	 * 终端发送的Response信令
	 * 服务器下发给终端的Request得到响应
	 */
	public void processResponse(ResponseEvent evt) {
		Response response = evt.getResponse();
		logger.info("processResponse >>> {}",response);
		CSeqHeader cseqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
		String method = cseqHeader.getMethod();
		try{
			//实时流请求响应
			if(Request.INVITE.equals(method)){
				int statusCode = response.getStatusCode();
				//trying不会回复
				if(statusCode == Response.TRYING){

				}
				//成功响应
				//下发ack
				if(statusCode == Response.OK){
					ClientTransaction clientTransaction = evt.getClientTransaction();
					if(clientTransaction == null){
						logger.error("回复ACK时，clientTransaction为null >>> {}",response);
						return;
					}
					Dialog clientDialog = clientTransaction.getDialog();

					CSeqHeader clientCSeqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
					long cseqId = clientCSeqHeader.getSeqNumber();
					/*
					createAck函数，创建的ackRequest，会采用Invite响应的200OK，中的contact字段中的地址，作为目标地址。
					有的终端传上来的可能还是内网地址，会造成ack发送不出去。接受不到音视频流
					所以在此处统一替换地址。和响应消息的Via头中的地址保持一致。
					 */
					Request ackRequest = clientDialog.createAck(cseqId);
					SipURI requestURI = (SipURI) ackRequest.getRequestURI();
					ViaHeader viaHeader = (ViaHeader) response.getHeader(ViaHeader.NAME);
					requestURI.setHost(viaHeader.getHost());
					requestURI.setPort(viaHeader.getPort());
					clientDialog.sendAck(ackRequest);
					CallIdHeader callIdHeader = (CallIdHeader) ackRequest.getHeader(CallIdHeader.NAME);
					//写入消息管理器
					mMessageManager.put(callIdHeader.getCallId(),clientDialog);
					logger.info("sendAck >>> {}",ackRequest);
				}
			}

		}catch(Exception e){
			e.printStackTrace();
			logger.error(e.toString());
		}
	}

	/**
	 * 停止推流，删除session，断开通道
	 * @throws SipException
	 */
	private void close(PushStreamDevice pushStreamDevice) throws SipException{
		if(pushStreamDevice != null){
			pushStreamDevice.getServer().stopServer();
			pushStreamDevice.getObserver().stopRemux();
		}
	}
	public void sendBye(String callId) throws SipException{
		PushStreamDevice pushStreamDevice = mPushStreamDeviceManager.removeByCallId(callId);
		if(pushStreamDevice != null){
			close(pushStreamDevice);
			Dialog dialog = pushStreamDevice.getDialog();
			if(dialog != null){
				Request byeRequest = dialog.createRequest(Request.BYE);
				ClientTransaction clientTransaction = (isTCP(byeRequest) ? mTCPSipProvider:mUDPSipProvider).getNewClientTransaction(byeRequest);
				dialog.sendRequest(clientTransaction);
				logger.info("sendRequest >>> {}",byeRequest);
			}
		}

	}
	public String getSsrc(boolean isRealTime){
		StringBuffer buffer = new StringBuffer(15);
		buffer.append(String.valueOf(isRealTime?0:1));
		buffer.append(mSsrcRealm);
		if(mSsrc >= SSRC_MAX_VALUE){
			mSsrc = SSRC_MIN_VALUE;
		}
		String ssrcStr = String.valueOf(mSsrc);
		int length = ssrcStr.length();
		for(int i =length;i<4;i++){
			buffer.append("0");
		}
		buffer.append(String.valueOf(ssrcStr));

		return buffer.toString();
	}
	public int getPort(boolean isTcp){
		int resultPort = 0;
		if(PortUtils.isUsing(isTcp, mStreamPort)){
			resultPort = PortUtils.findAvailablePortRange(STREAM_MEDIA_START_PORT, STREAM_MEDIA_END_PORT, isTcp);
		}
		resultPort = mStreamPort;
		mStreamPort++;
		if(mStreamPort>STREAM_MEDIA_END_PORT){
			mStreamPort = STREAM_MEDIA_START_PORT;
		}
		return resultPort;
	}
	/**
	 * 会话结束
	 */
	public void processDialogTerminated(DialogTerminatedEvent evt) {

	}

	/**
	 * UDPSocket通道异常
	 */
	public void processIOException(IOExceptionEvent evt) {

	}

	/**
	 * 超时
	 * 下发的Request未能得到响应
	 */
	public void processTimeout(TimeoutEvent evt) {

	}

	/**
	 * 事务结束
	 */
	public void processTransactionTerminated(TransactionTerminatedEvent evt) {

	}

}
