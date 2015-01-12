package controllers;

import models.*;
import models.Queue;
import play.*;
import play.data.validation.Required;
import play.libs.WS;
import play.mvc.*;
import play.mvc.Scope.RenderArgs;
import sun.security.krb5.Config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.management.Query;
import javax.swing.ListModel;
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
import flexjson.JSONSerializer;
import flexjson.transformer.StringTransformer;
import groovy.ui.SystemOutputInterceptor;

@With(Deadbolt.class)
public class AjaxManager extends Controller {
	
	public static void redirectToTest(){
		render("/Application/test.html");
	}
	public static void test(@Required String password) throws MessagingException{
		//List<TagCategory> listTagCategoryByTP = TagCategory.findByActivity("找教案");
		//JSONSerializer json = new JSONSerializer().include("id","name","tags.id","tags.name").exclude("*");
		//renderJSON(json.serialize(listTagCategoryByTP)) ;
		//System.out.println(MyUtil.getCurrentUser().email);
		//TeachingPlan tp = TeachingPlan.findById((long)1);
		//JSONSerializer json = new JSONSerializer().include("*").exclude("*");
		/*
		try {
			//MyUtil.sendMail("pasta0806@gmail.com","廣達文教基金會  報名完成信函", "testcontentNoFile");
			MyUtil.sendMail("pasta0806@gmail.com","廣達文教基金會  報名完成信函", "testcontentHasFile","/public/images/banner01.jpg");
		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		*/
		/*
		Q_TeacherApply apply = Q_TeacherApply.findApplyByDateAndUser("20131003", "A123456789");
		if(apply!=null)
			System.out.println("find it by date and userid"+apply.userId+"," );
		else
			System.out.println("not find it by date and userid" );
		*/
		
			
		//boolean connected = MyUtil.transport.isConnected();
		//Logger.info("isConnected:"+ connected);
		//System.out.println(connected);
		
	}

	@Restrict(ROLE.ADMIN)
	public static void getUnreplyMail()
	{
		//用來提示 admin 有幾封尚未回覆的意見
		int mails_count = Contact.findUnreplyList().size();
		renderText(mails_count); 
	}
	
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
	
	public static void addToFavorite(String email, Long id, String type) {
		Member m = Member.findUser(email);
		if(type.equals("tp")) {
			TeachingPlan tp = TeachingPlan.findById(id);
			m.favoriteTPs.add(tp);
			m.save();
		} else if(type.equals("oa")) {
			OutdoorActivity oa = OutdoorActivity.findById(id);
			m.favoriteOAs.add(oa);
			m.save();
		} else {
			Logger.error("Wrong type of addFavorite. Type=" + type);
		}
	}
	
	public static void removeToFavorite(String email, Long id, String type) {
		Member m = Member.findUser(email);
		if(type.equals("tp")) {
			TeachingPlan tp = TeachingPlan.findById(id);
			m.favoriteTPs.remove(tp);
			m.save();
		} else if(type.equals("oa")) {
			OutdoorActivity oa = OutdoorActivity.findById(id);
			m.favoriteOAs.remove(oa);
			m.save();
		} else {
			Logger.error("Wrong type of addFavorite. Type=" + type);
		}
	}
	
	public static void getTagCategoryJson() {
		
		List<TagCategory> listTagCategoryByTP = TagCategory.findByActivity("找教案");

		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		
		Collection<JSONObject> Categorys = new ArrayList<JSONObject>();

		if(listTagCategoryByTP!=null){
			response.put("result", "true");
			for (TagCategory tagCategory : listTagCategoryByTP) {
				JSONObject cate = new JSONObject();
				Collection<JSONObject> Tags = new ArrayList<JSONObject>();
				cate.put("id", tagCategory.id);
				cate.put("name", tagCategory.name);
				for (Tag tag : tagCategory.tags) {
					if(tag.enable){
						JSONObject tjson = new JSONObject();
						tjson.put("id", tag.id);
						tjson.put("name", tag.name);
						Tags.add(tjson);
					}
				}
				cate.put("tags", Tags);
				Categorys.add(cate);
			}
			response.put("categorys", Categorys);
		}
		else {
			response.put("result", "false");
			response.put("reason", "取得標籤類別失敗!");
		}
		json.put("response", response);
		
		String output = deepSerializeJson(json);
		renderJSON(output);
	}
	
