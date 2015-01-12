package controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.management.Query;
import javax.swing.ListModel;
import javax.xml.ws.RequestWrapper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
//import org.json.*;

//import java.util.HashMap;
//import java.util.Map;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;


import models.Answer;
import models.Apply;
import models.Attatch;
import models.Contact;
import models.Examine;
import models.Member;
import models.MemberGroup;
import models.OutdoorActivity;
import models.Photo;
import models.Queue;
import models.Tag;
import models.TagCategory;
import models.TeachingPlan;
import models.ViewCountHistory;
import play.Logger;
import play.Play;
import play.data.Upload;
import play.data.validation.Email;
import play.data.validation.Password;
import play.data.validation.Required;
import play.libs.Crypto;
import play.libs.WS;
import play.mvc.Before;
import play.mvc.Controller;
import Utility.Conf;
import Utility.MyOAuth2;
import Utility.MyUtil;
import Utility.ROLE;

import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import controllers.deadbolt.Restrict;


public class MemberManager extends Controller {

	public static void ParentLogin(){
		Member user = Member.login("parent@test.com", "parent");
		if(user!=null&&!user.isFacebookUser){
			session.put("email", user.email);
			session.put("username", user.username);
		}
		Application.index(null);
	}
	public static void StudentLogin(){
		Member user = Member.login("student@test.com", "student");
		if(user!=null&&!user.isFacebookUser){
			session.put("email", user.email);
			session.put("username", user.username);
		}
		Application.index(null);
	}
	public static void TeacherLogin(){
		Member user = Member.login("teacher@test.com", "teacher");
		if(user!=null&&!user.isFacebookUser){
			session.put("email", user.email);
			session.put("username", user.username);
		}
		Application.index(null);
	}
	public static void AdminLogin(){
		Member user = Member.login("admin@test.com", "admin");
		if(user!=null&&!user.isFacebookUser){
			session.put("email", user.email);
			session.put("username", user.username);	
		}
		Application.index(null);
	}
	
	@Before
	public static void getTag() {
		Application.getTag();	   
	}
	
	public static void linkRegister() {
		render("member/user_register.html");
	}

