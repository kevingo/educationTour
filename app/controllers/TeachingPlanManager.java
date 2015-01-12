package controllers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.Apply;
import models.Attatch;
import models.Cite;
import models.Examine;
import models.Link;
import models.Map;
import models.Member;
import models.OAIncludeTP;
import models.OutdoorActivity;
import models.Photo;
import models.Tag;
import models.TagCategory;
import models.Teacher;
import models.TeachingPlan;
import models.TeachingPlanAfter;
import models.TeachingPlanBefore;
import models.TeachingPlanNow;
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
import controllers.ExamineManager.authExamine;
import controllers.OutdoorActivityManager.authOutdoorActivity;
import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.Restrict;
import controllers.deadbolt.Restrictions;

@With(Deadbolt.class)
public class TeachingPlanManager extends Controller {
	
	@Before
	public static void getTag() {
		Application.getTag();	   
	}
	
	public static class authTeachingPlan{
		boolean allowRead = false;		//可看到某一特定教案
		boolean allowWrite = false;		//可編輯修改某一特定教案
		
		//針對特定tp的權限設定
		authTeachingPlan(Member member,TeachingPlan tp){
			if(tp==null) 
				return;
			else if(member==null){
				//判斷可Read(即可看)[當creator設定tp為公開時(沒有登入時)]			
				if(tp.publish){
					this.allowRead=true;
					return;
				}
			}
			else{
				//admin全開
				if(member.roleList.contains(ROLE.ADMIN)){
					this.allowWrite=true;
					this.allowRead=true;
				}
				
				//判斷是否被admin 關閉 或 creator 自己刪除
				if(tp.enable){
					//判斷可Write (登入者=建立者 )
					if(tp.creator.equals(member)){
						this.allowWrite=true;
						this.allowRead=true;
					}	
					//判斷可Read(即可看)[當creator設定TP為公開時]			
					if(tp.publish){
						this.allowRead=true;
					}
				}			
			}
		}
		
		//針對 登入者 對於評量功能之權限
		authTeachingPlan(Member member){
			if(member==null)
				return;
			else {
				//判斷是否可使用 評量管理功能 當身分具有老師或學生或admin時可使用
				if (member.roleList.contains(ROLE.TEACHINGPLANNER))
					this.allowWrite = true;
				else if (member.roleList.contains(ROLE.STUDENT))
					this.allowWrite = true;
				else if (member.roleList.contains(ROLE.ADMIN))
					this.allowWrite = true;
			}
		}
	}
	
	@Restrictions({@Restrict(ROLE.TEACHINGPLANNER),@Restrict(ROLE.STUDENT) })
	public static void createAgree() {
		Member member = MyUtil.getCurrentUser();
		authTeachingPlan auth = new authTeachingPlan(member);
		if (auth.allowWrite) {
			render("tp/create_lesson_agree.html");
		} else {
			Application.index("您尚未登入或權限不足");
		}
	}
	
	@Restrictions({@Restrict(ROLE.TEACHINGPLANNER), @Restrict(ROLE.STUDENT) })
	public static void index(Long id) {
		Member member = MyUtil.getCurrentUser();
		authTeachingPlan auth = new authTeachingPlan(member);
		if(auth.allowWrite){
			List<TagCategory> listCategory = TagCategory.findByActivity("找教案");
			//有id是編輯(否則為新建) 吐JSON 給前端parse
			if(id!=0){
				TeachingPlan tp = TeachingPlan.findById(id);
				authTeachingPlan authTP = new authTeachingPlan(member,tp);
				if(authTP.allowWrite)
					render("tp/create_lesson.html",listCategory,tp);
				else {
					Logger.info("user="+member.email+" try to edit tp:"+ id + ",but it's not belong to him/her.");
					index((long) 0);
				}
			}	
			else {
				render("tp/create_lesson.html",listCategory);
			}
		}
		else{
			Application.index("您尚未登入或權限不足");
		}
	}
	
	
	public static void search(String kw) {
		List<TeachingPlan> tps = new ArrayList<TeachingPlan>();
		tps = TeachingPlan.findByKeyword(kw);
		if (tps != null) {
			for (TeachingPlan tp : tps) {
				if(tp.name.length()>22)
					tp.name = tp.name.substring(0, 22) + "...";
			}
		}
		
		render("Application/searchTP_kw.html", tps, kw);
	}
	
	@Restrictions({@Restrict(ROLE.TEACHINGPLANNER),@Restrict(ROLE.STUDENT) })
	public static void create_basicInfo(@Required Long tpid , @Required String lesson_name, @Required int publish , @Required int share , @Required Long[] element ,
										@Required String lesson_subject , @Required String lesson_unit ,String introduction){
		Member member = MyUtil.getCurrentUser();
		authTeachingPlan auth = new authTeachingPlan(member);
		if(auth.allowWrite){
			TeachingPlan tp = null;
			if(tpid==null || tpid==0)
				tp = new TeachingPlan();
			else {
				if(tpid>0){
					tp = TeachingPlan.findById(tpid);
					authTeachingPlan authTP = new authTeachingPlan(member,tp);
					if(!authTP.allowWrite){
						Logger.info("user="+member.email+" try to edit tp:"+ tpid + ",but it's not belong to him/her.");
						tp = null;
					}	
				}	
			}

			if(tp!=null){
				tp.creator = member;
				tp.createDate=MyUtil.now();
				
				tp.name = lesson_name;
				tp.publish= (publish==1)? true:false;
				tp.share = (share==1)? true:false;
				for (Long tid : element) {
					Tag tag = Tag.findById(tid);
					if(tag!=null && !tp.tags.contains(tag))
						tp.tags.add(tag);
				}
				tp.subject=lesson_subject;
				tp.unit =lesson_unit;
				tp.introduction=introduction;

				tp.save();
				MyUtil._tpid=tp.id;
			}
			else {
				Application.index("您無此權限。");
			}
		}
		else {
			Application.index("您尚未登入或權限不足");
		}
	}
	