	public static void getListTPByTag(@Required Long tagId){
		List<TeachingPlan> lisTpList =null;
		Member member = MyUtil.getCurrentUser();
		if(tagId>0){
			//by tag
			Tag tag = Tag.findById(tagId);
			lisTpList = TeachingPlan.findByTag(tag); 
		}
		else if(tagId==0){
			//by member (我的收藏)
			lisTpList=new ArrayList<TeachingPlan>();
			lisTpList.addAll(member.favoriteTPs) ;
		}
		else {
			//by member (我的教案)

			lisTpList=new ArrayList<TeachingPlan>();
			lisTpList.addAll(TeachingPlan.listByCreator(member.id)) ;
		}
		//找出擁有該tag之TP
		 
		//組合成json
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		Collection<JSONObject> TeachingPlans = new ArrayList<JSONObject>();
		
		if(lisTpList!=null){
			response.put("result", "true");
			for (TeachingPlan tp : lisTpList) {
				JSONObject tpJson = new JSONObject();
				Collection<JSONObject> beforeJson = new ArrayList<JSONObject>();
				Collection<JSONObject> nowJson = new ArrayList<JSONObject>();
				Collection<JSONObject> afterJson = new ArrayList<JSONObject>();
				tpJson.put("id", tp.id);
				tpJson.put("name", tp.name);
				tpJson.put("creator", tp.creator.username);
				
				//解析before (七大類 排除cite)
				if(tp.tpBefore!=null && tp.tpBefore.orderNumber!=null){
					String[] components = tp.tpBefore.orderNumber.split("##");
					for (String component : components) {
						if(component.startsWith("cite"))
							continue;
						JSONObject com = new JSONObject();
						String[] content = component.split(",");
						if(content.length==2){
							com.put("style", content[0]);
							com.put("id", content[1]);
							com.put("name", getComponentName(content[0],content[1]));
						}
						beforeJson.add(com);
					}
					tpJson.put("before",beforeJson);
				}	
				
				//解析now (七大類 排除cite)
				if(tp.tpNow!=null && tp.tpNow.orderNumber!=null){
					String[] components = tp.tpNow.orderNumber.split("##");
					for (String component : components) {
						if(component.startsWith("cite"))
							continue;
						JSONObject com = new JSONObject();
						String[] content = component.split(",");
						if(content.length==2){
							com.put("style", content[0]);
							com.put("id", content[1]);
							com.put("name", getComponentName(content[0],content[1]));
						}
						nowJson.add(com);
					}
					tpJson.put("now",nowJson);
				}
				
				//解析after (七大類 排除cite)
				if(tp.tpAfter!=null && tp.tpAfter.orderNumber!=null){
					String[] components = tp.tpAfter.orderNumber.split("##");
					for (String component : components) {
						if(component.startsWith("cite"))
							continue;
						JSONObject com = new JSONObject();
						String[] content = component.split(",");
						if(content.length==2){
							com.put("style", content[0]);
							com.put("id", content[1]);
							com.put("name", getComponentName(content[0],content[1]));
						}
						afterJson.add(com);
					}
					tpJson.put("after",afterJson);
				}
				TeachingPlans.add(tpJson);
			}
			
			response.put("TeachingPlans", TeachingPlans);
		}
		else {
			response.put("result", "false");
			response.put("reason", "取得教案列表元件失敗!");
		}
		
		json.put("response", response);
		String output = deepSerializeJson(json);
		renderJSON(output);
		
	}
	

	public static String getComponentName(String style, String cid) {
		//一共有 map, youtube ,text , photo , attatch , link ,teacher簡歷
		Long id=Long.parseLong(cid);
		String name = null;
		if(style.equals("map")){
			models.Map m = models.Map.findById(id);
			if(m!=null)
				name = m.title;
		}
		else if (style.equals("text")) {
			Text t = Text.findById(id);
			if(t!=null)
				name=t.title;
		}
		else if (style.equals("youtube")) {
			Youtube y = Youtube.findById(id);
			if(y!=null)
				name=y.title;
		}
		else if (style.equals("photo")) {
			Photo p = Photo.findById(id);
			if(p!=null)
				name=p.title;	
		}
		else if (style.equals("attatch")) {
			Attatch a = Attatch.findById(id);
			if(a!=null)
				name=a.title;	
		}
		else if (style.equals("link")) {
			Link l = Link.findById(id);
			if(l!=null)
				name=l.title;
		}
		else if (style.equals("teacher")) {
			Teacher t = Teacher.findById(id);
			if(t!=null)
				name=t.title;
		}		
		if(name==null){
			name="error";
		}
		return name;
	}
	
	private static String deepSerializeJson(JSONObject json){
		
		JSONSerializer serializer = new JSONSerializer();
		serializer.deepSerialize(json);
		String output = serializer.deepSerialize(json);
		return output;
	}

	public static void getTagByCategory(String category, Long activity){
		TagActivity tagActivity = TagActivity.findById(activity);
		TagCategory tagCategory = TagCategory.findByNameAndActivity(category, tagActivity.name);
		String  htmlCode = "";
		switch (activity.intValue()) {
		case 1:
			for (Tag tag : tagCategory.tags) {
				if(tag.enable)
					htmlCode+="<a href='/searchOA/"+tag.id+"'>" + tag.name + "</a> | " ;
			}
			htmlCode+="<a href='/searchOA/0'>所有活動</a>";
			renderText(htmlCode);
			break;
		case 2:
			for (Tag tag : tagCategory.tags) {
				if(tag.enable)
					htmlCode+="<a href='/searchTeacher/"+tag.id+"'>" + tag.name + "</a> | " ;
			}
			htmlCode+="<a href='/searchTeacher/0'>所有老師</a>";
			renderText(htmlCode);
			break;
		case 3:
			for (Tag tag : tagCategory.tags) {
				if(tag.enable)
					htmlCode+="<a href='/searchTP/"+tag.id+"'>" + tag.name + "</a> | " ;
			}
			htmlCode+="<a href='/searchTP/0'>所有教案</a>";
			renderText(htmlCode);
			break;
		}	
	}
	
	public static void getTagInfo(@Required Long tagId){
		Tag tag = Tag.findById(tagId);
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		if(tag!=null){
			response.put("result", "true");
			response.put("tag", tag);
			//找到此tag之category
			TagCategory cate = TagCategory.findByTag(tag);
			if(cate!=null){
				response.put("tagActivity", cate.tagActivity.id);
				response.put("tagCategory", cate.id);
			}
			
				
		}
		else {
			response.put("result", "false");
			response.put("reason", "can not find this tag!");
		}
		
		json.put("response", response);
		String output = deepSerializeJson(json);
		renderJSON(output);
		
	}
	
	public static void getOaByCreatorId(Long creatorId) {
		String html = "";
		List<OutdoorActivity> oas = OutdoorActivity.listByCreator(creatorId);
		for(OutdoorActivity oa : oas) {
			html += "<option value=" + oa.id + ">" + oa.name + "</option>";
		}
		renderText(html);
	}
	
	public static void getOaByAttendId(Long childId) {
		String html = "";
		List<OutdoorActivity> oas = OutdoorActivity.listChildOAByStatus(childId);		
		if(null != oas) {
			for(int i=0 ; i<oas.size() ; i++) {
				OutdoorActivity oa = oas.get(i);
				html += buildOAHtml(oa);
			}
		}
		
		renderText(html);
	}
	
