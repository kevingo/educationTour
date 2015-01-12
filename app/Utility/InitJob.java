package Utility;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

import flexjson.transformer.StringTransformer;

import models.Answer;
import models.Apply;
import models.Attatch;
import models.Cite;
import models.Contact;
import models.Examine;
import models.HistoryNode;
import models.Link;
import models.Map;
import models.Member;
import models.MemberGroup;
import models.NFC;
import models.OAIncludeTP;
import models.OutdoorActivity;
import models.Photo;
import models.Q_Showpiece;
import models.Q_ShowpieceHistoryNode;
import models.Q_ShowpieceVersion;
import models.Question;
import models.TagActivity;
import models.Teacher;
import models.TeachingPlan;
import models.TeachingPlanAfter;
import models.TeachingPlanBefore;
import models.TeachingPlanNow;
import models.Youtube;

import java.sql.*; //引入JDBC套件

//import models.InviteCode;
//import models.Member;
//import models.Q_ShowpieceVersion;
//import models.Q_VolunteerSeesion;
import play.Logger;
import play.data.validation.Phone;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.mvc.Http.Request;

@OnApplicationStart
public class InitJob extends Job {
	
    public void doJob() throws Exception {
    	
    	Logger.info("init job start");	
    	
    	createQuantaAccount();
    	createQuantaShowpieceVersion();
    	Logger.info("quanta job finish");
    	
    	createTagActivity();
    	Logger.info("TagActivity job finish");
    	
    	//createPlatformTester();
    	//Logger.info("create default account finish");
    	
    	createSMTPConnection();
    	Logger.info("create SMTP connection job finish");
    	
    	//restoreDataFromEC2();
    	
    	//cleanAllTesterData();
    	/*
    	cleanTesterData("kevingo75@gmail.com");
    	cleanTesterData("pasta0806@hotmail.com");
    	cleanTesterData("pasta0806@gmail.com");
    	cleanTesterData("admin@gmail.com");
    	cleanTesterData("vincent@mohas.cc");
    	cleanTesterData("kevingo75@itri.org.tw");
    	cleanTesterData("miou@ms55.url.com.tw");
    	cleanTesterData("vin_test01@gmail.com");
    	cleanTesterData("vin_test002@gmail.com");
    	cleanTesterData("miou1107@gmail.com");
    	cleanTesterData("annchen1982@gmail.com");
    	cleanTesterData("nelson@mohas.cc");
    	cleanTesterData("test@vincent.com");
    	Logger.info("clean test account data job finish");
    	*/
    	raisePriorityJob(); 	
    	Logger.info("do raisePriorityJob job finish");
    	
    	Logger.info("init job finish");
    }
    

    private void restoreDataFromEC2() {
    	try{
            //載入JDBC驅動程式
            Class.forName("com.mysql.jdbc.Driver");
            //建立資料庫連線
            Connection conn = DriverManager.getConnection("jdbc:mysql://www.funnytrip.org:3306/edutour" +
            		"?user=root&password=7575&autoReconnect=true");
            //設定Statement物件，此物件負責處理靜態SQL指令
            Statement stmt = conn.createStatement();
            //將查詢結果存放至 rs
            ResultSet rs = stmt.executeQuery("select * from edutour.member m where m.isQuantaUser=1");   //將查詢結果存入rs

            
            while (rs.next()) {
				//Member type = (Member) rs.next().nextElement();
				Long id =rs.getLong("id");
				String email =rs.getString("email");
				String password = rs.getString("password");
				String username = rs.getString("username");
				String schoolName = rs.getString("schoolName");
				boolean isAdult  = rs.getBoolean("isAdult");
				
				Member member=Member.findUser(email);
            	if(member==null){	       
            		if((id>=294&& id<=454)||id==553){
            			member = new Member(email, password,ROLE.Q_VOLUNTEER,true,username);
            			//email , pwd , rolename , isQuantaUser ,username 
            			//member.isAdult=isAdult;
            			member.save();
            		}
            		else if (id>=540&& id<=552) {
            			//member = new Member(email, password,ROLE.Q_SHOWPIECE,true ,schoolName);
		                //email , password , roleName , isQuantaUser , schoolName
            			//member.save();
					}
	                
	                //member.save();
            		//System.out.println(rolename.toString());
            		
            	}

			}
            
           
            //關閉各項連結方法
            rs.close();
            stmt.close();
            conn.close();

	    }catch(SQLException e1){
	            e1.printStackTrace();  //用來拋出SQL錯誤訊息
	    }catch(ClassNotFoundException e2){
	            e2.printStackTrace();  //用來拋出Class如果沒有找的錯誤訊息
	    }
		
	}


