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
import javax.swing.text.AbstractDocument.Content;

import org.apache.commons.io.IOUtils;
//import org.json.*;

//import java.util.HashMap;
//import java.util.Map;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;


import models.Contact;
import models.Member;
import models.Tag;
import models.TagActivity;
import models.TagCategory;
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


public class Admin extends Controller {
	
	@Before
	public static void getTag() {
		Application.getTag();	   
	}
	
	public static void index(){
		Member member = MyUtil.getCurrentUser();
		if(member!=null && member.roleList.contains(ROLE.ADMIN)){
			//Tag Activity
			List<TagActivity> listTagActivity = TagActivity.findAll();
			List<TagCategory> listTagCategory = TagCategory.findAll();
			
			//Contact list
			List<Contact> list_contact = Contact.findAll();
			
			
			render("/management/admin_manage.html",listTagActivity,listTagCategory,list_contact);
		}
	}
	
	public static void replyContact(@Required Long contactId , @Required String replyContent , @Required String status) throws AddressException, MessagingException{
		Member member = MyUtil.getCurrentUser();
		if(member!=null && member.roleList.contains(ROLE.ADMIN)){
			Contact contact = Contact.findById(contactId);
			if(contact!=null){
				//儲存
				contact.re_content+=replyContent+"##";
				contact.re_count++;
				contact.re_time=MyUtil.now();
				contact.reviewer=member;
				contact.status = status;
				contact.replied=true;
				contact.save();
				
				//寄信    設定標題
				String title =" Re: "+contact.subject;
				MyUtil.sendMail(contact.email, title, contact.content);
				
				renderText("回覆完成 。");
			}
			else {
				renderText("查無此意見。");
			}
		}
	}
	
	public static void contactUs(){
		Member member = MyUtil.getCurrentUser();
		//若有登入則帶入資訊
		render("Application/contactUs.html",member);
	}
	
	public static void recieveContactUs(@Required String category , @Required String name , @Required String email , String phone , @Required String subject , @Required String content){
		Contact contact = new Contact();
		contact.category = category;
		contact.name=name;
		contact.email=email;
		contact.phone=phone;
		contact.subject=subject;
		contact.content=content;
		contact.save();
		renderText("已完成送出 。");
	}


	
}
