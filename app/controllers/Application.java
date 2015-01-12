package controllers;

import play.*;
import play.data.validation.Required;
import play.libs.WS;
import play.mvc.*;
import play.mvc.Scope.RenderArgs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.Map;
import java.util.Queue;

import javax.swing.text.html.HTML;

import org.json.simple.JSONObject;

import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import Utility.Conf;
import Utility.MyOAuth2;
import Utility.MyUtil;
import Utility.ROLE;

import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.Restrict;
import controllers.deadbolt.Restrictions;
import flexjson.JSONSerializer;
import flexjson.transformer.StringTransformer;
import models.*;

@With(Deadbolt.class)
public class Application extends Controller {
	
	public static String TAB_3 = "北部地區";
	public static String TAB_4 = "中部地區";
	public static String TAB_5 = "南部地區";
	public static String TAB_6 = "所有地區";
	
	public static int ALL = 0;
	
	@Before
	public static void getTag() {
		//for tag search
		List<TagCategory> listTagCategoryByOA = TagCategory.findByActivity("找活動");
		List<TagCategory> listTagCategoryByTeacher = TagCategory.findByActivity("找老師");
		List<TagCategory> listTagCategoryByTP = TagCategory.findByActivity("找教案");
				
	   renderArgs.put("listTagCategoryByOA" , listTagCategoryByOA);
	   renderArgs.put("listTagCategoryByTeacher" , listTagCategoryByTeacher);
	   renderArgs.put("listTagCategoryByTP" , listTagCategoryByTP);
	   
	}
	
	public static void license() {
		render("license.html");
	}
	
	public static void index(String msg) {
		String user_agent = request.headers.get("user-agent").value();				
		Logger.info("Request index, user_agent=" + user_agent);
		Logger.info("Request ip = " + request.remoteAddress);
		
		normalPage(msg);
		
    }
	
