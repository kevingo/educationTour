package controllers;

import java.io.BufferedReader;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import models.*;



import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.internal.core.search.PathCollector;
import org.json.simple.JSONObject;

import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thoughtworks.xstream.io.path.Path;

import play.Logger;
import play.Play;
import play.data.Upload;
import play.data.validation.Error;
import play.data.validation.Required;
import play.mvc.Controller;
import play.mvc.With;
import sun.awt.RepaintArea;
import Utility.Conf;
import Utility.MyUtil;
import Utility.ROLE;
import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.Restrict;
import flexjson.JSONSerializer;

@With(Deadbolt.class)
public class AppApiManager extends Controller {

	public static void login(@Required String email , @Required String password) {

		Member user = Member.login(email, password);
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		
		if(user!=null){
			response.put("result", "true");
			response.put("userid", user.id);
		}
		else {
			response.put("result", "false");
			response.put("reason", "帳號或密碼錯誤，登入失敗!");
		}
		json.put("response", response);
		
		String output = deepSerializeJson(json);
		renderJSON(output);
	}

	
	
	public static void listMyOA(@Required String email) {
		Member user = Member.findUser(email);
		
		
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		//存OA資訊
		Collection<JSONObject> OutdoorActivitys;
		
		if(null != user) {
			//找到屬於該老師的OA
			List<OutdoorActivity> listOA = OutdoorActivity.listByCreator(user.id);
			if(listOA!=null){
				response.put("result", "true");
				OutdoorActivitys = composeListOAJson(listOA);
				response.put("OutdoorActivitys", OutdoorActivitys);
			}
			else {
				response= composeFailJson();
			}
			
		} 
		else {			
			response= composeFailJson();
		}
		
		json.put("response", response);
		
		String output = deepSerializeJson(json);
		renderJSON(output);
	}
	

	
	public static void listMyTP(@Required String email) {
		Member user = Member.findUser(email);
		
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		
		//存某一OA中的TPS 且只給Id與Name
		Collection<JSONObject> TeachingPlans = new ArrayList<JSONObject>();
		
		if(null != user) {
			response.put("result", "true");		
			//找到屬於該老師的TP
			List<TeachingPlan> listTP = TeachingPlan.listByCreator(user.id);
			TeachingPlans = composeListTPJson(listTP);
			response.put("TeachingPlans", TeachingPlans);
		} 
		else {			
			response= composeFailJson();
		}
		
		json.put("response", response);

		String output = deepSerializeJson(json);
		renderJSON(output);
	}
	
