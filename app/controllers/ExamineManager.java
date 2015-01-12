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
import javax.management.relation.Role;
import javax.persistence.Query;
import javax.xml.ws.RequestWrapper;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.simple.JSONObject;

import models.Apply;
import models.Answer;
import models.Contact;
import models.Examine;
import models.Member;
import models.OutdoorActivity;
import models.Question;
import models.TeachingPlan;
import play.Logger;
import play.Play;
import play.data.Upload;
import play.data.validation.Email;
import play.data.validation.Password;
import play.data.validation.Required;
import play.db.jpa.JPA;
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
import com.itextpdf.text.pdf.PdfStructTreeController.returnType;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import com.sun.xml.internal.bind.v2.model.core.ID;

import controllers.deadbolt.Restrict;
import controllers.deadbolt.Restrictions;
import flexjson.JSONSerializer;




public class ExamineManager extends Controller {
	
	@Before
	public static void getTag() {
		Application.getTag();	   
	}
	
	public static class authExamine{
		boolean allowRead = false;		//可看到某一特定評量
		boolean allowFill = false;		//可填寫某一特定評量
		boolean allowWrite = false;		//可編輯修改某一特定評量 與 可使用評量管理功能
		boolean allowComment = false; 	//老師可否批改某學生填寫之答案
		
		//針對特定Examine
		authExamine(Member member,Examine examine){
			if(member==null || examine==null) 
				return;
			else{
				//判斷可Write (登入者=建立者 或登入者=admin)
				if(examine.creator.equals(member) || member.roleList.contains(ROLE.ADMIN)){
					this.allowWrite=true;
					this.allowRead=true;
				}			
				//判斷可Read(及可看)[當學生(登入者)有報名此活動時且評量為公開時]			
				if(examine.publish){
					OutdoorActivity oa = OutdoorActivity.findOAByExamID(examine.id);
					if(oa!=null){
						Apply apply =Apply.findApplyByOAandAttendMember(oa.id, member.id);
						if(apply!=null)
							this.allowRead=true;						
						//判斷可填寫此評量[同上，另外加上條件:當此評量的Status為開放填寫時]
						if(apply!=null && examine.status==Examine.progress)
							this.allowFill=true;
					}
				}
			}
		}
		
		//針對特定某一學生之填答
		authExamine(Member member,Answer answer){
			if(member==null || answer==null) 
				return;
			else{
				//判斷可批改 (登入者=建立者 或登入者=admin)
				if(answer.examine.creator.equals(member) || member.roleList.contains(ROLE.ADMIN)){
					this.allowWrite=true;
					this.allowRead=true;
					this.allowComment=true;
				}	
				//判斷登入者可否看到此特定學生之回答
				if(member.equals(answer.apply.attendantMember)){
					this.allowRead=true;
				}
			}
		}
		