	private static String buildOAHtml(OutdoorActivity oa) {
		
		String status = "";
		if(oa.status==OutdoorActivity.STATUS_Progress)
			status = "<span class='trip_status ts01'>"+status+"</span>";
		else if(oa.status==OutdoorActivity.STATUS_Finish)
			status = "<span class='trip_status ts03'>已截止</span>";
			
		String tags = "";
		List<Tag> ts = Tag.findTagByOA(oa.id);
		for(int i=0 ; i<ts.size() ; i++) {
			tags += "<span class='tag tag_c"+(i+1)+"'>"+ts.get(i).name+"</span>";
		}
				
		String imgSrc = "";
		if(oa.photos.size()==0)
			imgSrc = "/public/images/default/oa_default_100.png";
		else
			imgSrc = ((Photo)oa.photos.toArray()[0]).path;
		
		if(oa.name.length()>10)
			oa.name = oa.name.substring(0, 10) + "...";
		
		String s = "<div class='trip_item'>" + 
				   "<div class='trip_item_img'>" + 
				   "<div class='nailthumb-container nc80' style='overflow: hidden; padding: 0px; width: 80px; height: 80px;'>" + 
				   "<img src='"+imgSrc+"' style='position: relative; width: 121.547px; height: 80px; top: 0px; left: -20.7735px;' class='nailthumb-image'>" + 
				   "</div>" + 		
				   "<span class='trip_status ts01'>"+status+"</span>" + 
				   "<span class='trip_teacher'><a href='#'>"+oa.creator.username+"老師</a></span>" + 
				   "</div>" + 
				   "<div class='trip_item_info'>" + 
				   "<p class='trip_item_title'><a href='/oa/show/"+oa.id+"'>"+oa.name+"</a></p>" + 
				   "<p class='tags'>" + 				
				   tags + 
				   "</p>" + 
				   "<p class='trip_item_note'>"+oa.introduction+"</p>" + 
				   "</div>" + 
				   "</div>";
		return s;
	}
	
	public static void getApplyByOaId(Long oaid, int start, int end) {
		String html = "";
		List<Apply> apply = Apply.findApplyByOa(oaid);
		if(null!= apply && apply.size()>Conf.pagination) {
			//buildPagination(apply);			
			apply = apply.subList(start, end);
		}
		OutdoorActivity oa = OutdoorActivity.findById(oaid);
		html = buildHtmlByApplyAndOa(apply, oa);
		renderText(html);
	}
	
	public static void getExamByAttendId(Long childId) {
		String html = "";
		List<Examine> exams = Examine.findExamineByAttendId(childId);
		for(int i=0 ; i<exams.size() ; i++) {
			Examine exam = exams.get(i);
			OutdoorActivity oa = OutdoorActivity.findOAByExamID(exam.id);
			html += buildExamHtml(exam, oa, childId);
		}
		renderText(html);
	}
	
	private static String buildExamHtml(Examine exam, OutdoorActivity oa, Long childId) {
		
		String status = "";
		Apply apply = Apply.findApplyByOAandAttendMember(oa.id, childId);
		Answer ans = Answer.findAnsByExamAndApply(exam.id, apply.id);
		if(ans==null)
			status = "未作答";
		else
			status = "已作答";
			
		String s = "<div class='exam'>" + 
				"<p class='exam_title'><i class='icon-file-text'></i><a href='/examine/show/"+exam.id+"'>"+exam.title+"</a></p>" + 
				"<div class='nc160 nailthumb-container exam_img' style='overflow: hidden; padding: 0px; width: 160px; height: 160px;'>" +
				"<img src='/public/images/default/exam_default_100.png'/>" +
				"<p class='exam_trip_name'>"+oa.name+"</p>" +
				"<p class='exam_status'>"+status+"</p>" +
				"</div>" + 
				"<p class='exam_teacher'>" + 
				"<a href='/member/profile/"+oa.creator.id+"'>"+oa.creator.username+"老師</a>" +
				"</p>" + 
				"</div>";
		
		return s;
	}
	
	// 搜尋報名者姓名
	public static void getApplyByKw(String kw, Long oaid) {
		String html = "";
		List<Apply> apply = Apply.findApplyByOaidAndKwOnUserName(oaid, kw);
		OutdoorActivity oa = OutdoorActivity.findById(oaid);
		html = buildHtmlByApplyAndOa(apply, oa);
		renderText(html);
	}
	
	public static void saveApplyStatus(String[] status) {
		String result = "";		
		if(null != status && status.length > 0) {
			for(int i=0 ; i<status.length ; i++) {
				Long applyId = Long.parseLong(status[i].split(",")[0]);
				int new_status = Integer.parseInt(status[i].split(",")[1]);
				Apply a = Apply.findById(applyId);
				a.status = new_status;
				a.save();
			}
			result = "ok";
		}
		else {
			result = "fail";
		}
		
		renderText(result);
	}
	
