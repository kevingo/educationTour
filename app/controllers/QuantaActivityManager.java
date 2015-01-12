package controllers;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bouncycastle.jce.provider.JCEMac.SHA1;
import org.json.simple.JSONObject;

import com.google.gson.JsonObject;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.sun.org.apache.bcel.internal.generic.Select;

import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.Restrict;

import Utility.Conf;
import Utility.MyUtil;
import Utility.ROLE;

import java.awt.Checkbox;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.MalformedURLException;
//import java.nio.file.Files;


import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.management.relation.Role;


import flexjson.JSONSerializer;
import models.Apply;
import models.Attatch;
import models.Member;
import models.Photo;
import models.Q_Show;
import models.Q_Showpiece;
import models.Q_ShowpieceHistoryNode;
import models.Q_ShowpieceVersion;
import models.Q_TeacherApply;
import models.Q_TeacherSeesion;
import models.Q_VolunteerApply;
import models.Q_VolunteerGroup;
import models.Q_VolunteerSeesion;
import play.Logger;
import play.Play;
import play.data.Upload;
import play.data.validation.Required;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;
import play.mvc.results.RenderText;
import sun.applet.resources.MsgAppletViewer;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import sun.security.provider.SHA;

@With(Deadbolt.class)
public class QuantaActivityManager extends Controller {
	
	@Before
	public static void getTag() {
	//	Application.getTag();	   
	}
	
	public static class  AppShowpiece{
		public Long id;
		public String status;
	}
	
	public static void login(@Required String email, @Required String password ) throws Exception {	
	
		Member member = Member.findUser(email);
		
		if(member!=null && member.password.equals(password)) {
			Logger.info("Quatan login success, user=" + member.email);
			session.put("email", email);			
			admin_login(null);
		}
		else {
			Logger.info("Quanta login failed. Email=" + email+" Password="+password);
			String failedMsg = "登入失敗,帳號密碼錯誤";
			render("quanta/login.html", failedMsg);
		}
	}

	//admin link to manage volunteer , showpiece and teacher .html
	@Restrict(ROLE.Q_USER)
	public static void admin_volunteer(){
		
		render("/quanta/admin/volunteer/volunteer.html");
	}
	@Restrict(ROLE.Q_USER)
	public static void admin_showpiece(){
		render("/quanta/admin/showpiece/showpiece.html");
	}
	@Restrict(ROLE.Q_USER)
	public static void admin_teacher(){
		render("/quanta/admin/teacher/teacher.html");
	}
	
