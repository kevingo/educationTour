package Utility;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane.SystemMenuBar;

import models.Attatch;
import models.Cite;
import models.Examine;
import models.Link;
import models.Map;
import models.Member;
import models.OAIncludeTP;
import models.OutdoorActivity;
import models.Photo;
import models.Q_Show;
import models.Q_Showpiece;
import models.Q_TeacherApply;
import models.Q_VolunteerSeesion;
import models.Question;
import models.Teacher;
import models.TeachingPlan;
import models.Text;
import models.ViewCountHistory;
import models.Youtube;
import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import antlr.collections.impl.LList;

import com.google.zxing.qrcode.QRCodeWriter;

import controllers.AjaxManager;

import play.Logger;
import play.Play;
import play.data.Upload;
import play.data.validation.Email;
import play.data.validation.Required;
import play.mvc.Scope;

public class MyUtil {
	
	//用來記錄儲存TP與OA時 連續form post的 tp&oa.id  ，order紀錄順序 (當有多人剛好同時編輯同時送出時，有錯亂之可能性?)
	public static Long _tpid=(long) 0;
	public static Long _oaid=(long) 0;
	public static Long _examid = (long) 0;
	public static String _orderNumber="";
	public static int _html_index = 0 , _pic_index = 0 , _youtube_index =0 , _file_index=0 , _web_index=0 , _spot_index=0 , _res_index=0 , _ref_index  =0;
	public static int _ox_index = 0, _singleAnswer_index = 0, _shortAnswer_index = 0;
	
	public static Session mailsess;
	public static Transport transport;
	
	public static void mail_conn() throws MessagingException {
		
		//Get system properties
		Properties props = System.getProperties();
		String host = "smtp.gmail.com";
		String u = Conf.mail_account;
		String p = Conf.mail_password;
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.timeout","86400000"); // 24 hr protocol timeout

		// 產生新的Session
		mailsess = Session.getDefaultInstance(props);
		transport = mailsess.getTransport("smtp"); //只支持IMAP、 SMTP和 POP3
		transport.connect(host, u, p); //以smtp的方式登入mail server
		
	} 
	
	public static Member getCurrentUser() {
		String mail = Scope.Session.current().get("email");
		Member user = Member.findUser(mail);
		return user;
	}
	
	public static boolean isAdminByCurrentUser() {
		
		Member user = getCurrentUser();
		if(user!=null&&user.roleList.contains(ROLE.ADMIN))
			return true;
		else
			return false;
	}
	
	public static boolean isMobile(String user_agent) {
		
		boolean mobile = false;
		
		if(user_agent.contains("iPhone"))
			mobile = true;
		else if(user_agent.contains("Android"))
			mobile = true;
		
		return mobile;		
	}
	
	public static void removeUTF8FileBOM(String filepath){
		File file = new File(filepath);
		try{
	    	byte[] bs = FileUtils.readFileToByteArray(file);  
	        if (bs[0] == -17 && bs[1] == -69 && bs[2] == -65) {  
	            byte[] nbs = new byte[bs.length - 3];  
	            System.arraycopy(bs, 3, nbs, 0, nbs.length);  
	            FileUtils.writeByteArrayToFile(file, nbs);  
	            //System.out.println("Remove BOM: " + file);  
	        }
		}
		catch (Exception e) {
			Logger.info(e.toString());
		}
	}
	
	public static void calculateViewCount(int contentType,Long contentId){
		String ip = play.mvc.Http.Request.current().remoteAddress;
		ViewCountHistory view =ViewCountHistory.findByIpAndContent(ip,contentType,contentId);
		if(view==null){
			
			view = new ViewCountHistory();
			view.ip=ip;
			view.contentType=contentType;
			view.contentId=contentId;
			view.date=MyUtil.getDate();
			view.save();
			addViewCount(contentType,contentId);
			
		}
		else {
			if(!view.date.equals(MyUtil.getDate())){
				view.date=MyUtil.getDate();
				view.save();
				addViewCount(contentType,contentId);
			}
		}
	}
	
	private static void addViewCount(int contentType,Long contentId) {
		switch (contentType) {
		case 0:
			Member user = Member.findById(contentId);
			if(user!=null){
				user.visit++;
				user.save();
			}
			break;
		case 1:
			TeachingPlan tp = TeachingPlan.findById(contentId);
			if(tp!=null){
				tp.viewCount++;
				tp.save();
			}				
			break;
		case 2:
			OutdoorActivity oa = OutdoorActivity.findById(contentId);
			if(oa!=null){
				oa.viewCount++;
				oa.save();
			}
			break;
		}
	}
	
	public static String GenerateShowpieceSerialNumber(){
		SecureRandom ran = new SecureRandom();
		String serialNumber;

		while(true){
			//產生序號
			serialNumber = new BigInteger(130, ran).toString(32).substring(0, 9);
			//檢查是否重複
			boolean IsExist = Q_Showpiece.IsExistBySerialNumber(serialNumber);
			if(IsExist)
				continue;
			else 
				break;
		}	
		return serialNumber;
	}
	
	public static String GenerateVolunteerSerialNumber(){
		SecureRandom ran = new SecureRandom();
		String serialNumber;

		while(true){
			//產生序號
			serialNumber = new BigInteger(130, ran).toString(32).substring(0, 5);
			//檢查是否重複
			boolean IsExist = Q_VolunteerSeesion.IsExistBySerialNumber(serialNumber);
			if(IsExist)
				continue;
			else 
				break;
		}	
		return serialNumber;
	}
	
	public static String GenerateShowSerialNumber() {
		SecureRandom ran = new SecureRandom();
		String serialNumber;

		while(true){
			//產生序號
			serialNumber = new BigInteger(130, ran).toString(32).substring(0, 5);
			//檢查是否重複
			boolean IsExist = Q_Show.IsExistBySerialNumber(serialNumber);
			if(IsExist)
				continue;
			else 
				break;
		}	
		return serialNumber;
	}
	