	private static void normalPage(String msg) {
		Logger.info(request.remoteAddress + " access index");	
		
		//熱門教案  sort by viewcount
		List<TeachingPlan> list_tp = new ArrayList<TeachingPlan>();
		list_tp = TeachingPlan.listHotAndPublishTpDESC(8);
		for(int i=0 ; i<list_tp.size() ; i++) {
			if(list_tp.get(i).name.length()>10)
				list_tp.get(i).name = list_tp.get(i).name.substring(0, 10) + "...";
		}
		
		//最新活動 取後面兩個
		List<OutdoorActivity> list_oa = new ArrayList<OutdoorActivity>();
		list_oa = OutdoorActivity.listPublishOA();

		if(list_oa!=null&&list_oa.size()>2)
			list_oa = list_oa.subList(list_oa.size()-2, list_oa.size());
		for(int i=0 ; i<list_oa.size() ; i++) {
			if(list_oa.get(i).name.length()>10)
				list_oa.get(i).name = list_oa.get(i).name.substring(0, 10) + "...";
		}
		
		//熱門活動 取兩個
		List<OutdoorActivity> list_hot_oa = OutdoorActivity.findHotOutdoorActivity();
		for(int i=0 ; i<list_hot_oa.size() ; i++) {
			if(list_hot_oa.get(i).name.length()>10)
				list_hot_oa.get(i).name = list_hot_oa.get(i).name.substring(0, 10) + "...";
		}
		
		//熱門老師 (且將其自我介紹字數限制在15個字內)
		List<Member> list_member = new ArrayList<Member>();
		list_member = Member.findMemberByViewCount(-1);
		List<Member> list_teacher = new ArrayList<Member>();
		
		for(int i=0 ; i<list_member.size() ; i++) {
			Member m = list_member.get(i);
			if(m.roleList.contains(ROLE.TEACHINGPLANNER))
				list_teacher.add(m);
			
			if(list_teacher.size()==6)
				break;
			else if(i==list_member.size()-1)
				break;
		}
		
		for(int i =0 ;i<list_teacher.size();i++) {
			if(list_teacher.get(i).mood!=null && list_teacher.get(i).mood.length()>15)
				list_teacher.get(i).mood = list_teacher.get(i).mood.substring(0,15)+"...";
		}
		
		List<OutdoorActivity> list_oas_tab3 = new ArrayList<OutdoorActivity>();
		List<OutdoorActivity> list_oas_tab4 = new ArrayList<OutdoorActivity>();
		List<OutdoorActivity> list_oas_tab5 = new ArrayList<OutdoorActivity>();
		List<OutdoorActivity> list_oas_tab6 = new ArrayList<OutdoorActivity>();
		
		Tag t = Tag.findTagByName(Application.TAB_3);
		if(null != t) {
			list_oas_tab3 = OutdoorActivity.findByTag(t.id);
			if(list_oas_tab3!=null && list_oas_tab3.size()>2) 
				list_oas_tab3 = list_oas_tab3.subList(0, 2);
		}
		
		t = Tag.findTagByName(Application.TAB_4);
		if(null != t) {
			list_oas_tab4 = OutdoorActivity.findByTag(t.id);
			if(list_oas_tab4!=null && list_oas_tab4.size()>2) 
				list_oas_tab4 = list_oas_tab4.subList(0, 2);
		}
		
		t = Tag.findTagByName(Application.TAB_5);
		if(null != t) {
			list_oas_tab5 = OutdoorActivity.findByTag(t.id);
			if(list_oas_tab5!=null && list_oas_tab5.size()>2) 
				list_oas_tab5 = list_oas_tab5.subList(0, 2);
		}
		
		list_oas_tab6 = OutdoorActivity.findAll();
		if(list_oas_tab6!=null && list_oas_tab6.size()>2)
			list_oas_tab6 = list_oas_tab6.subList(0, 2);		
		
		for(int i=0 ; i<list_tp.size() ; i++)
			if(list_tp.get(i).name.length()>10)
				list_tp.get(i).name = list_tp.get(i).name.substring(0, 10) + "...";
		
		for(int i=0 ; i<list_oa.size() ; i++)
			if(list_oa.get(i).name.length()>10)
				list_oa.get(i).name = list_oa.get(i).name.substring(0, 10) + "...";
		
		if(list_oas_tab3!=null){
			for(int i=0 ; i<list_oas_tab3.size() ; i++)
				if(list_oas_tab3.get(i).name.length()>10)
					list_oas_tab3.get(i).name = list_oas_tab3.get(i).name.substring(0, 10) + "...";
		}
		
		if(list_oas_tab4!=null){
			for(int i=0 ; i<list_oas_tab4.size() ; i++)
				if(list_oas_tab4.get(i).name.length()>10)
					list_oas_tab4.get(i).name = list_oas_tab4.get(i).name.substring(0, 10) + "...";
		}
		
		if(list_oas_tab5!=null){
			for(int i=0 ; i<list_oas_tab5.size() ; i++)
				if(list_oas_tab5.get(i).name.length()>10)
					list_oas_tab5.get(i).name = list_oas_tab5.get(i).name.substring(0, 10) + "...";
		}
		if(list_oas_tab6!=null){
			for(int i=0 ; i<list_oas_tab6.size() ; i++)
				if(list_oas_tab6.get(i).name.length()>10)
					list_oas_tab6.get(i).name = list_oas_tab6.get(i).name.substring(0, 10) + "...";
		}
		// 確認是否有尚未審核的老師
		int uncheck_size = 0; 
		Member m = MyUtil.getCurrentUser();
		List<Member> uncheck = Member.findUncheckTeacher();
		if(m != null && m.roleList.contains(ROLE.ADMIN) && uncheck.size()>0) {
			uncheck_size = uncheck.size();
		}
		
		boolean hasNoReply = false;
		if(m != null) {
			if(models.Queue.hasUnReplyRelationMember(m.id)) {
				hasNoReply = true;
			}
		}
		
		render("Application/index.html" , msg ,
				list_oa , list_hot_oa,  list_oas_tab3, list_oas_tab4, list_oas_tab5, list_oas_tab6,
				list_tp , list_teacher,
				uncheck_size, hasNoReply);
	}
	
	public static void searchOA(Long tagId) {
		int activityTab =1;
		List<OutdoorActivity> listOA=null;
		Tag tag = Tag.findById(tagId);
		if(tagId==ALL) {
			Member member = MyUtil.getCurrentUser();
			if(member!=null && member.roleList.contains(ROLE.ADMIN))
				listOA = OutdoorActivity.listPublishOAByAdmin();
			else
				listOA = OutdoorActivity.listPublishOA();
			if (listOA != null) {
				for (OutdoorActivity oa : listOA) {
					if(oa.name.length()>10)
						oa.name = oa.name.substring(0, 10) + "...";
				}
			}
			render("Application/searchOA.html", listOA, activityTab);
		}
		else {
			listOA = OutdoorActivity.findByTag(tagId);
			if (listOA != null) {
				for (OutdoorActivity oa : listOA) {
					if(oa.name.length()>10)
						oa.name = oa.name.substring(0, 10) + "...";
				}
			}
		}
		
		render("Application/searchOA.html",listOA, activityTab, tag);
		
		//Tag selectedTag = Tag.findById(tagId);
		//TagCategory selectedTagCategory = TagCategory.findByTag(selectedTag);
		
		//render("Application/searchOA.html",listOA,selectedTag,selectedTagCategory);
	}
	