	@Restrictions({@Restrict(ROLE.TEACHINGPLANNER),@Restrict(ROLE.STUDENT) })
	public static void create_Before(@Required Long tpBeforeid ,String illustration , String[] seq , 
			 @Required Long[] html_id , String[] html_fieldname , String[] html_content , String[] html_share ,
			 @Required Long[] pic_id , String[] pic_fieldname , String[] pic_filename ,  String[] pic_memo , String[] pic_share , 
			 @Required Long[] youtube_id , String[] youtube_fieldname , String[] youtube_web , String[] youtube_memo , String[] youtube_share,
			 @Required Long[] file_id , String[] file_fieldname , String[] file_filename  , String[] file_memo , String[] file_share , 
			 @Required Long[] web_id , String[] web_fieldname , String[] web_name,String[] web_web , String[] web_memo , String[] web_share,
			 @Required Long[] spot_id , String[] spot_fieldname , String[] spot_name , String[] spot_addr , String[] spot_lat ,String[] spot_lng , String[] spot_tel , String[] spot_open , String[] spot_end , String[] spot_memo, String[] spot_share,
			 @Required Long[] res_id , String[] res_fieldname , String[] res_name , String[] res_grad , String[] res_count,String[] res_content , String[] res_pic_filename , String[] res_share,
			 @Required Long[] cite_id , String[] ref_style , String[] ref_id , String[] ref_title , String[] ref_tpid ,  String[] ref_memo	){
		
		
		Member member = MyUtil.getCurrentUser();
		TeachingPlan tp = TeachingPlan.findById(MyUtil._tpid);
		authTeachingPlan authTP = new authTeachingPlan(member,tp);
		if (authTP.allowWrite) {
			// reset 八類的index 與 orderNumber (全域變數)
			MyUtil.ResetComponentIndexVar();
			TeachingPlanBefore tpBefore = null;
			if(tpBeforeid==null || tpBeforeid==0)
				tpBefore = new TeachingPlanBefore();
			else {
				if(tpBeforeid>0)
					tpBefore = TeachingPlanBefore.findById(tpBeforeid);	
			}
			tpBefore.illustration=illustration;
			tpBefore.creator=member;
			//紀錄老師學經歷的count
			int res_gradCount=0;
			//依序儲存元件
			if(seq!=null){
				for (String componentStr : seq) {				
					String style = componentStr.substring(componentStr.indexOf(',')+1);
					if(style.equals("text")){
						MyUtil.SaveTextComponent(html_id[MyUtil._html_index],html_fieldname[MyUtil._html_index] ,html_content[MyUtil._html_index] , html_share[MyUtil._html_index]);
					}else if (style.equals("photo")) {
						MyUtil.SavePhotoComponent(pic_id[MyUtil._pic_index] ,pic_fieldname[MyUtil._pic_index] , pic_memo[MyUtil._pic_index] ,pic_filename[MyUtil._pic_index], pic_share[MyUtil._pic_index] ,tp);					
					}else if (style.equals("youtube")) {
						MyUtil.SaveYoutubeComponent(youtube_id[MyUtil._youtube_index],youtube_fieldname[MyUtil._youtube_index], youtube_web[MyUtil._youtube_index], youtube_memo[MyUtil._youtube_index], youtube_share[MyUtil._youtube_index]);				
					}else if (style.equals("attatch")) {
						MyUtil.SaveAttatchComponent(file_id[MyUtil._file_index],file_fieldname[MyUtil._file_index] ,file_filename[MyUtil._file_index] , file_memo[MyUtil._file_index], file_share[MyUtil._file_index] ,tp);
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
						MyUtil.SaveTeacherComponent(res_id[MyUtil._res_index],res_fieldname[MyUtil._res_index], res_name[MyUtil._res_index] , grad  ,res_content[MyUtil._res_index] , res_pic_filename[MyUtil._res_index] ,res_share[MyUtil._res_index],tp);
					}else if (style.equals("cite")) {
						MyUtil.SaveTeachingPlanCiteComponent(cite_id[MyUtil._ref_index],ref_style[MyUtil._ref_index],ref_id[MyUtil._ref_index],ref_title[MyUtil._ref_index], ref_tpid[MyUtil._ref_index] ,ref_memo[MyUtil._ref_index]);
					}	
				}
			}
			tpBefore.orderNumber=MyUtil._orderNumber;
			tpBefore.save();
			tp.tpBefore=tpBefore;
			tp.save();
		}
		else {
			Application.index("您尚未登入或權限不足");
		}
	}
	
