package controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.commons.io.IOUtils;
//import org.json.*;

//import java.util.HashMap;
//import java.util.Map;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;


import models.Apply;
import models.Contact;
import models.Member;
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
import com.sun.xml.internal.bind.v2.model.core.ID;

import controllers.deadbolt.Restrict;


public class ApplyManager extends Controller {
	
	@Before
	public static void getTag() {
		Application.getTag();	   
	}

	@Restrict(ROLE.TEACHINGPLANNER)
	public static void searchApply(String keyword,Long oaid)
	{
		List<Apply> listApply = Apply.findApplyByOaidAndKwOnUserName(oaid,keyword);
		render(listApply);
	}
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void listApplyByOA(Long oaid)
	{
		List<Apply> listApply = Apply.findApplyByOa(oaid);
		render(listApply);
	}
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void resendInvite(Long applyid) throws AddressException, MessagingException{
		Apply apply = Apply.findById(applyid);
		String subject = "您有  \""+ apply.oa.name + "\" 的活動邀請尚未確認" ;
		String content = "您有  \""+ apply.oa.name + "\" 的活動邀請尚未確認，請至  \"我的活動\" 中確認是否參與，謝謝。" ;
		MyUtil.sendMail(apply.regMember.email, subject , content);
	}
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void deleteApply(Long applyid) throws AddressException, MessagingException{
		Apply apply = Apply.findById(applyid);
		
		String subject = "您有  \""+ apply.oa.name + "\" 的活動報名已被老師刪除" ;
		String content = "您有  \""+ apply.oa.name + "\" 的活動報名紀錄已被老師刪除，可於  \"我的活動\" 中查詢，謝謝。" ;
		MyUtil.sendMail(apply.regMember.email, subject , content);
		
		apply.status=Apply.Reject;
		apply.save();
	}
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void updateApplyStatus(Long applyid, int status) throws AddressException, MessagingException{
		Apply apply = Apply.findById(applyid);
		
		String statusStr = "";
		switch (status) {
		case 0:
			statusStr="邀請中，家長尚未確認";
			break;
		case 1:
			statusStr="已報名，尚未繳費";		
			break;
		case 2:
			statusStr="已繳費，繳費資料驗證中";	
			break;
		case 3:
			statusStr="已確認繳費資料";	
			break;
		case 4:
			statusStr="(被)拒絕參加";	
			break;
		}
		
		String subject = "您有  \""+ apply.oa.name + "\" 的活動報名狀態已被老師變更為 "+"\"" + statusStr + "\"" ;
		String content = "您有  \""+ apply.oa.name + "\" 的活動報名紀錄已被老師刪除，可於  \"我的活動\" 中查詢，謝謝。" ;
		MyUtil.sendMail(apply.regMember.email, subject , content);
		
		apply.status=status;
		apply.save();
	}
	
	
	
}