		//針對 登入者 對於評量功能之權限
		authExamine(Member member){
			if(member==null)
				return;
			else {
				//判斷是否可使用 評量管理功能 當身分具有老師或admin時可使用
				if(member.roleList.contains(ROLE.TEACHINGPLANNER)||member.roleList.contains(ROLE.ADMIN))
					this.allowWrite=true;
			}
		}
	}
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void index() {
		Member member = MyUtil.getCurrentUser();
		authExamine auth = new authExamine(member);
		
		if(auth.allowWrite){
			List<Member> teachers  = new ArrayList<Member>();
			teachers = Member.findMemberByRole(ROLE.TEACHINGPLANNER);
			List<OutdoorActivity> oas = new ArrayList<OutdoorActivity>();
			render("/management/teacher_manage_exam.html", member, teachers, oas);
		}
		else {
			Application.index("您尚未登入或權限不足");
		}
	}
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void createAgree() {
		Member member = MyUtil.getCurrentUser();
		authExamine auth = new authExamine(member);
		if(auth.allowWrite){
			render("exam/create_exam_agree.html");	
		}
		else {
			Application.index("您尚未登入或權限不足");
		}
	}
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void create(Long id) {
		Member member = MyUtil.getCurrentUser();
		authExamine auth = new authExamine(member);
		if(auth.allowWrite){
			List<OutdoorActivity> listMyOA =OutdoorActivity.listByCreator(member.id);
			if(id!=0){
				Examine examine = Examine.findById(id);
				authExamine authExamine = new authExamine(member,examine);
				if(authExamine.allowWrite)
					render("exam/create_exam.html",examine,listMyOA);
				else {
					Logger.info("user="+member.email+" try to edit examine:"+ id + ",but it's not belong to him/her.");
					create((long) 0);
				}
			}
			else {
				render("exam/create_exam.html",listMyOA);
			}
		} else {
			Application.index("未登入或您的權限不足");
		}
	}
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void create_basicInfo(@Required Long oaid , @Required Long examID , @Required String exam_title, @Required int publish , @Required int status,
			@Required String startDate ,@Required String endDate , @Required String startTime, @Required String endTime, String introduction){
		Member member = MyUtil.getCurrentUser();
		authExamine auth = new authExamine(member);
		if(auth.allowWrite){
			Examine examine = null;
			if(examID==null || examID==0)
				examine = new Examine();
			else{
				if(examID>0)
					examine = Examine.findById(examID);
					authExamine authExamine = new authExamine(member,examine);
					if(!authExamine.allowWrite){
						Logger.info("user="+member.email+" try to edit examine:"+ examID + ",but it's not belong to him/her.");
						examine = null;
					}	
			}
			OutdoorActivity oa = OutdoorActivity.findById(oaid);
			if(examine!=null && oa!=null){
				examine.creator =member;
				examine.title=exam_title;
				examine.publish=(publish==1)?true:false;
				examine.status=status;
				examine.startDate=startDate;
				examine.endDate=endDate;
				examine.startTime=startTime;
				examine.endTime =endTime;
				examine.introduction=introduction;
				examine.save();
				
				//移除舊有的belong OA 再加入新的OA
				OutdoorActivity oldOA = OutdoorActivity.findOAByExamID(examID);
				if(oldOA!=null){
					oldOA.examines.remove(examine);
					oldOA.save();
				}
				if(!oa.examines.contains(examine))
					oa.examines.add(examine);
				oa.save();

				MyUtil._examid=examine.id;
			}
			else {
				Application.index("您無此權限。");
			}
		}
		else {
			Application.index("未登入或您的權限不足");
		}
	}
	
	public static void create_Question(String[] examType ,String questionExplain,String illustration,
			Long[] ox_id , String[] ox_Question , String[] ox_Filename, int[] ox_Answer , String[] ox_Intro , int[] ox_Score,
			Long[] singleAnswer_id, String[] singleAnswer_Question , String[] singleAnswer_Filename,
			String[] singleAnswer_OptionA, String[] singleAnswer_OptionB, String[] singleAnswer_OptionC ,String[] singleAnswer_OptionD,
			int[] singleAnswer_Answer , String[] singleAnswer_Intro , int[] singleAnswer_Score,
			Long[] shortAnswer_id , String[] shortAnswer_Question,String[] shortAnswer_Filename, String[] shortAnswer_Intro){
		if(MyUtil._examid>0){
			Member member = MyUtil.getCurrentUser();
			Examine examine = Examine.findById(MyUtil._examid);
			authExamine authExamine = new authExamine(member,examine);
			if(authExamine.allowWrite){
				MyUtil.ResetQuestionsIndexVar();
				examine.questionExplain=questionExplain;
				examine.illustration=illustration;
				//依序儲存元件
				if(examType!=null){
					
					for (String style : examType) {
						if(style.equals("ox")){
							MyUtil.SaveOXQuestion(member,ox_id[MyUtil._ox_index],ox_Question[MyUtil._ox_index] , ox_Filename[MyUtil._ox_index] ,ox_Answer[MyUtil._ox_index] , ox_Intro[MyUtil._ox_index] , ox_Score[MyUtil._ox_index]);			
						}else if (style.equals("singleAnswer")) {
							MyUtil.SaveSingleAnswerQusetion(member,singleAnswer_id[MyUtil._singleAnswer_index],singleAnswer_Question[MyUtil._singleAnswer_index] ,singleAnswer_Filename[MyUtil._singleAnswer_index],
									singleAnswer_OptionA[MyUtil._singleAnswer_index],singleAnswer_OptionB[MyUtil._singleAnswer_index],singleAnswer_OptionC[MyUtil._singleAnswer_index],singleAnswer_OptionD[MyUtil._singleAnswer_index],
									singleAnswer_Answer[MyUtil._singleAnswer_index] ,singleAnswer_Intro[MyUtil._singleAnswer_index] , singleAnswer_Score[MyUtil._singleAnswer_index]);					
						}else if (style.equals("shortAnswer")) {
							MyUtil.SaveShortAnswerQuestion(member,shortAnswer_id[MyUtil._shortAnswer_index],shortAnswer_Question[MyUtil._shortAnswer_index], shortAnswer_Filename[MyUtil._shortAnswer_index], shortAnswer_Intro[MyUtil._shortAnswer_index]);	
						}
						
					}
				}
				Long eid = MyUtil._examid;
				//reset _examid
				MyUtil._examid=(long) 0;
				savedInfo(eid);
			}
			else {
				Application.index("未登入或您的權限不足");
			}
		}
	}
	