	@Restrictions({@Restrict(ROLE.TEACHINGPLANNER),@Restrict(ROLE.STUDENT) })
	public static void create_Now(@Required Long tpNowid ,String illustration , String[] seq , 
			 @Required Long[] html_id , String[] html_fieldname , String[] html_content , String[] html_share ,
			 @Required Long[] pic_id , String[] pic_fieldname , String[] pic_filename ,  String[] pic_memo , String[] pic_share , 
			 @Required Long[] youtube_id , String[] youtube_fieldname , String[] youtube_web , String[] youtube_memo , String[] youtube_share,
			 @Required Long[] file_id , String[] file_fieldname , String[] file_filename  , String[] file_memo , String[] file_share , 
			 @Required Long[] web_id , String[] web_fieldname , String[] web_name,String[] web_web , String[] web_memo , String[] web_share,
			 @Required Long[] spot_id , String[] spot_fieldname , String[] spot_name , String[] spot_addr , String[] spot_lat ,String[] spot_lng , String[] spot_tel , String[] spot_open , String[] spot_end , String[] spot_memo, String[] spot_share,
			 @Required Long[] res_id , String[] res_fieldname , String[] res_name , String[] res_grad , String[] res_count,String[] res_content , String[] res_pic_filename , String[] res_share,
			 @Required Long[] cite_id , String[] ref_style , String[] ref_id , String[] ref_title , String[] ref_tpid ,  String[] ref_memo	){
		
		Member member = MyUtil.getCurrentUser();
		TeachingPlan tp = TeachingPlan.findById(MyUtil._tpid);
		authTeachingPlan authTP = new authTeachingPlan(member,tp);
		if (authTP.allowWrite) {
			//reset 八類的index 與 orderNumber (全域變數)
			MyUtil.ResetComponentIndexVar();
			TeachingPlanNow tpNow = null;
			if(tpNowid==null || tpNowid==0)
				tpNow = new TeachingPlanNow();
			else {
				if(tpNowid>0)
					tpNow = TeachingPlanNow.findById(tpNowid);	
			}
			tpNow.illustration=illustration;
			tpNow.creator=member;
			//紀錄老師學經歷的count
			int res_gradCount=0;
			//依序儲存元件
			if(seq!=null){
				for (String componentStr : seq) {
					String style = componentStr.substring(componentStr.indexOf(',')+1);
					if(style.equals("text")){
						MyUtil.SaveTextComponent(html_id[MyUtil._html_index],html_fieldname[MyUtil._html_index] ,html_content[MyUtil._html_index] , html_share[MyUtil._html_index]);			
					}else if (style.equals("photo")) {
						MyUtil.SavePhotoComponent(pic_id[MyUtil._pic_index] ,pic_fieldname[MyUtil._pic_index] , pic_memo[MyUtil._pic_index] ,pic_filename[MyUtil._pic_index], pic_share[MyUtil._pic_index] ,tp);					
					}else if (style.equals("youtube")) {
						MyUtil.SaveYoutubeComponent(youtube_id[MyUtil._youtube_index],youtube_fieldname[MyUtil._youtube_index], youtube_web[MyUtil._youtube_index], youtube_memo[MyUtil._youtube_index], youtube_share[MyUtil._youtube_index]);				
					}else if (style.equals("attatch")) {
						MyUtil.SaveAttatchComponent(file_id[MyUtil._file_index],file_fieldname[MyUtil._file_index] ,file_filename[MyUtil._file_index] , file_memo[MyUtil._file_index], file_share[MyUtil._file_index] ,tp);
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
						MyUtil.SaveTeacherComponent(res_id[MyUtil._res_index],res_fieldname[MyUtil._res_index], res_name[MyUtil._res_index] , grad  ,res_content[MyUtil._res_index] , res_pic_filename[MyUtil._res_index] ,res_share[MyUtil._res_index],tp);
					}else if (style.equals("cite")) {
						MyUtil.SaveTeachingPlanCiteComponent(cite_id[MyUtil._ref_index],ref_style[MyUtil._ref_index],ref_id[MyUtil._ref_index],ref_title[MyUtil._ref_index], ref_tpid[MyUtil._ref_index] ,ref_memo[MyUtil._ref_index]);
					}	
				}
			}
			tpNow.orderNumber=MyUtil._orderNumber;
			tpNow.save();
			tp.tpNow=tpNow;
			tp.save();
		}
		else {
			Application.index("您尚未登入或權限不足");
		}
	}
	