	public static void listMyFavoriteTP(@Required String email){
		Member user = Member.findUser(email);
		
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		
		//存某一OA中的TPS 且只給Id與Name
		Collection<JSONObject> TeachingPlans = new ArrayList<JSONObject>();
		
		if(null != user) {
			response.put("result", "true");	
			List<TeachingPlan> listTP = new ArrayList<TeachingPlan>();
			//找到屬於該老師的TP
			if(user.favoriteTPs.size()>0)
				listTP.addAll(user.favoriteTPs);
			TeachingPlans = composeListTPJson(listTP);
			response.put("TeachingPlans", TeachingPlans);
		} 
		else {			
			response= composeFailJson();
		}
		
		json.put("response", response);

		String output = deepSerializeJson(json);
		renderJSON(output);
	}
	
	
	public static void listMyTodayOA(@Required String email){
		Member user = Member.findUser(email);
		
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		//存OA資訊
		Collection<JSONObject> OutdoorActivitys = null;
		
		if(null != user) {
			response.put("result", "true");		
			//找到屬於該老師的OA
			List<OutdoorActivity> listOA = OutdoorActivity.listTodayOAByEmail(email);
			OutdoorActivitys = composeListOAJson(listOA);
			response.put("OutdoorActivitys", OutdoorActivitys);
		} 
		else {			
			response= composeFailJson();
		}
		
		json.put("response", response);

		String output = deepSerializeJson(json);
		renderJSON(output);
		
	}
	
	
	public static void searchTeachingPlan(@Required String keyword){
		
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		
		//存某一OA中的TPS 且只給Id與Name
		Collection<JSONObject> TeachingPlans = new ArrayList<JSONObject>();
		if(keyword !=null && !keyword.equals("")) {
			response.put("result", "true");
			
			//找到 具有keyword 且公開的  TP
			List<TeachingPlan> listTP = TeachingPlan.findByKeyword(keyword);
			TeachingPlans = composeListTPJson(listTP);
			response.put("TeachingPlans", TeachingPlans);
		} 
		else {			
			response= composeFailJson();
		}
		
		json.put("response", response);

		String output = deepSerializeJson(json);
		renderJSON(output);
	}
	
	
	public static void searchOutdoorActivity(@Required String keyword){
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		//存OA資訊
		Collection<JSONObject> OutdoorActivitys = null;
		
		if(keyword !=null && !keyword.equals("")) {
			response.put("result", "true");
			
			//找到 關鍵字的 OA 且上架中的 
			List<OutdoorActivity> listOA = OutdoorActivity.findByKeyword(keyword);
			if(listOA != null)
				OutdoorActivitys = composeListOAJson(listOA);
			response.put("OutdoorActivitys", OutdoorActivitys);
		} 
		else {			
			response= composeFailJson();
		}
		
		json.put("response", response);

		String output = deepSerializeJson(json);
		renderJSON(output);
	}

	public static void showOA(@Required Long id){
		OutdoorActivity oa = OutdoorActivity.findById(id);
		
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		
		if(oa!=null){
			response = composeOAJson(oa);
		}
		else {
			response= composeFailJson();
		}
		
		json.put("response", response);

		String output = deepSerializeJson(json);
		renderJSON(output);
	}
	

	
	public static void showTP(@Required Long id){
		TeachingPlan tp = TeachingPlan.findById(id);
		
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		
		if(tp!=null){
			response = composeTPJson(tp);
		}
		else {
			response= composeFailJson();
		}
		
		json.put("response", response);

		String output = deepSerializeJson(json);
		renderJSON(output);
		
	}
	