	private static String buildHtmlByApplyAndOa(List<Apply> applies, OutdoorActivity oa) {
		String html = "";
	
		for(int i=0 ; i<applies.size() ; i++) {
			Apply a = applies.get(i);
			String account = a.ATMaccount !=null ? a.ATMaccount : "-";
			html += "<tr>";
			html += "<td class='ms_td1'>" + (i+1) + "</td>";
			html += "<td class='ms_td2'>" + oa.name + "</td>";
			html += "<td class='ms_td3'>" + a.username + "</td>";
			html += "<td class='ms_td4'>" + a.applyTime + "</td>";
			html += "<td class='ms_td5'>" + a.phone + "</td>";
			html += "<td class='ms_td6'>" + account + "</td>";
			
			String status = "";
			if(a.status==Apply.Unpay) {
				status += "<option id="+ a.id + "," + Apply.Unpay +" selected='selected'>已報名但未繳費</option>";
				status += "<option id="+ a.id + "," + Apply.Payed +">報名成功</option>";
				status += "<option id="+ a.id + "," + Apply.Validate+">已繳費待確認</option>";
			}
			else if(a.status==Apply.Payed) {
				status += "<option id="+ a.id + "," + Apply.Payed +" selected='selected'>報名成功</option>";
				status += "<option id="+ a.id + "," + Apply.Unpay +">已報名但未繳費</option>";
				status += "<option id="+ a.id + "," + Apply.Validate +">已繳費待確認</option>";
			}
			else if(a.status==Apply.Validate) {
				status += "<option id="+ a.id + "," + Apply.Validate +" selected='selected'>已繳費待確認</option>";
				status += "<option id="+ a.id + "," + Apply.Payed +">報名成功</option>";
				status += "<option id="+ a.id + "," + Apply.Unpay +">已報名但未繳費</option>";
			}
			
			else
				status += "<option>狀態異常，請洽管理者</option>";
			
			html += "<td class='ms_td7'><select class='status'>" + status + "</select></td>";
			html += "<td class='ms_td8'><input type='button' id="+ a.id +" class='delBtn btn btn-mini btn-danger' value='移除' onclick='delApply("+ a.id +", " + oa.id +")'></td>";
			html += "</tr>";
		}
		
		return html; 
	}
	
	public static String getPagination(Long id, String type, String kw) {
		String html = "";
		List list = new ArrayList();
		if(type.equals("apply")) {
			list = Apply.findApplyByOa(id);
		} else if(type.equals("examine")) {
			list = Examine.findExamineByOa(id, ROLE.TEACHINGPLANNER);
		} else if(type.equals("exam_search")) {
			list = Examine.findExamineByOaidAndKwOnExamineName(id, kw);
		} else if(type.equals("apply_search")) {
			list = Apply.findApplyByOaidAndKwOnUserName(id, kw);
		}
		html = buildPagination(id, list, type);
		return html;
	}
	
	private static String buildPagination(Long id, List list, String type) {
		String html = "";

		int numRow = list.size();
		int numPages = 0;
		if(numRow % Conf.pagination == 0)
			numPages = numRow / Conf.pagination;
		else
			numPages = (numRow / Conf.pagination) + 1;
		
		html += "<ul>";
		for(int i=0 ; i<numPages ; i++) {
			int start = i*Conf.pagination;
			int end = (i==(numPages-1)) ? (list.size()) : (i+1)*Conf.pagination;
			html += "<li><a href='#' onclick=getList("+id+","+start+","+end+")>"+(i+1)+"</a></li>";			
		}
		html += "</ul>";
		return html;
	}
	
	public static void delApply(Long applyId, Long oaid) {
		Apply a = Apply.findById(applyId);
		a.delete();
		List<Apply> apply = Apply.findApplyByOa(oaid);
		OutdoorActivity oa = OutdoorActivity.findById(oaid);
		String html = buildHtmlByApplyAndOa(apply, oa);
		renderHtml(html);
	}
	
	public static void getTagCategoryByTagActivity(@Required Long activity){
		TagActivity tagActivity = TagActivity.findById(activity);
		if(tagActivity!=null){
			String  htmlCode = "";
			List<TagCategory> listCategory = TagCategory.findByActivity(tagActivity.name);
			for (TagCategory cate : listCategory) {
				htmlCode+="<option value='"+cate.id+"'>"+cate.name+"</option>";
			}
			renderText(htmlCode);
		}
	}
	
	public static void getContactInfo(@Required Long cid){
		Contact contact =Contact.findById(cid);
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		if(contact!=null){
			response.put("result", "true");
			response.put("contact", contact);
		}
		else {
			response.put("result", "false");
			response.put("reason", "can not find this contact!");
		}
		
		json.put("response", response);
		String output = deepSerializeJson(json);
		renderJSON(output);
	}
	