	public static void searchTP(Long tagId) {
		int activityTab =3;
		List<TeachingPlan> listTP=null;
		Tag tag = Tag.findById(tagId);
		
			if(tagId==ALL) {
				Member member = MyUtil.getCurrentUser();
				if(member!=null && member.roleList.contains(ROLE.ADMIN))
					listTP = TeachingPlan.listPublishTpByAdmin();
				else
					listTP = TeachingPlan.listPublishTp();
				if (listTP != null) {
					for (TeachingPlan tp : listTP) {
						if(tp.name.length()>23)
							tp.name = tp.name.substring(0, 23) + "...";
					}
				}
				render("Application/searchTP.html", listTP, activityTab);
			}
			else if(tag!=null) {
				listTP = TeachingPlan.findByTag(tag);
				if (listTP != null) {
					for (TeachingPlan tp : listTP) {
						if(tp.name.length()>23)
							tp.name = tp.name.substring(0, 23) + "...";
					}
				}
			}
		
		
		render("Application/searchTP.html",listTP, activityTab, tag);
	}
	
	public static void searchTeacher(Long tagId) {
		int activityTab =2;
		List<Member> listMember=null;
		Tag tag = Tag.findById(tagId);
		
		if(tagId==ALL) {
			Member member = MyUtil.getCurrentUser();
			if(member!=null && member.roleList.contains(ROLE.ADMIN))
				listMember = Member.findMemberByRoleByAdmin(ROLE.TEACHINGPLANNER);
			else
				listMember = Member.findMemberByRole(ROLE.TEACHINGPLANNER);
		}
		else if(tag!=null){
			listMember = Member.findByTag(tag);
		}
		
		//限制mood字數(16個字內)
		//temData 拿來儲存活動與教案之數量 (eg:03個活動報名中與02個教案
		for (Member member : listMember) {
			if(member.mood!=null && member.mood.length()>16)
				member.mood=member.mood.substring(0,15);
			member.tempData=OutdoorActivity.listByStatusAndUserId(member.id,1).size()+"個活動報名中與"+
						 TeachingPlan.listByPublishAndUserId(member.id, true).size()+"個教案";

			System.out.println(member.tempData);				
		}
		
		render("Application/searchTeacher.html",listMember, activityTab, tag);
	}

	
	
	public static String access_token = "";
	
	public static void toLogin(){
		render("member/user_login.html");
	}
	
	public static void login(@Required String email, @Required String password) {
		Member user = Member.findUser(email);
		if(user!=null && !user.isFacebookUser) {
			//密碼加密(Hash Code)
			//String passwordEncrypt = MyUtil.passwordEncrypt(password);
			String passwordEncrypt=password;
			//因應一部分member已將 密碼加密
			if(!user.password.equals(passwordEncrypt))
				passwordEncrypt = MyUtil.passwordEncrypt(password);
			if(user.password.equals(passwordEncrypt)) {
				session.put("email", user.email);
				session.put("username", user.username);
				Logger.info("Member login success. email=" + user.email + ", username=" + user.username+", ip="+request.remoteAddress);
				Application.index(null);
			} else{
				Logger.info("Member login fail. email=" + user.email + ", password=" + password +", ip="+request.remoteAddress);
				Application.normalPage("密碼錯誤");
			}	
		}
		else {
			Application.normalPage("請使用 Facebook 帳號或 Google+登入之功能");
		}
	}

	public static MyOAuth2 FACEBOOK = new MyOAuth2(
		"https://graph.facebook.com/oauth/authorize",
		"https://graph.facebook.com/oauth/access_token",
		Conf.appID,
		Conf.appSecret
	);
		
	public static void FBlogin() {		
		Logger.info("Start Facebook login.");
		JsonObject me = null;		
	    me = WS.url("https://graph.facebook.com/me?access_token=%s", WS.encode(access_token)).get().getJson().getAsJsonObject();
	    String email = String.valueOf(me.get("email")).replace("\"", "");
		String username = String.valueOf(me.get("username")).replace("\"", "");
		Member user = Member.findUser(email);
		Logger.info("Facebook login success, email=" + email + ", username=" + username);
		
		if(user!=null){
			session.put("email", user.email);
			session.put("username", user.username);
			Application.index(null);
		}
		else {
			render("member/user_register.html",email);
		}	
	}
	

	public static void resetPwd() {
		render("member/reset_pwd.html");
	}

	
	public static void auth() {
		if(MyOAuth2.isCodeResponse()) {
	        MyOAuth2.Response response = FACEBOOK.retrieveAccessToken(authURL());
	        	access_token = response.accessToken;	
	        	FBlogin();
		}
	    FACEBOOK.retrieveVerificationCode(authURL());
	}
	
