package controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.relation.Role;

import org.apache.commons.lang.StringEscapeUtils;

import com.itextpdf.text.pdf.PdfStructTreeController.returnType;
import com.sun.corba.se.spi.activation.Server;

import models.Apply;
import models.Attatch;
import models.Examine;
import models.Link;
import models.Map;
import models.Member;
import models.NFC;
import models.OAIncludeTP;
import models.OutdoorActivity;
import models.Photo;
import models.Tag;
import models.TagCategory;
import models.Teacher;
import models.TeachingPlan;
import models.Text;
import models.ViewCountHistory;
import models.Youtube;
import play.Logger;
import play.data.validation.Required;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;
import Utility.MyUtil;
import Utility.ROLE;
import controllers.TeachingPlanManager.authTeachingPlan;
import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.Restrict;

@With(Deadbolt.class)
public class OutdoorActivityManager extends Controller {
	
	@Before
	public static void getTag() {
		Application.getTag();	   
	}
	
	public static class authOutdoorActivity{
		boolean allowRead = false;		//可看到某一特定活動
		boolean allowWrite = false;		//可編輯修改某一特定活動
		boolean allowApply=false;		//可報名
		
		//針對特定oa的權限設定
		authOutdoorActivity(Member member,OutdoorActivity oa){
			if(oa==null) 
				return;
			else if(member==null){
				//判斷可Read(即可看)[當creator設定OA為公開時(沒有登入時)]			
				if(oa.publish){
					this.allowRead=true;
					return;
				}
			}
			else {
				//admin全開
				if(member.roleList.contains(ROLE.ADMIN)){
					this.allowWrite=true;
					this.allowRead=true;
					this.allowApply=true;
				}	
				//判斷是否被admin 關閉 或 creator 自己刪除
				if(oa.enable){
					//判斷可Write (登入者=建立者 )
					if(oa.creator.equals(member)){
						this.allowWrite=true;
						this.allowRead=true;
					}
					//判斷可apply[當oa為公開時 && status為報名中 && member有登入]
					if(oa.publish){
						this.allowRead=true;
						if(oa.status==OutdoorActivity.STATUS_Progress)
							this.allowApply=true;
					}
				}	
			}
		}
		
		//針對 登入者 對於戶外活動功能之權限
		authOutdoorActivity(Member member){
			if(member==null)
				return;
			else {
				//判斷是否可使用 評量管理功能 當身分具有老師或admin時可使用
				if (member.roleList.contains(ROLE.TEACHINGPLANNER))
					this.allowWrite = true;
				else if (member.roleList.contains(ROLE.ADMIN))
					this.allowWrite = true;
			}
		}
	}
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void createAgree() {
		Member member = MyUtil.getCurrentUser();
		authTeachingPlan auth = new authTeachingPlan(member);
		if (auth.allowWrite) {
			render("oa/create_trip_agree.html");	
		} else {
			Application.index("您尚未登入或權限不足");
		}
	}
	
	
	public static void search(String kw) {
		List<OutdoorActivity> oas = new ArrayList<OutdoorActivity>();
		oas = OutdoorActivity.findByKeyword(kw);
		if (oas != null) {
			for (OutdoorActivity oa : oas) {
				if(oa.name.length()>10)
					oa.name = oa.name.substring(0, 10) + "...";
			}
		}
		render("Application/searchOA_kw.html", oas, kw);
	} 
	