	public static void uploadCheckinFile(@Required File uploadFile){
		// 格式為 oaid##applyId##Date(20131008)##checkTime(0830)##lat##lng
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		if(uploadFile.exists())
			response.put("result", "true");
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
	
	public static void showSingleComponentJson(@Required String style , @Required String id){
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		
		response = composeComponentToJson(style, id);
		json.put("response", response);

		String output = deepSerializeJson(json);
		renderJSON(output);
	}
	
	public static String deepSerializeJson(JSONObject json){
		
		JSONSerializer serializer = new JSONSerializer();
		serializer.deepSerialize(json);
		String output = serializer.deepSerialize(json);
		return output;
	}
	
	
	public static Collection<JSONObject> composeListOAJson(List<OutdoorActivity> listOA){
		
		//存OA資訊
		Collection<JSONObject> OutdoorActivitys = new ArrayList<JSONObject>();
		
		
		for (OutdoorActivity oa : listOA) {
			//存某一OA中的TPS 且只給Id與Name
			Collection<JSONObject> TeachingPlans = new ArrayList<JSONObject>();
			//OA基本資料
			JSONObject jsonOA = new JSONObject();
			jsonOA.put("id",oa.id);
			jsonOA.put("name",oa.name);
			jsonOA.put("gatherPoint",oa.gatherPoint);
			jsonOA.put("createDate", oa.createDate);
			if(oa.photos.size()>0)
				jsonOA.put("frontCover", ((Photo)oa.photos.toArray()[0]).path);
			else
				jsonOA.put("frontCover", null);
			
			//OA中引用的TP
			List<OAIncludeTP> listTps = OAIncludeTP.findByOA(oa.id);
			if(listTps!=null){
				for (OAIncludeTP oa_tp : listTps) {
					TeachingPlan tp = oa_tp.tp;
					//TP基本資料
					JSONObject jsonTP = new JSONObject();
					jsonTP.put("id",tp.id);
					jsonTP.put("name",tp.name);
					jsonTP.put("introduction", tp.introduction);
					TeachingPlans.add(jsonTP);
				}
			}
			jsonOA.put("TeachingPlans", TeachingPlans);
			OutdoorActivitys.add(jsonOA);
		}

		return OutdoorActivitys;
		
	}
	
	public static Collection<JSONObject> composeListTPJson(List<TeachingPlan> listTP){
		//存某一OA中的TPS 且只給Id與Name
		Collection<JSONObject> TeachingPlans = new ArrayList<JSONObject>();
		
		for (TeachingPlan tp : listTP) {
			//TP基本資料
			JSONObject jsonTP = new JSONObject();
			jsonTP.put("id",tp.id);
			jsonTP.put("name",tp.name);
			jsonTP.put("introduction",tp.introduction);
			jsonTP.put("createDate", tp.createDate);
			if(tp.photo!=null)
				jsonTP.put("frontCover", tp.photo.path);
			else
				jsonTP.put("frontCover", null);
			TeachingPlans.add(jsonTP);
		}
		return TeachingPlans;
			
	}
	
	public static JSONObject composeOAJson(OutdoorActivity oa){
		
		JSONObject response = new JSONObject();
		response.put("result", "true");
		response.put("id", oa.id);
		response.put("name", oa.name);
		response.put("createDate", oa.createDate);
		response.put("attendeeLowerLimit", oa.attendeeLowerLimit);
		response.put("attendeeUpperLimit", oa.attendeeUpperLimit);
		response.put("price", oa.price);
		response.put("applyCount", Apply.countApplyByOa(oa.id));
		response.put("gatherPoint", oa.gatherPoint);

		
		//存某一OA中的TPS 且只給Id與Name
		Collection<JSONObject> TeachingPlans = new ArrayList<JSONObject>();
		//OA中引用的TP
		List<OAIncludeTP> listTps = OAIncludeTP.findByOA(oa.id);
		for (OAIncludeTP oa_tp : listTps) {
			TeachingPlan tp = oa_tp.tp;
			//TP基本資料
			JSONObject jsonTP = new JSONObject();
			jsonTP.put("id",tp.id);
			jsonTP.put("name",tp.name);
			jsonTP.put("introduction", tp.introduction);
			TeachingPlans.add(jsonTP);
		}
		response.put("TeachingPlans", TeachingPlans);
		
		return response;
	}
	
	public static JSONObject composeTPJson(TeachingPlan tp){
		
		JSONObject response = new JSONObject();
		
		response.put("result", "true");
		response.put("id", tp.id);
		response.put("name", tp.name);
		response.put("createDate", tp.createDate);
		response.put("publish", tp.publish);
		response.put("share", tp.share);
		response.put("introduction", tp.introduction);
		
		response.put("beforeOrder", tp.tpBefore.orderNumber);
		response.put("nowOrder", tp.tpNow.orderNumber);
		response.put("afterOrder", tp.tpAfter.orderNumber);

		//行前  行中  行後
		Collection<JSONObject> tpBefore = new ArrayList<JSONObject>();
		Collection<JSONObject> tpNow = new ArrayList<JSONObject>();
		Collection<JSONObject> tpAfter = new ArrayList<JSONObject>();
		
		if(tp.tpBefore!=null){
			String[] beforeOrder = tp.tpBefore.orderNumber.split("##");
			//TP 中的 行前
			for (String obj : beforeOrder) {
				String type = obj.substring(0, obj.indexOf(','));
				String objId = obj.substring(obj.indexOf(',')+1);
				//組合成一個 json obj 
				JSONObject componentJson = composeComponentToJson(type, objId);
				tpBefore.add(componentJson);
			}
		}
		
		if(tp.tpNow!=null){
			String[] nowOrder = tp.tpNow.orderNumber.split("##");
			//TP 中的 行中
			for (String obj : nowOrder) {
				String type = obj.substring(0, obj.indexOf(','));
				String objId = obj.substring(obj.indexOf(',')+1);
				//組合成一個 json obj 
				JSONObject componentJson = composeComponentToJson(type, objId);
				tpNow.add(componentJson);
			}
		}
		
		if(tp.tpAfter!=null){
			String[] afterOrder = tp.tpAfter.orderNumber.split("##");
			//TP 中的 行後
			for (String obj : afterOrder) {
				String type = obj.substring(0, obj.indexOf(','));
				String objId = obj.substring(obj.indexOf(',')+1);
				//組合成一個 json obj 
				JSONObject componentJson = composeComponentToJson(type, objId);
				tpAfter.add(componentJson);
			}
		}

		response.put("tpBefore", tpBefore);
		response.put("tpNow", tpNow);
		response.put("tpAfter", tpAfter);
		
		return response;
	}
	
	public static JSONObject composeFailJson(){
		JSONObject response = new JSONObject();
		response.put("result", "false");
		response.put("reason", "搜尋失敗，找不到您的資料!");
		return response;
	}
	
	public static JSONObject composeComponentToJson(String type, String objId) {
		
		Long id = Long.parseLong(objId);
		JSONObject json = new JSONObject();
		if(type.equals("map")){
			models.Map m = models.Map.findById(id);
			if(m!=null){
				json.put("type","map");
				json.put("id",m.id);
				json.put("title", m.title);
				json.put("introduction", m.intro);
				json.put("lat", m.lat);
				json.put("lng", m.lng);
				json.put("address", m.address);
				json.put("phone", m.phone);
				json.put("startTime", m.startTime);
				json.put("endTime", m.endTime);
			}
		}else if (type.equals("youtube")) {
			Youtube y = Youtube.findById(id);
			if(y!=null){
				json.put("type","youtube");
				json.put("id",y.id);
				json.put("title", y.title);
				json.put("introduction", y.intro);
				json.put("url", y.url);
			}
		}else if (type.equals("text")) {
			Text t = Text.findById(id);
			if(t!=null){
				json.put("type","txt");
				json.put("id",t.id);
				json.put("title", t.title);
				json.put("introduction", t.intro);
			}
		}else if (type.equals("photo")) {			
			Photo p = Photo.findById(id);
			if(p!=null){
				json.put("type","photo");
				json.put("id",p.id);
				json.put("title", p.title);
				json.put("introduction", p.intro);
				json.put("path", p.path);
				json.put("fileName", p.fileName);
			}
		}else if (type.equals("attatch")) {
			Attatch a = Attatch.findById(id);
			if(a!=null){			
				json.put("type","attatch");
				json.put("id",a.id);
				json.put("title", a.title);
				json.put("introduction", a.intro);
				json.put("path", a.path);
				json.put("fileName", a.fileName);
			}
		}else if (type.equals("link")) {
			Link l = Link.findById(id);
			if(l!=null){
				json.put("type","link");
				json.put("id",l.id);
				json.put("title", l.title);
				json.put("introduction", l.intro);
				json.put("url", l.url);
			}
		}else if (type.equals("teacher")) {
			Teacher t = Teacher.findById(id);
			if(t!=null){
				json.put("type","teacher");
				json.put("id",t.id);
				json.put("title", t.title);
				json.put("teacherName", t.teacherName);
				json.put("photo",t.photo);
				json.put("introduction", t.introduction);
				json.put("education", t.education);
			}
		}else if (type.equals("cite")) {
			Cite c = Cite.findById(id);
			if(c!=null){
				json.put("type","cite");
				json.put("id",c.id);
				json.put("style", c.style);
				json.put("componentId", c.tid);
				json.put("introduction", c.intro);
			}
		}else {
			return json;
		}
		return json;
	}
	
	

}