	public static void savedInfo(@Required Long id){
		Member member = MyUtil.getCurrentUser();
		Examine examine = Examine.findById(id);
		authExamine authExamine = new authExamine(member,examine);
		if(authExamine.allowWrite){
			render("exam/create_exam_saved.html",examine);
		}
		else {
			Application.index("未登入或您的權限不足");
		}
	}
	
	public static void show(@Required Long id) {
		Member member = MyUtil.getCurrentUser();
		Examine examine = Examine.findById(id);
		authExamine authExamine = new authExamine(member,examine);
		if(authExamine.allowRead) {
			
			//將斷行字元轉為<BR>
			examine.introduction=MyUtil.replaceTextAreaToHtml(examine.introduction);
			
			OutdoorActivity oa = OutdoorActivity.findOAByExamID(id);
			Apply apply = null;
			if(oa!=null)
				apply=Apply.findApplyByOAandAttendMember(oa.id, member.id);
			
			Answer answer = null;
			if(member.roleList.contains(ROLE.STUDENT)) {
				//若已回答過則有answer，否則為null (學生)
				answer = Answer.findAnsByExamAndApply(examine.id, apply.id);
			}

			//判斷是否已作答過
			if(answer!=null)
				showResult(answer.id);
			else {
				int totalScore =0;
				for (Question q : examine.questions) {
					totalScore = totalScore+q.score;
				}
				
				//總人數
				List<Apply> listApply = Apply.findApplyByOAByStatus(oa.id, Apply.Payed);
				//已填寫此評量人數
				int answerCount = Answer.getCurrTakerCount(oa.id, examine.id);
				//列出已作答之學生(老師用)
				List<Answer> listAns = Answer.findAnsByExam(examine.id);			
				render("exam/exam_detail.html",member, examine, oa, totalScore, listApply, answerCount, answer, listAns);
			}	
		}
		else {
			Application.index("您沒有權限檢視此評量");
		}
	}
	
	public static void showExplain(@Required Long id){
		Member member = MyUtil.getCurrentUser();
		Examine examine = Examine.findById(id);
		authExamine authExamine = new authExamine(member,examine);
		if(authExamine.allowFill){
			//將斷行字元轉為<BR>
			examine.questionExplain=MyUtil.replaceTextAreaToHtml(examine.questionExplain);
			OutdoorActivity oa = OutdoorActivity.findOAByExamID(id);
			render("exam/exam_answer1.html",examine,oa);
		}
		else {
			Application.index("您沒有權限檢視此評量");
		}	
	}
	
	public static void showQuestion(@Required Long id){
		Member member = MyUtil.getCurrentUser();
		Examine examine = Examine.findById(id);
		authExamine authExamine = new authExamine(member,examine);
		if(authExamine.allowFill){
			OutdoorActivity oa = OutdoorActivity.findOAByExamID(id);
			render("exam/exam_answer2.html",examine,oa);
		}
		else {
			Application.index("您沒有權限檢視此評量");
		}
	}
	