	public static void register(@Required String reg_mail, @Required String password, @Required String username, 
								   @Required int gender, @Required int title ,String intro,
								   String school_full_name, String grade, String className ,String parentEmail,
								   File file,
								   String childEmail){
		if(Member.findUser(reg_mail)!=null)
			Application.index(null);
		
		Member member = new Member();
		member.email=reg_mail;
		//密碼加密(Hash Code)
		//password = MyUtil.passwordEncrypt(password);
		member.password=password;
		member.username=username;
		member.mood=intro;
		
		if(password==null){
			member.isFacebookUser=true;
		}else {
			member.isFacebookUser=false;
		}
		//性別
		switch (gender) {
		case 0:
			member.sex="女";
			break;
		case 1:
			member.sex="男";
			break;
		}
		//角色
		switch (title) {
		case 0:
			member.roleList.add(ROLE.STUDENT);
			member.schoolName = school_full_name;
			member.className = grade + " 年 " + className + " 班 ";
			break;
		case 1:
			member.roleList.add(ROLE.PARENT);
			break;
		case 2:
			//先給家長(一般使用者之權限，待審核後才給予老師的權限)
			member.roleList.add(ROLE.PARENT);
			
			String path = "/public/member/"+member.email+"/attatch/" + file.getName();
			File des = new File(Play.getFile("").getAbsolutePath() + "/public/member/" + member.email+"/attatch/");
			
			//File output = new File(Play.getFile("").getAbsolutePath() + path);
			if(!des.exists())
				des.mkdirs();
			if (file.getName() != "") {
				Attatch attatch = new Attatch();
				attatch.title = "老師簡歷";
				attatch.fileName = file.getName();
				attatch.path=path;
				attatch.createTime=MyUtil.now();
				attatch.save();
					
				member.attatch=attatch;
				try {				
					//InputStream is = new FileInputStream(file);
					//IOUtils.copy(is, new FileOutputStream(output));
					//is.close();	
					FileUtils.copyFileToDirectory(file, des,true); 

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			break;
		}
		member.createTime=MyUtil.now();
		member.save();
		//For queue 邀請確認關係
		switch (title) {
		case 0:
			if(parentEmail!=null&&parentEmail!=""){
				Member parent = Member.findUser(parentEmail);
				if(parent!=null){
					Queue queue = new Queue();
					queue.master = member;
					queue.guest=parent;
					queue.time = MyUtil.now();
					queue.save();
				}
			}
			break;
		case 1:
			if(childEmail!=null&&childEmail!=""){
				Member child = Member.findUser(childEmail);
				if(child!=null){
					Queue queue = new Queue();
					queue.master = member;
					queue.guest=child;
					queue.time = MyUtil.now();
					queue.save();
				}
			}	
			break;
		}
		
		//註冊後 自動登入 並跳轉到歡迎頁面
		if(password!=null){
			Member user = Member.login(member.email, member.password);
			if(user!=null&&!user.isFacebookUser){
				session.put("email", user.email);
				session.put("username", user.username);
				Logger.info("Member register success. email=" + user.email + ", username=" + user.username);
				userWelcome();
			}
		}
		else {
			session.put("email", member.email);
			session.put("username", member.username);
			Logger.info("Facebook login success. email=" + member.email + ", username=" + member.username);
			userWelcome();
		}
	}
	
	public static void userProfile(Long userId) {
		
		Member user = Member.findById(userId);
		if(user!=null){
			//計算ViewCount
			MyUtil.calculateViewCount(ViewCountHistory.MemberView,user.id);
				
			// 建立的活動
			List<OutdoorActivity> my_create_oa = new ArrayList<OutdoorActivity>();
			my_create_oa = OutdoorActivity.listByCreator(userId);
			
			for(int i=0 ; i<my_create_oa.size() ; i++) {
				if(my_create_oa.get(i).name.length()>22)
					my_create_oa.get(i).name = my_create_oa.get(i).name.substring(0, 22) + "...";
			}
			
			// 建立的教案
			List<TeachingPlan> my_create_tp = new ArrayList<TeachingPlan>();
			my_create_tp = TeachingPlan.listByCreator(userId);
			
			for(int i=0 ; i<my_create_tp.size() ; i++) {
				if(my_create_tp.get(i).name.length()>22)
					my_create_tp.get(i).name = my_create_tp.get(i).name.substring(0, 22) + "...";
			}
			
			// 參加的活動
			List<OutdoorActivity> my_join_oa = new ArrayList<OutdoorActivity>();
			my_join_oa = OutdoorActivity.listMyJoinOa(userId);
			
			for(int i=0 ; i<my_join_oa.size() ; i++) {
				if(my_join_oa.get(i).name.length()>22)
					my_join_oa.get(i).name = my_join_oa.get(i).name.substring(0, 22) + "...";
			}
			
			// 建立的班群
			List<MemberGroup> my_create_group = new ArrayList<MemberGroup>();
			my_create_group = MemberGroup.findByCreator(userId);
			// 參加的評量
			List<Examine> my_join_exam = new ArrayList<Examine>();
			my_join_exam = Examine.findExamineByAttendId(userId);
			// 參加的班群
			List<MemberGroup> my_join_group = new ArrayList<MemberGroup>();
			my_join_group = MemberGroup.findByMember(userId);
			
			render("member/user_profile.html", user, my_create_oa, my_create_tp, my_join_oa,
					my_create_group, my_join_exam, my_join_group);
		}
	}
	
	private static void userWelcome(){
		render("member/user_welcome.html");
	}
	
	public static void forgotPassword(@Required String email){
		Member member = Member.findUser(email);
		if(member!=null){
			//ramdom 新密碼 (6碼)
			SecureRandom ran = new SecureRandom();
			String newPassword= new BigInteger(130, ran).toString(32).substring(0, 6);
			
			try {
				MyUtil.sendMail(member.email,"放學趣-帳號密碼重設通知","您的密碼已由系統隨機產生重設，請以下列密碼盡速登入修改。\n新密碼:"+newPassword);
				//密碼加密(Hash Code)
				//newPassword = MyUtil.passwordEncrypt(newPassword);
				member.password=newPassword;
				member.save();
			} catch (AddressException e) {
				e.printStackTrace();
			} catch (MessagingException e) {
				e.printStackTrace();
			}
			renderText("已發送新密碼至您註冊之信箱，請盡速登入後修改密碼。");
		}
		else {
			renderText("Email輸入錯誤!!");
		}
	}
	
	public  static void configure(String msg) {
		Member member = MyUtil.getCurrentUser();
		String grade="";
		String className="";
		if(member!=null){
			if(member.className!=null && !member.className.equals("")){
				grade = member.className.substring(0,member.className.indexOf(" 年 "));
				className = member.className.substring(member.className.indexOf(" 年 ")+3,member.className.indexOf(" 班 "));
			}
			
			List<TagCategory> listCategory = TagCategory.findByActivity("找老師");
			
			//尚未確認之家長關係(被邀請人)
			List<Queue> list_parentsByGuest = Queue.findRelationByGuest(member.id);
			//尚未確認之家長關係(邀請人)
			List<Queue> list_parentsByMaster = Queue.findRelationByMaster(member.id);
			
			//找出已加入之班群
			List<MemberGroup> list_group = MemberGroup.findByMember(member.id);
			//尚未接受加入之班群(接受邀請)
			List<Queue> list_groupsByGuest = Queue.findGroupByGuestAndMember(member.id);	
			//尚未被接受加入之班群(送出邀請)
			List<Queue> list_groupsByMaster = Queue.findGroupByMasterAndMember(member.id);
			

			//尚未確認之孩童關係(被邀請人)
			List<Queue> list_childByGuest = Queue.findRelationByGuest(member.id);
			//尚未確認之家長關係(邀請人)
			List<Queue> list_childByMaster = Queue.findRelationByMaster(member.id);
			
			//班群列表(老師creator)
			List<MemberGroup> list_groupByCreator = MemberGroup.findByCreator(member.id);
			int groupQueueCount =0;
			for (MemberGroup group : list_groupByCreator) {
				//學生申請加入
				List<Queue> queues = Queue.findGroupByGuestAndGroup(group.id);
				groupQueueCount +=queues.size();
				//老師邀請學生加入
				queues = Queue.findGroupByMasterAndGroup(group.id);
				groupQueueCount +=queues.size();
			}
			
			render("member/user_setup_account.html",msg,member,grade,className,listCategory
					,list_parentsByGuest,list_parentsByMaster
					,list_group,list_groupsByGuest,list_groupsByMaster
					,list_childByGuest,list_childByMaster
					,list_groupByCreator,groupQueueCount);
		}	
		else {
			Application.toLogin();
		}
	}
	
	public  static void updateProfile(@Required String username, @Required int sex, @Required String mood,
										String school_full_name, String grade, String className,
										Long[] element){
		Member member = MyUtil.getCurrentUser();
		if(member!=null){
			member.username=username;
			//性別
			switch (sex) {
			case 0:
				member.sex="女";
				break;
			case 1:
				member.sex="男";
				break;
			}
			//學生
			if(member.roleList.contains(ROLE.STUDENT)){
				member.schoolName=school_full_name;
				member.className = grade + " 年 " + className + " 班 ";
			}
			
			//老師
			if(member.roleList.contains(ROLE.TEACHINGPLANNER)){
				member.tags.clear();
				if(element!=null && element.length>0){
					for (Long tid : element) {
						Tag tag = Tag.findById(tid);
						if(tag!=null && !member.tags.contains(tag))
							member.tags.add(tag);
					}
				}
			}
			member.mood=mood;
			member.save();
			configure("更新完成!");
		}
		
	}
	
	public  static void updatePassword(@Required String oldPassword,@Required String newPassword ){
		Member member = MyUtil.getCurrentUser();
		//密碼加密(Hash Code)
		//oldPassword = MyUtil.passwordEncrypt(oldPassword);
		if(member!=null && member.password.equals(oldPassword)){
			//密碼加密(Hash Code)
			newPassword = MyUtil.passwordEncrypt(newPassword);
			member.password=newPassword;
			member.save();
			renderText("變更成功!");
		}
		renderText("帳號密碼錯誤!");
	}
	
	public static void updateBankAccount(@Required String bankCode, @Required String atmAccount){
		Member member = MyUtil.getCurrentUser();
		Pattern p = Pattern.compile("^[0-9]{10,16}$");
		if(member!=null){
			Matcher m = p.matcher(atmAccount);
			if(m.find()){
				member.bankCode=bankCode;
				member.accountATM=atmAccount;
				member.save();
				renderText("變更成功!");
			}
			else
				renderText("銀行帳號錯誤!");
		}
		else
			renderText("帳號密碼錯誤!");
	}
	
	private static void switchToTeacherHome() {
		Member member = MyUtil.getCurrentUser();
		List<OutdoorActivity> my_oa_now = OutdoorActivity.listByStatusAndUserId(member.id, OutdoorActivity.STATUS_Progress);
		
		for(int i=0 ; i<my_oa_now.size() ; i++) {
			if(my_oa_now.get(i).name.length()>10)
				my_oa_now.get(i).name = my_oa_now.get(i).name.substring(0, 10) + "...";
		}
		
		List<OutdoorActivity> my_oa_past = OutdoorActivity.listByStatusAndUserId(member.id, OutdoorActivity.STATUS_Finish);

		for(int i=0 ; i<my_oa_past.size() ; i++) {
			if(my_oa_past.get(i).name.length()>10)
				my_oa_past.get(i).name = my_oa_past.get(i).name.substring(0, 10) + "...";
		}
		
		List<TeachingPlan> my_tp_open = TeachingPlan.listByPublishAndUserId(member.id, Boolean.TRUE);
		
		for(int i=0 ; i<my_tp_open.size() ; i++) {
			if(my_tp_open.get(i).name.length()>10)
				my_tp_open.get(i).name = my_tp_open.get(i).name.substring(0, 10) + "...";
		}
		
		List<TeachingPlan> my_tp_close = TeachingPlan.listByPublishAndUserId(member.id, Boolean.FALSE);
		
		for(int i=0 ; i<my_tp_close.size() ; i++) {
			if(my_tp_close.get(i).name.length()>10)
				my_tp_close.get(i).name = my_tp_close.get(i).name.substring(0, 10) + "...";
		}
		
		List<TeachingPlan> my_favo_tp = TeachingPlan.listMyFavoTp(member.id);
		
		for(int i=0 ; i<my_favo_tp.size() ; i++) {
			if(my_favo_tp.get(i).name.length()>10)
				my_favo_tp.get(i).name = my_favo_tp.get(i).name.substring(0, 10) + "...";
		}
		
		List<OutdoorActivity> my_favo_oa = OutdoorActivity.listMyFavoOa(member.id);
		
		for(int i=0 ; i<my_favo_oa.size() ; i++) {
			if(my_favo_oa.get(i).name.length()>10)
				my_favo_oa.get(i).name = my_favo_oa.get(i).name.substring(0, 10) + "...";
		}
		
		List<MemberGroup> my_groups = MemberGroup.findByCreator(member.id);
		
		List<Examine> my_exam_progress = Examine.findExamineByCreatorAndStatus(member.id, Examine.progress);
		List<Examine> my_exam_finish = Examine.findExamineByCreatorAndStatus(member.id, Examine.finished);
		List<Examine> my_exam_unstart = Examine.findExamineByCreatorAndStatus(member.id, Examine.unstart);
 		System.out.println("s="+my_exam_progress.size());
		
		render("member/user_teacher_myhome.html",member, my_oa_now, my_oa_past, my_tp_open, 
				my_tp_close, my_favo_tp, my_favo_oa, my_groups, my_exam_progress, my_exam_finish, my_exam_unstart);
	}
	
	private static void switchToParentHome() {
		Member member = MyUtil.getCurrentUser();
		List<Member> my_children = Member.findRelations(member.id);
		List<Member> children_teacher = Member.findTeacherByChildren(my_children);
		
		List<OutdoorActivity> child_oa_now = new ArrayList<OutdoorActivity>();
		List<Examine> my_child_exam = new ArrayList<Examine>();
		for(int i=0 ; i<my_children.size() ; i++) {
			child_oa_now.addAll(OutdoorActivity.listChildOAByStatus(my_children.get(i).id));
			my_child_exam.addAll(Examine.findExamineByAttendId(my_children.get(i).id));
		}
		
		List<TeachingPlan> my_favo_tp = TeachingPlan.listMyFavoTp(member.id);
		List<OutdoorActivity> my_favo_oa = OutdoorActivity.listMyFavoOa(member.id);
		

		for(int i=0 ; i<child_oa_now.size() ; i++)
			if(child_oa_now.get(i).name.length()>10)
				child_oa_now.get(i).name = child_oa_now.get(i).name.substring(0, 10) + "...";
		
		for(int i=0 ; i<my_favo_oa.size() ; i++)
			if(my_favo_oa.get(i).name.length()>10)
				my_favo_oa.get(i).name = my_favo_oa.get(i).name.substring(0, 10) + "...";

		for(int i=0 ; i<my_favo_tp.size() ; i++)
			if(my_favo_tp.get(i).name.length()>10)
				my_favo_tp.get(i).name = my_favo_tp.get(i).name.substring(0, 10) + "...";
		
		render("member/user_parents_myhome.html", member, my_children, children_teacher, my_favo_tp, 
				my_favo_oa, child_oa_now, my_child_exam);
	}
	
	private static void switchToStudentHome() {
		Member member = MyUtil.getCurrentUser();
		// Student
		List<OutdoorActivity> my_oa_now = OutdoorActivity.listMyOaByStatus(member.id, OutdoorActivity.STATUS_Progress);
		List<OutdoorActivity> my_oa_past = OutdoorActivity.listMyOaByStatus(member.id, OutdoorActivity.STATUS_Finish);
		List<TeachingPlan> my_favo_tp = TeachingPlan.listMyFavoTp(member.id);
		List<OutdoorActivity> my_favo_oa = OutdoorActivity.listMyFavoOa(member.id);				
		List<MemberGroup> my_groups = MemberGroup.findByMember(member.id);
		List<TeachingPlan> my_tp_open = TeachingPlan.listByPublishAndUserId(member.id, Boolean.TRUE);
		List<TeachingPlan> my_tp_close = TeachingPlan.listByPublishAndUserId(member.id, Boolean.FALSE);
		List<Member> my_parents = Member.findRelations(member.id);
		List<Member> my_children = new ArrayList<Member>();
		my_children.add(member);
		List<Member> my_teachers = Member.findTeacherByChildren(my_children);
		
		List<Examine> my_exams = Examine.findExamineByAttendId(member.id);
		List<Examine> my_exams_unAnswer = new ArrayList<Examine>();
		List<Examine> my_exams_Answered = new ArrayList<Examine>();
		
		for(int i=0 ; i<my_exams.size() ; i++) {
			Examine exam = my_exams.get(i);
			OutdoorActivity oa = OutdoorActivity.findOAByExamID(exam.id);
			Apply apply = Apply.findApplyByOAandAttendMember(oa.id, member.id);
			Answer ans = Answer.findAnsByExamAndApply(exam.id, apply.id);
			if(null==ans)
				my_exams_unAnswer.add(exam);
			else
				my_exams_Answered.add(exam);
		}
		
		render("member/user_student_myhome.html",member, my_favo_tp, my_favo_oa, my_oa_now,
				my_oa_past, my_groups, my_teachers,my_tp_open,my_tp_close, my_parents,
				my_exams_unAnswer, my_exams_Answered);
	} 
	
	public static void myHomePage() {
		Member member = MyUtil.getCurrentUser();
		if(member!=null) {
			if(member.roleList.contains(ROLE.ADMIN)) {
				switchToTeacherHome();
			}
			else if (member.roleList.contains(ROLE.TEACHINGPLANNER)) {
				switchToTeacherHome();
			}
			else if (member.roleList.contains(ROLE.PARENT)) {
				switchToParentHome();
			} 
			else if (member.roleList.contains(ROLE.STUDENT)) {
				switchToStudentHome();
			}
			else {
				//都沒有腳色就到選擇註冊之畫面
				linkRegister();
			}
		} 
		else {
			Application.toLogin();
		}
	}
	
	public static void teacherSwitchToParent(){
		switchToParentHome();
	}
	
	public static void registerValidate(@Required String email){
		Member member = Member.findUser(email);
		if(member==null) {
			renderText("ok"); 
		} else { 
			renderText("no");
		}
		
	}

	public static void uploadPhoto(String subfile) {
		System.out.println("got submit");
		Member member = MyUtil.getCurrentUser();
		if(member!=null){
			// find the file
			InputStream file = MyUtil.findInputStreamFromUpload(subfile);
			if (file != null) {
				// 儲存檔案
				String path = "/public/member/" + member.email + "/photo/"+ subfile;
				File des = new File(Play.getFile("").getAbsolutePath()+ "/public/member/" + member.email + "/photo");
				File output = new File(Play.getFile("").getAbsolutePath() + path);
				if (!des.exists())
					des.mkdirs();
				if (subfile != "") {
					Photo p =new Photo();
					p.title = "會員大頭貼";
					p.intro = "會員大頭貼";
					p.share = false;
					p.fileName = subfile;
					p.path = path;
					p.save();

					try {
						//FileUtils.copyFileToDirectory(file, des, true);
						IOUtils.copy(file, new FileOutputStream(output));
						member.photo=p;
						file.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					
				}
			}
		}

		member.save();
		Logger.info("upload user photo success.");
		myHomePage();
	}
	
//	public static void resetPwd(String email) {
//		
//		String newPwd = MyUtil.ranPwd();
//		System.out.println(newPwd);
//		String receiver = email;
//		String subject = "放學趣重設密碼通知信件";
//		String content = "親愛的放學趣用戶：\n\n" +
//				"您在放學趣註冊帳號為　" +
//				email +
//				"\n\n您的新密碼為：" + 
//				newPwd + 
//				"\n\n為了確保您的帳戶安全，請使用新密碼登入後重新設定您的密碼。";
//		
//		Member member = Member.findUser(email);
//		System.out.println("b=" + member.password);
//		System.out.println("newpwd=" + newPwd);
//		member.password = newPwd;	
//		member.save();
//		System.out.println("a=" + member.password);
//		
//		try {
//			MyUtil.sendMail(receiver, subject, content);
//			
//		} catch (MessagingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
	
	public static void searchTeacherWithKw(String kw) {
		List<Member> listTeacher = new ArrayList<Member>();
		if(!kw.equals("")) {
			List<Member> listMember = new ArrayList<Member>(); 
			listMember = Member.findByKeyword(kw);
			for (Member member : listMember) {
				if(member.roleList.contains(ROLE.TEACHINGPLANNER))
					listTeacher.add(member);		
			}	
		} else {
			listTeacher = Member.findMemberByRole(ROLE.TEACHINGPLANNER);
			kw = "所有老師";
		}
		
		//限制mood字數(16個字內)
		//temData 拿來儲存活動與教案之數量 (eg:03個活動報名中與02個教案
		for (Member member : listTeacher) {
			if(member.mood!=null && member.mood.length()>16)
				member.mood=member.mood.substring(0,15);
			member.tempData=OutdoorActivity.listByStatusAndUserId(member.id,1).size()+"個活動報名中與"+
						 TeachingPlan.listByPublishAndUserId(member.id, true).size()+"個教案";				
		}
		
		render("Application/searchTeacher_kw.html", listTeacher, kw);
	}
	
	public static void searchStudent(String keyword){
		
		List<Member> listMember = Member.findByKeyword(keyword);
		List<Member> listStudent = new ArrayList<Member>();
		for (Member member : listMember) {
			if(member.roleList.contains(ROLE.STUDENT))
				listStudent.add(member);				
		}		
		render(listStudent);		
	}
	
	public static void searchTeacher(String keyword){
		List<Member> listMember = Member.findByKeyword(keyword);
		List<Member> listTeacher = new ArrayList<Member>();
		for (Member member : listMember) {
			if(member.roleList.contains(ROLE.TEACHINGPLANNER))
				listTeacher.add(member);		
		}	
		render(listTeacher);	
	}
	
	public static boolean notHasRole(Member user, String role) {
		boolean notHasRole = true;
		if (user.roleList.contains(role)) {
			notHasRole = false;
		}
		
		return notHasRole;
	}
	
	@Restrict(ROLE.ADMIN)
	public static void validateTeacher(){
		//List<Member> listTeacher = Member.findUncheckTeacher();
		List<Member> listTeacher = Member.findMemberByRole(ROLE.TEACHINGPLANNER);
		//先暫時列出所有老師與尚未確認為老師的帳號
		listTeacher.addAll(Member.findUncheckTeacher());
		render("/member/validateTeacher.html",listTeacher);
	}
	
	@Restrict(ROLE.ADMIN)
	public static void ok(Long tid) {
		Member member = Member.findById(tid);
		if(!member.roleList.contains(ROLE.TEACHINGPLANNER))
			member.roleList.add(ROLE.TEACHINGPLANNER);
		member.save();		
	}
	
	@Restrict(ROLE.ADMIN)
	public static void reject(Long tid) {
		Member member = Member.findById(tid);
		if(member.roleList.contains(ROLE.TEACHINGPLANNER))
			member.roleList.remove(ROLE.TEACHINGPLANNER);
		member.save();		
	}
	
	public static void enableAccount(Long userId){
		String msg="變更失敗。";
		if(MyUtil.isAdminByCurrentUser()){
			Member member = Member.findById(userId);
			if(member!=null){
				member.enable=true;
				member.save();
				msg="變更成功。";
			}
		}
		renderText(msg);
	}
	
	public static void disableAccount(Long userId){
		String msg="變更失敗。";
		if(MyUtil.isAdminByCurrentUser()){
			Member member = Member.findById(userId);
			if(member!=null){
				member.enable=false;
				member.save();
				msg="變更成功。";
			}
		}
		renderText(msg);
	}
	
	//個人設定頁面 ajax部分
		public static void rejectQueue(@Required Long queue_id){
			Queue queue = Queue.findById(queue_id);
			if(queue!=null){
				queue.delete();
				renderText("已刪除邀請。");
			}
			else {
				renderText("無此邀請。");
			}
		}
		
		
		public static void addRelation(@Required String email) {
			Member member = MyUtil.getCurrentUser();
			Member parent = Member.findUser(email);
			
			if(member!=null && parent!=null){	
				if(!member.equals(parent)){
					Queue queue = Queue.findQueueByRelation(member.id, parent.id);
					if (queue == null) {
						queue = new Queue();
						queue.master = member;
						queue.guest = parent;
						queue.time = MyUtil.now();
						queue.save();
						renderText("已新增成功，等待對方確認。");
					} else {
						renderText("已重複新增，尚待對方確認中。");
					}
				}
				else {
					renderText("不可邀請自己。");
				}
			}
			else
				renderText("查無此帳號");
		}
		
		public static void deleteRelation(@Required String parentsId){
			Pattern p = Pattern.compile("[0-9]+");
			String message="已刪除成功。";
			Member member = MyUtil.getCurrentUser();
			if (member != null) {
				String[] pid = parentsId.split(",");
				for (String id : pid) {
					Matcher m = p.matcher(id);
					if (id != null && m.find()) {
						
						Member relationMember = Member.findById(Long.parseLong(id));
						if (relationMember != null) {
							// 已加入
							if (member.relationMember.contains(relationMember))
								member.relationMember.remove(relationMember);
							if(relationMember.relationMember.contains(member))
								relationMember.relationMember.remove(member);
							member.save();
							relationMember.save();
							
						}
						else
							message="刪除失敗，尚未完全刪除。";
					}
				}
			}
			renderText(message);
		}
		
		public static void acceptRelation(@Required Long queue_id){
			Queue queue = Queue.findById(queue_id);
			if(queue!=null){
				Member master = queue.master;
				Member guest = queue.guest;
				//互相加入
				if(!master.relationMember.contains(guest))
					master.relationMember.add(guest);
				if(!guest.relationMember.contains(master))
					guest.relationMember.add(master);
				master.save();
				guest.save();
				queue.delete();
				renderText("已確認關係。");
			}
			else {
				renderText("無此邀請。");
			}
		}
		
		public static void acceptGroupByGuest(@Required Long queue_id){
			Queue queue = Queue.findById(queue_id);
			if(queue!=null){
				Member guest = queue.guest;
				MemberGroup group = queue.group;
				if(!group.members.contains(guest))
					group.members.add(guest);
				group.save();	
				queue.delete();
				renderText("已接受邀請。");
			}
			else {
				renderText("無此邀請。");
			}
		}
		
		public static void deleteGroupByMember(@Required Long group_id){
			MemberGroup group = MemberGroup.findById(group_id);
			if(group!=null){
				Member member=MyUtil.getCurrentUser();
				if(member!=null && group.members.contains(member)){
					group.members.remove(member);
					group.save();
					renderText("已成功退出。");
				}
			}
			else {
				renderText("無此邀請。");
			}
		}
}