	@Restrictions({@Restrict(ROLE.TEACHINGPLANNER),@Restrict(ROLE.STUDENT) })
	public static void create_After(@Required Long tpAfterid ,String illustration , String[] seq , 
			 @Required Long[] html_id , String[] html_fieldname , String[] html_content , String[] html_share ,
			 @Required Long[] pic_id , String[] pic_fieldname , String[] pic_filename ,  String[] pic_memo , String[] pic_share , 
			 @Required Long[] youtube_id , String[] youtube_fieldname , String[] youtube_web , String[] youtube_memo , String[] youtube_share,
			 @Required Long[] file_id , String[] file_fieldname , String[] file_filename  , String[] file_memo , String[] file_share , 
			 @Required Long[] web_id , String[] web_fieldname , String[] web_name,String[] web_web , String[] web_memo , String[] web_share,
			 @Required Long[] spot_id , String[] spot_fieldname , String[] spot_name , String[] spot_addr , String[] spot_lat ,String[] spot_lng , String[] spot_tel , String[] spot_open , String[] spot_end , String[] spot_memo, String[] spot_share,
			 @Required Long[] res_id , String[] res_fieldname , String[] res_name , String[] res_grad , String[] res_count,String[] res_content , String[] res_pic_filename , String[] res_share,
			 @Required Long[] cite_id , String[] ref_style , String[] ref_id , String[] ref_title , String[] ref_tpid ,  String[] ref_memo	){
		
		Member member = MyUtil.getCurrentUser();
		TeachingPlan tp = TeachingPlan.findById(MyUtil._tpid);
		authTeachingPlan authTP = new authTeachingPlan(member,tp);
		if (authTP.allowWrite) {
			//reset 八類的index 與 orderNumber (全域變數)
			MyUtil.ResetComponentIndexVar();
			TeachingPlanAfter tpAfter = null;
			if(tpAfterid==null || tpAfterid==0)
				tpAfter = new TeachingPlanAfter();
			else {
				if(tpAfterid>0)
					tpAfter = TeachingPlanAfter.findById(tpAfterid);	
			}
			tpAfter.illustration=illustration;
			tpAfter.creator=member;
			//紀錄老師學經歷的count
			int res_gradCount=0;
			//依序儲存元件
			if(seq!=null){
				for (String componentStr : seq) {
					String style = componentStr.substring(componentStr.indexOf(',')+1);
					if(style.equals("text")){
						MyUtil.SaveTextComponent(html_id[MyUtil._html_index],html_fieldname[MyUtil._html_index] ,html_content[MyUtil._html_index] , html_share[MyUtil._html_index]);			
					}else if (style.equals("photo")) {
						MyUtil.SavePhotoComponent(pic_id[MyUtil._pic_index] ,pic_fieldname[MyUtil._pic_index] , pic_memo[MyUtil._pic_index] ,pic_filename[MyUtil._pic_index], pic_share[MyUtil._pic_index] ,tp);					
					}else if (style.equals("youtube")) {
						MyUtil.SaveYoutubeComponent(youtube_id[MyUtil._youtube_index],youtube_fieldname[MyUtil._youtube_index], youtube_web[MyUtil._youtube_index], youtube_memo[MyUtil._youtube_index], youtube_share[MyUtil._youtube_index]);				
					}else if (style.equals("attatch")) {
						MyUtil.SaveAttatchComponent(file_id[MyUtil._file_index],file_fieldname[MyUtil._file_index] ,file_filename[MyUtil._file_index] , file_memo[MyUtil._file_index], file_share[MyUtil._file_index] ,tp);
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
						MyUtil.SaveTeacherComponent(res_id[MyUtil._res_index],res_fieldname[MyUtil._res_index], res_name[MyUtil._res_index] , grad  ,res_content[MyUtil._res_index] , res_pic_filename[MyUtil._res_index] ,res_share[MyUtil._res_index],tp);
					}else if (style.equals("cite")) {
						MyUtil.SaveTeachingPlanCiteComponent(cite_id[MyUtil._ref_index],ref_style[MyUtil._ref_index],ref_id[MyUtil._ref_index],ref_title[MyUtil._ref_index], ref_tpid[MyUtil._ref_index] ,ref_memo[MyUtil._ref_index]);
					}	
				}
			}
			tpAfter.orderNumber=MyUtil._orderNumber;
			tpAfter.save();
			tp.tpAfter=tpAfter;
			tp.save();
			tp.photo=MyUtil.getFrontCoverForTP(tp.id);
			tp.save();
			Logger.info("user="+member.email+" create tp:"+ tp.id );
			//結束，reset tpid
			MyUtil._tpid=(long) 0;
			//跳選至該tp detail
			show(tp.id);
		}
		else {
			Application.index("您尚未登入或權限不足");
		}
		
		
	}
	
	@Restrict(ROLE.ADMIN)
	public static void disable(Long id) {
		Member member = MyUtil.getCurrentUser();
		TeachingPlan tp = TeachingPlan.findById(id);
		authTeachingPlan authTP = new authTeachingPlan(member,tp);
		if(authTP.allowWrite){
			tp.enable = false;
			tp.save();
		}
	}
	
	@Restrict(ROLE.ADMIN)
	public static void enable(Long id) {
		Member member = MyUtil.getCurrentUser();
		TeachingPlan tp = TeachingPlan.findById(id);
		authTeachingPlan authTP = new authTeachingPlan(member,tp);
		if(authTP.allowWrite){
			tp.enable = true;
			tp.save();
		}
	}
	
	public static void delete(Long id) {
		Member member = MyUtil.getCurrentUser();
		TeachingPlan tp = TeachingPlan.findById(id);
		authTeachingPlan authTP = new authTeachingPlan(member,tp);
		if(authTP.allowWrite){
			tp.enable = false;
			tp.save();
			Logger.info("user="+member.email+" disable tp:"+ tp.id );
		}
	} 
	