	//需補上判斷登入中的member 與 此tp是否是相同的member ,否則 任何一位老師可打上網址直接修改別人的教案
	public static void getTeachingPlanInfo(@Required Long tpId){
		if(tpId!=null&&tpId>0){
			TeachingPlan tp = TeachingPlan.findById(tpId);
			//組合成json
			JSONObject json = new JSONObject();
			JSONObject response = new JSONObject();
			
			if(tp!=null){
				Collection<JSONObject> TagsJson = new ArrayList<JSONObject>();
				response.put("result", "true");
				response.put("id", tp.id);
				response.put("name", tp.name);
				response.put("subject", tp.subject);
				response.put("unit", tp.unit);
				response.put("createDate", tp.createDate);
				response.put("publish", tp.publish);
				response.put("share", tp.share);
				for (Tag tag : tp.tags) {
					JSONObject tagjson = new JSONObject();
					tagjson.put("id", tag.id);
					tagjson.put("name", tag.name);
					tagjson.put("color", tag.color);
					TagsJson.add(tagjson);
				}
				response.put("tags", TagsJson);
				response.put("introduction", tp.introduction);
				
				//解析before
				if(tp.tpBefore!=null){
					JSONObject Bjson = new JSONObject();
					Collection<JSONObject> ComponentsJson = new ArrayList<JSONObject>();
					Bjson.put("id", tp.tpBefore.id);
					Bjson.put("illustration",tp.tpBefore.illustration);
					Bjson.put("orderNumber", tp.tpBefore.orderNumber);
					if(tp.tpBefore.orderNumber!=null){
						String[] components = tp.tpBefore.orderNumber.split("##");
						for (String componentStr : components) {
							JSONObject ComponentJson = ComposeComponentJson(componentStr,true);
							ComponentsJson.add(ComponentJson);
						}
						Bjson.put("components", ComponentsJson);
					}
					response.put("before", Bjson);
				}
				
				//解析Now
				if(tp.tpNow!=null){
					JSONObject Bjson = new JSONObject();
					Collection<JSONObject> ComponentsJson = new ArrayList<JSONObject>();
					Bjson.put("id", tp.tpNow.id);
					Bjson.put("illustration",tp.tpNow.illustration);
					Bjson.put("orderNumber", tp.tpNow.orderNumber);
					if(tp.tpNow.orderNumber!=null){
						String[] components = tp.tpNow.orderNumber.split("##");
						for (String componentStr : components) {
							JSONObject ComponentJson = ComposeComponentJson(componentStr,true);
							ComponentsJson.add(ComponentJson);
						}
						Bjson.put("components", ComponentsJson);
					}
					response.put("now", Bjson);
				}
				
				//解析 After
				if(tp.tpAfter!=null){
					JSONObject Bjson = new JSONObject();
					Collection<JSONObject> ComponentsJson = new ArrayList<JSONObject>();
					Bjson.put("id", tp.tpAfter.id);
					Bjson.put("illustration",tp.tpAfter.illustration);
					Bjson.put("orderNumber", tp.tpAfter.orderNumber);
					if(tp.tpAfter.orderNumber!=null){
						String[] components = tp.tpAfter.orderNumber.split("##");
						for (String componentStr : components) {
							JSONObject ComponentJson = ComposeComponentJson(componentStr,true);
							ComponentsJson.add(ComponentJson);
						}
						Bjson.put("components", ComponentsJson);
					}
					response.put("after", Bjson);
				}
			}
			else {
				response.put("result", "false");
				response.put("error", "can not find this tp!");
			}
			
			json.put("response", response);
			String output = deepSerializeJson(json);
			renderJSON(output);
		}
	}
	
	//需補上判斷登入中的member 與 此oa是否是相同的member ,否則 任何一位老師可打上網址直接修改別人的活動
	public static void getOutdoorActivityInfo(@Required Long oaId) {
		if (oaId != null && oaId > 0) {
			OutdoorActivity oa = OutdoorActivity.findById(oaId);
			// 組合成json
			JSONObject json = new JSONObject();
			JSONObject response = new JSONObject();

			if (oa != null
					&& (oa.enable || oa.creator == MyUtil.getCurrentUser())) {
				Collection<JSONObject> TagsJson = new ArrayList<JSONObject>();

				response.put("result", "true");
				response.put("id", oa.id);
				response.put("name", oa.name);
				response.put("publish", oa.publish);
				response.put("regStatus", oa.status);
				for (Tag tag : oa.tags) {
					JSONObject tagjson = new JSONObject();
					tagjson.put("id", tag.id);
					tagjson.put("name", tag.name);
					tagjson.put("color", tag.color);
					TagsJson.add(tagjson);
				}
				response.put("tags", TagsJson);
				response.put("fromDate", oa.fromDate);
				response.put("fromTime", oa.fromTime);
				response.put("toDate", oa.toDate);
				response.put("toTime", oa.toTime);
				response.put("lowerLimit", oa.attendeeLowerLimit);
				response.put("upperLimit", oa.attendeeUpperLimit);
				response.put("gatherPoint", oa.gatherPoint);
				response.put("price", oa.price);
				response.put("introduction", oa.introduction);
				response.put("orderNumber", oa.componentOrder);
				
				// NFC部分******* 
				Collection<JSONObject> NFCsJson = new ArrayList<JSONObject>();
				for (NFC nfc : oa.nfc) {
					JSONObject NFCjson1 = new JSONObject();
					NFCjson1.put("id", nfc.id);
					NFCjson1.put("title", nfc.title);
					NFCjson1.put("orderNumber", nfc.componentOrder);
					NFCjson1.put("serialNumber", nfc.serialNumber);
					NFCsJson.add(NFCjson1);
				}
				response.put("NFCs", NFCsJson);
				
				// 活動的報名資料**************(先塞假資料)
				/*
				 String[] nameArray = {"點點","佛佛","布布","阿如","阿玉","僾臻","姿姿","翠翠","阿芯","彤彤","采筠","韻蓉"};
				Collection<JSONObject> ApplysJson = new ArrayList<JSONObject>();
				JSONObject Applyjson1 = new JSONObject();
				Applyjson1.put("id", "1");
				Applyjson1.put("username", "我是學生1");
				ApplysJson.add(Applyjson1);
				JSONObject Applyjson2 = new JSONObject();
				Applyjson2.put("id", "2");
				Applyjson2.put("username", "我是學生2");
				ApplysJson.add(Applyjson2);
				response.put("Applys", ApplysJson);
				*/
				
				String[] nameArray = {"點點","佛佛","布布","阿如","阿玉","姿姿","翠翠","阿芯","彤彤","采筠"};
				Collection<JSONObject> ApplysJson = new ArrayList<JSONObject>();
				for(int i=1;i<=nameArray.length;i++){
					JSONObject Applyjson1 = new JSONObject();
					Applyjson1.put("id", i);
					Applyjson1.put("username", nameArray[i-1]);
					ApplysJson.add(Applyjson1);
				}
				response.put("Applys", ApplysJson);
				
				// *********************
				// 解析元件
				if (oa.componentOrder != null) {
					Collection<JSONObject> ComponentsJson = new ArrayList<JSONObject>();
					String[] components = oa.componentOrder.split("##");
					for (String componentStr : components) {
						JSONObject ComponentJson = ComposeComponentJson(componentStr, false);
						ComponentsJson.add(ComponentJson);
					}
					response.put("components", ComponentsJson);
				}
			} else {
				response.put("result", "false");
				response.put("error", "can not find this oa!");
			}

			json.put("response", response);
			String output = deepSerializeJson(json);
			renderJSON(output);
		}
	}
	