	public static String GenerateTeacherApplySerialNumber() {
		SecureRandom ran = new SecureRandom();
		String serialNumber;

		while(true){
			//產生序號
			serialNumber = new BigInteger(130, ran).toString(32).substring(0, 9);
			//檢查是否重複
			boolean IsExist = Q_TeacherApply.IsExistBySerialNumber(serialNumber);
			if(IsExist)
				continue;
			else 
				break;
		}	
		return serialNumber;
	}
	
	
	public static void sendMail(String receiver, String subject, String content) throws AddressException, MessagingException {
		long startTime = System.currentTimeMillis();
//		String host = "smtp.gmail.com";
//		String username = Conf.mail_account;
//		String password = Conf.mail_password;

		boolean sessionDebug = true;

		//Get system properties
//		Properties props = System.getProperties();
//		props.put("mail.smtp.auth", "true");
//		props.put("mail.smtp.starttls.enable", "true");
//		props.put("mail.smtp.host", host);
//		props.put("mail.smtp.port", "587");

		// 產生新的Session
//		javax.mail.Session mailsess = Session.getDefaultInstance(props);
//		Session mailsess = InitJob.mailsess;
		mailsess.setDebug(sessionDebug); //是否在控制台顯示debug訊息

		Message msg = new MimeMessage(mailsess);
		//設定郵件
		msg.setFrom(new InternetAddress("funnytrip")); // 設定傳送郵件的發信人
		InternetAddress[] address= {new InternetAddress(receiver)}; // 設定傳送郵件的收件者
		msg.setRecipients(Message.RecipientType.TO, address);
		msg.setSubject(subject); //設定主題
		msg.setText(content); //設定內文
		
		//發送郵件
//		Transport transport = mailsess.getTransport("smtp"); //只支持IMAP、 SMTP和 POP3
//		transport.connect(host, username, password); //以smtp的方式登入mail server
		Logger.info("start to send mail ... ");
        if(transport==null||!transport.isConnected())
        	mail_conn();
		transport.sendMessage(msg,msg.getAllRecipients());
//		transport.close();
		long endTime = System.currentTimeMillis();
		Logger.info("execute time=" + (endTime-startTime));
	}
	