	public static void index(Long id) {
		
		Member member = MyUtil.getCurrentUser();
		authOutdoorActivity auth = new authOutdoorActivity(member);
		if(auth.allowWrite){
			List<TagCategory> listCategory = TagCategory.findByActivity("找活動");
			if(id!=0) {
				OutdoorActivity oa = OutdoorActivity.findById(id);
				authOutdoorActivity authOA = new authOutdoorActivity(member,oa);
				if(authOA.allowWrite)
					render("oa/create_trip.html",listCategory,oa);
				else {
					Logger.info("user="+member.email+" try to edit oa:"+ id + ",but it's not belong to him/her.");
					index((long) 0);
				}
			}
			else {
				render("oa/create_trip.html", listCategory);
			}
		}
		else {
			Application.index("您尚未登入或權限不足");
		}
	}
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void create_basicInfo(@Required Long oaid , @Required String trip_name, @Required int publish , @Required int regStatus , @Required Long[] element ,
										@Required String fromDate , @Required String toDate , @Required String fromTime , @Required String toTime ,@Required String regDue,
										@Required int lowerLimit , @Required int upperLimit , @Required String gatherPoint , @Required int price){
		Member member = MyUtil.getCurrentUser();
		authOutdoorActivity auth = new authOutdoorActivity(member);
		if(auth.allowWrite) {
			OutdoorActivity oa = null;
			if(oaid==null || oaid==0)
				oa = new OutdoorActivity();
			else {
				if(oaid>0)
					oa = OutdoorActivity.findById(oaid);
					authOutdoorActivity authOA = new authOutdoorActivity(member,oa);
					if(!authOA.allowWrite){
						Logger.info("user="+member.email+" try to edit oa:"+ oaid + ",but it's not belong to him/her.");
						oa = null;
					}
			}
			if(oa!=null){
				oa.creator = member;
				oa.createDate=MyUtil.now();
				oa.name = trip_name;
				oa.publish= (publish==1)? true:false;
				oa.status = regStatus;
				//tag
				for (Long tid : element) {
					Tag tag = Tag.findById(tid);
					if(tag!=null){
						if(oa.tags==null)
							oa.tags = new HashSet();
						if(!oa.tags.contains(tag))
							oa.tags.add(tag);
					}
				}
				oa.fromDate=fromDate;
				oa.toDate =toDate;
				oa.fromTime=fromTime;
				oa.toTime =toTime;
				oa.regDue=regDue;
				
				oa.attendeeLowerLimit=lowerLimit;
				oa.attendeeUpperLimit=upperLimit;
				oa.gatherPoint=gatherPoint;
				oa.price=price;
				oa.save();
				MyUtil._oaid=oa.id;
			}
			else {
				Application.index("您無此權限。");
			}
		}
		else {
			Application.index("您尚未登入或權限不足");
		}
		
	}
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void create_detail(String introduction , String[] seq ,
			 @Required Long[] html_id , String[] html_fieldname , String[] html_content , String[] html_share ,
			 @Required Long[] pic_id , String[] pic_fieldname , String[] pic_filename ,  String[] pic_memo , String[] pic_share , 
			 @Required Long[] youtube_id , String[] youtube_fieldname , String[] youtube_web , String[] youtube_memo , String[] youtube_share,
			 @Required Long[] file_id , String[] file_fieldname , String[] file_filename  , String[] file_memo , String[] file_share , 
			 @Required Long[] web_id , String[] web_fieldname , String[] web_name,String[] web_web , String[] web_memo , String[] web_share,
			 @Required Long[] spot_id , String[] spot_fieldname , String[] spot_name , String[] spot_addr , String[] spot_lat ,String[] spot_lng , String[] spot_tel , String[] spot_open , String[] spot_end , String[] spot_memo, String[] spot_share,
			 @Required Long[] res_id , String[] res_fieldname , String[] res_name , String[] res_grad , String[] res_count,String[] res_content , String[] res_pic_filename , String[] res_share,
			 @Required Long[] cite_id , String[] ref_style , String[] ref_id , String[] ref_title , String[] ref_tpid ,  String[] ref_memo	){

		Member member = MyUtil.getCurrentUser();
		OutdoorActivity oa = OutdoorActivity.findById(MyUtil._oaid);
		authOutdoorActivity authOA = new authOutdoorActivity(member,oa);
			
		if(authOA.allowWrite){
			//reset 八類的index 與 orderNumber (全域變數)
			MyUtil.ResetComponentIndexVar();
			//紀錄老師學經歷的count
			int res_gradCount=0;
			//依序儲存元件
			if(seq!=null){
				for (String componentStr : seq) {
					String style = componentStr.substring(componentStr.indexOf(',')+1);
					if(style.equals("text")){
						MyUtil.SaveTextComponent(html_id[MyUtil._html_index],html_fieldname[MyUtil._html_index] ,html_content[MyUtil._html_index] , html_share[MyUtil._html_index]);			
					}else if (style.equals("photo")) {
						MyUtil.SavePhotoComponent(pic_id[MyUtil._pic_index] ,pic_fieldname[MyUtil._pic_index] ,pic_memo[MyUtil._pic_index] , pic_filename[MyUtil._pic_index] , pic_share[MyUtil._pic_index] ,oa);					
					}else if (style.equals("youtube")) {
						MyUtil.SaveYoutubeComponent(youtube_id[MyUtil._youtube_index],youtube_fieldname[MyUtil._youtube_index], youtube_web[MyUtil._youtube_index], youtube_memo[MyUtil._youtube_index], youtube_share[MyUtil._youtube_index]);				
					}else if (style.equals("attatch")) {
						MyUtil.SaveAttatchComponent(file_id[MyUtil._file_index],file_fieldname[MyUtil._file_index] , file_filename[MyUtil._file_index], file_memo[MyUtil._file_index], file_share[MyUtil._file_index] ,oa);
					}else if (style.equals("link")) {
						MyUtil.SaveLinkComponent(web_id[MyUtil._web_index],web_fieldname[MyUtil._web_index]  ,web_name[MyUtil._web_index], web_web[MyUtil._web_index] , web_memo[MyUtil._web_index] , web_share[MyUtil._web_index]);
					}else if (style.equals("map")) {
						MyUtil.SaveMapComponent(spot_id[MyUtil._spot_index],spot_fieldname[MyUtil._spot_index],spot_name[MyUtil._spot_index],spot_addr[MyUtil._spot_index] ,spot_lat[MyUtil._spot_index],spot_lng[MyUtil._spot_index],spot_tel[MyUtil._spot_index] ,spot_open[MyUtil._spot_index],spot_end[MyUtil._spot_index],spot_memo[MyUtil._spot_index],spot_share[MyUtil._spot_index]);
					}else if (style.equals("teacher")) {
						List<String> grad = new ArrayList<String>();
						for(int i=0;i<Integer.parseInt(res_count[MyUtil._res_index]);i++){
							grad.add(res_grad[res_gradCount+i]);
						}
						res_gradCount+=Integer.parseInt(res_count[MyUtil._res_index]);
						MyUtil.SaveTeacherComponent(res_id[MyUtil._res_index],res_fieldname[MyUtil._res_index], res_name[MyUtil._res_index], grad  ,res_content[MyUtil._res_index], res_pic_filename[MyUtil._res_index],res_share[MyUtil._res_index],oa);
					}else if (style.equals("cite")) {
						MyUtil.SaveOutdoorActivityCiteComponent(cite_id[MyUtil._ref_index],ref_title[MyUtil._ref_index], ref_tpid[MyUtil._ref_index] ,ref_memo[MyUtil._ref_index],oa);
					}		
				}
			}
			
			oa.introduction=introduction;
			oa.componentOrder=MyUtil._orderNumber;
			oa.save();
			oa.photos.clear();
			oa.photos.addAll(MyUtil.getFrontCoverForOA(oa.id));
			oa.save();
			
			//reset _oaid
			MyUtil._oaid=(long) 0;
			//導回到detail show之頁面
			show(oa.id);
		}
		else {
			Application.index("您尚未登入或權限不足");
		}
	}
	