	public static void fillExamine(@Required Long oaID ,@Required Long examID,@Required String[] ans){
		
		Member member = MyUtil.getCurrentUser();
		Examine examine = Examine.findById(examID);	
		authExamine authExamine = new authExamine(member,examine);
		JSONObject json = new JSONObject();
		JSONObject response = new JSONObject();
		if(authExamine.allowFill){
			OutdoorActivity oa = OutdoorActivity.findById(oaID);
			Apply apply = Apply.findApplyByOAandAttendMember(oa.id, member.id);
			Answer answer =new Answer();
			//組成字串
			for (String a : ans) {
				answer.ans+=a+"##";
			}
			answer.apply = apply;
			answer.do_response=true;
			answer.examine=examine;
			answer.oa=oa;
			answer.score=calculateScore(examine,ans);
			answer.responseTime=MyUtil.now();
			answer.save();
			response.put("result", "true");
			response.put("id", answer.id);
		}
		else {
			response.put("result", "false");
			response.put("id", "0");
		}
		
		json.put("response", response);
		JSONSerializer serializer = new JSONSerializer();
		serializer.deepSerialize(json);
		String output = serializer.deepSerialize(json);
		renderJSON(output);
	}
	
	private static int calculateScore(Examine examine, String[] ans) {
		int score =0;
		int i=0;
		String[] orderNumbers = examine.orderNumber.split("##");
		for (String order : orderNumbers) {
			Question question = Question.findById(Long.parseLong(order));
			//只計算選擇&是非
			System.out.println(question.style);
			if(question.style!=2){
				if(question.answer== Integer.parseInt(ans[i]))
					score=score+question.score;
			}
			i++;
		}
		return score;
	}
	
	public static void showResult(@Required Long id){
		Member member = MyUtil.getCurrentUser();
		Answer answer = Answer.findById(id);
		authExamine authAnswer = new authExamine(member,answer);
		
		if(authAnswer.allowRead){
			//將斷行字元轉為<BR>
			answer.examine.illustration=MyUtil.replaceTextAreaToHtml(answer.examine.illustration);
			render("exam/exam_review.html",answer);
		}
		else {
			Application.index("您沒有權限檢視此評量結果");
		}	
	}
	
	public static void commentExamine(@Required Long ansId ,@Required Long questionId,@Required String comment, @Required int bonusScore){
		Member member = MyUtil.getCurrentUser();
		Question question = Question.findById(questionId);
		Answer ans = Answer.findById(ansId);
		authExamine authAnswer = new authExamine(member,ans);

		if(authAnswer.allowComment && ans.examine.questions.contains(question)){
			ans.commentTime+=question.id+","+MyUtil.now()+"##,";
			ans.teacher_comment+=question.id+","+comment+"##,";
			ans.bonusScore+=bonusScore;
			ans.save();		
			renderText("批改已送出。");
		}
		else {
			renderText("批改失敗。");
		}
	}
	
	public static void finishExamine(@Required Long ansId){
		Member member = MyUtil.getCurrentUser();
		Answer ans = Answer.findById(ansId);
		authExamine authAnswer = new authExamine(member,ans);

		if(authAnswer.allowComment){
			ans.do_comment=true;
			ans.save();	
			renderText("閱卷已完成。");
		}
		else if (authAnswer.allowRead) {
			renderText("閱卷已完成。");
		}
		else {
			renderText("閱卷失敗。");
		}
	}
	
	//於show examine detail 之頁面 按下 刪除按鈕
	public static void deleteExamine(Long eid) {		
		Logger.info("execute del exam, examId="+eid);
		Examine examine = Examine.findById(eid);
		Member member = MyUtil.getCurrentUser();
		authExamine authExamine= new authExamine(member,examine);
		if(authExamine.allowWrite) {
			OutdoorActivity oa = OutdoorActivity.findOAByExamID(eid);
			Query q = JPA.em().createNativeQuery("DELETE from OutdoorActivity_Examine where OutdoorActivity_id=" + oa.id + " and examines_id=" + eid);
			q.executeUpdate();
		
			Question.delQuesByExam(eid);
			
			List<Answer> listAnswer = Answer.findAnsByExam(examine.id);
			if(listAnswer!=null) {
				for (Answer ans : listAnswer) 
					ans.delete();	
			}
			
			examine.delete();
			index();
		} else {
			Application.index("您沒有此權限");
		}
	}
	