	//清除特定帳號下之資料
	private void cleanTesterData(String email) {
		Member member = Member.findUser(email);
		if(member!=null){
			Logger.info("delete member id="+member.id+",email="+member.email);
			System.out.println("delete member id="+member.id+",email="+member.email);
			//刪除順序為: queue => answer=> historyNode(點名紀錄) => apply => OAincludeTP & OA & NFC => 
			//		   TP & cite & TPBefor& Now & After => Examine & Question => 八大元件 => MemberGroup => Member			
			//Queue
			List<models.Queue> listQueue = new ArrayList<models.Queue>();
			//被群組邀請加入
			List<models.Queue> queues = models.Queue.findGroupByGuestAndMember(member.id);			
			if(queues!=null)
				listQueue.addAll(queues);	
			//主動申請加入群組
			queues = models.Queue.findGroupByMasterAndMember(member.id);
			if(queues!=null)
				listQueue.addAll(queues);		
			//被邀請確認關係
			queues = models.Queue.findRelationByGuest(member.id);
			if(queues!=null)
				listQueue.addAll(queues);		
			//發出邀請確認關係
			queues = models.Queue.findRelationByMaster(member.id);
			if(queues!=null)
				listQueue.addAll(queues);
			
			for (models.Queue queue : listQueue) {
				queue.delete();
			}
			
			//Answer(先找出oa ,後面也會用到)[也要找出參與listOA之Apply的answer]
			List<OutdoorActivity> listOA = OutdoorActivity.listByCreator(member.id);
			List<Answer> listAnswer = Answer.findAnsByCreator(member.id);
			for (OutdoorActivity oa : listOA) {
				listAnswer.addAll(Answer.findAnswerByOa(oa.id));
			}
			for (Answer answer : listAnswer) {
				answer.delete();
			}
			
			//HistoryNode(點名紀錄) & Apply 
			List<Apply> listApply = new ArrayList<Apply>();
			listApply.addAll(Apply.findApplyByUser(member.id));
			for (Apply a : Apply.findApplyByAttendantMember(member.id)) {
				if(!listApply.contains(a))
					listApply.add(a);
			}
			for (OutdoorActivity oa : listOA) {
				listApply.addAll(Apply.findApplyByOa(oa.id));
			}
			
			
			for (Apply apply : listApply) {
				List<HistoryNode> nodes = new ArrayList<HistoryNode>();
				nodes.addAll(apply.nodes);
				apply.nodes.clear();
				for (HistoryNode node : nodes) {
					node.delete();
				}
				apply.delete();
			}
			
			
			//Favorite OA & TP 先清除 (加入別人的資料到自己的最愛)		
			if(member.favoriteOAs!=null)
				member.favoriteOAs.clear();
			if(member.favoriteTPs!=null)
				member.favoriteTPs.clear();
			member.save();
			
			//
			
			
			//OA & OAincludeTP && NFC set  & Favorite OA ( 自己的資料被別人加入最愛)
			for (OutdoorActivity oa : listOA) {
				List<OAIncludeTP> includes = new ArrayList<OAIncludeTP>();
				List<NFC> nfcs = new ArrayList<NFC>();
				List<Member> members = new ArrayList<Member>();
				nfcs.addAll(oa.nfc);
				includes.addAll(oa.tps);
				oa.nfc.clear();
				oa.tps.clear();
				oa.save();
				System.out.println("oaid="+oa.id);
				members.addAll(Member.findByFavoriteOA(oa.id));
				for (Member m : members) {
					m.favoriteOAs.remove(oa);
					m.save();
				}
				for (NFC nfc : nfcs) {
					nfc.delete();
				}
				for (OAIncludeTP node : includes) {
					node.delete();
				}
				oa.delete();
			}
			
			//TP & TP before & now & after && cite & Favorite TP ( 自己的資料被別人加入最愛)
			List<TeachingPlan> listTP = TeachingPlan.listByCreator(member.id);
			List<TeachingPlanBefore> listTPBefore = new ArrayList<TeachingPlanBefore>(); 
			List<TeachingPlanNow> listTPNow = new ArrayList<TeachingPlanNow>(); 
			List<TeachingPlanAfter> listTPAfter = new ArrayList<TeachingPlanAfter>(); 
			for (TeachingPlan tp : listTP) {
				
				List<Member> members = new ArrayList<Member>();
				members.addAll(Member.findByFavoriteTP(tp.id));
				for (Member m : members) {
					m.favoriteTPs.remove(tp);
					m.save();
				}
				
				//此教案被OA引用的cite 也需移除 (且取出OA orderNumber 中包含 cite,id 字串,移除掉)
				List<OAIncludeTP> includes = new ArrayList<OAIncludeTP>();
				includes.addAll(OAIncludeTP.findByTP(tp.id));
				for (OAIncludeTP oaIncludeTP : includes) {
					List<OutdoorActivity> OAs = OutdoorActivity.findByCiteTP(oaIncludeTP.id);
					for (OutdoorActivity oa : OAs) {
						oa.componentOrder = oa.componentOrder.replaceAll("cite,"+oaIncludeTP.id+"##", "");
						oa.tps.remove(oaIncludeTP);
						oa.save();
					}
					oaIncludeTP.delete();
				}
				listTPBefore.add(tp.tpBefore);
				listTPNow.add(tp.tpNow);
				listTPAfter.add(tp.tpAfter);
				
				List<Cite> cites = Cite.findByTP(tp.id);
				for (Cite cite : cites) {
					cite.delete();
				}
				tp.delete();
			}
			for (TeachingPlanBefore before : listTPBefore) {
				if(before!=null)
					before.delete();
			}
			for (TeachingPlanNow now : listTPNow) {
				if(now!=null)
					now.delete();
			}
			for (TeachingPlanAfter after : listTPAfter) {
				if(after!=null)
					after.delete();
			}
			
			//Examine && Question 
			List<Examine> listExamines = Examine.findExamineByCreator(member.id);
			for (Examine examine : listExamines) {
				List<Question> questions = new ArrayList<Question>();
				questions.addAll(examine.questions);
				examine.delete();
				for (Question question : questions) {
					question.delete();
				}
			}
			
			//七大元件(cite已刪除,暫時不刪除實體檔案)
			List<Attatch> attatchs = Attatch.findByCreator(member.id);
			List<Link> links = Link.findByCreator(member.id);
			List<Map> maps = Map.findByCreator(member.id);
			List<Photo> photos = Photo.findByCreator(member.id);
			List<Teacher> teachers = Teacher.findByCreator(member.id);
			List<models.Text> texts = models.Text.findByCreator(member.id);
			List<Youtube> youtubes = Youtube.findByCreator(member.id);
			for (Attatch attatch : attatchs) 
				attatch.delete();
			for (Link link : links) 
				link.delete();
			for (Map map : maps) 
				map.delete();
			for (Photo photo : photos) 
				photo.delete();
			for (Teacher teacher : teachers) 
				teacher.delete();
			for (models.Text text : texts) 
				text.delete();
			for (Youtube youtube : youtubes) 
				youtube.delete();
			
			
			//MemberGroup && MemberRelation  (and group in queue)
			
			List<MemberGroup> groups = new ArrayList<MemberGroup>();
			groups.addAll(MemberGroup.findByCreator(member.id));
			groups.addAll(MemberGroup.findByMember(member.id));
			for (MemberGroup group : groups) {
				List<models.Queue> groupQ = models.Queue.findGroupByGroupId(group.id);
				for (models.Queue queue : groupQ) {
					queue.delete();
				}
				group.delete();
			}
			
			List<Member> listRelation = Member.findRelations(member.id);
			for (Member m : listRelation) {
				m.relationMember.clear();
				m.save();
			}
			
			//Contact reviewer
			List<Contact> listContact = Contact.findByReviewer(member.id);
			for (Contact contact : listContact) {
				contact.reviewer=null;
				contact.save();
			}
			member.delete();
		}
		
	}