	public static void admin_login(String msg) throws Exception {
		
		Member member = MyUtil.getCurrentUser();
		if(member!=null) {
			
			Logger.info("go into had login view by "+member.id);
			String message=null;
			if(msg!=null){
				message = MyUtil.decryptString(msg);
			}
			if(member.roleList.contains(ROLE.Q_ADMIN) || member.roleList.contains(ROLE.ADMIN))
				render("quanta/admin/index.html",message);
			else if(member.roleList.contains(ROLE.Q_VOLUNTEER))
				render("quanta/volunteer/index.html",message);
			else if(member.roleList.contains(ROLE.Q_SHOWPIECE))
				render("quanta/showpiece/index.html",message);
			else if(member.roleList.contains(ROLE.Q_TEACHER))
				render("quanta/teacher/index.html",message);
		}
		else {
			render("quanta/login.html");
		}
		
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_listVolunteerSession(){
		List<Q_VolunteerSeesion> listSessions = Q_VolunteerSeesion.findAll();
		//反轉list (將最新的場次放在最前面)
		Collections.reverse(listSessions);
		render("/quanta/admin/volunteer/listVolunteerSession.html",listSessions);
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_listVolunteer(){
		List<Member> listMembers = Member.findMemberByRole(ROLE.Q_VOLUNTEER);
		render("/quanta/admin/volunteer/listVolunteer.html",listMembers);
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_deleteVolunteer(@Required Long m_id){	
		
		Member member = Member.findById(m_id);
		if(member!=null){
			
			//找出與他相關的FK的欄位  (包含 群組,參加的場次 apply)
			Q_VolunteerGroup group = Q_VolunteerGroup.findGroupByMember(m_id);
			//若從member找不到 則再用creator去找group
			List<Q_VolunteerGroup> gs = Q_VolunteerGroup.findGroupByCreator(m_id);
			if(group==null) {
				if(gs!=null)
					group = gs.get(0);
			}
			else {
				//刪除此creator 其他多餘沒用的group
				if(gs!=null){
					for (Q_VolunteerGroup g : gs) {
						if(!g.equals(group))
							g.delete();
					}
				}
			}
			
			if(group!=null){
				group.members.remove(member);
				group.save();
				//若此群組的creator剛好是他，則換掉creator 若無人可換 則刪除此group
				if(group.members.size()<2){
					group.delete();
				}else {
					for (Member user : group.members) {
						if(user!=member){
							group.creator=user;
							break;
						}	
					}
					group.save();
				}
				
			}
			
			List<Q_VolunteerApply> lisApply = Q_VolunteerApply.findApplyByUser(m_id);
			for (Q_VolunteerApply apply : lisApply) {
				apply.delete();
			}
			Logger.info("Quatan Admin Delete Quanta member success, user=" + member.username);
			member.delete();
			renderText("success"); 
		}
		else {
			Logger.info("Quatan Admin Delete Quanta member fail.");
			renderText("fail"); 
		}
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_newVolunteer(){
		render("/quanta/admin/volunteer/newVolunteer.html");
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_checkVolunteer(@Required String email){
		Member member = Member.findUser(email);
		if(member!=null){
			renderText("exist"); 
		}
		else {
			renderText("notExist"); 
		}
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_saveVolunteer(@Required String email ,@Required String pwd ,@Required String confirmpwd ,@Required String username ,@Required int age  ){
		if(Member.findUser(email)==null){
			Member member = new Member(email, pwd, ROLE.Q_VOLUNTEER, true, username);
			if(age==0)
				member.isAdult=false;
			member.save();
			Logger.info("Quatan Admin create Quanta member success, user=" + member.username);
		}
		admin_listVolunteer();
	}
	
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_deleteSchool(@Required Long m_id){	
		Member member = Member.findById(m_id);
		if(member!=null){
			
			//找出與他相關的FK的欄位  (包含 ...)
			
			
			Logger.info("Quatan Admin Delete Quanta member success, user=" + member.username);
			member.delete();
			renderText("success"); 
		}
		else {
			Logger.info("Quatan Admin Delete Quanta member fail.");
			renderText("fail"); 
		}
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_disableSchool(@Required Long m_id){
	
		
		Member member = Member.findById(m_id);
		if(member!=null){
			
			//找出與他相關的FK的欄位  (包含 ...)
			
			
			Logger.info("Quatan Admin Disable Quanta member success, user=" + member.username);
			member.enable=false;
			member.save();
			renderText("success"); 
		}
		else {
			Logger.info("Quatan Admin Disable Quanta member fail.");
			renderText("fail"); 
		}
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_enableSchool(@Required Long m_id){	
		Member member = Member.findByIdAndDisable(m_id);
		if(member!=null){
			
			//找出與他相關的FK的欄位  (包含 ...)
			
			
			Logger.info("Quatan Admin Enable Quanta member success, user=" + member.username);
			member.enable=true;
			member.save();
			renderText("success"); 
		}
		else {
			Logger.info("Quatan Admin Enable Quanta member fail.");
			renderText("fail"); 
		}
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_newSchool(){
		render("/quanta/admin/showpiece/newSchool.html");
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_checkSchool(@Required String email){
		Member member = Member.findUser(email);
		if(member!=null){
			renderText("exist"); 
		}
		else {
			renderText("notExist"); 
		}
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_saveSchool(@Required String email ,@Required String pwd ,@Required String confirmpwd ,@Required String schoolname){
		if(Member.findUser(email)==null){
			Member member = new Member(email, pwd, ROLE.Q_SHOWPIECE, true, schoolname);
			member.save();
			Logger.info("Quatan Admin create Quanta member success, user=" + member.username);
		}
		admin_listSchool();
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_queryShowpiece(@Required Long id){
		Q_Showpiece showpiece = Q_Showpiece.findById(id);
		List<Q_ShowpieceHistoryNode> listHistoryNodes = new ArrayList<Q_ShowpieceHistoryNode>();
		if(showpiece!=null){
			//依據接收時間排序
			for (Q_ShowpieceHistoryNode node : showpiece.historyNodes) {
				listHistoryNodes.add(node);
			}
			Collections.sort(listHistoryNodes,new Comparator<Q_ShowpieceHistoryNode>() {
	            public int compare(Q_ShowpieceHistoryNode o1, Q_ShowpieceHistoryNode o2) {
	                return o1.recieveTime.compareTo(o2.recieveTime);
	            }
	        });		
		}
		render("/quanta/admin/showpiece/queryShowpiece.html",listHistoryNodes,showpiece);
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_deleteShowpiece(@Required Long id){
		Logger.info("delete showpiece:"+id);
		Q_Showpiece showpiece = Q_Showpiece.findById(id);
		if(showpiece!=null){
			
			//找出與他相關的FK的欄位  (包含  history node)
			//先找出node後刪除，否則在DB中會有很多無歸屬的node
			
			List<Q_ShowpieceHistoryNode> listNode = new ArrayList<Q_ShowpieceHistoryNode>();
			for (Q_ShowpieceHistoryNode node : showpiece.historyNodes) {
				listNode.add(node);
			}
			
			if(listNode.size()>0){
				showpiece.historyNodes.clear();
				showpiece.save();
				for (Q_ShowpieceHistoryNode node : listNode) {
					node.delete();
				}
			}
			
			
			
			Logger.info("Quatan Admin Delete Quanta showpiece success, showpiece=" + showpiece.showpieceName);
			showpiece.delete();
			renderText("success"); 
		}
		else {
			Logger.info("Quatan Admin Delete Quanta showpiece fail.");
			renderText("fail"); 
		}
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_deleteShow(@Required Long id) throws IOException{

		Q_Show show = Q_Show.findById(id);
		if(show!=null){
			
			//找出與他相關的FK的欄位刪除(包含  展覽下的showpiece)
			List<Q_Showpiece> listShowpiece = Q_Showpiece.findShowpieceByShow(show.id);
			
			for (Q_Showpiece showpiece : listShowpiece) {
				//刪除所有的showpiece
				List<Q_ShowpieceHistoryNode> listNode = new ArrayList<Q_ShowpieceHistoryNode>();
				for (Q_ShowpieceHistoryNode node : showpiece.historyNodes) {
					listNode.add(node);
				}
				
				if(listNode.size()>0){
					showpiece.historyNodes.clear();
					showpiece.save();
					for (Q_ShowpieceHistoryNode node : listNode) {
						node.delete();
					}
				}
				showpiece.delete();
			}
			
			
			if(Q_Showpiece.findShowpieceByShow(show.id).size()==0){
				Logger.info("Quatan Admin Delete Quanta show success, show=" + show.showName);
				show.delete();
				renderText("success"); 
			}
			else {
				Logger.info("Quatan Admin Delete Quanta show fail.");
				renderText("fail! There are still some showpieces which can not delete. "); 
			}
		}
		else {
			Logger.info("Quatan Admin Delete Quanta show fail.");
			renderText("fail"); 
		}
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_getShowpiece(@Required Long id){
		Q_Showpiece showpiece = Q_Showpiece.findById(id);
		render("/quanta/admin/showpiece/editShowpiece.html",showpiece);
			
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_editShowpiece(@Required Long id , String showpieceName , String status , String information){
		Q_Showpiece showpiece = Q_Showpiece.findById(id);
		System.out.println(status);
		if(showpiece!=null){
			showpiece.showpieceName = showpieceName;
			showpiece.status = status;
			showpiece.information = information;
			showpiece.save();
			admin_listShowpiece(showpiece.show.id);
		}	
	}

	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_listShow(){
		List<Q_Show> listShows = Q_Show.findAll();
		render("/quanta/admin/showpiece/listShow.html",listShows);
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_listSchool(){

		List<Member> listMembers = Member.findMemberByRole(ROLE.Q_SHOWPIECE);
		render("/quanta/admin/showpiece/listSchool.html",listMembers);
	}
	
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_listShowpiece(@Required Long show_id){
		List<Q_Showpiece> listShowpieces = Q_Showpiece.findShowpieceByShow(show_id);
		render("/quanta/admin/showpiece/listShowpiece.html",listShowpieces);
	}
	
	//for volunteer  login API on app    
	public static void volunteer_login(@Required String username, @Required String password) {

		Member user = Member.login(username, password);
	
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		Collection<JSONObject> relationMember = new ArrayList<JSONObject>();
		
		if(null != user) {
			response.put("result", "true");
			response.put("userid", user.id);
			//搜尋是否有親屬
			Q_VolunteerGroup group = Q_VolunteerGroup.findGroupByMember(user.id);
			if(group!=null){
				//加入親屬成員
				for (Member member : group.members) {
					//排除自己
					if(!member.equals(user)){
						JSONObject m = new JSONObject();
						m.put("id",member.id);
						m.put("name",member.username);
						relationMember.add(m);
					}
				}
			}
			response.put("members", relationMember);
			Logger.info("Quanta volunteer login success : "+user.email );
		} else {			
			response.put("result", "false");
			response.put("reason", "auth failed");
			Logger.info("Quanta volunteer login fail : username="+username+" password="+password );
		}
		
		json.put("response", response);
		JSONSerializer serializer = new JSONSerializer();
		serializer.deepSerialize(json);
		String output = serializer.deepSerialize(json);
		
		renderJSON(output);
	}
	
	
	//for volunteer check-in and check-out API on app
	public static void volunteer_check(@Required Long[] arrayMember, @Required String serialNumber,@Required String date,@Required String action,@Required String time ) {
		
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		Q_VolunteerSeesion session = Q_VolunteerSeesion.findSessionByserialNumber(serialNumber);
		String message = "";
		//app也會傳時間近來，但先以Server取得的時間為主
		time=MyUtil.getTime();
		
		if(session==null)
			message="QRCode錯誤，沒有登錄這場志工服務!";
		else {
			if(!session.date.substring(0,8).equals(MyUtil.getDate()))
				message="這場志工服務日期已過或尚未開始!";
			else if(action.equals("checkin"))
			{
				for (Long user_id : arrayMember) {
					Member user = Member.findById(user_id);
					Q_VolunteerApply apply = Q_VolunteerApply.findApplyBySessionAndUser(session.id,user.id);
					if(apply!=null && apply.checkinTime==null){
						apply.checkinTime = time;
						apply.save();
					}
					else {
						message=message+user.username+" 沒有報名參加該場志工服務或已經簽到過了!\n";
					}
				}
			}
			else if(action.equals("checkout")){
				for (Long user_id : arrayMember) {
					Member user = Member.findById(user_id);
					Q_VolunteerApply apply = Q_VolunteerApply.findApplyBySessionAndUser(session.id,user.id);
					if(apply!=null && apply.checkinTime!=null){
						if(apply.checkoutTime==null){
							apply.checkoutTime = time;
							apply.totalTime=calcularTotalHour(apply.checkinTime,time);
							apply.save();
						}
						else {
							message=message+user.username+" 已經簽退過了!無法再次簽退\n";
						}
					}
					else {
						message=message+user.username+" 沒有報名參加該場志工服務或尚未簽到!\n";
					}
				}
			}
		}

		//return json
		if(message==""){
			response.put("result", "true");
			Logger.info("Quanta volunteer " +action+ " success " );
		}
		else {
			response.put("result", "false");
			response.put("reason", message);
			Logger.info("Quanta volunteer " + action+" fail : "+message );
		}
		
		json.put("response", response);
		JSONSerializer serializer = new JSONSerializer();
		serializer.deepSerialize(json);
		String output = serializer.deepSerialize(json);
		renderJSON(output);
	}
	
	//計算志工服務的總時數，以小時計算
	private static String calcularTotalHour(String checkin,String checkout) {

		int checkinMin = Integer.parseInt(checkin.substring(0, 2))*60 + Integer.parseInt(checkin.substring(2, 4)) ;
		int checkoutMin = Integer.parseInt(checkout.substring(0, 2))*60 + Integer.parseInt(checkout.substring(2, 4)) ;
		int totalMin = checkoutMin-checkinMin;

		float totaltime = (float)totalMin/60;
		
		NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits( 2 );    //小數後兩位
		return String.valueOf(nf.format(totaltime));
	}
	
	//for admin upload account and session on web
	@Restrict(ROLE.Q_ADMIN)
	public static void volunteer_uploadSession(@Required File[] files) {
		String message="";
		List<Upload> uploadfiles = (List<Upload>) request.args.get("__UPLOADS");
		for(Upload myUpload : uploadfiles) {
			String path = "/public/quanta/volunteer/"+ myUpload.getFileName();
			File des = new File(Play.getFile("").getAbsolutePath() + "/public/quanta/volunteer/");				
			File output = new File(Play.getFile("").getAbsolutePath() + path);
			if(!des.exists())
				des.mkdirs();
			if (myUpload.getFileName() != "") {
				
				InputStream is = myUpload.asStream();
				try {
					IOUtils.copy(is, new FileOutputStream(output));
					is.close();						
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//parse file to MySQL
				InputStreamReader fr = null;
		        BufferedReader br = null;
		        try {
		        	MyUtil.removeUTF8FileBOM(Play.getFile("").getAbsolutePath() + path); 
		            fr = new InputStreamReader(new FileInputStream(Play.getFile("").getAbsolutePath() + path));
		            br = new BufferedReader(fr);
		            String rec = null;
		            String[] argsArr = null;
		            while ((rec = br.readLine()) != null) {
		                argsArr = rec.split(",");
		                if(argsArr.length>1){
		                	//檢查是否重複
		                	if(Q_VolunteerSeesion.findByNameAndDate(argsArr[0],argsArr[1])==null){
				                //產生不重複的隨機序號
				                String serialNumber = MyUtil.GenerateVolunteerSerialNumber();
				                Q_VolunteerSeesion session = new Q_VolunteerSeesion(argsArr[0],argsArr[1],serialNumber); 
				                //sessionName, date, serialNumber 
				                session.save();
				                MyUtil.genVolunteerQRCode(session.serialNumber);
		                	}
		                }
		                else {
		                	message="上傳檔案格式有誤，請依序填入'場次名稱','日期(eg:20130101)'";
		            		render("/quanta/admin/volunteer/volunteer.html",message);
						}
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		        } finally {
		            try {
		                if ( fr != null )
		                    fr.close();
		                if ( br != null )
		                    br.close();
		            } catch(IOException ex){
		                ex.printStackTrace();
		            }
		        }
		       
			}
		}
		if(message==""){
			message="志工服務場次上傳完成!";
			Logger.info("Quanta volunteer upload session : "+message );
		}
		
		render("/quanta/admin/volunteer/volunteer.html",message);		
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void volunteer_uploadAccount(@Required File[] files) {
		String message = "";
		List<Upload> uploadfiles = (List<Upload>) request.args.get("__UPLOADS");
		for(Upload myUpload : uploadfiles){
			String path = "/public/quanta/volunteer/"+ myUpload.getFileName();
			File des = new File(Play.getFile("").getAbsolutePath() + "/public/quanta/volunteer/");				

			File output = new File(Play.getFile("").getAbsolutePath() + path);
			if(!des.exists())
				des.mkdirs();
			if (myUpload.getFileName() != "") {
				
				InputStream is = myUpload.asStream();
				try {
					IOUtils.copy(is, new FileOutputStream(output));
					is.close();						
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//parse file to MySQL
				InputStreamReader fr = null;
		        BufferedReader br = null;
		        try {
		        	MyUtil.removeUTF8FileBOM(Play.getFile("").getAbsolutePath() + path); 
		        	//親子的群組
                	HashMap<String, List<Member>> hashMap = new HashMap<String, List<Member>>();
		            fr = new InputStreamReader(new FileInputStream(Play.getFile("").getAbsolutePath() + path));
		            br = new BufferedReader(fr);
		            String rec = null;
		            String[] argsArr = null;
		            while ((rec = br.readLine()) != null) {
		                argsArr = rec.split(",");
		                //文青(upload: email,password,username)
		                if(argsArr.length==3){
		                	//檢查是否重複
		                	Member member=Member.findUser(argsArr[0]);
		                	if(member==null){	                		
				                member = new Member(argsArr[0], argsArr[1],ROLE.Q_VOLUNTEER,true,argsArr[2]);
				                //email , pwd , rolename , isQuantaUser ,username 
				                member.save();
		                	}
		                	else {
		                		member.password=argsArr[1];
		                		member.username=argsArr[2];
		                		member.save();
							}
		                }
		                //親子(upload:email,password,username,groupName,大/小)
		                else if(argsArr.length==5){
		                	//檢查是否重複
		                	Member member=Member.findUser(argsArr[0]);
		                	
		                	if(member==null){	                		
				                member = new Member(argsArr[0], argsArr[1],ROLE.Q_VOLUNTEER,true,argsArr[2]);
				                //email , pwd , rolename , isQuantaUser ,username 
				                //判斷大人或小孩 (中文會有亂碼因此改為用hashcode來判斷)
				                if(argsArr[4].hashCode()=="大".hashCode())
				                	member.isAdult=true;
				                else if(argsArr[4].hashCode()=="小".hashCode())
				                	member.isAdult=false;
				                else {
				                	message=argsArr[0]+" 上傳檔案格式有誤，依序填入'Email','密碼','姓名','群組名稱','大/小'(填寫'大'或'小')";
				            		render("/quanta/admin/volunteer/volunteer.html",message);
								}
				                member.save();
		                	}
		                	else {

		                		member.password=argsArr[1];
		                		member.username=argsArr[2];
		                		//判斷大人或小孩 (中文會有亂碼因此改為用hashcode來判斷)
				                if(argsArr[4].hashCode()=="大".hashCode())
				                	member.isAdult=true;
				                else if(argsArr[4].hashCode()=="小".hashCode())
				                	member.isAdult=false;
				                else {
				                	message=argsArr[0]+" 上傳檔案格式有誤，依序填入'Email','密碼','姓名','群組名稱','大/小'(填寫'大'或'小')";
				            		render("/quanta/admin/volunteer/volunteer.html",message);
								}
		                		member.save();
		                		
		                		//表示此member原先就存在 則先將原本的group清除
		                		//找出與他相關的FK的欄位  (包含 群組,參加的場次 apply)
		            			Q_VolunteerGroup group = Q_VolunteerGroup.findGroupByMember(member.id);
		            			//若從member找不到 則再用creator去找group
		            			if(group==null) {
		            				List<Q_VolunteerGroup> gs = Q_VolunteerGroup.findGroupByCreator(member.id);
		            				if(gs!=null)
		            					group = gs.get(0);
		            			}
		            			
		            			if(group!=null){
		            				group.members.remove(member);
		            				if(group.creator==member ){
		            					if(group.members.size()>2){
		            						//要移除Creator 且有人可以替換
		            						group.creator = group.members.iterator().next();
		            						group.save();
		            					}
		            					else {
		            						//要移除Creator 且無人可以替換(刪除後只剩1個人) 則刪除group
		            						group.delete();
										}
		            				}
		            			}
		                		
							}
		                	//親子的群組HashMap建立   
		                	//先搜尋是否已有此key  若有則加入   若無則新建
		                	if(hashMap.containsKey(argsArr[3])){
		                		 hashMap.get(argsArr[3]).add(member);
		                	}
		                	else {
		                		List<Member> list =new ArrayList<Member>();
		                		list.add(member);
								hashMap.put(argsArr[3], list);
							}

		                }
		                else {
		                	message=argsArr[0]+" 上傳檔案格式有誤，請依序填入'Email','密碼',''姓名' 或  依序填入'Email','密碼',''姓名','群組名稱','大/小'";
		            		render("/quanta/admin/volunteer/volunteer.html",message);
						}
		                
		            }
		            
		            //將親子的hashMap 存進DB
		            if(hashMap.size()>0){
		            	for(String currentKey : hashMap.keySet()){
		            		//同一群組
		            		//先建立Group 再依序加入其member
		            		Q_VolunteerGroup group = new Q_VolunteerGroup();
		            		group.createDate=MyUtil.now();
		            		group.groupName=currentKey;
		            		group.active=true;
		            		
		            		for (Member member : hashMap.get(currentKey)) {
		            			//若該member已在另外一個群組，則清除掉原先的group
		            			Q_VolunteerGroup g =Q_VolunteerGroup.findGroupByMember(member.id);
		            			if(g!=null){
		            				g.members.remove(member);
		            				g.save();
		            			}
								group.members.add(member);
								if(group.creator==null && member.isAdult)
									group.creator=member;
							}
		            		group.save();
		            	}
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		        }finally{
		            try{
		                if ( fr != null )
		                    fr.close();
		                if ( br != null )
		                    br.close();
		            }catch(IOException ex){
		                ex.printStackTrace();
		            }
		        }
		        
			}
		}
		if(message==""){
			message="志工帳號上傳完成!";
			Logger.info("Quanta volunteer upload account : "+message );
		}
			
		render("/quanta/admin/volunteer/volunteer.html",message);
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void volunteer_uploadApply(@Required File[] files ,@Required Long sid){
		
		String msg="";
        Q_VolunteerSeesion vs = Q_VolunteerSeesion.findById(sid);
		List<Upload> uploadfiles = (List<Upload>) request.args.get("__UPLOADS");
		for(Upload myUpload : uploadfiles){
			String path = "/public/quanta/volunteer/"+ myUpload.getFileName();
			File des = new File(Play.getFile("").getAbsolutePath() + "/public/quanta/volunteer/");				

			File output = new File(Play.getFile("").getAbsolutePath() + path);
			if(!des.exists())
				des.mkdirs();
			if (myUpload.getFileName() != "") {
				
				InputStream is = myUpload.asStream();
				try {
					IOUtils.copy(is, new FileOutputStream(output));
					is.close();						
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//parse file to MySQL
				InputStreamReader fr = null;
		        BufferedReader br = null;
		        try {
		        	MyUtil.removeUTF8FileBOM(Play.getFile("").getAbsolutePath() + path);  
		            fr = new InputStreamReader(new FileInputStream(Play.getFile("").getAbsolutePath() + path));
		            br = new BufferedReader(fr);
		            String rec = null;
		            String[] argsArr = null;
		            while ((rec = br.readLine()) != null) {
		                argsArr = rec.split(",");
		                Member member = Member.findUser(argsArr[0]);
		               
		                if(vs!=null){
			                if(member==null){
			                	msg=msg+argsArr[0]+" 無此帳號,\n";
			                }
			                else {
			                	//先檢查是否已重複
			                	if(Q_VolunteerApply.findApplyBySessionAndUser(vs.id, member.id)==null){
			                		Q_VolunteerApply apply = new Q_VolunteerApply();
									apply.member=member;
									apply.session=vs;
									apply.save();
			                	}			                		
			                	else {
			                		msg=msg+argsArr[0]+",此帳號在此服務地點已經存在\n";
								}
								
							}
		                }
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		        }finally{
		            try{
		                if ( fr != null )
		                    fr.close();
		                if ( br != null )
		                    br.close();
		            }catch(IOException ex){
		                ex.printStackTrace();
		            }
		        }
		        
			}
		}
		if(msg==""){
			msg="上傳完成!";
			Logger.info("Quanta volunteer upload apply : "+msg );
		}

		List<Q_VolunteerApply> listApplies = Q_VolunteerApply.findApplyBySession(vs.id);
		render("/quanta/admin/volunteer/showSession.html",listApplies,vs,msg);
		
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void volunteer_exportCSV(@Required Long s_id){

		try{
			//建立檔案
			String path = Play.getFile("").getAbsolutePath() + File.separator
					+ "public" + File.separator + "quanta" + File.separator
					+ "volunteer"+ File.separator +   "export.csv";
			File file = new File(path);  //logFile 是路径，就叫做String logFile = 生成路径+想要的文件名+".csv";
		    FileOutputStream out = new FileOutputStream(file);
		    OutputStreamWriter osw = new OutputStreamWriter(out, "BIG5");

		     BufferedWriter bw = new BufferedWriter(osw);
		     //然后就随便你自己去构造了，我写个第一行表示表头，然后第二行起循环
		     
		     bw.write("服務活動名稱" + "," + "日期" + "," + "帳號" + "," + "姓名" + ","+ "報到時間" + "," + "離開時間" +"," + "服務總時數(小時)" + "\r\n");//请注意，CSV默认是已逗号","分隔单元格的。这里是表头
		     
		     List<Q_VolunteerApply> listApplies = Q_VolunteerApply.findApplyBySession(s_id);
		     Logger.info("apply size="+listApplies);
		     boolean firstTag =true;
		     for (Q_VolunteerApply apply : listApplies) {
		    	 //只列出有完成志工服務者
		    	 if(apply.totalTime!=null && !apply.totalTime.equals("")){
		    		 if(firstTag){
		    			 bw.write(apply.session.sessionName + "," + apply.session.date + "," + apply.member.email+ "," + apply.member.username + "," + apply.checkinTime + "," + apply.checkoutTime + "," + apply.totalTime +"\r\n");
		    			 firstTag=false;
		    		 }
		    		 else {
		    			 bw.write(",," + apply.member.email+ "," + apply.member.username + "," + apply.checkinTime + "," + apply.checkoutTime + "," + apply.totalTime +"\r\n");
					}
		    	 }
		    }
		     
		     bw.close();
		     osw.close();
		     out.close();

		     renderText("ok"); 
		}
		catch (Exception e) {
			renderText(e.toString()); 
		}
		
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void volunteer_show(@Required Long s_id){
		
		List<Q_VolunteerApply> listApplies = Q_VolunteerApply.findApplyBySession(s_id);
		Q_VolunteerSeesion vs = Q_VolunteerSeesion.findById(s_id);
		render("/quanta/admin/volunteer/showSession.html",listApplies,vs);
	}
	
	//for volunteer link to page[*.html] (check-in, check-out and query personal history) on web
	@Restrict(ROLE.Q_VOLUNTEER)
	public static void volunteer_webcheckin(){
		//搜尋是否有親子群組關係
		String email = session.get("email");
		Member member = Member.findUser(email);
		List<Member> listMembers = new ArrayList<Member>(); 
		Q_VolunteerGroup group = Q_VolunteerGroup.findGroupByMember(member.id);
		if(group!=null){
			for (Member m : group.members) {
				if(!listMembers.contains(m))
					listMembers.add(m);
			}
		}
		render("/quanta/volunteer/checkin.html",listMembers);
	}
	
	@Restrict(ROLE.Q_VOLUNTEER)
	public static void volunteer_webcheckout(){
		//搜尋是否有親子群組關係
		String email = session.get("email");
		Member member = Member.findUser(email);
		List<Member> listMembers = new ArrayList<Member>(); 
		Q_VolunteerGroup group = Q_VolunteerGroup.findGroupByMember(member.id);
		if(group!=null){
			for (Member m : group.members) {
				if(!listMembers.contains(m))
					listMembers.add(m);
			}
		}
		render("/quanta/volunteer/checkout.html",listMembers);
	}
	
	@Restrict(ROLE.Q_VOLUNTEER)
	public static void volunteer_webquery(@Required String email ){
		//list all personal history
		Member user = Member.findUser(email);
		List<Q_VolunteerApply> listApplys =null;
		//僅限查詢自己的帳號
		if(user!=null&&user.email.equals(session.get("email"))){
			 listApplys = Q_VolunteerApply.findApplyByUser(user.id);		
		}
		render("/quanta/volunteer/myVolunteer.html",listApplys);
	}
	
	//for volunteer check-in on web (submit)
	@Restrict(ROLE.Q_VOLUNTEER)
	public static void volunteer_checkin(@Required String serialNumber ,String[] memberCheckbox) throws Exception {
			
		String time = MyUtil.getTime();
		
		Member user = MyUtil.getCurrentUser();
		Q_VolunteerSeesion session = Q_VolunteerSeesion.findSessionByserialNumber(serialNumber);
		String message="";
		
		if(session==null){
			message="驗證碼錯誤，請重新輸入!";
			admin_login(MyUtil.encryptString(message));
		}

		if(!session.date.substring(0, 8).equals(MyUtil.getDate())){
			message="這場志工服務日期已過或尚未開始!";
			admin_login(MyUtil.encryptString(message));
		}

		//先為親屬報到在為自己報到
		if(memberCheckbox!=null&&memberCheckbox.length>0){
			for (String mail : memberCheckbox) {
				Member familyMember = Member.findUser(mail);
				Q_VolunteerApply familyApply = Q_VolunteerApply.findApplyBySessionAndUser(session.id, familyMember.id);
				if(familyApply!=null){
					if(familyApply.checkinTime==null){
						familyApply.checkinTime=time;
						familyApply.save();
						message=message+familyMember.username+" 完成報到!\n";
					}
					else {
						message=message+familyMember.username+" 已經報到過了!\n";
					}
				}
				else {
					message=message+familyMember.username+" 沒有報名參與這場志工服務!\n";
				}
			}	
		}
		
		//自己
		Q_VolunteerApply apply = Q_VolunteerApply.findApplyBySessionAndUser(session.id, user.id);
		if(apply!=null){
				if(apply.checkinTime!=null){
					message=message+user.username+" 已經報到過了!\n";
				}
				else {
					apply.checkinTime = time;
					apply.save();	
					message=message+user.username+" 完成報到!\n";
				}		
		}
		else {
			message=message+user.username+" 沒有報名參與這場志工服務!\n";	
		}

		admin_login(MyUtil.encryptString(message));
	}

	@Restrict(ROLE.Q_VOLUNTEER)
	public static void volunteer_checkout(@Required String serialNumber,String[] memberCheckbox) throws Exception{
		String email = session.get("email");
		Member user = Member.findUser(email);
		Q_VolunteerSeesion session = Q_VolunteerSeesion.findSessionByserialNumber(serialNumber);
		String message="";
		String time = MyUtil.getTime();
		
		if(session==null){
			message="驗證碼錯誤，請重新輸入!";
			admin_login(MyUtil.encryptString(message));
		}
		
		//先為親屬簽退在為自己簽退
		if(memberCheckbox!=null&&memberCheckbox.length>0){
			for (String mail : memberCheckbox) {
				Member familyMember = Member.findUser(mail);
				Q_VolunteerApply familyApply = Q_VolunteerApply.findApplyBySessionAndUser(session.id, familyMember.id);
				if(familyApply!=null){
					if(familyApply.checkinTime==null){
						message=message+familyMember.username+" 沒有報到紀錄，因此無法簽退!\n";
					}
					else {
						if(familyApply.checkoutTime!=null){
							//已簽退過
							message=message+familyMember.username+" 已經簽退過了，因此無法再次簽退!\n";
						}else {
							familyApply.checkoutTime=time;
					        familyApply.totalTime = calcularTotalHour(familyApply.checkinTime, familyApply.checkoutTime);
					        familyApply.save();
							
							//session的visit +1
							session.visit=session.visit+1;
							session.save();
							message=message+familyMember.username+"簽退完成!\n";
						}
					}
				}
				else {
					message=message+familyMember.username+" 沒有報名參與這場志工服務!\n";
				}
			}
		}
		
		//自己
		Q_VolunteerApply apply = Q_VolunteerApply.findApplyBySessionAndUser(session.id, user.id);
		if(apply!=null){
			if(apply.checkinTime==null){
				message=message+user.username+" 沒有報到紀錄，因此無法簽退!\n";
			}
			else {
				if(apply.checkoutTime!=null){
					//已簽退過
					message=message+user.username+" 已經簽退過了，因此無法再次簽退!\n";
				}else {
					apply.checkoutTime=time;
					apply.totalTime = calcularTotalHour(apply.checkinTime, apply.checkoutTime);
					apply.save();
					
					//session的visit +1
					session.visit=session.visit+1;
					session.save();
					message=message+user.username+"簽退完成!\n";
				}
			}
		}
		else {
			message=message+user.username+" 沒有報名參與這場志工服務!\n";
		}
		
		admin_login(MyUtil.encryptString(message));
	}
	
	
	//for showpiece  login API on app    
	public static void showpiece_login(@Required String username, @Required String password) {
		Logger.info("Quatan app login , user=" + username+ " , password="+password);
		Logger.info("Quanta showpiece login.");
		Member user = Member.login(username, password);
				
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		
		if(null != user) {
			response.put("result", "true");
			response.put("userid", user.id);
		} else if(null == user) {			
			response.put("result", "false");
			response.put("reason", "auth failed");
		}
		
		json.put("response", response);
		JSONSerializer serializer = new JSONSerializer();
		serializer.deepSerialize(json);
		String output = serializer.deepSerialize(json);
		renderJSON(output);
	}
	
	//for Showpiece upload account and show on web
	@Restrict(ROLE.Q_ADMIN)
	public static void showpiece_uploadShow(@Required File[] files) {
		String message="";
		List<Upload> uploadfiles = (List<Upload>) request.args.get("__UPLOADS");
		for(Upload myUpload : uploadfiles){
			String path = "/public/quanta/showpiece/"+ myUpload.getFileName();
			File des = new File(Play.getFile("").getAbsolutePath() + "/public/quanta/showpiece/");				
			//System.out.println("path = " + path);
			File output = new File(Play.getFile("").getAbsolutePath() + path);
			if(!des.exists())
				des.mkdirs();
			if (myUpload.getFileName() != "") {
				
				InputStream is = myUpload.asStream();
				try {
					IOUtils.copy(is, new FileOutputStream(output));
					is.close();						
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//parse file to MySQL
				InputStreamReader fr = null;
		        BufferedReader br = null;
		        try {
		        	MyUtil.removeUTF8FileBOM(Play.getFile("").getAbsolutePath() + path); 
		            fr = new InputStreamReader(new FileInputStream(Play.getFile("").getAbsolutePath() + path));
		            br = new BufferedReader(fr);
		            String rec = null;
		            String[] argsArr = null;
		            
		            while ((rec = br.readLine()) != null) {
		                argsArr = rec.split(",");
		                if(argsArr.length>3){
		                	//檢查是否重複
		                	if(Q_Showpiece.findShowpieceByShowAndShowpiece(argsArr[1],argsArr[0])==null){	
				                //產生不重複的隨機序號
				                String serialNumber = MyUtil.GenerateShowpieceSerialNumber();
				                //建立該筆資料
				                Q_Showpiece showpiece = new Q_Showpiece(argsArr[1],argsArr[0],serialNumber,argsArr[2],argsArr[3]); 
				                Logger.info(showpiece.show.showName+"--"+showpiece.show.showName.hashCode());
				                //showName, showpieceName, serialNumber  , status, information
				                showpiece.save();
				                //建立展品的QRCode
				                MyUtil.genShowpieceQRCode(serialNumber);
		                	}
		                }
		                else {
		                	message="上傳檔案格式有誤，請依序填入'展覽名稱','展品名稱','狀態(正常,損壞)','補充說明'";
		            		render("/quanta/admin/showpiece/showpiece.html",message);
						}
		            }
		            // 新增版號 (+0.1)
		            Q_ShowpieceVersion showpieceVersion = Q_ShowpieceVersion.findVersionByName("q_showpiece");
					double version = Double.parseDouble(showpieceVersion.version)+(double)0.1;
					DecimalFormat df = new DecimalFormat("#.#");   
					showpieceVersion.version=String.valueOf(df.format(version));
					showpieceVersion.time=MyUtil.now();
					showpieceVersion.save();
		            
		        } catch (IOException e) {
		            e.printStackTrace();
		        }finally{
		            try{
		                if ( fr != null )
		                    fr.close();
		                if ( br != null )
		                    br.close();
		            }catch(IOException ex){
		                ex.printStackTrace();
		            }
		        }
			}
		}
		if(message=="")
			message="上傳展品資料完成";
		render("/quanta/admin/showpiece/showpiece.html",message);
		
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void showpiece_uploadAccount(@Required File[] files) {
		String message="";
		List<Upload> uploadfiles = (List<Upload>) request.args.get("__UPLOADS");
		for(Upload myUpload : uploadfiles){
			String path = "/public/quanta/showpiece/"+ myUpload.getFileName();
			File des = new File(Play.getFile("").getAbsolutePath() + "/public/quanta/showpiece/");				
			//System.out.println("path = " + path);
			File output = new File(Play.getFile("").getAbsolutePath() + path);
			if(!des.exists())
				des.mkdirs();
			if (myUpload.getFileName() != "") {
				
				InputStream is = myUpload.asStream();
				try {
					IOUtils.copy(is, new FileOutputStream(output));
					is.close();						
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//parse file to MySQL
				InputStreamReader fr = null;
		        BufferedReader br = null;
		        try {
		        	MyUtil.removeUTF8FileBOM(Play.getFile("").getAbsolutePath() + path); 
		            fr = new InputStreamReader(new FileInputStream(Play.getFile("").getAbsolutePath() + path));
		            br = new BufferedReader(fr);
		            String rec = null;
		            String[] argsArr = null;
		            while ((rec = br.readLine()) != null) {
		                argsArr = rec.split(",");
		                if(argsArr.length>2){
		                	//檢查是否重複
		                	Member member = Member.findUser(argsArr[0]);
		                	if(member==null){	
				                member = new Member(argsArr[0], argsArr[1],ROLE.Q_SHOWPIECE,true ,argsArr[2]);
				                //email , password , roleName , isQuantaUser , schoolName
				                member.save();
		                	}
		                	else {
								member.password=argsArr[1];
								member.schoolName=argsArr[2];
								member.save();
							}
		                }
		                else {
		                	message="上傳檔案格式有誤，請依序填入'email','password','schoolName'";
		            		render("/quanta/admin/showpiece/showpiece.html",message);
						}
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		        }finally{
		            try{
		                if ( fr != null )
		                    fr.close();
		                if ( br != null )
		                    br.close();
		            }catch(IOException ex){
		                ex.printStackTrace();
		            }
		        }

			}
		}
		if(message=="")
			message="游於藝帳號上傳完成!";
		render("/quanta/admin/showpiece/showpiece.html",message);
	}

	//for showpiece scan show serialNumber  API 
	public static void showpiece_scanShow(@Required String serialNumber) {

		List<Q_Showpiece> listShowpieces = Q_Showpiece.findShowpieceByShowSerialNumber(serialNumber);
				
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		Collection<JSONObject> items = new ArrayList<JSONObject>();
		
		if(null != listShowpieces) {
			response.put("result", "true");
		
			for (Q_Showpiece Showpiece : listShowpieces) {
				JSONObject item = new JSONObject();
				item.put("showpiece", Showpiece.showpieceName);
			    items.add(item);
			}

			response.put("list", items);
		} else {			
			response.put("result", "false");
			response.put("reason", "The show serial number is not active.Please enter \"Sync\" to update the database.");
		}
		
		json.put("response", response);
		JSONSerializer serializer = new JSONSerializer();
		serializer.deepSerialize(json);
		String output = serializer.deepSerialize(json);
		
		renderJSON(output);
	}
	
	public static void showpiece_sync(@Required String show,@Required String showpiece) {
		Q_ShowpieceVersion showVersion = Q_ShowpieceVersion.findVersionByName("q_show");
		Q_ShowpieceVersion showpieceVersion = Q_ShowpieceVersion.findVersionByName("q_showpiece");
		
		Boolean showIsOld = true;
		Boolean showpieceIsOld = true;
		
		if(showVersion.version.equals(show))
			showIsOld=false;
		if(showpieceVersion.version.equals(showpiece))
			showpieceIsOld=false;
		
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		

		if(showIsOld || showpieceIsOld){
			//有任何一個需要更新
			response.put("result", "false");
			//version
			JSONObject version = new JSONObject();
			version.put("show",showVersion.version);
			version.put("showpiece", showpieceVersion.version);
			response.put("version", version);
			
			//showList content if need
			if(showIsOld){
				Collection<JSONObject> items = new ArrayList<JSONObject>();
				List<Q_Show> listShows = Q_Show.findAll();
				if(null != listShows) {
					for (Q_Show Show : listShows) {
						JSONObject item = new JSONObject();
						item.put("id", Show.id);
						item.put("showName", Show.showName);
						item.put("serialNumber", Show.serialNumber);
						item.put("year", Show.year);
					    items.add(item);
					}
					response.put("showList", items);
				}	
			}
			
			//showpieceList content if need
			if(showpieceIsOld){
				Collection<JSONObject> items = new ArrayList<JSONObject>();
				List<Q_Showpiece> listShowpieces = Q_Showpiece.findAll();
				if(null != listShowpieces) {
					for (Q_Showpiece Showpiece : listShowpieces) {
						JSONObject item = new JSONObject();
						item.put("id", Showpiece.id);
						item.put("show_id", Showpiece.show.id);
						item.put("showpieceName", Showpiece.showpieceName);
						item.put("serialNumber", Showpiece.serialNumber);
						item.put("information", Showpiece.information);
						item.put("status", Showpiece.status);
					    items.add(item);
					}
					response.put("showpieceList", items);
				}	
			}
		}
		else {
			//都已經是最新版
			response.put("result", "true");
		}
		
		json.put("response", response);
		JSONSerializer serializer = new JSONSerializer();
		serializer.deepSerialize(json);
		String output = serializer.deepSerialize(json);
		
		renderJSON(output);
	}
	
	public static void showpiece_upload(@Required String[] arrayShowpiece,@Required String showId, @Required Long userId, @Required String username, @Required String phone, @Required String role) {
		
		//將收到的array資料轉換成list 且轉成自訂class AppShowpiece
		List<AppShowpiece> listShowpiece = new ArrayList<AppShowpiece>();
		for (String showpiece : arrayShowpiece) {
			AppShowpiece node = new AppShowpiece();
			String id= showpiece.substring(0, showpiece.indexOf("#"));
			String status = showpiece.substring(showpiece.indexOf("#")+1);
			if(status.equals("normal")){
				status="正常";
			}
			else if (status.equals("damage")) {
				status="損壞";	
			}else if (status.equals("lose")) {
				status="遺失";
			}
			node.id=(Long)Long.parseLong(id);
			node.status=status;
			
			listShowpiece.add(node);	
			
		}
		
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		
		if(listShowpiece.size()<1){
			response.put("result", "false");
			response.put("reason", "展品數量至少一個以上");
			json.put("response", response);
			JSONSerializer serializer = new JSONSerializer();
			serializer.deepSerialize(json);
			String output = serializer.deepSerialize(json);
			
			renderJSON(output);
			return;
		}
		
		try{			
			//先找到member 並更新他的電話姓名
			Member member = Member.findById(userId);
			// 更新展品資訊
			for (AppShowpiece appShowpiece : listShowpiece) {
				
				Q_Showpiece showpiece = Q_Showpiece.findById(appShowpiece.id);
				//檢查role 是否正確 (判斷的依據是 當reciever已經是該member時 就表示已經上船過了 或 選錯腳色)
				showpiece.status=appShowpiece.status;			
				if(role.equals("reciever")){
					
					if(showpiece.reciever.equals(member)){
						throw new Exception("此次上傳的資料已經登錄過了或應該選擇送出方");
					}	
					showpiece.reciever=member;
					
					//加入 historyNode (歷程記錄)
					Q_ShowpieceHistoryNode node = new Q_ShowpieceHistoryNode();
					node.member=member;
					node.recieveTime=MyUtil.now();
					node.recieveStatus=showpiece.status;
					node.save();
					showpiece.historyNodes.add(node);	
				}
				else {
					if(!showpiece.reciever.equals(member) || showpiece.sender.equals(member)){
						throw new Exception("此次上傳的資料已經登錄過了 或 應該選擇接收方");
					}
					showpiece.sender=member;
					
					//加入 historyNode (歷程記錄) 必須先找到接收拾建立的資料 ，
					Q_ShowpieceHistoryNode node = Q_ShowpieceHistoryNode.findByMemberAndShowpiece(member.id,showpiece.id);
					Logger.info("member.id="+member.id+",showpiece.id="+showpiece.id+",node.id="+node.id);
					if(node!=null){
						node.sendTime=MyUtil.now();
						node.sendStatus=showpiece.status;
						node.save();
					}
					
				}
				showpiece.save();
			}
			
			member.phone=phone;
			member.username=username;
			member.save();
			response.put("result", "true");
		}
		
		catch (Exception e) {
			response.put("result", "false");
			response.put("reason", e.toString());
		}
		
		finally{
			json.put("response", response);
			JSONSerializer serializer = new JSONSerializer();
			serializer.deepSerialize(json);
			String output = serializer.deepSerialize(json);
			
			renderJSON(output);
		}	
	}
	
	@Restrict(ROLE.Q_SHOWPIECE)
	public static void showpiece_webListShowpiece(@Required String serialNumber,@Required String role) {
		
		String message="";
		Q_Show show = Q_Show.findShowBySerialNumber(serialNumber);
		if(show==null){
			message="\""+serialNumber+"\" 此序號驗證失敗，請輸入正確序號";
			render("/quanta/showpiece/index.html",message);
		}
		
		List<Q_Showpiece> listShowpieces = Q_Showpiece.findShowpieceByShow(show.id);
		message = "請選取已確認的展品";
		int showpieceSize = listShowpieces.size();

		render("/quanta/showpiece/check.html",listShowpieces,role,message,showpieceSize);
		
		
	}
	@Restrict(ROLE.Q_SHOWPIECE)
	public static void showpiece_webCheckShowpiece(@Required String[] comfirmedCheckbox,@Required String[] statuses,@Required String role) {
		Member member =Member.findUser(session.get("email"));
		String message="";
		
		if(member!=null){

			for(int i=0; i<comfirmedCheckbox.length ; i++){
				String id = comfirmedCheckbox[i];
				String status = statuses[i];
				Q_Showpiece showpiece = Q_Showpiece.findById(Long.parseLong(id));
				
				showpiece.status=status;
				
				if(role.equals("接收展品")){
					showpiece.reciever = member;
					
					//加入 historyNode (歷程記錄)
					Q_ShowpieceHistoryNode node = new Q_ShowpieceHistoryNode();
					node.member=member;
					node.recieveTime=MyUtil.now();
					node.save();
					showpiece.historyNodes.add(node);	
				}
				else {
					showpiece.sender=member;
					//加入 historyNode (歷程記錄) 必須先找到接收拾建立的資料 ，
					Q_ShowpieceHistoryNode node = Q_ShowpieceHistoryNode.findByMemberAndShowpiece(member.id,showpiece.id);
					if(node!=null){
						node.sendTime=MyUtil.now();
						node.save();
					}
					else {
						message = message+ "您沒有 展品: "+showpiece.showpieceName+" 的接收紀錄\n";
					}
				}			
				showpiece.save();
			}
			if(message=="")
				message = "完成登記";
			render("/quanta/showpiece/index.html",message);
		}
		
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void teacher_uploadAccount(@Required File[] files) {
		String message="";
		List<Upload> uploadfiles = (List<Upload>) request.args.get("__UPLOADS");
		for(Upload myUpload : uploadfiles){
			String path = "/public/quanta/teacher/"+ myUpload.getFileName();
			File des = new File(Play.getFile("").getAbsolutePath() + "/public/quanta/teacher/");				
			//System.out.println("path = " + path);
			File output = new File(Play.getFile("").getAbsolutePath() + path);
			if(!des.exists())
				des.mkdirs();
			if (myUpload.getFileName() != "") {
				
				InputStream is = myUpload.asStream();
				try {
					IOUtils.copy(is, new FileOutputStream(output));
					is.close();						
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//parse file to MySQL
				InputStreamReader fr = null;
		        BufferedReader br = null;
		        try {
		        	MyUtil.removeUTF8FileBOM(Play.getFile("").getAbsolutePath() + path); 
		            fr = new InputStreamReader(new FileInputStream(Play.getFile("").getAbsolutePath() + path));
		            br = new BufferedReader(fr);
		            String rec = null;
		            String[] argsArr = null;
		            while ((rec = br.readLine()) != null) {
		                argsArr = rec.split(",");
		                if(argsArr.length>3){
		                	//檢查是否重複
		                	Member member = Member.findUser(argsArr[0]);
		                	if(member==null){	
				                member = new Member(argsArr[0], argsArr[1],ROLE.Q_TEACHER,true ,argsArr[2]);
				                //email , password , roleName , isQuantaUser , userName , phoneNumber
				                member.phone=argsArr[3];
				                member.save();
		                	}
		                	else {
								member.password=argsArr[1];
								member.username=argsArr[2];
								member.phone=argsArr[3];
								member.save();
							}
		                }
		                else {
		                	message="上傳檔案格式有誤，請依序填入'email','password','userName' , 'phoneNumber'";
		                	Logger.info("Quatan upload account fail.");
		            		render("/quanta/admin/teacher/teacher.html",message);
						}
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		        }finally{
		            try{
		                if ( fr != null )
		                    fr.close();
		                if ( br != null )
		                    br.close();
		            }catch(IOException ex){
		                ex.printStackTrace();
		            }
		        }

			}
		}
		if(message=="")
			message="老師資料上傳完成!";
		Logger.info("Quatan upload account success.");
		render("/quanta/admin/teacher/teacher.html",message);
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void teacher_uploadSession(@Required File[] files) {
		String message="";
		List<Upload> uploadfiles = (List<Upload>) request.args.get("__UPLOADS");
		for(Upload myUpload : uploadfiles) {
			String path = "/public/quanta/teacher/"+ myUpload.getFileName();
			File des = new File(Play.getFile("").getAbsolutePath() + "/public/quanta/teacher/");				
			File output = new File(Play.getFile("").getAbsolutePath() + path);
			if(!des.exists())
				des.mkdirs();
			if (myUpload.getFileName() != "") {
				
				InputStream is = myUpload.asStream();
				try {
					IOUtils.copy(is, new FileOutputStream(output));
					is.close();						
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//parse file to MySQL
				InputStreamReader fr = null;
		        BufferedReader br = null;
		        try {
		        	MyUtil.removeUTF8FileBOM(Play.getFile("").getAbsolutePath() + path); 
		            fr = new InputStreamReader(new FileInputStream(Play.getFile("").getAbsolutePath() + path));
		            br = new BufferedReader(fr);
		            String rec = null;
		            String[] argsArr = null;
		            while ((rec = br.readLine()) != null) {
		                argsArr = rec.split(",");
		                if(argsArr.length>3){
		                	//檢查是否重複
		                	Q_TeacherSeesion session = Q_TeacherSeesion.findByDateAndName(argsArr[0],argsArr[2]);
		                	if(session==null){
				                session = new Q_TeacherSeesion(argsArr[0],argsArr[1],argsArr[2],argsArr[3]); 
				                // date, time ,sessionName, location 
				                session.save();
		                	}
		                	else {
		                		session.time=argsArr[1];
		                		session.location=argsArr[3];
		                		session.save();
							}
		                }
		                else {
		                	message="上傳檔案格式有誤，請依序填入'日期(eg:20130101)','時間(eg:0830)','場次名稱','地點'";
		                	Logger.info("Quatan upload teacher session fail.");
		            		render("/quanta/admin/teacher/teacher.html",message);
						}
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		        } finally {
		            try {
		                if ( fr != null )
		                    fr.close();
		                if ( br != null )
		                    br.close();
		            } catch(IOException ex){
		                ex.printStackTrace();
		            }
		        }
		       
			}
		}
		if(message==""){
			message="研習會場次上傳完成!";
			Logger.info("Quanta teacher upload session : "+message );
		}
		
		render("/quanta/admin/teacher/teacher.html",message);		
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void teacher_uploadApply(@Required File[] files ,@Required Long sid){
		String message="";
		Q_TeacherSeesion ts = Q_TeacherSeesion.findById(sid);
		List<Upload> uploadfiles = (List<Upload>) request.args.get("__UPLOADS");
		for(Upload myUpload : uploadfiles){
			String path = "/public/quanta/teacher/"+sid+"/"+ myUpload.getFileName();
			File des = new File(Play.getFile("").getAbsolutePath() + "/public/quanta/teacher/"+sid+"/");				

			File output = new File(Play.getFile("").getAbsolutePath() + path);
			if(!des.exists())
				des.mkdirs();
			if (myUpload.getFileName() != "") {
				
				InputStream is = myUpload.asStream();
				try {
					IOUtils.copy(is, new FileOutputStream(output));
					is.close();						
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//parse file to MySQL
				InputStreamReader fr = null;
		        BufferedReader br = null;
		        try {
		        	MyUtil.removeUTF8FileBOM(Play.getFile("").getAbsolutePath() + path); 
		            fr = new InputStreamReader(new FileInputStream(Play.getFile("").getAbsolutePath() + path));
		            br = new BufferedReader(fr);
		            String rec = null;
		            String[] argsArr = null;
		            while ((rec = br.readLine()) != null) {
		            	argsArr = rec.split(",");
		                if(ts!=null && argsArr.length>6){ //身分證字號、所屬縣市、單位名稱、姓名、手機、Emai、飲食(0,1,2)
		                	String userId = argsArr[0];
		                	Pattern p = Pattern.compile("[a-zA-Z][0-9]{9}");
		                	Matcher m = p.matcher(userId);
		    				if(m.find())
		    					userId=m.group(0);	
		                	String belongArea = argsArr[1];
		                	String department = argsArr[2];
		                	String username = argsArr[3];
		                	String phone =argsArr[4];
		                	String email = argsArr[5];
		                	int food = Integer.parseInt(argsArr[6]);
		                	//找出不同天 但相同研習會名稱的場次 (代表是同一場次，只是多天的活動)
		                	List<Q_TeacherSeesion> listSession = Q_TeacherSeesion.findSessionByName(ts.sessionName);
		                	for (Q_TeacherSeesion tsession : listSession) {
		                		//檢查是否重複
		                		Q_TeacherApply apply = Q_TeacherApply.findApplyBySessionAndUser(tsession.id, userId);
			                	if(apply==null){
			                		apply= new Q_TeacherApply(userId,username,phone,email,food); 
			                		
			                		apply.session=tsession;
			                		apply.belongArea=belongArea;
			                		apply.departmentName=department;
			                		//產生唯一識別碼 與建立QRCode
			                		apply.serialNumber=MyUtil.GenerateTeacherApplySerialNumber();
			                		apply.save();
			                		MyUtil.genTeacherRCode(userId);
			                		genTeacherNotificationPdf(userId, apply);
			                	}
			                	else {
			                		
			                		apply.belongArea=belongArea;
			                		apply.departmentName=department;
			                		apply.username=username;
			                		apply.phone=phone;
			                		apply.email=email;
			                		apply.food=food;
			                		apply.save();
			                		genTeacherNotificationPdf(userId, apply);
								}
							}
		                	
	                		String content ="親愛的學員 您好: \n恭喜您完成 "+ts.sessionName+" 活動報名，請詳閱附件內容，以利活動報名。";
	                		List<String> list_filepath = new ArrayList<String>();
	                		//badge(.pdf)
	                		File badgePdf = new File(Play.getFile("").getAbsolutePath() + "/public/quanta/teacher/"+ ts.id + "/" + userId +".pdf");	
	                		if(badgePdf!=null)
	                			//System.out.println("123"+badgePdf.getPath());
	                			list_filepath.add("/public/quanta/teacher/"+ ts.id + "/" + userId +".pdf");
	                		for (Attatch file : ts.files) {
								list_filepath.add(file.path);
							}
	                		
	                		try {
								MyUtil.sendMail(email,"廣達文教基金會 "+ts.sessionName+" 報名完成信函", content,list_filepath);
							} catch (AddressException e) {
								e.printStackTrace();
							} catch (MessagingException e) {
								e.printStackTrace();
							}
							
		                	
		                	
		                }
		                else {
		                	message="上傳檔案格式有誤，請依序填入'身分證字號','所屬縣市','單位名稱','姓名','手機','Email','飲食(無[0]、葷[1]、素[2])'";
						}
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		        }finally{
		            try{
		                if ( fr != null )
		                    fr.close();
		                if ( br != null )
		                    br.close();
		            }catch(IOException ex){
		                ex.printStackTrace();
		            }
		        }
		        
			}
		}
		if(message==""){
			message="上傳完成!";
			Logger.info("Quanta teacher upload apply : "+message );
		}
		admin_showTeacherSession(sid,message);
		
	}
	
	private static void genTeacherNotificationPdf(String userId, Q_TeacherApply apply) {
		
//		String templatePath = "/public/quanta/teacher/q_template.pdf";
		String desPath = Play.getFile("").getAbsolutePath() +"/public/quanta/teacher/"+ apply.session.id + "/" + userId +".pdf";
		String qrcodePath = Play.getFile("").getAbsolutePath() +"/public/quanta/teacher/qrcode/" + userId + ".jpg";
//		String badgePath = "/public/quanta/teacher/q_badge.jpg";
		
		Document document = new Document();
		
		try {
			String username = apply.username;
			String sessionName = apply.session.sessionName;
			PdfWriter.getInstance(document, new FileOutputStream(desPath));
			document.open();
			Image top = Image.getInstance(Play.getFile("").getAbsolutePath() +"/public/quanta/teacher/top.png");
			Image badge = Image.getInstance(Play.getFile("").getAbsolutePath() +"/public/quanta/teacher/badge.png");
			Image qrcode = Image.getInstance(qrcodePath);
			top.scaleAbsolute(500, 500);
			badge.scaleAbsolute(275,160);
			qrcode.scaleAbsolute(50,50);
			qrcode.setAbsolutePosition(245,314);
			badge.setAbsolutePosition(134,285);
			
	        document.add(top);
	        document.add(badge);
	        document.add(qrcode);
			
	        BaseFont bf = BaseFont.createFont(Play.getFile("").getAbsolutePath() + "/public/font/kaiu.ttf", BaseFont.IDENTITY_H,  BaseFont.NOT_EMBEDDED);
	        Font name_font = new Font(bf, 24, Font.NORMAL);
	        Font session_font = new Font(bf, 24, Font.BOLD);
	        Font title_font = new Font(bf, 24, Font.UNDERLINE);
	        
	        
            Paragraph name = new Paragraph();
            name.setSpacingAfter(25);
            name.setSpacingBefore(-30);
            name.setAlignment(Element.ALIGN_CENTER);
            name.setIndentationLeft(130);
            name.setIndentationRight(50);
            Chunk chunk_name = new Chunk(username, name_font);
            name.add(chunk_name);
            document.add(name);
            
            Paragraph session = new Paragraph();
            session.setSpacingAfter(25);
            session.setSpacingBefore(-105);
            session.setAlignment(Element.ALIGN_CENTER);
            session.setIndentationLeft(60);
            session.setIndentationRight(50);
            Chunk chunk_session = new Chunk(sessionName, session_font);
            session.add(chunk_session);
            document.add(session);
            
            Paragraph title = new Paragraph();
            title.setSpacingAfter(25);
            title.setSpacingBefore(-300);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setIndentationLeft(60);
            title.setIndentationRight(50);
            Chunk chunk_title = new Chunk(sessionName, title_font);
            title.add(chunk_title);
            document.add(title);
	        
		} catch (DocumentException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
        document.close();
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void teacher_uploadAttatch(@Required String attatchName ,@Required Long sid){
		
		String message="";
		Q_TeacherSeesion ts = Q_TeacherSeesion.findById(sid);
		
		if(ts!=null){
			// find the file
			InputStream file = MyUtil.findInputStreamFromUpload(attatchName);
			if (file != null) {
				// 儲存檔案
				String path = "/public/quanta/teacher/"+sid+"/"+ attatchName;
				File des = new File(Play.getFile("").getAbsolutePath() + "/public/quanta/teacher/"+sid+"/");	
				File output = new File(Play.getFile("").getAbsolutePath() + path);
				if (!des.exists())
					des.mkdirs();
				if (attatchName != "") {
					try {
						//FileUtils.copyFileToDirectory(file, des, true);
						IOUtils.copy(file, new FileOutputStream(output));
						Attatch attatch = new Attatch();
						attatch.fileName=attatchName;
						attatch.path=path;
						attatch.title=sid.toString();
						attatch.intro="研習會檔案附件";
						attatch.share=false;
						attatch.createTime=MyUtil.now();
						attatch.save();
						ts.files.add(attatch);
						ts.save();
						file.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		if(message==""){
			message="上傳完成!";
			Logger.info("Quanta teacher upload apply : "+message );
		}
		admin_showTeacherSession(sid,message);
		
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_listTeacherSession(){
		List<Q_TeacherSeesion> listSessions = Q_TeacherSeesion.findAll();
		//反轉list (將最新的場次放在最前面)
		Collections.reverse(listSessions);
		render("/quanta/admin/teacher/listTeacherSession.html",listSessions);
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_listTeacher(){
		List<Member> listMembers = Member.findMemberByRole(ROLE.Q_TEACHER);
		render("/quanta/admin/teacher/listTeacher.html",listMembers);
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_deleteTeacher(@Required Long m_id){	
		Member member = Member.findById(m_id);
		if(member!=null){
			
			//找出與他相關的FK的欄位  (包含 ...)

			Logger.info("Quatan Admin Delete Quanta member success, user=" + member.username);
			member.delete();
			renderText("success"); 
		}
		else {
			Logger.info("Quatan Admin Delete Quanta member fail.");
			renderText("fail"); 
		}
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_deleteTeacherSession(@Required Long s_id){	
		Q_TeacherSeesion s = Q_TeacherSeesion.findById(s_id);
		if(s!=null){
			
			//找出與他相關的FK的欄位  (包含Teacher Apply)
			
			List<Q_TeacherApply> listApply = Q_TeacherApply.findApplyBySession(s.id);
			for (Q_TeacherApply apply : listApply) {
				apply.delete();
			}

			Logger.info("Quatan Admin Delete Quanta teacher session success, session=" + s.sessionName);
			s.delete();
			renderText("success"); 
		}
		else {
			Logger.info("Quatan Admin Delete Quanta teacher session fail.");
			renderText("fail"); 
		}
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_showTeacherSession(@Required Long s_id,String message){
		
		List<Q_TeacherApply> listApplies = Q_TeacherApply.findApplyBySession(s_id);
		Q_TeacherSeesion ts = Q_TeacherSeesion.findById(s_id);
		int totalApply = listApplies.size();
		int arrivedApply = Q_TeacherApply.countArrivedApplyBySession(s_id);
		int meatApply =  Q_TeacherApply.countMeatApplyBySession(s_id, 1);
		int meatlessApply =  Q_TeacherApply.countMeatApplyBySession(s_id, 2);
		int meatArrivedApply =  Q_TeacherApply.countMeatArrivedApplyBySession(s_id, 1);
		int meatlessArrivedApply =  Q_TeacherApply.countMeatArrivedApplyBySession(s_id, 2);
		render("/quanta/admin/teacher/showSession.html",message,listApplies,ts,
				totalApply,arrivedApply,meatApply,meatlessApply,meatArrivedApply,meatlessArrivedApply);
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_newTeacher(){
		render("/quanta/admin/teacher/newTeacher.html");
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_saveTeacher(@Required String email ,@Required String pwd ,@Required String confirmpwd ,@Required String username , @Required String phone){
		if(Member.findUser(email)==null){
			Member member = new Member(email, pwd, ROLE.Q_TEACHER, true, username);
			member.phone=phone;
			member.save();
			Logger.info("Quatan Admin create Quanta member success, user=" + member.username);
		}
		admin_listTeacher();
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void admin_teacherCheckinAndOut(@Required String action ,@Required Long a_id){
		
		String time = MyUtil.getTime();
		Q_TeacherApply apply = Q_TeacherApply.findById(a_id);
		if(apply!=null){
			if(action.equals("checkin")){
				apply.checkinTime=time;
			}
			else if (action.equals("checkout")) {
				apply.checkoutTime=time;
				apply.totalTime=calcularTotalHour(apply.checkinTime,time);
			}
			else {
				renderText("fail!");
			}
			apply.save();
			renderText("success!");
		}
		renderText("fail!");
	}
	
	@Restrict(ROLE.Q_ADMIN)
	public static void teacher_exportCSV(@Required Long s_id){

		try{
			//建立檔案
			String path = Play.getFile("").getAbsolutePath() + File.separator
					+ "public" + File.separator + "quanta" + File.separator
					+ "teacher"+ File.separator +   "export_"+s_id.toString()+".csv";
			File file = new File(path);  //logFile 是路径，就叫做String logFile = 生成路径+想要的文件名+".csv";
		    FileOutputStream out = new FileOutputStream(file);
		    OutputStreamWriter osw = new OutputStreamWriter(out, "BIG5");

		     BufferedWriter bw = new BufferedWriter(osw);
		     //然后就随便你自己去构造了，我写个第一行表示表头，然后第二行起循环
		     
		     bw.write("場次活動名稱" + "," + "日期" + "," + "時間" + "," +"Email" + "," + "姓名" + ","+ "報到時間" + "," + "離開時間" +"," + "總時數(小時)" + "\r\n");//请注意，CSV默认是已逗号","分隔单元格的。这里是表头
		     
		     List<Q_TeacherApply> listApplies = Q_TeacherApply.findApplyBySession(s_id);
		     
		     boolean firstTag =true;
		     for (Q_TeacherApply apply : listApplies) {
		    	 //只列出有完成者 (有簽到與簽退紀錄者)
		    	 if(apply.totalTime!=null && !apply.totalTime.equals("")){
		    		 if(firstTag){
		    			 bw.write(apply.session.sessionName + "," + apply.session.date + "," + apply.session.date + "," + apply.email+ "," + apply.username + "," + apply.checkinTime + "," + apply.checkoutTime + "," + apply.totalTime +"\r\n");
		    			 firstTag=false;
		    		 }
		    		 else {
		    			 bw.write(",,," + apply.email+ "," + apply.username + "," + apply.checkinTime + "," + apply.checkoutTime + "," + apply.totalTime +"\r\n");
					}
		    	 }
		    }
		     
		     bw.close();
		     osw.close();
		     out.close();

		     renderText("ok"); 
		}
		catch (Exception e) {
			renderText(e.toString()); 
		}
		
	}
	
	public static void teacher_uploadCheckFile(@Required File uploadFile) {
		// 格式為   date##身分證##time##action[checkin,checkout] 
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		String message="";
		if(uploadFile!=null){
			
			String path = "/public/quanta/teacher/upload/"+MyUtil.getDate()+"/" + uploadFile.getName();
			File des = new File(Play.getFile("").getAbsolutePath()+ "/public/quanta/teacher/upload/"+MyUtil.getDate() );
			File output = new File(Play.getFile("").getAbsolutePath() + path);
			if (!des.exists())
				des.mkdirs();
			if (uploadFile.getName() != "") {
				
				try {
					InputStream is = new FileInputStream(uploadFile);
					OutputStream os = new FileOutputStream(output);
			        byte[] buffer = new byte[1024];
			        int length;
			        while ((length = is.read(buffer)) > 0) {
			            os.write(buffer, 0, length);
			        }
					//IOUtils.copy(uploadFile, new FileOutputStream(output));
					
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// parse file to MySQL
				InputStreamReader fr = null;
				BufferedReader br = null;
				try {
					fr = new InputStreamReader(new FileInputStream(Play.getFile("").getAbsolutePath() + path));
					br = new BufferedReader(fr);
					String rec = null;
					String[] argsArr = null;
					while ((rec = br.readLine()) != null) {
						argsArr = rec.split("##");
						// date##身分證##time##action[checkin,checkout]
						if (argsArr.length > 3) {
							String date = argsArr[0];
							String userID = argsArr[1];							
							String time = argsArr[2];
							String action = argsArr[3];
							// 找出該筆apply 及 seesion
							Q_TeacherApply apply = Q_TeacherApply.findApplyByDateAndUser(date, userID);
							if (apply != null) {
								if(action.equals("checkin")){
									apply.checkinTime =time;										
								}else if(action.equals("checkout")){
									apply.checkoutTime=time;
									apply.totalTime=calcularTotalHour(apply.checkinTime, time);
								}
								apply.save();			
							} else {
								message+=userID + " 此人未報名，或研習場次錯誤。";
							}
						}
						else
							message+= "參數長度:"+argsArr.length+"，點名之參數錯誤，請重新掃描。";
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (fr != null)
							fr.close();
						if (br != null)
							br.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
			if(message==""){
				message="上傳成功。";
				response.put("result", "true");
			}
			else {
				response.put("result", "false");
				response.put("reason", message);
			}
			
			
		}
		else {
			response.put("result", "false");
			response.put("reason", "the file is not exist.");
		}
		json.put("response", response);
		JSONSerializer serializer = new JSONSerializer();
		serializer.deepSerialize(json);
		String output = serializer.deepSerialize(json);
		
		renderJSON(output);
	}
	
}