	@Restrict(ROLE.ADMIN)
	public static void disable(Long id) {
		Member member = MyUtil.getCurrentUser();
		OutdoorActivity oa = OutdoorActivity.findById(id);
		authOutdoorActivity authOA = new authOutdoorActivity(member,oa);
		if(authOA.allowWrite){
			oa.enable = false;
			oa.save();
		}
	}
	
	@Restrict(ROLE.ADMIN)
	public static void enable(Long id) {
		Member member = MyUtil.getCurrentUser();
		OutdoorActivity oa = OutdoorActivity.findById(id);
		authOutdoorActivity authOA = new authOutdoorActivity(member,oa);
		if(authOA.allowWrite){
			oa.enable = true;
			oa.save();
		}
	}
	
	public static void delete(Long id) {
		Member member = MyUtil.getCurrentUser();
		OutdoorActivity oa = OutdoorActivity.findById(id);
		authOutdoorActivity authOA = new authOutdoorActivity(member,oa);
		if(authOA.allowWrite){
			oa.enable = false;
			oa.save();
		}
	}
	
	public static void getSlide(Long oaid) {
		Logger.info(request.remoteAddress + " access oa slide, id=" + oaid);	
		OutdoorActivity oa = OutdoorActivity.findById(oaid);
		Member m = MyUtil.getCurrentUser();
		authOutdoorActivity authOA = new authOutdoorActivity(m,oa);
		if(authOA.allowRead) {
			StringBuilder basicBuilder = new StringBuilder();
			StringBuilder outlineBuilder = new StringBuilder();
			StringBuilder detailBuilder = new StringBuilder();
			
			String componentOrder = oa.componentOrder;

			// Process 基本頁面
			basicBuilder.append(buildSlideBasicContent(oa));
			
			// Process 大綱 (10個元件標題為一頁 , 至多3頁)& 詳細內容
			if(null != componentOrder && !componentOrder.equals("")) {
				String [] components = componentOrder.split("##");
				
				// Outline
				outlineBuilder.append(MyUtil.buildSlideOutlineContent(components,false));
				
				//元件內容
				for(int i=0 ; i<components.length ; i++) {	
					String type = components[i].split(",")[0];
					Long cid = Long.parseLong(components[i].split(",")[1]);
					detailBuilder.append(MyUtil.buildSlideDetailContent(type,cid,false));
				}
			}

			String basicInfo = basicBuilder.toString();
			String outlineInfo = outlineBuilder.toString();
			String detailInfo = detailBuilder.toString();
			render("oa/trip_slide.html",oa,basicInfo,outlineInfo,detailInfo);
		}
		else {
			Application.index("您尚未登入或權限不足");
		}

	}
	
	private static String buildSlideBasicContent(OutdoorActivity oa){
		
		
		StringBuilder builder = new StringBuilder();
		// 封面 (目前不需要封面)
		/*
		builder.append("<slide class='logoslide nobackground'>");
		builder.append("<article class='flexbox vcenter'>");
		builder.append("<img style='width: 100%; height: 100%;'  src='"+oa.photos.iterator().next().path+"'>");
		builder.append("</article>");
		builder.append("</slide>");
		*/
		
		//basic info (主標題)
		builder.append("<slide class='title-slide segue nobackground'>");
		builder.append("<aside class='gdbar'><img style='width: 80%; height: 80%;'  src='/public/images/logo.png'></aside>");
		builder.append("<hgroup class='auto-fadein'>");
		
		builder.append("<h1 data-config-title>");
		builder.append(oa.name);
		builder.append("</h1>");
		
		builder.append("<h2 data-config-subtitle>");
		builder.append("  By  "+oa.creator.username);
		builder.append("</h2>");
		
		builder.append("<p data-config-presenter>");
		builder.append("活動日期： "+ oa.fromDate + " ~ " + oa.toDate);
		builder.append("</p>");
		
		builder.append("</hgroup>");
		builder.append("</slide>");
		
		
		//次標題頁(活動基本資訊)
		builder.append("<slide>");
		builder.append("<hgroup><h2>活動資訊</h2></hgroup>");
		builder.append("<article>");
		builder.append("<ul>");
		builder.append("<li>集合地點：<ul><li>" + oa.gatherPoint + "</li></ul></li>");
		builder.append("<li>活動時間：<ul><li>" + oa.fromDate + " " + oa.fromTime + " 開始 ， 至  " + oa.toDate + " " +oa.fromTime + " 結束</li></ul></li>");
		builder.append("<li>活動費用：<ul><li> $ " + oa.price + " 元" + "</li></ul></li>");
		builder.append("</ul>");
		builder.append(" </article>");
		builder.append("</slide>");
		
		//活動說明(introduction)
		String introduction = oa.introduction;
		if(introduction!=null&&introduction!=""){
			builder.append("<slide>");
			builder.append("<hgroup><h2>活動簡介說明</h2></hgroup>");
			builder.append("<article>");
			builder.append("<li><b>" + introduction + "</b></li>");
			builder.append("</article>");
			builder.append("</slide>");
		}
		
		return builder.toString();
	}