	public static void getNFCTagInfo(@Required String NFCserialNumber){
		//取得OAID
		OutdoorActivity oa = OutdoorActivity.findByNFC(NFCserialNumber);
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		if(oa!=null){
			// 組合成json
			response.put("result", "true");
			response.put("id", oa.id);
			response.put("name", oa.name);
			response.put("publish", oa.publish);

			// NFC部分******* 
			Collection<JSONObject> NFCsJson = new ArrayList<JSONObject>();
			for (NFC nfc : oa.nfc) {
				JSONObject NFCjson1 = new JSONObject();
				NFCjson1.put("id", nfc.id);
				NFCjson1.put("title", nfc.title);
				NFCjson1.put("orderNumber", nfc.componentOrder);
				NFCjson1.put("serialNumber", nfc.serialNumber);
				NFCsJson.add(NFCjson1);
			}
			response.put("NFCs", NFCsJson);
			

			/*
			// 解析元件
			if (oa.componentOrder != null) {
				Collection<JSONObject> ComponentsJson = new ArrayList<JSONObject>();
				String[] components = oa.componentOrder.split("##");
				for (String componentStr : components) {
					JSONObject ComponentJson = ComposeComponentJson(componentStr, false);
					ComponentsJson.add(ComponentJson);
				}
				response.put("components", ComponentsJson);
			}
			*/
		}
		else {
			response.put("result", "false");
			response.put("error", "can not find this oa!");
		}
		json.put("response", response);
		String output = deepSerializeJson(json);
		renderJSON(output);
	}
	public static void getExamineInfo(@Required Long examId){
		if (examId != null && examId > 0) {
			Examine examine = Examine.findById(examId);
			// 組合成json
			JSONObject json = new JSONObject();
			JSONObject response = new JSONObject();

			if (examine != null) {
				OutdoorActivity oa = OutdoorActivity.findOAByExamID(examine.id);
				response.put("result", "true");
				response.put("id", examine.id);
				response.put("oaid",(oa==null)?"null":oa.id);
				response.put("name", examine.title);
				response.put("publish", examine.publish);
				response.put("status", examine.status);
				response.put("startDate", examine.startDate);
				response.put("startTime", examine.startTime);
				response.put("endDate", examine.endDate);
				response.put("endTime", examine.endTime);
				response.put("introduction", examine.introduction);
				response.put("explain", examine.questionExplain);
				response.put("illustration", examine.illustration);
				Collection<JSONObject> QuestionsJson = new ArrayList<JSONObject>();
				
				String[] orderNumbers = examine.orderNumber.split("##");

				for(int i =0 ; i<orderNumbers.length ; i++ ){
					Long questionId = Long.parseLong(orderNumbers[i]);
					Question q = Question.findById(questionId);
					if(q!=null){
						System.out.println("qid="+q.id);
						JSONObject QJson = new JSONObject();
						QJson.put("order", i);
						QJson.put("style", q.style);
						QJson.put("id", q.id);
						QJson.put("question", q.title);
						if(q.attatchPath!=null){
							QJson.put("filePath", q.attatchPath);
							QJson.put("fileName", q.attatchPath.substring(q.attatchPath.lastIndexOf('/')+1));
						}
						QJson.put("optionA", q.optionA);
						QJson.put("optionB", q.optionB);
						QJson.put("optionC", q.optionC);
						QJson.put("optionD", q.optionD);
						QJson.put("answer", q.answer);
						QJson.put("score", q.score);
						QJson.put("illustration", q.illustration);
						
						QuestionsJson.add(QJson);
					}
				}
				response.put("questions", QuestionsJson);

			} else {
				response.put("result", "false");
				response.put("error", "can not find this oa!");
			}

			json.put("response", response);
			String output = deepSerializeJson(json);
			renderJSON(output);
		}
	}
	
	public static void getExamineAnswerInfo(@Required Long ansId){
		Member member = MyUtil.getCurrentUser();
		if (ansId != null && ansId > 0) {
			Answer ans = Answer.findById(ansId);
			// 組合成json
			JSONObject json = new JSONObject();
			JSONObject response = new JSONObject();

			if (ans != null ) {
				
				OutdoorActivity oa = OutdoorActivity.findOAByExamID(ans.examine.id);
				response.put("result", "true");
				response.put("isTeacher",(member.roleList.contains(ROLE.TEACHINGPLANNER))?true:false);
				response.put("ansId", ans.id);
				response.put("oaid",(oa==null)?"null":oa.id);
				response.put("name", ans.examine.title);
				response.put("score", ans.score);
				response.put("bonusScore", ans.bonusScore);
				response.put("publish", ans.examine.publish);
				response.put("status", ans.examine.status);
				response.put("startDate", ans.examine.startDate);
				response.put("startTime", ans.examine.startTime);
				response.put("endDate", ans.examine.endDate);
				response.put("endTime", ans.examine.endTime);
				response.put("illustration", ans.examine.illustration);
				String[] studentAnswer = ans.ans.split("##");
				
				Collection<JSONObject> QuestionsJson = new ArrayList<JSONObject>();
				String[] orderNumbers = ans.examine.orderNumber.split("##");

				for(int i =0 ; i<orderNumbers.length ; i++ ){
					Long questionId = Long.parseLong(orderNumbers[i]);
					Question q = Question.findById(questionId);
					JSONObject QJson = new JSONObject();
					QJson.put("order", i);
					QJson.put("style", q.style);
					QJson.put("id", q.id);
					QJson.put("question", q.title);
					if(q.attatchPath!=null){
						QJson.put("filePath", q.attatchPath);
						QJson.put("fileName", q.attatchPath.substring(q.attatchPath.lastIndexOf('/')+1));
					}
					QJson.put("optionA", q.optionA);
					QJson.put("optionB", q.optionB);
					QJson.put("optionC", q.optionC);
					QJson.put("optionD", q.optionD);
					QJson.put("answer", q.answer);
					QJson.put("studentAnswer", studentAnswer[i]);
					
					String[] commentArray = ans.teacher_comment.split("##");
					String[] commentTimeArray = ans.commentTime.split("##");
					String comment=null;
					String commentTime=null;
					Pattern p = Pattern.compile(","+q.id+",.+");
					for (String comStr : commentArray) {
						Matcher m = p.matcher(comStr);
						if(m.find())
							comment = comStr.substring(comStr.indexOf(",", 1)+1); //從第二個","起算
					}
					for (String comStr : commentTimeArray) {
						Matcher m = p.matcher(comStr);
						if(m.find())
							commentTime = comStr.substring(comStr.indexOf(",", 1)+1); //從第二個","起算
					}
					
					QJson.put("teacherComment", comment);	
					QJson.put("teacherCommentTime", commentTime);	
					QJson.put("score", q.score);
					QJson.put("illustration", q.illustration);
					QuestionsJson.add(QJson);
				}
				response.put("questions", QuestionsJson);

			} else {
				response.put("result", "false");
				response.put("error", "can not find this ans!");
			}

			json.put("response", response);
			String output = deepSerializeJson(json);
			renderJSON(output);
		}
	}