	public static void sendMail(String receiver, String subject, String content , List<String> filePath) throws AddressException, MessagingException {
		Logger.info("send mail ... ");
		long startTime = System.currentTimeMillis();
//		String host = "smtp.gmail.com";
//		String username = Conf.mail_account;
//		String password = Conf.mail_password;

		boolean sessionDebug = true;

		//Get system properties
//		Properties props = System.getProperties();
//		props.put("mail.smtp.auth", "true");
//		props.put("mail.smtp.starttls.enable", "true");
//		props.put("mail.smtp.host", host);
//		props.put("mail.smtp.port", "587");

		// 產生新的Session
//		javax.mail.Session mailsess = Session.getDefaultInstance(props);
//		Session mailsess = InitJob.mailsess;
		mailsess.setDebug(sessionDebug); //是否在控制台顯示debug訊息

		Message msg = new MimeMessage(mailsess);
		//設定郵件
		msg.setFrom(new InternetAddress("funnytrip")); // 設定傳送郵件的發信人
		InternetAddress[] address= {new InternetAddress(receiver)}; // 設定傳送郵件的收件者
		msg.setRecipients(Message.RecipientType.TO, address);
		msg.setSubject(subject); //設定主題
		msg.setText(content); //設定內文
		
		
		Multipart mp = new MimeMultipart();
		 //設定本文區的內容
        MimeBodyPart mbp1 = new MimeBodyPart();
        mbp1.setContent(content, "text/plain;charset=big5");
        mp.addBodyPart(mbp1);

        for (String path : filePath) {
            try {
            	String FullPath = Play.getFile("").getAbsolutePath() + path;
                //附加檔案 
        		MimeBodyPart mbp2 = new MimeBodyPart();
                FileDataSource fds = new FileDataSource(FullPath);
                mbp2.setDataHandler(new DataHandler(fds));
                //mbp2.setFileName("附件"+filePath.indexOf(path));
                String filename = fds.getName();
                //若檔案名稱為'身分證',則因應廣達活動報名系統，不顯示身分證字號檔名，而顯示 "活動報名流程"
            	Pattern p = Pattern.compile("[a-zA-Z][0-9]{9}");
            	Matcher m = p.matcher(filename);
				if(m.find()){
					filename=filename.replace(m.group(0), "活動報名流程");
				}
				mbp2.setFileName(MimeUtility.encodeText(filename));
				mp.addBodyPart(mbp2);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //附加檔在mail接收時，出現的檔案名稱
            //把所有MimeBodyPart加進Multipart，所以如果有第二個附加檔，應該要有另一MultiBodyPart
            
		}
        
        msg.setContent(mp);

		//發送郵件
//		Transport transport = mailsess.getTransport("smtp"); //只支持IMAP、 SMTP和 POP3
//		transport.connect(host, username, password); //以smtp的方式登入mail server
        Logger.info("start to send mail ... email="+receiver);
        if(transport==null||!transport.isConnected())
        	mail_conn();
		transport.sendMessage(msg, msg.getAllRecipients());
//		transport.close();
		long endTime = System.currentTimeMillis();
		Logger.info("execute time=" + (endTime-startTime));
	}
	
	public static String today() {
		String d = "";		
		Date d1 = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		Calendar cal = Calendar.getInstance();
		d1 = cal.getTime();
		d = sdf.format(d1);
		return d;
	}
	
	private static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

	public static String now() {
		Calendar cal = Calendar.getInstance();
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
		cal.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));
		
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}
	
	//取得日期 eg: 20130801  (格式)
	public static String getDate(){
		//獲得時間  24小時制    hhmm
		Calendar cal = Calendar.getInstance(); //宣告Calendar物件變數 
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
		cal.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));
		
		String format = "yyyyMMdd";
		
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		 
		String date =sdf.format(cal.getTime());
		return date;
	}
	
	//取得時間 eg: 0801  只有小時與分鐘
	public static String getTime(){
		//獲得時間  24小時制    hhmm
		Calendar cal = Calendar.getInstance(); //宣告Calendar物件變數 
		int hh = cal.get(Calendar.HOUR_OF_DAY); //取得鐘點資訊(24小時制) 
		int mm = cal.get(Calendar.MINUTE); //取得分鐘資訊
		String hhstr=String.valueOf(hh);
		String mmstr=String.valueOf(mm);
		if (hh<10)
			hhstr=0+String.valueOf(hh);
		if(mm<10)
			mmstr=0+String.valueOf(mm);	
		String time = hhstr+mmstr;
		return time;
	}
	
	
	public static void genQRCode(String oaid, String userid, String teacherEmail) {
	
		//String url = Conf.domain + "/call/" + oaid + "/" + userid;
		String encodeURL = oaid + "," + userid + "," + teacherEmail;
		File file = QRCode.from(encodeURL).to(ImageType.PNG).file();
		
		try {
			FileInputStream is = new FileInputStream(file);
			String filename = oaid + "_" + userid;
			File f = new File(Play.getFile("").getAbsolutePath() + File.separator
					+ "public" + File.separator + "oa" + File.separator
					+ oaid + File.separator + "qrcode");
			if (!f.exists())
				f.mkdirs();
			String original = File.separator
					+ "public" + File.separator + "oa" + File.separator
					+ oaid + File.separator + "qrcode" + File.separator + filename + ".jpg";
			IOUtils.copy(is, new FileOutputStream(Play.getFile(original)));
		
		} catch (IOException ex) {
			ex.printStackTrace();
			System.out.println(" In generateQRCode ");
		}
	}
	
	public static void genVolunteerQRCode(String content) {
		
		File file = QRCode.from(content).to(ImageType.PNG).file();
		
		try {
			FileInputStream is = new FileInputStream(file);
			String filename = content;
			File f = new File(Play.getFile("").getAbsolutePath() + File.separator
					+ "public" + File.separator + "quanta" + File.separator
					+ "volunteer" + File.separator + "qrcode");
			if (!f.exists())
				f.mkdirs();
			String original = File.separator
					+ "public" + File.separator + "quanta" + File.separator
					+ "volunteer" + File.separator + "qrcode" + File.separator + filename + ".jpg";
			IOUtils.copy(is, new FileOutputStream(Play.getFile(original)));
		
		} catch (IOException ex) {
			ex.printStackTrace();
			System.out.println(" In generateQRCode ");
		}
	}
	
	public static void genShowQRCode(String content) {
		
		File file = QRCode.from(content).to(ImageType.PNG).file();
		
		try {
			FileInputStream is = new FileInputStream(file);
			String filename = content;
			File f = new File(Play.getFile("").getAbsolutePath() + File.separator
					+ "public" + File.separator + "quanta" + File.separator
					+ "showpiece" + File.separator + "qrcode" + File.separator +"show");
			if (!f.exists())
				f.mkdirs();
			String original = File.separator
					+ "public" + File.separator + "quanta" + File.separator
					+ "showpiece" + File.separator + "qrcode" + File.separator +"show" + File.separator + filename + ".jpg";
			IOUtils.copy(is, new FileOutputStream(Play.getFile(original)));
		
		} catch (IOException ex) {
			ex.printStackTrace();
			System.out.println(" In generateQRCode ");
		}
	}
	
	public static void genShowpieceQRCode(String content) {
		
		File file = QRCode.from(content).to(ImageType.PNG).file();
		
		try {
			FileInputStream is = new FileInputStream(file);
			String filename = content;
			File f = new File(Play.getFile("").getAbsolutePath() + File.separator
					+ "public" + File.separator + "quanta" + File.separator
					+ "showpiece" + File.separator + "qrcode" + File.separator +"showpiece");
			if (!f.exists())
				f.mkdirs();
			String original = File.separator
					+ "public" + File.separator + "quanta" + File.separator
					+ "showpiece" + File.separator + "qrcode" + File.separator +"showpiece" + File.separator + filename + ".jpg";
			IOUtils.copy(is, new FileOutputStream(Play.getFile(original)));
		
		} catch (IOException ex) {
			ex.printStackTrace();
			System.out.println(" In generateQRCode ");
		}
	}
	
	public static void genTeacherRCode(String content) {
		
		File file = QRCode.from(content).to(ImageType.PNG).file();
		
		try {
			FileInputStream is = new FileInputStream(file);
			String filename = content;
			File f = new File(Play.getFile("").getAbsolutePath() + File.separator
					+ "public" + File.separator + "quanta" + File.separator
					+ "teacher" + File.separator + "qrcode" );
			if (!f.exists())
				f.mkdirs();
			String original = File.separator
					+ "public" + File.separator + "quanta" + File.separator
					+ "teacher" + File.separator + "qrcode" + File.separator + filename + ".jpg";
			IOUtils.copy(is, new FileOutputStream(Play.getFile(original)));
		
		} catch (IOException ex) {
			ex.printStackTrace();
			System.out.println(" In generateQRCode ");
			
		}
	}
	


	public static String encryptString(String input) throws Exception {
		
		String v_encoding = (new sun.misc.BASE64Encoder()).encodeBuffer( input.getBytes("UTF8") );
		return v_encoding;
	}
	
	public static String decryptString(String input) throws Exception {
		
		byte[] v_unencoding = (new sun.misc.BASE64Decoder()).decodeBuffer( input );
		String vData = new String(v_unencoding,"UTF8");
		return vData;
	}
	
	
	public static String passwordEncrypt(String password) {
        
        
        SecretKey key = new SecretKeySpec(Conf.hashKey.getBytes(), Conf.algorithm);
        
        try {
            Mac m = Mac.getInstance(Conf.algorithm);
            m.init(key);
            m.update(password.getBytes());
            byte[] mac = m.doFinal();
            return toHexString(mac);
        }
        catch (Exception e) {
            System.out.println(e.toString());
        }
        
        return StringUtils.EMPTY;
    }
    
	
    private static String toHexString(byte[] in) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < in.length; i++){
            String hex = Integer.toHexString(0xFF & in[i]);
            if (hex.length() == 1){
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
	
	
	public static Photo getFrontCoverForTP(Long tpid){
		TeachingPlan tp = TeachingPlan.findById(tpid);
		Photo photo =null;
		Long id=null;
		Pattern p = Pattern.compile("photo,[0-9]+");
		if(tp!=null){
			//從學前找
			if(id==null && tp.tpBefore!=null && tp.tpBefore.orderNumber!=null && !tp.tpBefore.orderNumber.equals("")){
				String orderNumber = tp.tpBefore.orderNumber;
				Matcher m = p.matcher(orderNumber);
				if(m.find()){
					String componentStr = m.group(0);
					id = Long.parseLong(componentStr.substring(componentStr.indexOf(',')+1));
				}
			}
			//從學中找
			if(id==null && tp.tpNow!=null && tp.tpNow.orderNumber!=null && !tp.tpNow.orderNumber.equals("")){
				String orderNumber = tp.tpNow.orderNumber;
				Matcher m = p.matcher(orderNumber);
				if(m.find()){
					String componentStr = m.group(0);
					id = Long.parseLong(componentStr.substring(componentStr.indexOf(',')+1));
				}
			}
			//從學後找
			if(id==null && tp.tpAfter!=null && tp.tpAfter.orderNumber!=null && !tp.tpAfter.orderNumber.equals("")){
				String orderNumber = tp.tpAfter.orderNumber;
				Matcher m = p.matcher(orderNumber);
				if(m.find()){
					String componentStr = m.group(0);
					id = Long.parseLong(componentStr.substring(componentStr.indexOf(',')+1));
				}
			}
			
			if(id!=null){
				photo=Photo.findById(id);
			}
		}
		return photo;
	}
	
	public static List<Photo> getFrontCoverForOA(Long oaid){
		OutdoorActivity oa = OutdoorActivity.findById(oaid);
		List<Photo> photos = new ArrayList<Photo>();
		Long id=null;
		Pattern p = Pattern.compile("photo,[0-9]+");
		if(oa!=null){
			//從orderNumber中找
			if(id==null && oa.componentOrder!=null &&  !oa.componentOrder.equals("")){
				String orderNumber = oa.componentOrder;
				Matcher m = p.matcher(orderNumber);

				while(m.find()){
					String componentStr = m.group(0);
					id = Long.parseLong(componentStr.substring(componentStr.indexOf(',')+1));
					Photo photo=Photo.findById(id);
					if(photo!=null && !photos.contains(photo))
						photos.add(photo);
					//只取四張
					if(photos.size()==4)
						break;
				}
			}
		}
		return photos;
	}


	public static void ResetComponentIndexVar() {
		_orderNumber="";
		_html_index=0; _pic_index = 0 ; _youtube_index =0 ; _file_index=0 ; _web_index =0; _spot_index=0; _res_index=0; _ref_index  =0;
	}
	
	public static void SaveMapComponent(Long id ,String title, String POIName,String address, String lat, String lng, String tel,String openTime, String endTime, String intro , String share) {//
		
		Map map = null;
		if(id!=null)
			map=map.findById(id);
		else 
			map = new Map();
		if(map!=null){
			map.title = title;
			map.POIName = POIName;
			map.address = address ;
			map.lat = lat ;
			map.lng = lng ;
			map.phone = tel ;
			map.intro = intro ;
			map.startTime = openTime ;
			map.endTime = endTime ;
			map.share = (share.equals("yes")) ? true : false;
			map.save();

			_orderNumber += "map," + map.id + "##";
			_spot_index++;

			System.out.println("map ok!");
		}
	}

	public static void SaveTeacherComponent(Long id ,String title, String teacherName,List<String> grad, String intro, String filename, String share, TeachingPlan tp) {
		// find the file
		InputStream photo = findInputStreamFromUpload(filename);
		
		Teacher teacher  = null;
		if(id!=null)
			teacher=Teacher.findById(id);
		else
			teacher = new Teacher();
		if(teacher!=null){
			teacher.title=title;
			teacher.teacherName=teacherName;
			if(photo!=null){
				// 儲存大頭貼檔案
				String path = "/public/tp/" + tp.id + "/teacher/" + filename;
				File des = new File(Play.getFile("").getAbsolutePath()+ "/public/tp/" + tp.id + "/teacher");
				File output = new File(Play.getFile("").getAbsolutePath() + path);

				if (!des.exists())
					des.mkdirs();
				if (filename != "") {
					Photo p = new Photo();
					p.title = teacherName;
					p.intro = "老師簡歷大頭貼";
					p.share = (share.equals("yes")) ? true : false;
					p.fileName = filename;
					p.path = path;
					p.createTime=MyUtil.now();
					p.save();

					try {
						//FileUtils.copyFileToDirectory(photo, des, true);
						IOUtils.copy(photo, new FileOutputStream(output));
						photo.close();
						teacher.photo = p;
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			//學經歷
			String education = "";
			for (String edu : grad) {
				education = education + edu + "##";
			}
			teacher.education=education;
			teacher.introduction=intro;
			teacher.share = (share.equals("yes"))?true:false;
			teacher.createTime=MyUtil.now();
			teacher.save();
			
			_orderNumber += "teacher," + teacher.id + "##";
			_res_index ++;
			System.out.println("teacher ok!");
		}
	}
	
	public static void SaveTeacherComponent(Long id ,String title, String teacherName,List<String> grad, String intro, String filename, String share, OutdoorActivity oa) {
		// find the file
		InputStream photo = findInputStreamFromUpload(filename);
				
		Teacher teacher  = null;
		if(id!=null)
			teacher = Teacher.findById(id);
		else
			teacher = new Teacher();
		if (teacher != null) {
			teacher.title = title;
			teacher.teacherName = teacherName;
			// 儲存大頭貼檔案
			if(photo!=null){
				// 儲存大頭貼檔案
				String path = "/public/oa/" + oa.id + "/teacher/" + filename;
				File des = new File(Play.getFile("").getAbsolutePath()+ "/public/oa/" + oa.id + "/teacher");
				File output = new File(Play.getFile("").getAbsolutePath() + path);

				if (!des.exists())
					des.mkdirs();
				if (filename != "") {
					Photo p = new Photo();
					p.title = teacherName;
					p.intro = "老師簡歷大頭貼";
					p.share = (share.equals("yes")) ? true : false;
					p.fileName = filename;
					p.path = path;
					p.createTime=MyUtil.now();
					p.save();

					try {
						//FileUtils.copyFileToDirectory(photo, des, true);
						IOUtils.copy(photo, new FileOutputStream(output));
						photo.close();
						teacher.photo = p;
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}


			// 學經歷
			String education = "";
			for (String edu : grad) {
				education = education + edu + "##";
			}
			teacher.education = education;
			teacher.introduction = intro;
			teacher.share = (share.equals("yes")) ? true : false;
			teacher.createTime=MyUtil.now();
			teacher.save();

			_orderNumber += "teacher," + teacher.id + "##";
			_res_index++;
			System.out.println("teacher ok!");
		}
	}

	public static void SaveYoutubeComponent(Long id ,String title, String url,	String intro, String share) {
		Youtube y  =null;
		if(id!=null){
			y= Youtube.findById(id);
		}
		else {
			y=new Youtube();
		}
		y.title=title;
		y.intro=intro;
		y.url=url;
		y.share = (share.equals("yes"))?true:false;
		y.createTime=MyUtil.now();
		y.save();
		
		_orderNumber += "youtube," + y.id + "##";
		_youtube_index ++;	
		System.out.println("youtube ok!");
	}
	
	public static void SaveAttatchComponent(Long id ,String title, String filename , String intro,String share,TeachingPlan tp) {
		// find the file
		InputStream file = findInputStreamFromUpload(filename);
		
		Attatch p = null;
		if(id!=null){
			p = Attatch.findById(id);
		}
		else {
			p = new Attatch();
		}
		if (p != null) {
			// 編輯時有選取新檔案
			if (file != null) {
				// 儲存檔案
				String path = "/public/tp/" + tp.id + "/attatch/"+ filename;
				File des = new File(Play.getFile("").getAbsolutePath()+ "/public/tp/" + tp.id + "/attatch");
				File output = new File(Play.getFile("").getAbsolutePath() + path);
				if (!des.exists())
					des.mkdirs();
				if (filename != "") {

					p.title = title;
					p.intro = intro;
					p.share = (share.equals("yes")) ? true : false;
					p.fileName = filename;
					p.path = path;
					p.createTime=MyUtil.now();
					p.save();

					try {
						// 若有相同檔案名稱則刪除舊檔案
						//FileUtils.copyFileToDirectory(file, des, true);
						IOUtils.copy(file, new FileOutputStream(output));
						file.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			else {
				//只修改title , 說明 ,share
				p.title = title;
				p.intro = intro;
				p.share = (share.equals("yes")) ? true : false;
				p.save();
			}
			_orderNumber += "attatch," + p.id + "##";
			_file_index++;
			System.out.println("attatch ok!");
			
		}
		
	}
	
	public static void SaveAttatchComponent(Long id ,String title, String filename, String intro,String share,OutdoorActivity oa) {
		// find the file
		InputStream file = findInputStreamFromUpload(filename);
				
		Attatch p = null;
		if (id != null) {
			p = Attatch.findById(id);
		} else {
			p = new Attatch();
		}
		if (p != null) {
			// 編輯時有選取新檔案
			if (file != null) {
				// 儲存檔案
				String path = "/public/oa/" + oa.id + "/attatch/"+ filename;
				File des = new File(Play.getFile("").getAbsolutePath()+ "/public/oa/" + oa.id + "/attatch");
				File output = new File(Play.getFile("").getAbsolutePath() + path);
				if (!des.exists())
					des.mkdirs();
				if (filename != "") {

					p.title = title;
					p.intro = intro;
					p.share = (share.equals("yes")) ? true : false;
					p.fileName = filename;
					p.path = path;
					p.createTime=MyUtil.now();
					p.save();

					try {
						// 若有相同檔案名稱則刪除舊檔案
						//FileUtils.copyFileToDirectory(file, des, true);
						IOUtils.copy(file, new FileOutputStream(output));
						file.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				// 只修改title , 說明 ,share
				p.title = title;
				p.intro = intro;
				p.share = (share.equals("yes")) ? true : false;
				p.save();
			}
			_orderNumber += "attatch," + p.id + "##";
			_file_index++;
			System.out.println("attatch ok!");
		}
		
	}
	
	public static void SaveLinkComponent(Long id ,String title, String webName,String url,String intro, String share) {
		Link y = null;
		if (id!=null) {
			y= Link.findById(id);
		}
		else {
			y=new Link();
		}
		if (y != null) {
			y.title = title;
			y.linkName = webName;
			y.intro = intro;
			y.url = url;
			y.share = (share.equals("yes")) ? true : false;
			y.createTime=MyUtil.now();
			y.save();

			_orderNumber += "link," + y.id + "##";
			_web_index++;
			System.out.println("link ok!");
		}
	}

	public static void SaveTextComponent(Long id ,String title, String intro,String share) {
		Text t = null;
		if (id != null)
			t = Text.findById(id);
		else
			t = new Text();
		if (t != null) {
			t.title = title;
			t.intro = intro;
			t.share = (share.equals("yes")) ? true : false;
			t.createTime=MyUtil.now();
			t.save();
			_orderNumber += "text," + t.id + "##";
			_html_index++;
			System.out.println("text ok!");
		}
	
	}
	
	public static void SavePhotoComponent(Long id ,String title,  String intro, String filename,String share,  TeachingPlan tp) {
		
		Photo p = null;
		if (id != null) {
			p = Photo.findById(id);
		} else {
			p = new Photo();
		}
		if (p != null) {
			// find the file
			InputStream file = findInputStreamFromUpload(filename);
			if (file != null) {
				// 儲存檔案
				String path = "/public/tp/" + tp.id + "/photo/"+ filename;
				File des = new File(Play.getFile("").getAbsolutePath()+ "/public/tp/" + tp.id + "/photo");
				File output = new File(Play.getFile("").getAbsolutePath() + path);
				if (!des.exists())
					des.mkdirs();
				if (filename != "") {

					p.title = title;
					p.intro = intro;
					p.share = (share.equals("yes")) ? true : false;
					p.fileName = filename;
					p.path = path;
					p.createTime=MyUtil.now();
					p.save();

					try {
						//FileUtils.copyFileToDirectory(file, des, true);
						IOUtils.copy(file, new FileOutputStream(output));
						file.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				}
				/*
				// process 500 and 100 resize photos
				try {
					String path_500 = "/public/tp/" + tp.id + "/photo/"+ filename.split("[.]")[0] + "_500" + "." + filename.split("[.]")[1];
					String path_100 = "/public/tp/" + tp.id + "/photo/"+ filename.split("[.]")[0] + "_100" + "." + filename.split("[.]")[1];
					BufferedImage origin = ImageIO.read(output);
					BufferedImage img_500 = resizeImg(origin, 500, 500);
					BufferedImage img_100 = resizeImg(origin, 100, 100);
					writeImage(img_500, path_500, filename.split("[.]")[1]);
					writeImage(img_100, path_100, filename.split("[.]")[1]);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				*/
			} 
			else {
				p.title = title;
				p.intro = intro;
				p.share = (share.equals("yes")) ? true : false;

				p.save();
			}
			_orderNumber += "photo," + p.id + "##";
			_pic_index++;
		}

	}
	
	public static void SavePhotoComponent(Long id ,String title, String intro, String filename , String share, OutdoorActivity oa) {
		
		Photo p = null;
		if (id != null) {
			p = Photo.findById(id);
		} else {
			p = new Photo();
		}
		if (p != null) {
			// find the file
			InputStream file = findInputStreamFromUpload(filename);
			if (file != null) {
				// 儲存檔案
				String path = "/public/oa/" + oa.id + "/photo/"+ filename;
				File des = new File(Play.getFile("").getAbsolutePath()+ "/public/oa/" + oa.id + "/photo");
				File output = new File(Play.getFile("").getAbsolutePath() + path);
				if (!des.exists())
					des.mkdirs();
				if (filename != "") {

					p.title = title;
					p.intro = intro;
					p.share = (share.equals("yes")) ? true : false;
					p.fileName = filename;
					p.path = path;
					p.createTime=MyUtil.now();
					p.save();

					try {
						//FileUtils.copyFileToDirectory(file, des, true);
						IOUtils.copy(file, new FileOutputStream(output));
						file.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				/*
				// process 500 and 100 resize photos
				try {
					String path_500 = "/public/oa/" + oa.id + "/photo/"+ filename.split("[.]")[0] + "_500" + "." + filename.split("[.]")[1];
					String path_100 = "/public/oa/" + oa.id + "/photo/"+ filename.split("[.]")[0] + "_100" + "." + filename.split("[.]")[1];
					BufferedImage origin = ImageIO.read(output);
					BufferedImage img_500 = resizeImg(origin, 500, 500);
					BufferedImage img_100 = resizeImg(origin, 100, 100);
					writeImage(img_500, path_500, filename.split("[.]")[1]);
					writeImage(img_100, path_100, filename.split("[.]")[1]);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				*/
				
			}  else {
				p.title = title;
				p.intro = intro;
				p.share = (share.equals("yes")) ? true : false;

				p.save();
			}
			_orderNumber += "photo," + p.id + "##";
			_pic_index++;
		}
	}
	
	private static void writeImage(BufferedImage img, String to, String extName) {
		File file = new java.io.File(to.substring(0,to.lastIndexOf("/")));
	    if(!file.exists())
	      file.mkdir();
	    file = null ;
	    FileOutputStream newimage = null;
		try {
			newimage = new FileOutputStream(to);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	     
	    try {
			ImageIO.write(img, extName, newimage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	       if(newimage!=null)
	       {
	         try {
				newimage.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	         newimage = null ;
	       }
	} 
	
	private static BufferedImage resizeImg(BufferedImage src, double width, double hight) {
	      double factorX = 1.00;
	      double factorY = 1.00;
	      int w = src.getWidth();
	      double wD = Double.parseDouble(""+w);
	      int h = src.getHeight();
	      double hD = Double.parseDouble(""+h);
	      factorX = width / wD;
	      if (factorX>1) factorX=1;
	      factorY = hight / hD;
	      if (factorY>1) factorY=1;
	      if (factorX<factorY) {
	        factorY = factorX;
	      } else {
	        factorX = factorY;
	      }
	      int newW = (int)(Math.floor(factorX * w));
	      int newH = (int)(Math.floor(factorY * h));
	      Image temp = src.getScaledInstance(newW, newH, Image.SCALE_AREA_AVERAGING);
	      BufferedImage tgt = createBlankImage(src, newW, newH);
	      Graphics2D g = tgt.createGraphics();
	      g.drawImage(temp, 0, 0, null);
	      g.dispose();
	      return tgt;
	  }
	  
	  //Uses image to create another image of the same "type", but of a factored size
	  private static BufferedImage createBlankImage(BufferedImage src, int w, int h) {
	      int type = src.getType();
	      if (type != BufferedImage.TYPE_CUSTOM)
	          return new BufferedImage(w, h, type);
	      else {
	          ColorModel cm = src.getColorModel();
	          WritableRaster raster = src.getRaster().createCompatibleWritableRaster(w, h);
	          boolean isRasterPremultiplied = src.isAlphaPremultiplied();
	          return new BufferedImage(cm, raster, isRasterPremultiplied, null);
	      }
	  } 
	
	public static void SaveTeachingPlanCiteComponent(Long id ,String style, String ref_id,String ref_title , String ref_tpid, String intro) {
		Cite cite = null;
		if (id != null) {
			cite = Cite.findById(id);
		} else {
			cite = new Cite();
		}
		if (cite != null) {
			cite.style = style;
			cite.tid = Long.parseLong(ref_id);
			TeachingPlan tp = TeachingPlan.findById(Long.parseLong(ref_tpid));
			if (tp != null) {
				cite.citeTP = tp;
			}
			cite.citeTitle = ref_title;
			cite.intro = intro;
			cite.createTime=MyUtil.now();
			cite.save();

			_orderNumber += "cite," + cite.id + "##";
			_ref_index++;
			System.out.println("cite component ok!");
		}
		
	}
	
	public static void SaveOutdoorActivityCiteComponent(Long id ,String ref_title , String ref_tpid, String intro , OutdoorActivity oa) {
		OAIncludeTP cite = null;
		if (id != null) {
			cite = OAIncludeTP.findById(id);
		} else {
			cite = new OAIncludeTP();
		}
		if (cite != null) {
			TeachingPlan tp = TeachingPlan.findById(Long.parseLong(ref_tpid));
			if (tp != null) {
				cite.tp = tp;
			}
			cite.citeTitle = ref_title;
			cite.intro = intro;
			cite.createTime=MyUtil.now();
			cite.save();

			if (oa != null && !oa.tps.contains(cite)) {
				oa.tps.add(cite);
				oa.save();
			}

			_orderNumber += "cite," + cite.id + "##";
			_ref_index++;
			System.out.println("cite tp ok!");
		}
		
	}
	

	public static File findFileFromUpload(String filename){
		// find the file
		File file = null;
		List<Upload> uploads = (List<Upload>) play.mvc.Http.Request.current().args.get("__UPLOADS");
		for (Upload myUpload : uploads) {
			if (filename.equals(myUpload.getFileName())) {
				file = myUpload.asFile();
			}
		}
		return file;
	}
	
	public static InputStream findInputStreamFromUpload(String filename){
		// find the file
		InputStream file = null;
		List<Upload> uploads = (List<Upload>) play.mvc.Http.Request.current().args.get("__UPLOADS");
		for (Upload myUpload : uploads) {
			if (filename.equals(myUpload.getFileName())) {
				file = myUpload.asStream();
			}
		}
		return file;
	}

	public static int dateDiff(Date d1, Date d2) {
		Calendar calendar1 = Calendar.getInstance();
	    calendar1.setTime(d1);
	    Calendar calendar2 = Calendar.getInstance();
	    calendar2.setTime(d2);
	    long diffInMillis = calendar2.getTimeInMillis() - calendar1.getTimeInMillis();
	    return (int) (diffInMillis / (24* 1000 * 60 * 60) + 1);
	}
	
	public static String youtubeSrcParser(String url) {
		Pattern p = Pattern.compile("http.*\\?v=([a-zA-Z0-9_\\-]+)(?:&.)*");
		Matcher m = p.matcher(url);
		 
		if (m.matches()) {
			url = m.group(1);
		}
		 
		return url;
	}
	
	public static String replaceTextAreaToHtml(String str) {
		if(str!=null)
			str = str.replaceAll("\r","<br>");
		return str;
	}
	
	public static String ranPwd() {
		return String.valueOf((int)(10000000 * Math.random()+1));
	}

	
	public static void ResetQuestionsIndexVar() {
		_orderNumber="";
		_ox_index=0;
		_singleAnswer_index=0;
		_shortAnswer_index=0;
	}
	
	public static void SaveOXQuestion(Member member ,Long id, String questionStr, String filename, int answer, String illustration, int score) {
		Examine examine = Examine.findById(_examid);
		Question question = null;
		if(id!=null)
			question = Question.findById(id);
		else
			question = new Question();
		if(question!=null){
			question.creator=member;
			question.style=0;
			question.title=questionStr;
			question.answer = answer;
			question.illustration=illustration;
			question.score=score;
			InputStream file = findInputStreamFromUpload(filename);
			if(filename!=null && !filename.equals("")){
				// 儲存檔案
				String path = "/public/examine/" + examine.id +"/"+ filename;
				File des = new File(Play.getFile("").getAbsolutePath()+ "/public/examine/" + examine.id);
				File output = new File(Play.getFile("").getAbsolutePath() + path);
				if (!des.exists())
					des.mkdirs();
				try {
					question.attatchPath = path;
					IOUtils.copy(file, new FileOutputStream(output));
					file.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			question.save();
			examine.questions.add(question);
			_orderNumber+=question.id+"##";
			examine.orderNumber=_orderNumber;
			examine.save();
			_ox_index++;
			System.out.println("OX ok");
		}
	}

	public static void SaveSingleAnswerQusetion(Member member , Long id, String questionStr,String filename, 
			String optionA, String optionB, String optionC,String optionD, int answer, String illustration, int score) {
		Examine examine = Examine.findById(_examid);
		Question question = null;
		if(id!=null)
			question = Question.findById(id);
		else
			question = new Question();
		if(question!=null){
			question.creator=member;
			question.style=1;
			question.title=questionStr;
			question.optionA=optionA;
			question.optionB=optionB;
			question.optionC=optionC;
			question.optionD=optionD;
			question.answer = answer;
			question.illustration=illustration;
			question.score=score;
			InputStream file = findInputStreamFromUpload(filename);
			if(filename!=null && !filename.equals("")){
				// 儲存檔案
				String path = "/public/examine/" + examine.id +"/"+ filename;
				File des = new File(Play.getFile("").getAbsolutePath()+ "/public/examine/" + examine.id);
				File output = new File(Play.getFile("").getAbsolutePath() + path);
				if (!des.exists())
					des.mkdirs();
				try {
					question.attatchPath = path;
					IOUtils.copy(file, new FileOutputStream(output));
					file.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			question.save();
			examine.questions.add(question);
			_orderNumber+=question.id+"##";
			examine.orderNumber=_orderNumber;
			examine.save();
			_singleAnswer_index++;
			System.out.println("singleAnswer ok");
		}
	}

	public static void SaveShortAnswerQuestion(Member member , Long id, String questionStr,	String filename, String illustration) {
		Examine examine = Examine.findById(_examid);
		Question question = null;
		if(id!=null)
			question = Question.findById(id);
		else
			question = new Question();
		if(question!=null){
			question.creator=member;
			question.style=2;
			question.title=questionStr;
			question.illustration=illustration;
			InputStream file = findInputStreamFromUpload(filename);
			if(filename!=null && !filename.equals("")){
				// 儲存檔案
				String path = "/public/examine/" + examine.id +"/"+ filename;
				File des = new File(Play.getFile("").getAbsolutePath()+ "/public/examine/" + examine.id);
				File output = new File(Play.getFile("").getAbsolutePath() + path);
				if (!des.exists())
					des.mkdirs();
				try {
					question.attatchPath = path;
					IOUtils.copy(file, new FileOutputStream(output));
					file.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			question.save();
			examine.questions.add(question);
			_orderNumber+=question.id+"##";
			examine.orderNumber=_orderNumber;
			examine.save();
			_shortAnswer_index++;
			System.out.println("shortAnswer ok");
		}
		
	}
	
	public static String buildSlideOutlineContent(String[] components , boolean isTeachingPlan) {
		//大綱 (10個元件標題為一頁 , 至多3頁)
		StringBuilder builder = new StringBuilder();
		if(components.length>20){
			for(int i=1 ; i<4 ; i++) {	
				builder.append(buildSlideOnePageOutline(components,i,isTeachingPlan));	
			}	
		}
		else if(components.length>10 && components.length<=20){
			for(int i=1 ; i<3 ; i++) {	
				builder.append(buildSlideOnePageOutline(components,i,isTeachingPlan));	
			}	
		}
		else if(components.length>0 && components.length<=10){
			for(int i=1 ; i<2 ; i++) {	
				builder.append(buildSlideOnePageOutline(components,i,isTeachingPlan));	
			}	
		}
		return builder.toString();
	}
	
	private static String buildSlideOnePageOutline(String[] components, int pageIndex , boolean isTeachingPlan) {
		StringBuilder builder = new StringBuilder();
		builder.append("<slide>");
		builder.append("<hgroup  class='auto-fadein'><h2>內容大綱</h2></hgroup>");
		builder.append("<article>");
		builder.append("<ul>");
		int startIndex=0;
		int endIndex=components.length;
		if(pageIndex==1)
			endIndex=(10>components.length)?components.length:10;
		else if (pageIndex==2) {
			startIndex=10;
			endIndex=(20>components.length)?components.length:20;
		}else if(pageIndex==3)
			startIndex=20;
		
		for(int i=startIndex ; i<endIndex ; i++) {	
			builder.append("<li>");
			String type = components[i].split(",")[0];
			Long cid = Long.parseLong(components[i].split(",")[1]);
			if(type.equals("cite")){
				if(isTeachingPlan){
					Cite component =Cite.findById(cid);
					if(component!=null)
						builder.append(component.citeTitle);
				}
				else {
					OAIncludeTP component = OAIncludeTP.findById(cid);
					if(component!=null)
						builder.append(component.citeTitle);	
				}
			}
			else 
				builder.append(AjaxManager.getComponentName(type, cid.toString()));	
			builder.append("</li>");
		}
		builder.append("</ul>");
		builder.append(" </article>");
		builder.append("</slide>");
		return builder.toString();
	}

	public static String buildSlideDetailContent(String type, Long id , boolean isTeachingPlan) {
		StringBuilder builder = new StringBuilder();
		try {
			builder.append("<slide>");
			if(type.equals("map")) {				
				Map component = Map.findById(id);
				if(component!=null){
					component.intro=replaceTextAreaToHtml(component.intro);
					builder.append("<hgroup><h2>" + component.title +"</h2></hgroup>");
					builder.append("<article>");
					builder.append("<li><small><a style='color:#0000FF;text-align:left' href='https://maps.google.com/maps?q="+component.lat+","+component.lng+"'>顯示詳細地圖</a></small></li><br>");
					builder.append("<li><b>地址 ： " + component.address + "</b></li><br>");
					builder.append("<li><b>電話 ： " + component.phone + "</b></li><br>");
					builder.append("<li><b>說明 ： " + component.intro + "</b></li>");
					builder.append("</article>");
				}					
			} else if(type.equals("youtube")) {
				Youtube component = Youtube.findById(id);
				if(component!=null){
					component.intro=replaceTextAreaToHtml(component.intro);
					//取出?v= 後面的序號
					//http://www.youtube.com/watch?v=jMK1mKIzqlc 須轉為
					//http://www.youtube.com/v/jMK1mKIzqlc
					component.url=component.url.replace("watch?", "");
					component.url=component.url.replace("=", "/");
					
					builder.append("<hgroup><h2>" + component.title +"</h2></hgroup>");
					builder.append("<article>");
					builder.append("<li><b>說明 ： " + component.intro + "</b></li>");
					builder.append("<embed src='"+component.url+"' type='application/x-shockwave-flash' wmode='transparent' width='700' height='480'></embed>");
					builder.append("</article>");
					
				}
			} else if(type.equals("text")) {
				Text component = Text.findById(id);
				if(component!=null){
					//process html tag
					String noHTMLString = component.intro;
					Pattern pattern = Pattern.compile("</?[a-z][a-z0-9]*[^<>]*>");
				    Matcher matcher = pattern.matcher(noHTMLString);
				    while (matcher.find()) {
				    	String componentStr = matcher.group(0);
				    	noHTMLString = noHTMLString.replace(componentStr, "");
					}
				    pattern = Pattern.compile("<([A-Z][A-Z0-9]*)[^>]*>(.*?)</\1>");
				    matcher = pattern.matcher(noHTMLString);
				    while (matcher.find()) {
				    	String componentStr = matcher.group(0);
				    	noHTMLString = noHTMLString.replace(componentStr, "");
					}
					
				    builder.append("<hgroup><h2>" + component.title +"</h2></hgroup>");
					builder.append("<article>");
					builder.append("<li><b>說明 ： " + noHTMLString + "</b></li>");
					builder.append("</article>");
				}
			} else if (type.equals("teacher")) {
				Teacher component = Teacher.findById(id);
				if(component!=null){
					component.introduction=replaceTextAreaToHtml(component.introduction);
					builder.append("<hgroup><h2>" + component.title +"</h2></hgroup>");
					builder.append("<article>");
					builder.append("<li><b>照片 ：</b></li><br>");
					if(component.photo!=null)
						builder.append("<img style='width: 150px;height:150px;' src='"+component.photo.path+"'>");
					builder.append("<li><b>姓名 ： " + component.teacherName + "</b></li><br>");
					builder.append("<li><b>說明 ： " + component.introduction + "</b></li>");
					builder.append("</article>");
				}
			} else if (type.equals("attatch")) {
				Attatch component = Attatch.findById(id);
				if(component!=null){
					component.intro=replaceTextAreaToHtml(component.intro);
					builder.append("<hgroup><h2>" + component.title +"</h2></hgroup>");
					builder.append("<article>");
					builder.append("<li><a href='" + component.path+ "'>檔案下載</a></li><br>");
					builder.append("<li><b>說明 ： " + component.intro + "</b></li>");
					builder.append("</article>");
				}
			} else if(type.equals("cite")) {
				if(isTeachingPlan){
					Cite component = Cite.findById(id);
					if(component!=null){
						component.intro=replaceTextAreaToHtml(component.intro);
						builder.append("<hgroup><h2>" + component.citeTitle +"</h2></hgroup>");
						builder.append("<article>");
						builder.append("<li><b>引用自 <a href='/member/profile/"+component.citeTP.creator.id+"'>"+component.citeTP.creator.username+"</a> 老師的  <a href='/tp/show/"+component.citeTP.id+"'>"+component.citeTP.name+"</a> 教案</b></li><br>");
						//builder.append(buildSlideDetailContent(component.style, component.tid, true));
						builder.append("<li><b>補充說明 ： " + component.intro + "</b></li>");
						builder.append("</article>");
					}	
				}
				else {
					OAIncludeTP component = OAIncludeTP.findById(id);
					if(component!=null){
						component.intro=replaceTextAreaToHtml(component.intro);
						builder.append("<hgroup><h2>" + component.citeTitle +"</h2></hgroup>");
						builder.append("<article>");
						builder.append("<li><b>引用自 <a href='/member/profile/"+component.tp.creator.id+"'>"+component.tp.creator.username+"</a> 老師的  <a href='/tp/show/"+component.tp.id+"'>"+component.tp.name+"</a> 教案</b></li><br>");
						builder.append("<li><b>教案說明 ： " + component.tp.introduction + "</b></li><br>");
						builder.append("<li><b>補充說明 ： " + component.intro + "</b></li>");
						builder.append("</article>");
					}	
				}
					
			} else if(type.equals("link")) {
				Link component = Link.findById(id);
				if(component!=null){
					builder.append("<hgroup><h2>" + component.title +"</h2></hgroup>");
					builder.append("<article>");
					builder.append("<li><a href='" + component.url+ "'>前往連結</a></li><br>");
					builder.append("<li><b>說明 ： " + component.intro + "</b></li>");
					builder.append("</article>");
				}
			} else if (type.equals("photo")) {
				Photo component = Photo.findById(id);
				if(component!=null){
					builder.append("<hgroup><h2>" + component.title +"</h2></hgroup>");
					builder.append("<article class='flexbox vcenter'>");
					builder.append("<footer>" + component.intro + "</footer>");
					builder.append("<div><img style='max-width: 700px; max-height: 520px;' src='"+component.path+"'></div>");
					builder.append("</article>");
				}
			}
			builder.append("</slide>");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return builder.toString();
	}

	
}