	// 於評量管理葉面之刪除按鈕 function
	public static void delExamine(Long e_id, Long oaid) {	
		Logger.info("execute del exam, e_id="+e_id);
		Examine e = Examine.findById(e_id);
		Member member = MyUtil.getCurrentUser();
		authExamine authExamine= new authExamine(member,e);
		if(authExamine.allowWrite) {
			Query q = JPA.em().createNativeQuery("DELETE from OutdoorActivity_Examine where OutdoorActivity_id=" + oaid + " and examines_id=" + e_id);
			q.executeUpdate();
			Question.delQuesByExam(e_id);
			List<Answer> listAnswer = Answer.findAnsByExam(e_id);
			if(listAnswer!=null) {
				for (Answer ans : listAnswer) 
					ans.delete();	
			}
			
			e.delete();
			
			List<Examine> examine = Examine.findExamineByOa(oaid, ROLE.TEACHINGPLANNER);
			if(null!= examine && examine.size()>Conf.pagination) {
				//buildPagination(apply);			
				examine = examine.subList(0, Conf.pagination);
			}
			OutdoorActivity oa = OutdoorActivity.findById(oaid);
			String html = buildHtmlByExamineAndOa(examine, oa);
			renderHtml(html);
		} else {
			Application.index("您沒有此權限");
		}
	}
	
	
	
	public static void getExamineByOaId(Long oaid, int start, int end) {
		String html = "";
		List<Examine> examine = new ArrayList<Examine>();
		examine = Examine.findExamineByOa(oaid, ROLE.TEACHINGPLANNER);
		if(null!= examine && examine.size()>Conf.pagination) {
			//buildPagination(apply);			
			examine = examine.subList(start, end);
		}
		OutdoorActivity oa = OutdoorActivity.findById(oaid);
		html = buildHtmlByExamineAndOa(examine, oa);
		renderText(html);
	}
	
	public static void getExamineByKw(String kw, Long oaid) {
		kw = StringEscapeUtils.escapeSql(kw);
		String html = "";
		List<Examine> examine = Examine.findExamineByOaidAndKwOnExamineName(oaid, kw);
		if(null!= examine && examine.size()>Conf.pagination) {
			//buildPagination(apply);			
			examine = examine.subList(0, Conf.pagination);
		}
		OutdoorActivity oa = OutdoorActivity.findById(oaid);
		html = buildHtmlByExamineAndOa(examine, oa);
		renderText(html);
	}

	private static String buildHtmlByExamineAndOa(List<Examine> examine,
			OutdoorActivity oa) {
		String html = "";
		for(int i=0 ; i<examine.size() ; i++) {
			Examine e = examine.get(i);
			
			String status = "";
			if(e.status==Examine.unstart)
				status = "尚未開放作答";
			else if(e.status==Examine.progress)
				status = "開放作答中";
			else if(e.status==Examine.finished)
				status = "已結束作答";
			else
				status = "評量狀況異常，請洽管理員";
			
			int total = OutdoorActivity.getCurrApplyCount(oa.id); // 總共參加此活動的人數
			int taker = Answer.getCurrTakerCount(oa.id, e.id);
			int unCheck = Answer.getUnCommentCount(oa.id, e.id);
			
			html += "<tr>";
			html += "<td class='mse_td1'>"+(i+1)+"</td>";
			html += "<td class='mse_td2'><a href='/examine/show/"+e.id+"'>"+e.title+"</a></td>";
			html += "<td class='mse_td3'>"+oa.name+"</td>";
			html += "<td class='mse_td4'>"+e.startTime+"</td>";
			html += "<td class='mse_td5'>"+e.endTime+"</td>";
			html += "<td class='mse_td6'>";
			html += "<button class='btn btn-mini disabled'>"+status+"</button>";
			html += "</td>";
			html += "<td class='mse_td7'>"+taker + "/" + total +"</td>";
			html += "<td class='mse_td8'>"+unCheck+"</td>";
			html += "<td class='mse_td9'>";
			html += "<a class='btn btn-mini btn-info' href='/examine/show/"+e.id+"'>閱卷</a>";
			html += "<a class='btn btn-mini btn-google' href='/examine/create/"+e.id+"'>編輯</a>";
			//html += "<button class='btn btn-mini btn-info'>預覽</button>";
			//html += "<button class='btn btn-mini btn-danger' onclick='delExamine("+e.id+","+oa.id+")'>刪除</button>";
			html += "<a class='btn btn-mini btn-danger' href='#' onclick='delExamine("+e.id+","+oa.id+")'>刪除</a>";
			html += "</td>";
			html += "</tr>";
		}
		
		return html;
	}
	
}
