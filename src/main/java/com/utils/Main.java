package com.utils;

import java.security.GeneralSecurityException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.util.MailSSLSocketFactory;

public class Main {
	private final static  String SERVICE_HOST = "smtp.qq.com";//QQ������

	private final static  int    PORT = 465; //smtp�Ķ˿ں�

	private final static  String PROTOCOL = "smtp"; //Э�����ơ�smtp��ʾ���ʼ�����Э��

	private final static  String ACCOUNT = "3499732874@qq.com"; //�����ʼ���QQ�˺�

	private final static  String AUTH_CODE = "uxtcqhikhxzmdafd"; //QQ��Ȩ��(��Ҫ��https://mail.qq.com/����)

	public static void main(String[] args) throws Exception{
	/*	Properties prop = new Properties();  
		// �����ʼ�������������
		prop.setProperty("mail.host", SERVICE_HOST );
		// ���ͷ�������Ҫ�����֤
		prop.setProperty("mail.smtp.auth", "true");
		// �����ʼ�Э������
		prop.setProperty("mail.transport.protocol", "smtp");
		
		// ����SSL���ܣ������ʧ��
		MailSSLSocketFactory sf = new MailSSLSocketFactory();
		sf.setTrustAllHosts(true);
		prop.put("mail.smtp.ssl.enable", "true");
		prop.put("mail.smtp.ssl.socketFactory", sf);

		// ����session
		Session session = Session.getInstance(prop);
		// ͨ��session�õ�transport����
		Transport ts = session.getTransport();
		// �����ʼ����������������ͣ��ʺţ���Ȩ��������루����ȫ��
		ts.connect("SERVICE_HOST",ACCOUNT, AUTH_CODE);//������ַ�����Ȩ�룬��qq���뷴������ʧ���ˣ����Լ��ģ������ҵģ����������Ϲ��ģ�Ϊ�ˡ���������
		// �����ʼ�
		Message message = createSimpleMail(session);
		// �����ʼ�
		ts.sendMessage(message, message.getAllRecipients());
		ts.close();*/
		InternetAddress[] addresses = new InternetAddress[1];
		addresses[0] = new InternetAddress("1419281032@qq.com","zgw", "UTF-8");
		SendMailUtil2.sendMail("test xx", addresses);
	}


	/**
	* @Method: createSimpleMail
	* @Description: ����һ��ֻ�����ı����ʼ�
	*/
	public static MimeMessage createSimpleMail(Session session)
	throws Exception {
	// �����ʼ�����
	MimeMessage message = new MimeMessage(session);
	// ָ���ʼ��ķ�����
	message.setFrom(new InternetAddress("2306045401@qq.com"));
	// ָ���ʼ����ռ��ˣ����ڷ����˺��ռ�����һ���ģ��Ǿ����Լ����Լ���
	message.setRecipient(Message.RecipientType.TO, new InternetAddress("2306045401@qq.com"));
	// �ʼ��ı���
	message.setSubject("JavaMail����");
	// �ʼ����ı�����
	message.setContent("JavaMail�����ʼ��ɹ���", "text/html;charset=UTF-8");
	// ���ش����õ��ʼ�����
	return message;
	}
}