	public static void show(@Required Long id) {
		Logger.info(request.remoteAddress + " access oa, id=" + id);	
		
		OutdoorActivity oa = OutdoorActivity.findById(id);
		Member m = MyUtil.getCurrentUser();
		authOutdoorActivity authOA = new authOutdoorActivity(m,oa);
		if(authOA.allowRead) {
			//計算ViewCount
			MyUtil.calculateViewCount(ViewCountHistory.OutdoorActivityView,oa.id);
			
			String componentOrder = oa.componentOrder;
			
			StringBuilder intro_content = new StringBuilder();
			StringBuilder detail_content = new StringBuilder(); 	// 詳細內容
			StringBuilder refTp_content = new StringBuilder();  	// 引用教案		
			StringBuilder pois_content = new StringBuilder();   	// 行程景點
			//StringBuilder examine_content = new StringBuilder();	// 測驗評量
			//StringBuilder apply_content = new StringBuilder();  	// 報名名單
			StringBuilder photos_contents = new StringBuilder();	// 相關圖片
			StringBuilder photos_nails500 = new StringBuilder();    // 圖片大圖
			StringBuilder photos_nails100 = new StringBuilder();    // 圖片小圖
			StringBuilder attatches_contents = new StringBuilder(); // 相關附件
			StringBuilder links_contents = new StringBuilder();		// 相關連結
			StringBuilder ref_tp = new StringBuilder(); // 引用教案區塊內容
			
			intro_content.append(buildIntro(oa.introduction));
			
			// Process 詳細內容
			if(null != componentOrder && !componentOrder.equals("")) {
				String [] components = componentOrder.split("##");
				List<Map> maps = new ArrayList<Map>();
				for(int i=0 ; i<components.length ; i++) {				 
					String type = components[i].split(",")[0];
					Long cid = Long.parseLong(components[i].split(",")[1]);
					try {
						detail_content.append(buildContent(type, cid));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					if(type.equals("photo")) {
						Photo photo = Photo.findById(cid);
						photos_contents.append(buildAllPhotos(photo.title, photo.intro, photo.path));
						photos_nails500.append(buildPhotosNail500(photo.path));
						photos_nails100.append(buildPhotosNail100(photo.path));
					}
					
					// Process 教案附件
					if(type.equals("attatch")) {
						Attatch attatch = Attatch.findById(cid);
						attatches_contents.append(buildAllAttatches(attatch.intro, attatch.path, attatch.fileName));
					}
					
					// Process 教案連結
					if(type.equals("link")) {
						Link link = Link.findById(cid);
						links_contents.append(buildAllLinks(link.url, link.linkName, link.intro));
					}
					
					// Process 行程景點
					if(type.equals("map")) {
						Map map = Map.findById(cid);
						maps.add(map);					
					}
				}
				
				// Process 行程景點
				for(int i=0 ; i<maps.size() ; i++) {
					pois_content.append(buildPois(maps.get(i).id, i));
				}
			}
			
			// Process 引用教案
			List<OAIncludeTP> oa_tp = OAIncludeTP.findByOA(oa.id);
			if(null != oa_tp) {
				for(int i=0 ; i<oa_tp.size() ; i++) {
					OAIncludeTP include = oa_tp.get(i);
					ref_tp.append(buildRefTpContent(include.tp.id, include.intro));
				}
			}
			
			
			// 建立者沒有上傳任何圖片
			if(photos_nails500.length()==0) {
				photos_nails500.append(buildDefaultPhotos("500"));
				photos_nails100.append(buildDefaultPhotos("100"));
			}
			
			
			String trip_introduction = intro_content.toString();
			String detail = detail_content.toString();
			String refTp = refTp_content.toString();
			String pois = pois_content.toString();
			//String examine = examine_content.toString();
	//		String apply = apply_content.toString();
			String photo_content = photos_contents.toString();
			String photo_nail_500 = photos_nails500.toString();
			String photo_nail_100 = photos_nails100.toString();
			String attatch_content = attatches_contents.toString();
			String link_content = links_contents.toString();
			String ref_content = ref_tp.toString();
			String apply_remain = String.valueOf(oa.attendeeUpperLimit - Apply.countApplyByOa(id)); // 剩餘報名名額
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			Date d1 = null; 
			Date today = new Date();
			try {
				d1 = formatter.parse(oa.regDue);
			} catch (ParseException e) {
				// TODO: handle exception
			}
			int apply_remain_day = MyUtil.dateDiff(today, d1); // 剩餘報名時間
			
			if(apply_remain_day<=0)
				oa.status = OutdoorActivity.STATUS_Finish;
			
			oa.save();
			
			// user login
			String add = "no";
			int price = 0;
			List<Apply> myApply = new ArrayList<Apply>();
			List<Member> children = new ArrayList<Member>();
			if(m!=null){
				if(m.addToFavorite("oa", m.id, id))
					add = "yes";
				
				if(m.roleList.contains(ROLE.PARENT) || m.roleList.contains(ROLE.TEACHINGPLANNER) || m.roleList.contains(ROLE.ADMIN)) {
					children.addAll(Member.findRelations(m.id));
				}

				/* cal price */
				myApply = Apply.findMyApply(oa.id, m.id);
				price = oa.price * myApply.size();
			}	
			
			
			List<Apply> applies = new ArrayList<Apply>();
			applies = Apply.findApplyByOa(id);
			
			oa.save();
			
			List<Examine> exams = new ArrayList<Examine>();
			List<Examine> exams_process = new ArrayList<Examine>();
			List<Examine> exams_finish = new ArrayList<Examine>();
	
			if(authOA.allowWrite)
				exams = Examine.findExamineByOa(oa.id, ROLE.TEACHINGPLANNER); //建立者本人可看到未公開之評量
			else
				exams = Examine.findExamineByOa(oa.id, ROLE.STUDENT);			//其他人(包含未登入者)只可看到公開之評量
			
			for(int i=0 ; i<exams.size() ; i++) {
				if(exams.get(i).status==Examine.progress && exams.get(i).publish)
					exams_process.add(exams.get(i));
				else if(exams.get(i).status==Examine.finished && exams.get(i).publish)
					exams_finish.add(exams.get(i));
			}
			

			render("oa/trip_detail.html",m, add, oa, trip_introduction, detail, refTp, pois,  applies,
					photo_content, photo_nail_500, photo_nail_100,
					attatch_content, link_content,
					ref_content, apply_remain, apply_remain_day, children, price, myApply,
					exams_process, exams_finish);
		}
		else {
			Application.index("您尚未登入或權限不足");
		}
	}
	
	private static String buildDefaultPhotos(String type) {
		StringBuilder s = new StringBuilder();
		if(type.equals("500")) {
			s.append("<li>");
			s.append("<div class=\"nailthumb-container nc500\">");
			s.append("<img src=");
			s.append("\"" + "/public/images/default/oa_default_500.png" + "\" />");
			s.append("</div>");
			s.append("</li>");
		} else if(type.equals("100")) {
			s.append("<li>");
			s.append("<div class=\"nailthumb-container nc100\">");
			s.append("<img src=");
			s.append("\"" + "/public/images/default/oa_default_100.png" + "\" />");
			s.append("</div>");
			s.append("</li>");
		}
		
		return s.toString();
	}

	private static String buildIntro(String introduction) {
		
		if(introduction==null || introduction.equals(""))
			introduction = "無";
		
		StringBuilder s = new StringBuilder();
		s.append("<div class=\"tdt_div\">");
		s.append("<h4 class=\"tdt_title\">活動說明</h4>");
		s.append("<div class=\"tdt_text\">");
		s.append(introduction);
		s.append("</div>");
		s.append("</div>");		
		return s.toString();
	}

	private static String buildPois(Long mid, int count) {
		StringBuilder s = new StringBuilder();
		Map m = Map.findById(mid);
		s.append("<div class=\"spot\">");
		s.append("<span class=\"spot_number\">");
		s.append((count+1));
		s.append("</span>");
		s.append("<p class=\"spot_title\"><i class=\"icon-map-marker\"></i>");
		s.append(m.POIName);
		s.append("<button class=\"pull-right btn btn-warning btn-mini\">顯示位置</button></p>");
		s.append("<p class=\"spot_tel\">");
		s.append(m.phone);
		s.append("</p>");
		s.append("<p class=\"spot_address\">");
		s.append(m.address);
		s.append("</p></div>");		
		return s.toString();
	}

	private static String buildRefTpContent(Long id, String intro) {

		StringBuilder s = new StringBuilder();
		TeachingPlan tp = TeachingPlan.findById(id);
		s.append("<div class=\"ciwt_item\">");
		s.append("<div class=\"ciwt_img\">");
		s.append("<div class=\"nailthumb-container nc120\">");
		s.append("<img src=\"/public/images/trip_detail_img6.jpg\"/>");
		s.append("</div>");
		s.append("</div>");
		s.append("<div class=\"ciwt_info\">");
		s.append("<p class=\"ciwt_title\">");
		s.append("<a href=/tp/show/"+tp.id+">"+tp.name+"</a>");
		s.append("</p>");
		s.append("<p class=\"tags\">");
		
		List<Tag> tags = Tag.findTagsByTeachingPlan(id);
		for(int a=0 ; a<tags.size() ; a++) {
			Tag t = (Tag)tags.get(a);
			int i=1;
			s.append("<span class=\"tag tag_c" + i + "\" onclick='goToTag("+id+", \"oa\")'>");
			s.append("<a>"+t.name+"</a>");
			s.append("</span>");
			i++;
		}		
		
		s.append("</p>");
		s.append("<p class=\"ciwt_note\">");
		s.append(intro);
		s.append("</p></div></div>");
		
		return s.toString();
	}

	private static String buildAllAttatches(String intro, String path, String fileName) {
		StringBuilder s = new StringBuilder();
		int startIndex = fileName.lastIndexOf(46) + 1;
	    int endIndex = fileName.length();
		String sub_fileType =  fileName.substring(startIndex, endIndex);
		s.append("<span class=");
		s.append("\"" + sub_fileType + "\">");
		s.append("<a href=");
		s.append("\"" + path + "\">");
		s.append(fileName);
		s.append("</a><b>");
		s.append(intro);
		s.append("</b></span>");
		return s.toString();
	}
	
	private static String buildAllLinks(String url, String name, String intro) {
		
		StringBuilder s = new StringBuilder();
		s.append("<span class=\"link\">");
		s.append("<a target=_blank href=");
		s.append("\"" + url + "\">");
		s.append(name);
		s.append("</a><b>");
		s.append(intro);
		s.append("</b></span>");
		return s.toString();
	}

	private static String buildAllPhotos(String title, String intro, String path) {
		StringBuilder s = new StringBuilder();
		
		s.append("<li>");
		s.append("<a class=\"upload_photo\" style=\"\">");
		s.append("<div class=\"nailthumb-container nc60\" style=\"overflow: hidden; padding: 0px; width: 60px; height: 60px;\">");
		s.append("<img title=");
		s.append("\"" + title + "\" ");
		s.append("img_info=");
		s.append("\"" + intro + "\" ");
		s.append("src=");
		s.append("\"" + path + "\" ");
		s.append("style=\"position: relative; width: 91.16022099447514px; height: 60px; top: 0px; left: -15.58011049723757px;\" class=\"nailthumb-image\">");
		s.append("</div></a></li>");		
		
		return s.toString();
	}
	
	private static String buildPhotosNail500(String path) {
		StringBuilder s = new StringBuilder();
		
		s.append("<li>");
		s.append("<div class=\"nailthumb-container nc500\">");
		s.append("<img src=");
		s.append("\"" + path + "\" />");
		s.append("</div>");
		s.append("</li>");
		
		return s.toString();
	}
	
	private static String buildPhotosNail100(String path) {
		StringBuilder s = new StringBuilder();
		
		s.append("<li>");
		s.append("<div class=\"nailthumb-container nc100\">");
		s.append("<img src=");
		s.append("\"" + path + "\" />");
		s.append("</div>");
		s.append("</li>");
		
		return s.toString();
	}

	private static String buildContent(String type, Long id) throws Exception {
		
		StringBuilder s = new StringBuilder();
		try {
			if(type.equals("map")) {				
				Map map = Map.findById(id);
				s.append(buildMap(map.title, map.POIName, map.intro, map.lat, 
						map.lng, map.address, map.phone, map.startTime, map.endTime));
			} else if(type.equals("youtube")) {
				Youtube youtube = Youtube.findById(id);
				s.append(buildYoutube(youtube.title, youtube.url, youtube.intro));
			} else if(type.equals("text")) {
				Text text = Text.findById(id);
				s.append(buildText(text.title, text.intro));
			} else if (type.equals("teacher")) {
				Teacher teacher = Teacher.findById(id);
				s.append(buildTeacher(teacher.title, teacher.teacherName, teacher.education, 
						teacher.introduction, teacher.photo));				
			} else if (type.equals("attatch")) {
				Attatch attatch = Attatch.findById(id);
				s.append(buildAttach(attatch.title, attatch.intro, attatch.path, attatch.fileName));
			} else if(type.equals("cite")) {
				OAIncludeTP cite = OAIncludeTP.findById(id);
				s.append(buildCite(cite.intro, cite.id));				
			} else if(type.equals("link")) {
				Link link = Link.findById(id);
				s.append(buildLink(link.title, link.url, link.linkName, link.intro));
			} else if (type.equals("photo")) {
				Photo photo = Photo.findById(id);
				s.append(buildPhoto(photo.title, photo.intro, photo.path, photo.fileName));
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return s.toString();
		
	}
	
	private static String buildPhoto(String title, String intro, String path, String fileName) {
		
		intro = MyUtil.replaceTextAreaToHtml(intro);
		StringBuilder s = new StringBuilder();
		s.append("<div class=\"tdt_div tdt_pic\">");
		s.append("<h4 class=\"tdt_title\">"+title+"</h4>");
		s.append("<div class=\"tdt_text\">");
		s.append("<div class=\"pic_item\">");
		s.append("<img src=");
		s.append("\"" + path + "\">");
		s.append("<p class=\"pic_info\">");
		s.append(intro);
		s.append("</p>");
		s.append("</div></div></div>");
		
		return s.toString();
	}

	private static String buildLink(String title, String url, String linkName,String intro) {

		intro = MyUtil.replaceTextAreaToHtml(intro);
		StringBuilder s = new StringBuilder();
		s.append("<div class=\"tdt_div tdt_link\">");
		s.append("<h4 class=\"tdt_title\">");
		s.append(title);
		s.append("</h4>");
		s.append("<div class=\"tdt_text\">");
		s.append("<div class=\"files\">");
		s.append("<span class=\"link\">");
		s.append("<a target=_blank href=");
		s.append("\"" + url + "\">");
		s.append(linkName);
		s.append("</a><b>");
		s.append(intro);
		s.append("</b></span>");
		//s.append("<a class=\"pull-right btn btn-info\" target=_blank value=\"前往連結\" href=\'" + url + "';" + "/>");
		s.append("</div></div></div>");
		return s.toString();
	}

	private static String buildCite(String intro, Long id) {
		intro = MyUtil.replaceTextAreaToHtml(intro);
		StringBuilder s = new StringBuilder();
		
		s.append("<div class=\"tdt_div lesson_html lesson_reference\">");
		s.append("<span class=\"reference_mark top\">\"</span>");
		System.out.println("citeId="+id);
		OAIncludeTP cite = OAIncludeTP.findById(id);
//		String t = cite.style;
//		Long i = cite.tid;
//		try {
//			s.append(buildContent(t, i));
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		s.append("<span class=\"reference_mark bottom\">\"</span>");
		s.append("<div class=\"tdt_reference\">引用自");
		s.append("<a href=\"/member/profile/"+cite.tp.creator.id+"\">");
		s.append(cite.tp.creator.username);
		s.append("</a>的");
		s.append("<a href=\"/tp/show/"+cite.tp.id+"\">");
		s.append(cite.citeTitle);
		s.append("</a></div>");
		s.append("<div class=\"tdt_refenence_addinfo\">");
		s.append(intro);
		s.append("</div>");
		s.append("</div>");
		
		
		return s.toString();
	}

	private static String buildAttach(String title, String intro, String path,String fileName) {
		intro = MyUtil.replaceTextAreaToHtml(intro);
		StringBuilder s = new StringBuilder();
		int startIndex = fileName.lastIndexOf(46) + 1;
	    int endIndex = fileName.length();
		String sub_fileType =  fileName.substring(startIndex, endIndex);
		
		s.append("<div class=\"tdt_div tdt_file\">");
		s.append("<h4 class=\"tdt_title\">");
		s.append(title);
		s.append("</h4>");
		s.append("<div class=\"tdt_text\">");
		s.append("<div class=\"files\">");
		s.append("<span class=");
		s.append("\"" + sub_fileType + "\">");
		s.append("<a href=");
		s.append("\"" + path + "\">");
		s.append(fileName);
		s.append("</a><b>");
		s.append(intro);
		s.append("</b></span>");
		//s.append("<button class=\"pull-right btn btn-mini btn-info\">下載附件</button>");
		s.append("</div></div></div>");
		return s.toString();
	}

	private static String buildTeacher(String title, String teacherName,String education, String introduction, Photo photo) {
			
		introduction = MyUtil.replaceTextAreaToHtml(introduction);
		StringBuilder s = new StringBuilder();
		s.append("<div class=\"tdt_div tdt_teacher\">");
		s.append("<h4 class=\"tdt_title\">老師簡歷</h4>");
		s.append("<div class=\"tdt_text\">");
		s.append("<div class=\"teacher_resume\">");
		s.append("<div class=\"pull-left teacher_resume_1\">");
		if(photo!=null){
			String path = photo.path;
			s.append("<div class=\"teacher_img\"><img class=\"\" src=");
			s.append("\""+ path + "\">");
			s.append("</div>");
		}
		s.append("</div>");
		s.append("<div class=\"pull-right teacher_resume_2\">");
		s.append("<p class=\"teacher_name\">");
		s.append("<a href=");
		s.append("\"" + "#" + "\">");
		s.append(teacherName);
		s.append("</a></p>");
		s.append("<p class=\"teacher_school\">");
		
		if(null!=education && !education.equals("")) {
			s.append("<ul>");
			
			for(int i=0 ; i<education.split("##").length ; i++) {
				s.append("<li>");
				s.append(education.split("##")[i]);
				s.append("</li>");
			}
			
			s.append("</ul>");
		}
		
		s.append("</p>");
		s.append("<p class=\"teacher_intro\">");
		s.append(introduction);
		s.append("</p>");
		s.append("</div><div class=\"clearfix\"></div></div></div></div>");
		return s.toString();
	}

	private static String buildText(String title, String intro) {

		StringBuilder s = new StringBuilder();
		s.append("<div class=\"tdt_div tdt_html\">");
		s.append("<h4 class=\"tdt_title\">");
		s.append(title);
		s.append("</h4>");
		s.append("<div class=\"tdt_text\">");
		s.append(intro);
		s.append("</div>");
		s.append("</div>");		
		return s.toString();
	}

	private static String buildYoutube(String title, String url, String intro) {
		intro = MyUtil.replaceTextAreaToHtml(intro);
		url = MyUtil.youtubeSrcParser(url);
		
		StringBuilder s = new StringBuilder();
		s.append("<div class=\"tdt_div tdt_youtube\">");
		s.append("<h4 class=\"tdt_title\">");
		s.append(title);
		s.append("</h4>");
		s.append("<div class=\"youtube_item\">");
		s.append("<iframe width=\"688\" height=\"400\" src=");
		s.append("\"//www.youtube.com/embed/" + url + "\"");
		s.append(" frameborder=\"0\" allowfullscreen></iframe>");
		s.append("<p class=\"youtube_info\">");
		s.append(intro);
		s.append("</p></div></div>");
		return s.toString();
	}

	private static String buildMap(String title, String pOIName, String intro,
			String lat, String lng, String address, String phone,
			String startTime, String endTime) {
		
		intro = MyUtil.replaceTextAreaToHtml(intro);
		StringBuilder s = new StringBuilder();
		s.append("<div class=\"tdt_div tdt_spot\">");
		s.append("<h4 class=\"tdt_title\">");
		s.append(title);
		s.append("</h4>");
		s.append("<div class=\"tdt_text\"><div class=\"spots\"><div class=\"spot\"><p class=\"spot_title\"><i class=\"icon-map-marker\"></i>");
		s.append(pOIName);
		s.append("<button class=\"pull-right btn btn-warning btn-mini onepoint\">地圖</button></p>");
		s.append("<p class=\"spot_tel\">");
		s.append(phone + "</p>");
		s.append("<p class=\"spot_intro\">");
		s.append(intro);
		s.append("</p>");
		s.append("<p class=\"spot_address\">");
		s.append(address);
		s.append("</p>");
		s.append("</div></div></div></div>");
		return s.toString();
	}
	
	// 報名-系名、身分證字號、電話 (丟到這個method之前已經透過javascript確認過未重複報名
	public static void apply(String childname, String userId, String phone, Long oaid, Long attendant_id,String birthday) {
		Member currentMember = MyUtil.getCurrentUser(); // 報名者
		OutdoorActivity oa = OutdoorActivity.findById(oaid);
		Apply apply = new Apply();
		authOutdoorActivity authOA = new authOutdoorActivity(currentMember,oa);
		//boolean notApply = false;

		if(authOA.allowApply){
			// 學生
			if(currentMember.roleList.contains(ROLE.STUDENT)) {
				//notApply = Apply.notApply(oaid, currentMember.id, currentMember.id); // 報名和參加者同一人，都是學生
				//if(notApply) {
				apply.username = childname;
				apply.phone = phone;
				apply.userid = userId;
				apply.oa = oa;
				apply.birth = birthday;
				apply.regMember = currentMember;
				apply.attendantMember = currentMember;
				apply.applyTime = MyUtil.now();
				apply.save();			
				show(oaid);
				//} else {
				//	show(oaid);
				//}
			} 
			else if(currentMember.roleList.contains(ROLE.PARENT) || currentMember.roleList.contains(ROLE.TEACHINGPLANNER) || currentMember.roleList.contains(ROLE.ADMIN)) {
				//notApply = Apply.notApply(oaid, currentMember.id, attendant_id); // 報名者為家長、參加者為學生
				//if(notApply) {
				//若孩子選:"無",自己輸入資料  ,則參加member會是null
				Member attendant = Member.findById(attendant_id);
				apply.username = childname;
				apply.phone = phone;
				apply.userid = userId;
				apply.oa = oa;
				apply.birth = birthday;
				apply.regMember = currentMember;
				apply.attendantMember = attendant;
				apply.applyTime = MyUtil.now();
				apply.save();
				show(oaid);
				//}
			}
		}
		else {
			Application.index("您尚未登入或權限不足");
		}
	}
	
	// 根據oa_id和報名者id來check
	public static void replyATM(Long oaid, Long member_id, String atm) {
		Member currentMember = MyUtil.getCurrentUser(); // 報名者
		OutdoorActivity oa = OutdoorActivity.findById(oaid);
		authOutdoorActivity authOA = new authOutdoorActivity(currentMember,oa);
		if(authOA.allowRead){
			List<Apply> myApply = Apply.findMyApply(oaid, member_id);
			for(int i=0 ; i<myApply.size() ; i++) {
				Apply apply = myApply.get(i);
				apply.ATMaccount = atm;
				apply.status = Apply.Validate;
				apply.save();
				show(oaid);
			}
			show(oaid);
		}
		else {
			Application.index("您尚未登入或權限不足");
		}
	}
	
	public static void gotoNFCPage(Long oaid) {
		Member member = MyUtil.getCurrentUser();
		OutdoorActivity oa = OutdoorActivity.findById(oaid);
		authOutdoorActivity authOA = new authOutdoorActivity(member, oa);
		
		if(authOA.allowWrite){
			List<String> component_name = new ArrayList<String>();
			String [] components = oa.componentOrder.split("##");
			if(components.length>0){  // (>0) 因為最少會有1個 空值
		 		for(int i=0 ; i<components.length ; i++) {
		 			
		 			String type = components[i].split(",")[0];
					Long cid = Long.parseLong(components[i].split(",")[1]);
					
					if(type.equals("map")) {
						Map map = Map.findById(cid);
						component_name.add(map.title + "##" + type + "," + cid);
					} else if(type.equals("youtube")) {
						Youtube youtube = Youtube.findById(cid);
						component_name.add(youtube.title + "##" + type + "," + cid);
					} else if(type.equals("text")) {
						Text text = Text.findById(cid);
						component_name.add(text.title + "##" + type + "," + cid);
					} else if (type.equals("teacher")) {
						Teacher teacher = Teacher.findById(cid);
						component_name.add(teacher.title + "##" + type + "," + cid);
					} else if (type.equals("attatch")) {
						Attatch attatch = Attatch.findById(cid);
						component_name.add(attatch.title + "##" + type + "," + cid);
					} else if(type.equals("cite")) {
						OAIncludeTP cite = OAIncludeTP.findById(cid);
						component_name.add(cite.citeTitle + "##" + type + "," + cid);
					} else if(type.equals("link")) {
						Link link = Link.findById(cid);
						component_name.add(link.title + "##" + type + "," + cid);
					} else if (type.equals("photo")) {
						Photo photo = Photo.findById(cid);
						component_name.add(photo.title + "##" + type + "," + cid);
					}
		 		}
			}
			render("oa/nfc.html", oa, component_name);
		}
		else {
			show(oaid);
		}
	}
	
	public static void setNFC(Long oaid, String[] title, String [] order,
			String[] components1,String[] components2,String[] components3,String[] components4,String[] components5) {
		
		Member member = MyUtil.getCurrentUser();
		OutdoorActivity oa = OutdoorActivity.findById(oaid);
		authOutdoorActivity authOA = new authOutdoorActivity(member, oa);
		if(authOA.allowWrite){
			oa.nfc.clear();
			oa.save();
			for(int i=0 ; i<order.length ; i++) {
				if(!title[i].equals("")) {
					String orderNumber = "";
					
					if(i==0){
						for(int j=0 ; j<components1.length ; j++) {
							String type = components1[j].split(",")[0];
							Long cid = Long.parseLong(components1[j].split(",")[1]);
							orderNumber += type + "," + cid + "##";
						}
					}
					else if (i==1) {
						for(int j=0 ; j<components2.length ; j++) {
							String type = components2[j].split(",")[0];
							Long cid = Long.parseLong(components2[j].split(",")[1]);
							orderNumber += type + "," + cid + "##";
						}
					}
					else if (i==2) {
						for(int j=0 ; j<components3.length ; j++) {
							String type = components3[j].split(",")[0];
							Long cid = Long.parseLong(components3[j].split(",")[1]);
							orderNumber += type + "," + cid + "##";
						}
					}
					else if (i==3) {
						for(int j=0 ; j<components4.length ; j++) {
							String type = components4[j].split(",")[0];
							Long cid = Long.parseLong(components4[j].split(",")[1]);
							orderNumber += type + "," + cid + "##";
						}
					}
					else if (i==4) {
						for(int j=0 ; j<components5.length ; j++) {
							String type = components5[j].split(",")[0];
							Long cid = Long.parseLong(components5[j].split(",")[1]);
							orderNumber += type + "," + cid + "##";
						}
					}
					System.out.println(orderNumber);
					NFC nfc = new NFC();
					nfc.title=title[i];
					nfc.componentOrder=orderNumber;
					nfc.save();
					oa.nfc.add(nfc);
					oa.save();
					/*
					NFC nfc = new NFC();
					nfc.title = title[i];
					nfc.componentOrder = orderNumber;
					nfc.save();
					//nfc.serialNumber = "nfc_" + nfc.id;
					//nfc.save();
					
					oa.nfc.add(nfc);
					oa.save();
					*/
				}
			}
			
		}
		else {
			show(oaid);
		}
		
	}
	
	public static void checkApply(Long oaid,Long attendant_id, String userId, String type) {
		boolean notApply = true;
		if(type.equals("child")) {
			notApply = Apply.notApply(oaid, attendant_id, userId);
		}
		else if(type.equals("parent")) {
			if(null != attendant_id) {
				notApply = Apply.notApply(oaid, attendant_id, userId);
			}
		}
		else
			Logger.error("wrong type error.");
			
		if(notApply)
			renderText("ok");
		else 
			renderText("no");
	}
	
	public static void gotoApplyMamangement() {
		Member member = MyUtil.getCurrentUser();
		authOutdoorActivity authOA = new authOutdoorActivity(member);
		if(authOA.allowWrite) {
			List<Member> teachers  = new ArrayList<Member>();
			teachers = Member.findMemberByRole(ROLE.TEACHINGPLANNER);
			List<OutdoorActivity> oas = new ArrayList<OutdoorActivity>();
			render("management/teacher_manage_signup.html", member, teachers);
		} else
			Application.index("您尚未登入或權限不足");
	}
	
	
	
}