	private static JSONObject ComposeComponentJson(String componentStr , boolean IsTeachingPlan) {
		String[] content = componentStr.split(",");
		JSONObject json = new JSONObject();
		if (content.length == 2) {
			String style = content[0];
			Long cid = Long.parseLong(content[1]);

			if (style.equals("text")) {
				Text c = Text.findById(cid);
				if (c != null) {
					json.put("id", c.id);
					json.put("style", style);
					json.put("title", c.title);
					json.put("intro", c.intro);
					json.put("share", c.share);
				}
			} else if (style.equals("photo")) {
				Photo c = Photo.findById(cid);
				if (c != null) {
					json.put("id", c.id);
					json.put("style", style);
					json.put("title", c.title);
					json.put("fileName", c.fileName);
					json.put("path", c.path);
					json.put("intro", c.intro);
					json.put("share", c.share);

				}
			} else if (style.equals("youtube")) {
				Youtube c = Youtube.findById(cid);
				if (c != null) {
					json.put("id", c.id);
					json.put("style", style);
					json.put("title", c.title);
					json.put("url", c.url);
					json.put("intro", c.intro);
					json.put("share", c.share);
				}
			} else if (style.equals("attatch")) {
				Attatch c = Attatch.findById(cid);
				if (c != null) {
					json.put("id", c.id);
					json.put("style", style);
					json.put("title", c.title);
					json.put("fileName", c.fileName);
					json.put("path", c.path);
					json.put("intro", c.intro);
					json.put("share", c.share);
				}
			} else if (style.equals("link")) {
				Link c = Link.findById(cid);
				if (c != null) {
					json.put("id", c.id);
					json.put("style", style);
					json.put("title", c.title);
					json.put("linkName", c.linkName);
					json.put("url", c.url);
					json.put("intro", c.intro);
					json.put("share", c.share);
				}
			} else if (style.equals("map")) {
				models.Map c = models.Map.findById(cid);
				if (c != null) {
					json.put("id", c.id);
					json.put("style", style);
					json.put("title", c.title);
					json.put("POIName", c.POIName);
					json.put("address", c.address);
					json.put("lat", c.lat);
					json.put("lng", c.lng);
					json.put("tel", c.phone);
					json.put("openTime", c.startTime);
					json.put("endTime", c.endTime);
					json.put("intro", c.intro);
					json.put("share", c.share);
				}
			} else if (style.equals("teacher")) {
				Teacher c = Teacher.findById(cid);
				if (c != null) {
					json.put("id", c.id);
					json.put("style", style);
					json.put("title", c.title);
					json.put("intro", c.introduction);
					json.put("share", c.share);
					json.put("teacherName", c.teacherName);
					json.put("education", c.education);
					if(c.photo!=null){
						json.put("photo",ComposeComponentJson("photo," + c.photo.id, true));
					}
					else {
						json.put("photo",null);
					}
					
				}
			} else if (style.equals("cite")) {
				if (IsTeachingPlan) {
					Cite c = Cite.findById(cid);
					if (c != null) {
						json.put("id", c.id);
						json.put("style", style);
						json.put("ref_style", c.style);
						json.put("intro", c.intro);
						json.put("citeId", c.tid);
						json.put("citeTpId", c.citeTP.id);
						json.put("citeTpName", c.citeTP.name);
						json.put("citeTitle", c.citeTitle);
						json.put("citeTeacher", c.citeTP.creator.username);
					}
				} else {
					OAIncludeTP c = OAIncludeTP.findById(cid);
					if (c != null) {
						json.put("id", c.id);
						json.put("style", style);
						json.put("intro", c.intro);
						json.put("citeTpId", c.tp.id);
						json.put("citeTitle", c.citeTitle);
						json.put("citeTeacher", c.tp.creator.username);
						if(c.tp.photo!=null)
							json.put("citeTpPhoto", c.tp.photo.path);
						else
							json.put("citeTpPhoto", null);
						json.put("citeTpCreateDate", c.tp.createDate);
					}
				}
			}

		}
		return json;
	}
	
	
	public  static void getMemberListByMemberGroup(Long group_id){
		//組合成json
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		Collection<JSONObject> Members = new ArrayList<JSONObject>();
		if (group_id > 0) {
			MemberGroup group = MemberGroup.findById(group_id);
			if (group != null) {
				response.put("result", "true");
				response.put("id", group.id);
				response.put("name", group.groupName);
				response.put("count", group.members.size());
				// 已加入
				if (group.members != null) {
					for (Member m : group.members) {
						JSONObject memberJson = new JSONObject();
						memberJson.put("id", m.id);
						memberJson.put("name", m.username);
						memberJson.put("email", m.email);
						memberJson.put("groupName", group.groupName);
						memberJson.put("schoolName", m.schoolName);
						memberJson.put("className", m.className);
						memberJson.put("status", "已經加入");
						Members.add(memberJson);
					}
				}

				// 尚待審核
				List<Queue> list_queue = Queue.findGroupByMasterAndGroup(group.id);
				if (list_queue != null) {
					for (Queue queue : list_queue) {
						JSONObject memberJson = new JSONObject();
						memberJson.put("id", queue.master.id);
						memberJson.put("name", queue.master.username);
						memberJson.put("email", queue.master.email);
						memberJson.put("groupName", group.groupName);
						memberJson.put("schoolName", queue.master.schoolName);
						memberJson.put("className", queue.master.className);
						memberJson.put("status", "尚未審核通過");
						Members.add(memberJson);
					}
				}
				// 已送出邀請，尚未被user接受
				list_queue = Queue.findGroupByGuestAndGroup(group.id);
				if (list_queue != null) {
					for (Queue queue : list_queue) {
						JSONObject memberJson = new JSONObject();
						memberJson.put("id", queue.guest.id);
						memberJson.put("name", queue.guest.username);
						memberJson.put("email", queue.guest.email);
						memberJson.put("groupName", group.groupName);
						memberJson.put("schoolName", queue.guest.schoolName);
						memberJson.put("className", queue.guest.className);
						memberJson.put("status", "尚未被接受");
						Members.add(memberJson);
					}
				}
				response.put("members", Members);

			} else {
				response.put("result", "false");
				response.put("reason", "找不到此班群。");
			}
		}
		else {
			//id<=0 代表要找尚待審核的Queue
			response.put("result", "true");
			response.put("id", group_id);
			//班群列表(老師creator)
			Member member = MyUtil.getCurrentUser();
			List<MemberGroup> list_groupByCreator = MemberGroup.findByCreator(member.id);

			for (MemberGroup group : list_groupByCreator) {
				//學生申請加入
				List<Queue> queues = Queue.findGroupByGuestAndGroup(group.id);
				for (Queue queue : queues) {
					JSONObject memberJson = new JSONObject();
					memberJson.put("id", queue.guest.id);
					memberJson.put("name", queue.guest.username);
					memberJson.put("email", queue.guest.email);
					memberJson.put("groupName", group.groupName);
					memberJson.put("schoolName", queue.guest.schoolName);
					memberJson.put("className", queue.guest.className);
					memberJson.put("status", "邀請尚未被接受");
					Members.add(memberJson);
					
				}
				
				//老師邀請學生加入
				queues = Queue.findGroupByMasterAndGroup(group.id);
				for (Queue queue : queues) {
					JSONObject memberJson = new JSONObject();
					memberJson.put("id", queue.master.id);
					memberJson.put("name", queue.master.username);
					memberJson.put("email", queue.master.email);
					memberJson.put("groupName", group.groupName);
					memberJson.put("schoolName", queue.master.schoolName);
					memberJson.put("className", queue.master.className);
					memberJson.put("status", "尚未審核通過");
					Members.add(memberJson);
				}
			}
			response.put("members", Members);	
		}
		json.put("response", response);
		String output = deepSerializeJson(json);
		renderJSON(output);
	}
	