	//清除測試帳號(admin, teacher , parent ,student)所建立出來之所有資料
	private void cleanAllTesterData() {
		cleanTesterData("parent@test.com");
		cleanTesterData("student@test.com");
    	cleanTesterData("teacher@test.com");
    	cleanTesterData("admin@test.com");	
	}
	private void  createQuantaAccount() {
    	//預設quanta_admin account    	
    	String email = "quanta@gmail.com";
    	String pwd = "quanta";
    	String roleName = ROLE.Q_ADMIN;
    	if(null == Member.findUser(email)) {
    		Member member = new Member(email, pwd, roleName,true,"admin");
    		member.save();
    	}
    	//預設quanta_Volunteer account    	
    	email = "volunteer@gmail.com";
    	pwd = "volunteer";
    	roleName = ROLE.Q_VOLUNTEER;
    	if(null == Member.findUser(email)) {
    		Member member = new Member(email, pwd, roleName,true,"volunteer");
    		member.save();
    	}
    	//預設quanta_Showpiece account    	
    	email = "showpiece@gmail.com";
    	pwd = "showpiece";
    	String schoolName="廣達小學";
    	roleName = ROLE.Q_SHOWPIECE;
    	if(null == Member.findUser(email)) {
    		Member member = new Member(email, pwd, roleName,true,schoolName);
    		member.save();
    	}
	}
    