	public static void googleAuth(){
		String state = params.get("state");
		String code =params.get("code");
		

		try {
		// Google取得access_token的url
		 URL urlObtainToken =  new URL("https://accounts.google.com/o/oauth2/token");
		 HttpURLConnection connectionObtainToken =  (HttpURLConnection) urlObtainToken.openConnection();
		   
		 // 設定此connection使用POST
		 connectionObtainToken.setRequestMethod("POST");
		 connectionObtainToken.setDoOutput(true);
		  
		 // 開始傳送參數 
		 OutputStreamWriter writer  = new OutputStreamWriter(connectionObtainToken.getOutputStream());
		 writer.write("code="+code+"&");   // 取得Google回傳的參數code
		 writer.write("client_id="+Conf.googleOAuthID+"&");   // 這裡請將xxxx替換成自己的client_id
		 writer.write("client_secret="+Conf.gooleOAuthSecret+"&");   // 這裡請將xxxx替換成自己的client_serect
		 writer.write("redirect_uri=http://www.funnytrip.net/google/login&");   // 這裡請將xxxx替換成自己的redirect_uri
		 writer.write("grant_type=authorization_code");  
		 writer.close();
		 // 如果認證成功
		 if (connectionObtainToken.getResponseCode() == HttpURLConnection.HTTP_OK){
		  StringBuilder sbLines   = new StringBuilder("");
		  
		  // 取得Google回傳的資料(JSON格式)
		  BufferedReader reader = new BufferedReader(new InputStreamReader(connectionObtainToken.getInputStream(),"utf-8"));
		  String strLine = "";
		  while((strLine=reader.readLine())!=null){
		   sbLines.append(strLine);
		  }
		 
		  JsonParser parser = new JsonParser();
		  JsonObject json=  (JsonObject)parser.parse(sbLines.toString());
		  
		//已取得access_token
		GoogleLogin(json);
		 }
		 
		 
		} 
		 catch (Exception e) {
		   e.printStackTrace();
		}
		 
	}
	
	private static void GoogleLogin(JsonObject json) {
		
		String email =null;
		
		JsonObject userProfile = null;
		try{
			access_token=String.valueOf(json.get("access_token"));
			userProfile= getGoogleEmail(json);
			email = String.valueOf(userProfile.get("email")).replace("\"", "");
			//username= String.valueOf(json.get("name")).replace("\"", "");
		}
		catch (Exception e) {
			System.out.println(e.toString());
		}
		Member user = Member.findUser(email);
		Logger.info("Google login success, email=" + email);
		
		if(user!=null){
			session.put("email", user.email);
			session.put("username", user.username);
			Application.index(null);
		}
		else {
			render("member/user_register.html",email);
		}	
		
	}
	

	@SuppressWarnings("finally")
	private static JsonObject getGoogleEmail(JsonObject json){
		JsonParser parser = new JsonParser();
		JsonObject jo = null;
		try {
			 
		URL urUserInfo =   
			    new URL("https://www.googleapis.com/oauth2/v1/userinfo?access_token="+String.valueOf(json.get("access_token")).replaceAll("\"", "")); 
			HttpURLConnection connObtainUserInfo =  (HttpURLConnection) urUserInfo.openConnection();
			 
			//如果認證成功
			if (connObtainUserInfo.getResponseCode() == HttpURLConnection.HTTP_OK){
			 StringBuilder sbLines   = new StringBuilder("");
			  
			 // 取得Google回傳的資料(JSON格式)
			 BufferedReader reader = 
			  new BufferedReader(new InputStreamReader(connObtainUserInfo.getInputStream(),"utf-8"));
			 String strLine = "";
			 while((strLine=reader.readLine())!=null){
			  sbLines.append(strLine);
			 }
			 
			 
			  // 把上面取回來的資料，放進JSONObject中，以方便我們直接存取到想要的參數
			
			jo = (JsonObject) parser.parse(sbLines.toString());
			return jo;
			     
			 }
		}
		catch (JsonIOException e) {
			e.printStackTrace();
		}
		finally{
			return jo;
		}
	}
	
	static String authURL() {		
	    return play.mvc.Router.getFullUrl("Application.auth");
	}
	
	@Restrictions({@Restrict(ROLE.TEACHINGPLANNER),@Restrict(ROLE.STUDENT) })
	public static void createLicense() {
		render("create_content_license.html");	
	}
	
	public static void logout() {
		Logger.info("logout by " + session.get("email"));
		session.clear();
		Application.index(null);
	}
	
	public static void about() {
		Logger.info("request about page.");
		render("about.html");
	}
	
	public static void document() {
		Logger.info("request document page.");
		render("document.html");
	}
	
	public static void quanta() {
		Logger.info("request quanta index page.");
		render("quanta/index.html");
	}
	
}