	public  static void getMemberGroupByTeacher(){
		Member member = MyUtil.getCurrentUser();
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		Collection<JSONObject> MemberGroups = new ArrayList<JSONObject>();
		if(member!=null){
			response.put("result", "true");
			List<MemberGroup> groups = MemberGroup.findByCreator(member.id);
			for (MemberGroup memberGroup : groups) {
				JSONObject groupJson = new JSONObject();
				groupJson.put("id", memberGroup.id);
				groupJson.put("name", memberGroup.groupName);
				groupJson.put("createDate", memberGroup.createDate);
				groupJson.put("memberSize", memberGroup.members.size());
				MemberGroups.add(groupJson);
			}
			response.put("groups", MemberGroups);
		}
		else {
			response.put("result", "false");
			response.put("reason", "無此老師帳號資料。");
		}
		json.put("response", response);
		String output = deepSerializeJson(json);
		renderJSON(output);
		
	}
	
	public static void getMemberListByKeyword(@Required Long group_id ,@Required String schoolName, @Required String grade , @Required String className , @Required String email){
		MemberGroup group = MemberGroup.findById(group_id);
		//組合成json
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		Collection<JSONObject> Members = new ArrayList<JSONObject>();
		if(group!=null){
			String fullCassName=null;
			if(!grade.equals("") && !className.equals(""))
				fullCassName = grade+" 年 " + className + " 班 ";
			List<Member> listMembers = Member.findMemberByKeyword(schoolName,fullCassName,email);
			
			if(listMembers!=null){
				response.put("result", "true");

				for (Member member : listMembers) {
					JSONObject memberJson = new JSONObject();
					memberJson.put("id", member.id);
					memberJson.put("name", member.username);
					memberJson.put("email", member.email);
					memberJson.put("schoolName", member.schoolName);
					memberJson.put("className", member.className);
					if(group.members.contains(member))
						memberJson.put("status", "已加入");
					else 
						memberJson.put("status", "尚未加入");
					Members.add(memberJson);
				}
				response.put("members", Members);
			}
			else {
				response.put("result", "false");
				response.put("reason", "此條件無任何學生。");
			}
		}
		else {
			response.put("result", "false");
			response.put("reason", "無此班群。");
		}
		
		json.put("response", response);
		String output = deepSerializeJson(json);
		renderJSON(output);
	}

}