	public static void getSlide(Long tpid) {
		Logger.info(request.remoteAddress + " access tp slide, id=" + tpid);	
		TeachingPlan tp = TeachingPlan.findById(tpid);
		Member m = MyUtil.getCurrentUser();
		authTeachingPlan authTP = new authTeachingPlan(m,tp);
		if(authTP.allowRead) {
			StringBuilder basicBuilder = new StringBuilder();
			StringBuilder tpBeforeOutlineBuilder = new StringBuilder();
			StringBuilder tpBeforeDetailBuilder = new StringBuilder();
			StringBuilder tpNowOutlineBuilder = new StringBuilder();
			StringBuilder tpNowDetailBuilder = new StringBuilder();
			StringBuilder tpAfterOutlineBuilder = new StringBuilder();
			StringBuilder tpAfterDetailBuilder = new StringBuilder();
			
			// Process 基本頁面
			basicBuilder.append(buildSlideBasicContent(tp));
			
			//*** Process tpBefore  大綱 (10個元件標題為一頁 , 至多3頁)& 詳細內容
			if(tp.tpBefore!=null){
				String beforeComponentOrder = tp.tpBefore.orderNumber;
				if(null != beforeComponentOrder && !beforeComponentOrder.equals("")) {
					String [] components = beforeComponentOrder.split("##");
					
					//學前課程 封面
					tpBeforeOutlineBuilder.append("<slide class='title-slide segue nobackground'>");
					tpBeforeOutlineBuilder.append("<aside class='gdbar'><img style='width: 80%; height: 80%;'  src='/public/images/logo.png'></aside>");
					tpBeforeOutlineBuilder.append("<hgroup class='auto-fadein'><h1 data-config-title>學前課程</h1></hgroup>");
					tpBeforeOutlineBuilder.append("</slide>");
					
					//學前課程 簡介說明
					String illustration = tp.tpBefore.illustration;
					if(illustration!=null&&illustration!=""){
						tpBeforeOutlineBuilder.append("<slide>");
						tpBeforeOutlineBuilder.append("<hgroup><h2>學前課程說明</h2></hgroup>");
						tpBeforeOutlineBuilder.append("<article>");
						tpBeforeOutlineBuilder.append("<li><b>" + illustration + "</b></li>");
						tpBeforeOutlineBuilder.append("</article>");
						tpBeforeOutlineBuilder.append("</slide>");
					}
					
					// Outline
					tpBeforeOutlineBuilder.append(MyUtil.buildSlideOutlineContent(components, true));
					
					//元件內容
					for(int i=0 ; i<components.length ; i++) {	
						String type = components[i].split(",")[0];
						Long cid = Long.parseLong(components[i].split(",")[1]);
						tpBeforeDetailBuilder.append(MyUtil.buildSlideDetailContent(type,cid,true));
					}
				}
			}
			
			//*** Process tpNow  大綱 (10個元件標題為一頁 , 至多3頁)& 詳細內容
			if(tp.tpNow!=null){
				String nowComponentOrder = tp.tpNow.orderNumber;
				if(null != nowComponentOrder && !nowComponentOrder.equals("")) {
					String [] components = nowComponentOrder.split("##");
					
					//學中課程 封面
					tpNowOutlineBuilder.append("<slide class='title-slide segue nobackground'>");
					tpNowOutlineBuilder.append("<aside class='gdbar'><img style='width: 80%; height: 80%;'  src='/public/images/logo.png'></aside>");
					tpNowOutlineBuilder.append("<hgroup class='auto-fadein'><h1 data-config-title>學中課程</h1></hgroup>");
					tpNowOutlineBuilder.append("</slide>");
					
					//學中課程 簡介說明
					String illustration = tp.tpNow.illustration;
					if(illustration!=null&&illustration!=""){
						tpNowOutlineBuilder.append("<slide>");
						tpNowOutlineBuilder.append("<hgroup><h2>學中課程說明</h2></hgroup>");
						tpNowOutlineBuilder.append("<article>");
						tpNowOutlineBuilder.append("<li><b>" + illustration + "</b></li>");
						tpNowOutlineBuilder.append("</article>");
						tpNowOutlineBuilder.append("</slide>");
					}
					
					// Outline
					tpNowOutlineBuilder.append(MyUtil.buildSlideOutlineContent(components, true));
					
					//元件內容
					for(int i=0 ; i<components.length ; i++) {	
						String type = components[i].split(",")[0];
						Long cid = Long.parseLong(components[i].split(",")[1]);
						tpNowDetailBuilder.append(MyUtil.buildSlideDetailContent(type,cid,true));
					}
				}
			}
			
			//*** Process tpAfter  大綱 (10個元件標題為一頁 , 至多3頁)& 詳細內容
			if(tp.tpAfter!=null){
				String afterComponentOrder = tp.tpAfter.orderNumber;
				if(null != afterComponentOrder && !afterComponentOrder.equals("")) {
					String [] components = afterComponentOrder.split("##");
					
					//學後課程 封面
					tpAfterOutlineBuilder.append("<slide class='title-slide segue nobackground'>");
					tpAfterOutlineBuilder.append("<aside class='gdbar'><img style='width: 80%; height: 80%;'  src='/public/images/logo.png'></aside>");
					tpAfterOutlineBuilder.append("<hgroup class='auto-fadein'><h1 data-config-title>學後課程</h1></hgroup>");
					tpAfterOutlineBuilder.append("</slide>");
					
					//學後課程 簡介說明
					String illustration = tp.tpAfter.illustration;
					if(illustration!=null&&illustration!=""){
						tpAfterOutlineBuilder.append("<slide>");
						tpAfterOutlineBuilder.append("<hgroup><h2>學中課程說明</h2></hgroup>");
						tpAfterOutlineBuilder.append("<article>");
						tpAfterOutlineBuilder.append("<li><b>" + illustration + "</b></li>");
						tpAfterOutlineBuilder.append("</article>");
						tpAfterOutlineBuilder.append("</slide>");
					}
					
					// Outline
					tpAfterOutlineBuilder.append(MyUtil.buildSlideOutlineContent(components, true));
					
					//元件內容
					for(int i=0 ; i<components.length ; i++) {	
						String type = components[i].split(",")[0];
						Long cid = Long.parseLong(components[i].split(",")[1]);
						tpAfterDetailBuilder.append(MyUtil.buildSlideDetailContent(type,cid,true));
					}
				}
			}
			

			String basicInfo = basicBuilder.toString();
			String beforeOutline = tpBeforeOutlineBuilder.toString();
			String beforeDetail = tpBeforeDetailBuilder.toString();
			String nowOutline = tpNowOutlineBuilder.toString();
			String nowDetail = tpNowDetailBuilder.toString();
			String afterOutline = tpAfterOutlineBuilder.toString();
			String afterDetail = tpAfterDetailBuilder.toString();

			render("tp/lesson_slide.html",tp,basicInfo,
					beforeOutline,beforeDetail,
					nowOutline,nowDetail,
					afterOutline,afterDetail);
		}
		else {
			Application.index("您尚未登入或權限不足");
		}

	}
	