    private void createPlatformTester() {
    	//預設註冊四個腳色的帳號
    	String email = "parent@test.com";
    	String username = "parent";
    	String password = "parent";
    	String roleName = ROLE.PARENT;
    	if(Member.findUser(email)==null) {
    		Member u = new Member(email, password, username, roleName, false);
    		u.resume = new String[1];
    		u.save();    		
    	}
    	
    	email = "teacher@test.com";
    	username = "teacher";
    	password = "teacher";
    	roleName = ROLE.TEACHINGPLANNER;
    	if(Member.findUser(email)==null) {
    		Member u = new Member(email, password, username, roleName, false);
    		u.resume = new String[1];
    		u.save();    		
    	}    	

    	email  = "student@test.com";
    	username = "student";
    	password = "student";
    	roleName = ROLE.STUDENT;
    	if(Member.findUser(email)==null) {
    		Member u = new Member(email, password, username, roleName, false);
    		u.resume = new String[1];
    		u.save();    		
    	}
    	
    	email  = "admin@test.com";
    	username = "admin";
    	password = "admin";
    	roleName = ROLE.ADMIN;
    	if(Member.findUser(email)==null) {
    		Member u = new Member(email, password, username, roleName, false);
    		u.resume = new String[1];
    		u.save();    		
    	}
	}
    
    private void createQuantaShowpieceVersion() {
    	//預設Showpiece and show table 這兩張table的板號(也就是version這張table裡面一定要有這兩筆資料)
    	if(null == Q_ShowpieceVersion.findVersionByName("q_show")) {
    		String time = MyUtil.now();
    		Q_ShowpieceVersion version = new Q_ShowpieceVersion("q_show","1.0",time);
    		version.save();
    	} 
    	if(null == Q_ShowpieceVersion.findVersionByName("q_showpiece")) {
    		String time = MyUtil.now();
    		Q_ShowpieceVersion version = new Q_ShowpieceVersion("q_showpiece","1.0",time);
    		version.save();
    	} 
	}
    
    private void createTagActivity() {
    	//新增"找活動" "找教案" "找老師"的TagActivity
    	//活動 must be 1 , 老師 2 , 教案:3
    	TagActivity tagActivity = TagActivity.findByName("找活動");
    	if(tagActivity==null){
    		tagActivity = new TagActivity();
    		tagActivity.name="找活動";
    		tagActivity.save();
    	}
    	tagActivity = TagActivity.findByName("找老師");
    	if(tagActivity==null){
    		tagActivity = new TagActivity();
    		tagActivity.name="找老師";
    		tagActivity.save();
    	}
    	tagActivity = TagActivity.findByName("找教案");
    	if(tagActivity==null){
    		tagActivity = new TagActivity();
    		tagActivity.name="找教案";
    		tagActivity.save();
    	}
	}
    
    private void createSMTPConnection() {
    	String domain = Request.current.get().domain; 
    	if(!domain.equals("localhost"))
			try {
				MyUtil.mail_conn();
			} catch (MessagingException e) {
				Logger.info(e.toString());		
			}
	}

	// To raise priority for somebody 
	private void raisePriorityJob() {
		
		String [] adminList = { "kevingo75@gmail.com", "pasta0806@hotmail.com","pasta0806@gmail.com",
				"judy@cacet.org","linda@cacet.org","Maggie@cacet.org","ho-chinghui@global.t-bird.edu" ,"s59a63@gmail.com"};
		
		for(String mail : adminList) {
			Member user = Member.findUser(mail);
			if(null != user) {				
				if(!user.getRoleList().contains(ROLE.ADMIN))
					user.addRole(ROLE.ADMIN);
				//if(!user.getRoleList().contains(ROLE.TEACHINGPLANNER))
				//	user.addRole(ROLE.TEACHINGPLANNER);
			} else {
				user = new Member(mail, mail.split("@")[0], ROLE.ADMIN);				
			}
			
			user.save();			
		}
	}
}
