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
import models.Examine;
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


public class TagManager extends Controller {
	
	@Before
	public static void getTag() {
		Application.getTag();	   
	}
	
	@Restrict(ROLE.ADMIN)
	public static void createTagActivity(@Required Long activityId , @Required String activityName){
		TagActivity tagActivity = null;
		if(activityId>0)
			tagActivity= TagActivity.findById(activityId);
		else
			tagActivity = new TagActivity();
		
		if(tagActivity!=null && activityName!=null && !activityName.equals("")){
			tagActivity.name = activityName;
			tagActivity.save();
			renderText("變更完成");
		}
		else {
			renderText("變更失敗");
		}
	}
	@Restrict(ROLE.ADMIN)
	public static void deleteTagActivity(@Required Long activityId ){
		TagActivity tagActivity =  TagActivity.findById(activityId);
		if(tagActivity!=null){
			//刪除大類別，需先將FK刪除 ,找出tagCategory中有用到此activity者
			List<TagCategory> listCate = TagCategory.findByActivity(tagActivity.name);
			for (TagCategory tagCategory : listCate) {			
				deleteTagCateogry(tagCategory.id);
			}
			tagActivity.delete();
			renderText("刪除大類別成功");
			
		}
		else {
			renderText("刪除大類別失敗");
		}
	}

	@Restrict(ROLE.ADMIN)
	public static void createTagCategory(@Required Long activityId , @Required Long categoryId ,@Required String categoryName){
		TagActivity tagActivity = TagActivity.findById(activityId);
		TagCategory tagCategory = null;
		if(categoryId>0)
			tagCategory =TagCategory.findById(categoryId);
		else
			tagCategory= new TagCategory();
		
		if(tagActivity!=null && tagCategory!=null){
			tagCategory.name=categoryName;
			tagCategory.tagActivity=tagActivity;
			tagCategory.save();
			renderText("變更完成");
		}
		else {
			renderText("變更失敗");
		}
	}
	
	@Restrict(ROLE.ADMIN)
	public static void deleteTagCateogry(@Required Long categoryId){
		TagCategory category = TagCategory.findById(categoryId);
		if(category!=null){
			//將其category底下的tagId 先記錄起來後 再刪除
			List<Long>  listTagId = new ArrayList<Long>();
			for (Tag t : category.tags) {
				if(!listTagId.contains(t.id))
					listTagId.add(t.id);
			}
			category.delete();
			
			//刪除tag
			for (Long tagid : listTagId) {
				Tag tag = Tag.findById(tagid);
				if(tag!=null)
					tag.delete();
			}
			renderText("刪除標籤分類成功");
		}
		else {
			renderText("刪除標籤分類失敗");
		}
	}

	@Restrict(ROLE.ADMIN)
	public static void createTag(@Required Long t_id, @Required String name, @Required String color , @Required Long categoryId , @Required int enable)
	{
		Tag tag = Tag.findById(t_id);
		if (tag == null) {
			tag = new Tag();
		}
		
		TagCategory tagCategory = TagCategory.findById(categoryId);
		if(tagCategory!=null){
			
			//從舊的category中移除
			TagCategory cate= TagCategory.findByTag(tag);
			if(cate!=null){
				cate.tags.remove(tag);
				cate.save();
			}
			tag.name=name;
			tag.color="#"+color;
			tag.enable= (enable==1)?true:false;
			tag.save();
			
			//加入到新的category中
			if(!tagCategory.tags.contains(tag))
				tagCategory.tags.add(tag);
			tagCategory.save();
			renderText("儲存成功。");
		}
		else {
			renderText("儲存失敗，請確認選擇的分類。");
		}		
	}
	
	@Restrict(ROLE.ADMIN)
	public static void deleteTag(@Required Long t_id){
		Tag tag = Tag.findById(t_id);
		
		if(tag!=null){
			//找出存在哪一個Category中
			//從舊的category中移除
			TagCategory cate= TagCategory.findByTag(tag);
			if(cate!=null){
				cate.tags.remove(tag);
				cate.save();
			}
			tag.delete();
			renderText("刪除成功。");
		}
		else
			renderText("刪除失敗，查無此標籤。");
	}

}