	private static String buildSlideBasicContent(TeachingPlan tp){

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
		builder.append(tp.name);
		builder.append("</h1>");
		
		builder.append("<h2 data-config-subtitle>");
		builder.append("  By  "+tp.creator.username);
		builder.append("</h2>");
		
		builder.append("<p data-config-presenter>");
		builder.append("建立日期： "+ tp.createDate);
		builder.append("</p>");
		
		builder.append("</hgroup>");
		builder.append("</slide>");
		
		
		//次標題頁(活動基本資訊)
		builder.append("<slide>");
		builder.append("<hgroup><h2>活動資訊</h2></hgroup>");
		builder.append("<article>");
		builder.append("<ul>");
		builder.append("<li>融入領域：<ul><li>" + tp.subject + " </li></ul></li><br>");
		builder.append("<li>對應課綱：<ul><li>" + tp.unit+ " </li></ul></li><br>");
		builder.append("<li>課程介紹：<ul><li> " + tp.introduction + "</li></ul></li>");
		builder.append("</ul>");
		builder.append(" </article>");
		builder.append("</slide>");
		
		return builder.toString();
	}

	

	 
	public static void show(@Required Long id) {
		Logger.info(request.remoteAddress + " access tp, id=" + id);		
		
		TeachingPlan tp = TeachingPlan.findById(id);
		Member m = MyUtil.getCurrentUser();
		
		authTeachingPlan authTP = new authTeachingPlan(m,tp);
		
		if(authTP.allowRead){
			//計算ViewCount
			MyUtil.calculateViewCount(ViewCountHistory.TeachingPlanView,tp.id);
			TeachingPlanBefore tpb = tp.tpBefore;
			TeachingPlanNow tpn = tp.tpNow;
			TeachingPlanAfter tpa = tp.tpAfter;
			
			StringBuilder tpb_contents = new StringBuilder();
			StringBuilder tpn_contents = new StringBuilder();
			StringBuilder tpa_contents = new StringBuilder();
			
			StringBuilder photos_contents = new StringBuilder();
			StringBuilder attatches_contents = new StringBuilder();
			StringBuilder links_contents = new StringBuilder();
			StringBuilder photos_nails500 = new StringBuilder();
			StringBuilder photos_nails100 = new StringBuilder();
	
			if(tpb!=null){
				tpb_contents.append(buildIntro(tpb.illustration));
				
				// Process TeachingPlanBefore
				if(null!=tpb.orderNumber && !tpb.orderNumber.equals("") ) {
					String s = tpb.orderNumber;
					int size = s.split("##").length;
					for(int i=0 ; i<size ; i++) {
						String myObj = s.split("##")[i];
						String type = myObj.split(",")[0];
						Long index = Long.parseLong(myObj.split(",")[1]);
						
						try {
							tpb_contents.append(buildContent(index, type));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
										
						// Process 教案圖片 
						if(type.equals("photo")) {
							Photo photo = Photo.findById(index);
							photos_contents.append(buildAllPhotos(photo.title, photo.intro, photo.path));
							photos_nails500.append(buildPhotosNail500(photo.path));
							photos_nails100.append(buildPhotosNail100(photo.path));
						}
						
						// Process 教案附件
						if(type.equals("attatch")) {
							Attatch attatch = Attatch.findById(index);
							attatches_contents.append(buildAllAttatches(attatch.intro, attatch.path, attatch.fileName));
						}
						
						// Process 教案連結
						if(type.equals("link")) {
							Link link = Link.findById(index);
							links_contents.append(buildAllLinks(link.url, link.linkName, link.intro));
						}
							
					}
				}
			}
			
			if(tpn!=null){
				tpn_contents.append(buildIntro(tpn.illustration));
				// Process TeachingPlanNow
				if(null!=tpn.orderNumber && !tpn.orderNumber.equals("") ) {
					
					String s = tpn.orderNumber;			
					int size = s.split("##").length;
					
					for(int i=0 ; i<size ; i++) {
						String myObj = s.split("##")[i];
						String type = myObj.split(",")[0];
						Long index = Long.parseLong(myObj.split(",")[1]);
						
						try {
							tpn_contents.append(buildContent(index, type));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						// Process 教案圖片 
						if(type.equals("photo")) {
							Photo photo = Photo.findById(index);
							photos_contents.append(buildAllPhotos(photo.title, photo.intro, photo.path));
							photos_nails500.append(buildPhotosNail500(photo.path));					
							photos_nails100.append(buildPhotosNail100(photo.path));
						}
						
						// Process 教案附件
						if(type.equals("attatch")) {
							Attatch attatch = Attatch.findById(index);
							attatches_contents.append(buildAllAttatches(attatch.intro, attatch.path, attatch.fileName));
						}
						
						// Process 教案連結
						if(type.equals("link")) {
							Link link = Link.findById(index);
							links_contents.append(buildAllLinks(link.url, link.linkName, link.intro));
						}
					}
				}
			}
			
			if(tpa!=null){
				tpa_contents.append(buildIntro(tpa.illustration));
				// Process TeachingPlanAfter
				if(null!=tpa.orderNumber && !tpa.orderNumber.equals("")) {
					
					String s = tpa.orderNumber;
					int size = s.split("##").length;
					for(int i=0 ; i<size ; i++) {				
						String myObj = s.split("##")[i];
						String type = myObj.split(",")[0];
						Long index = Long.parseLong(myObj.split(",")[1]);
						
						try {
							tpa_contents.append(buildContent(index, type));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						// Process 教案圖片 
						if(type.equals("photo")) {
							Photo photo = Photo.findById(index);
							photos_contents.append(buildAllPhotos(photo.title, photo.intro, photo.path));
							photos_nails500.append(buildPhotosNail500(photo.path));		
							photos_nails100.append(buildPhotosNail100(photo.path));
						}
						
						// Process 教案附件
						if(type.equals("attatch")) {
							Attatch attatch = Attatch.findById(index);
							attatches_contents.append(buildAllAttatches(attatch.intro, attatch.path, attatch.fileName));
						}
						
						// Process 教案連結
						if(type.equals("link")) {
							Link link = Link.findById(index);
							links_contents.append(buildAllLinks(link.url, link.linkName, link.intro));
						}
					}
				}
			}
			
			
			// user login
			String add = "no";
			if(m!=null){
				if(m.addToFavorite("tp", m.id, id))
					add = "yes";
			}
			
			// 建立者沒有上傳任何圖片
			if(photos_nails500.length()==0) {
				photos_nails500.append(buildDefaultPhotos("500"));
				photos_nails100.append(buildDefaultPhotos("100"));
			}
			
			String tpb_content = tpb_contents.toString();
			String tpn_content = tpn_contents.toString();
			String tpa_content = tpa_contents.toString();
			String photo_content = photos_contents.toString();
			String photo_nail_500 = photos_nails500.toString();
			String photo_nail_100 = photos_nails100.toString();
			String attatch_content = attatches_contents.toString();
			String link_content = links_contents.toString();
			
			tp.save();

			render("tp/lesson_detail.html",add, tp, tpb_content, tpn_content, tpa_content, photo_nail_500, photo_nail_100, photo_content, attatch_content, link_content, m);
		}
		else {
			Application.index("您尚未登入或權限不足");
		}
	}

	private static Object buildIntro(String illustration) {
		
		if(null==illustration || illustration.equals(""))
			illustration = "無";
		
		illustration = MyUtil.replaceTextAreaToHtml(illustration);
		StringBuilder s = new StringBuilder();
		s.append("<div class=\"ldt_div\">");
		s.append("<h4 class=\"ldt_title\">課程說明</h4>");
		s.append("<div class=\"ldt_text\">");
		s.append(illustration);
		s.append("</div>");
		s.append("</div>");		
		return s.toString();
	}

	private static String buildContent(Long id, String type) throws Exception {		
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
						teacher.introduction, teacher.photo.path));				
			} else if (type.equals("attatch")) {
				Attatch attatch = Attatch.findById(id);
				s.append(buildAttach(attatch.title, attatch.intro, attatch.path, attatch.fileName));
			} else if(type.equals("cite")) {
				Cite cite = Cite.findById(id);
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
	
	private static String buildText(String title, String intro) {
		
		StringBuilder s = new StringBuilder();		
		s.append("<div class=\"ldt_div lesson_html\">");
		s.append("<h4 class=\"ldt_title\">");
		s.append(title);
		s.append("</h4>");
		s.append("<div class=\"ldt_text\">");
		s.append("<p>");
		s.append(intro);
		s.append("</p>");
		s.append("</div>");
		s.append("</div>");
		
		return s.toString();		
	}
		
	private static String buildMap(String title, String POIName, String intro, String lat, String lng, String address,
			String phone, String startTime, String endTime) {
		intro=MyUtil.replaceTextAreaToHtml(intro);	
		StringBuilder s = new StringBuilder();		
		s.append("<div class=\"ldt_div lesson_spot\">");
		s.append("<h4 class=\"ldt_title\">");
		s.append(title);
		s.append("</h4>");
		s.append("<div class=\"ldt_text\"><div class=\"spots\"><div class=\"spot\"><p class=\"spot_title\"><i class=\"icon-map-marker\"></i>");
		s.append(POIName);
		s.append("<button class=\"pull-right btn btn-warning btn-mini onepoint\">地圖</button></p>");
		s.append("<p class=\"spot_tel\">");
		s.append(phone);
		s.append("</p>");
		s.append("<p class=\"spot_intro\">");
		s.append(intro);
		s.append("</p>");		
		s.append("<p class=\"spot_address\">");
		s.append(address);
		s.append("</p></div></div></div></div>");		
		
		return s.toString();
	}
	
	private static String buildYoutube(String title, String src, String intro) {
		
		intro=MyUtil.replaceTextAreaToHtml(intro);	
		src = MyUtil.youtubeSrcParser(src);
		StringBuilder s = new StringBuilder();		
		s.append("<div class=\"ldt_div lesson_youtube\">");
		s.append("<h4 class=\"ldt_title\">");
		s.append(title);
		s.append("</h4>");
		s.append("<div class=\"ldt_text\">");
		s.append("<iframe width=\"688\" height=\"400\" src=");
		s.append("\"//www.youtube.com/embed/" + src + "\"");
		s.append(" frameborder=\"0\" allowfullscreen></iframe>");
		s.append("<p>");
		s.append(intro);	
		s.append("</p>");
		s.append("</div>");
		s.append("</div>");
		
		return s.toString();
	}
	
	private static String buildTeacher(String title, String teacherName, String education, String intro, String pSrc) {
		intro=MyUtil.replaceTextAreaToHtml(intro);	
		StringBuilder s = new StringBuilder();		
		s.append("<div class=\"ldt_div lesson_teacher\">");
		s.append("<h4 class=\"ldt_title\">");
		s.append(title);
		s.append("</h4>");
		s.append("<div class=\"ldt_text\">");
		s.append("<div class=\"teacher_resume\">");
		s.append("<div class=\"pull-left teacher_resume_1\">");
		s.append("<div class=\"teacher_img\"><img class=\"\" src=");
		s.append("\"" + pSrc + "\"");
		s.append("></div>");
		s.append("</div>");
		s.append("<div class=\"pull-right teacher_resume_2\">");
		s.append("<p class=\"teacher_name\"><a>");
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
		s.append(intro);
		s.append("</p>");
		s.append("</div><div class=\"clearfix\"></div></div></div></div>");		
		
		return s.toString();
	}
		
	private static String buildAttach(String title, String intro, String path, String fileName) {
		intro=MyUtil.replaceTextAreaToHtml(intro);	
		StringBuilder s = new StringBuilder();		
		s.append("<div class=\"ldt_div lesson_file\">");
		//s.append("<a class=\"pull-right btn btn-info\" href='"+path+"'>下載附件</a>");
		s.append("<h4 class=\"ldt_title\">");
		s.append(title);
		s.append("</h4>");
		
		String extension =fileName.substring(fileName.lastIndexOf('.')+1).toLowerCase();
		s.append("<div class=\"ldt_text\"><div class=\"files\"><span class='"+extension+"'><a href=");
		s.append("\"" + path + "\">");
		s.append(fileName);
		s.append("</a><b>");
		s.append(intro);
		s.append("</b></span>");
		s.append("</div></div></div>");
		return s.toString();
	}
	
	private static String buildCite(String intro, Long Id) {
		
		StringBuilder s = new StringBuilder();
		Cite cite = Cite.findById(Id);
		if(cite!=null){
			intro=MyUtil.replaceTextAreaToHtml(intro);	
			s.append("<div class=\"ldt_div lesson_pic lesson_reference\">");
			s.append("<span class=\"reference_mark top\">\"</span>");
			
			String t = cite.style;
			Long i = cite.tid;
			try {
				s.append(buildContent(i, t));
			} catch (Exception e) {
				e.printStackTrace();
			}
		
			s.append("<span class=\"reference_mark bottom\">\"</span>");
			s.append("<div class=\"ldt_reference\">引用自");
			s.append("<a href='/member/profile/"+cite.citeTP.creator.id+"'>");
			s.append(cite.citeTP.creator.username);
			s.append("</a>的");
			s.append("<a href='/tp/show/"+cite.citeTP.id+"'>");
			s.append(cite.citeTP.name);
			s.append("</a></div>");
			s.append("<div class=\"ldt_refenence_addinfo\">");
			s.append(intro);
			s.append("</div>");
			s.append("</div>");
		}
		return s.toString();
	}
	
	private static String buildLink(String title, String url, String name, String intro) {
		intro=MyUtil.replaceTextAreaToHtml(intro);	
		StringBuilder s = new StringBuilder();		
		s.append("<div class=\"ldt_div lesson_link\">");
		//s.append("<a class=\"pull-right btn btn-info\" target=_blank  href='" +  url + "'>前往連結</a>");
		s.append("<h4 class=\"ldt_title\">");
		s.append(title);
		s.append("</h4>");
		s.append("<div class=\"ldt_text\"><div class=\"files\"><span class=\"link\"><a href='");
		s.append(url);
		s.append("'>");
		s.append(name);
		s.append("</a><b>");
		s.append(intro);
		s.append("</b>");
		s.append("</span>");
		s.append("</div></div></div>");
		
		return s.toString();
	}
	
	private static String buildPhoto(String title, String intro, String path, String fileName) {
		intro=MyUtil.replaceTextAreaToHtml(intro);	
		StringBuilder s = new StringBuilder();		
		s.append("<div class=\"ldt_div lesson_pic\"><h4 class=\"ldt_title\">");
		s.append(title);
		s.append("</h4>");
		s.append("<div class=\"ldt_text\"><img src=");
		s.append("\"" + path + "\">");
		s.append("<p>");
		s.append(intro);
		s.append("</p>");
		s.append("</div></div>");		
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
	
	private static String buildDefaultPhotos(String type) {
		StringBuilder s = new StringBuilder();
		if(type.equals("500")) {
			s.append("<li>");
			s.append("<div class=\"nailthumb-container nc500\">");
			s.append("<img src=");
			s.append("\"" + "/public/images/default/tp_default_500.png" + "\" />");
			s.append("</div>");
			s.append("</li>");
		} else if(type.equals("100")) {
			s.append("<li>");
			s.append("<div class=\"nailthumb-container nc100\">");
			s.append("<img src=");
			s.append("\"" + "/public/images/default/tp_default_100.png" + "\" />");
			s.append("</div>");
			s.append("</li>");
		}
		
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
}
