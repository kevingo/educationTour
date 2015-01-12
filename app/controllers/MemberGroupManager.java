package controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.management.Query;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;

import Utility.MyUtil;
import Utility.ROLE;
import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.Restrict;


import models.MemberGroup;
import models.Member;
import models.Queue;
import play.Play;
import play.data.Upload;
import play.data.validation.Required;
import play.i18n.Lang;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

@With(Deadbolt.class)
public class MemberGroupManager extends Controller {
	
	@Before
	public static void getTag() {
		Application.getTag();	   
	}
	
	public static void show(@Required Long id){
		Member member = MyUtil.getCurrentUser();
		MemberGroup group = MemberGroup.findById(id);
		if(group!=null){
			render("Application/class_detail.html",group,member);
		}
	}
	
	//新建
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void addGroup(@Required String GroupName){
		
		Member member = MyUtil.getCurrentUser();
		
		MemberGroup group =MemberGroup.findByNameAndCreator(GroupName,member.id);
		if(group==null){
			group = new MemberGroup();
			//名稱
			group.groupName=GroupName;
			//建立者
			group.creator = member;
			//成員  (先清空 在全部重新加入)
			group.members.clear();		
			group.save();
			renderText("建立完成!");
		}
		else {
			renderText("此名稱已存在，若需變更，請使用修改名稱!");
		}
		
	}
	
	//編輯名稱
	public static void editGroupName(@Required Long group_id , @Required String GroupName) {
		GroupName = StringEscapeUtils.escapeSql(GroupName);
		System.out.println(GroupName);
		//判斷是否此名稱已重複
		Member member = MyUtil.getCurrentUser();
		
		MemberGroup IsExist =MemberGroup.findByNameAndCreator(GroupName,member.id);
		if(IsExist==null){
			MemberGroup group = MemberGroup.findById(group_id);
			if(group!=null) {
				group.groupName=GroupName;
				group.save();
				renderText("變更完成!");
			}
			else {
				renderText("變更失敗!");
			}
		}
		else {
			renderText("此名稱已存在，若使用其他班群名稱!");
		}
		
	}
	
	//邀請 搜尋出來且勾選的Member 加入群組

	public static void inviteMemberJoinGroup(@Required Long group_id , @Required String selectedId){
		MemberGroup group = MemberGroup.findById(group_id);
		String message="邀請成功。";
		if(group!=null) {
			Pattern p = Pattern.compile("[0-9]+");
			String[] pid = selectedId.split(",");
			for (String id : pid) {
				Matcher m = p.matcher(id);
				if (id != null && m.find()) {
					Member member = Member.findById(Long.parseLong(id));
					if (member != null) {
						Queue queue = Queue.findQueueByGroup(group.id, member.id);
						if(null == queue) {
							queue = new Queue();
							queue.group=group;
							queue.guest=member;							
							queue.time=MyUtil.now();
							queue.save();
						}
					} else
						message = "邀請失敗，並未全部邀請成功。";
				}
			}	
		}
		else {
			message="無此群組。";
		}
		renderText(message);
	}
	
	public static void MemberJoinGroup(@Required Long group_id){
		Member member =MyUtil.getCurrentUser();
		MemberGroup group = MemberGroup.findById(group_id);
		String message="申請成功，等候老師確認。";
		if(group!=null && member!=null && member.roleList.contains(ROLE.STUDENT)) {
			if(!group.members.contains(member)){
				Queue queue = Queue.findQueueByGroup(group.id, member.id);
				if(null == queue) {
					queue = new Queue();
					queue.group=group;
					queue.master=member;							
					queue.time=MyUtil.now();
					queue.save();
				}else {
					message="申請失敗，已經申請過了。";
				}
			}
			else {
				message="申請失敗，已經加入了。";
			}
			
		}
		else {
			message="申請失敗，帳號權限不正確。";
		}
		renderText(message);
	}
	
	public static void acceptGroupInvite(@Required Long memberId, @Required String groupName){
		
		Member creator = MyUtil.getCurrentUser();
		Member member = Member.findById(memberId);
		
		MemberGroup group = MemberGroup.findByNameAndCreator(groupName, creator.id);
		if(creator!=null && member!=null  && group!=null){
			Queue queue = Queue.findQueueByGroup(group.id, member.id);
			if(queue!=null){
				if(!group.members.contains(member))
					group.members.add(member);
				group.save();
				queue.delete();
				renderText("已接受該學生之申請加入。");
			}
			else
				renderText("發生錯誤，請重新點選。");
		}
		else
			renderText("發生錯誤，請重新點選。");
	}
	
	public static void deleteMemberFromGroup(@Required Long groupId , @Required String selectedGroupMembersID){
		
		MemberGroup group = MemberGroup.findById(groupId);
		String message = "已成功刪除。";
		if( group!=null){
			Pattern p = Pattern.compile("[0-9]+");
			String[] membersId = selectedGroupMembersID.split(",");
			for (String id : membersId) {
				Matcher m = p.matcher(id);
				if (id != null && m.find()) {
					Member member = Member.findById(Long.parseLong(id));
					if (member != null) {
						//member in group
						if(group.members.contains(member)){
							group.members.remove(member);
							group.save();
						}
						else {
							//member in queue
							Queue queue = Queue.findQueueByGroup(group.id, member.id);
							if(queue!=null)
								queue.delete();
						}
					} 
				}
			}
			
		}
		else
			message = "刪除失敗，無此群組。";
		renderText(message);
	}
	
	public static void deleteGroupByCreator(@Required Long groupId){
		MemberGroup group = MemberGroup.findById(groupId);
		String message = "已成功刪除。";
		if (group != null) {
			//先找出Queue 中的 gorup
			List<Queue> listQueues = Queue.findGroupByGroupId(groupId);
			for (Queue queue : listQueues) {
				queue.delete();
			}
			//刪除group
			group.delete();
		}
		else
			message = "刪除失敗，無此群組。";
		renderText(message);
	}
	
	
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void gotoSendMail(@Required Long gid) {
		Member member = MyUtil.getCurrentUser();
		MemberGroup group = MemberGroup.findById(gid);
		if(member!=null &&member.roleList.contains(ROLE.TEACHINGPLANNER)){
			if(group!=null && group.creator==member){
				render("management/teacher_manage_sendmail.html",gid);
			}
			else {
				render("management/teacher_manage_sendmail.html");
			}
		}
		else {
			Application.index("權限不足");
		}
	}
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void sendMail(@Required String[] rec_mail , @Required String title , @Required String mail_content ,@Required String Parents) throws AddressException, MessagingException{
		
		for (String mail : rec_mail) {
			if(Parents!=null && Parents.equals("1")){
				//同時寄送給家長
				Member member = Member.findUser(mail);
				if(member!=null){
					for (Member parent : member.relationMember) {
						MyUtil.sendMail(parent.email, title, mail_content);
					}
				}
			}		
			MyUtil.sendMail(mail, title, mail_content);
		}	
		MemberManager.myHomePage();
	}
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void uploadRelationFile(@Required Long groupId , @Required String subfile){

			
			String msg = "";
			Member member = MyUtil.getCurrentUser();
			InputStream isFile = MyUtil.findInputStreamFromUpload(subfile);
		
			String path = "/public/temp/"+ subfile;
			File des = new File(Play.getFile("").getAbsolutePath() + "/public/temp/");				
			File output = new File(Play.getFile("").getAbsolutePath() + path);
			if(!des.exists())
				des.mkdirs();
			if (isFile!= null) {	
				try {
					IOUtils.copy(isFile, new FileOutputStream(output));
					isFile.close();						
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//parse file to MySQL
				InputStreamReader fr = null;
		        BufferedReader br = null;
		        try {
		            fr = new InputStreamReader(new FileInputStream(Play.getFile("").getAbsolutePath() + path));
		            br = new BufferedReader(fr);
		            String rec = null;
		            String[] argsArr = null;
		            while ((rec = br.readLine()) != null) {
		                argsArr = rec.split(",");
		                String childEmail = argsArr[0];
		                String parentEmail = argsArr[1];
		                Member parent = Member.findUser(parentEmail);
		                Member child = Member.findUser(childEmail);
		                System.out.println(childEmail+","+parentEmail);
		                if(parent!=null && child!=null){
		                	//配對關係
		                	if(!parent.relationMember.contains(child))
		                		parent.relationMember.add(child);
		                	if(!child.relationMember.contains(parent))
		                		child.relationMember.add(parent);
		                	parent.save();
		                	child.save();
		                	
		                	//邀請將學生加入班群
		                	MemberGroup group = MemberGroup.findById(groupId);
		                	if(group!=null && group.creator.equals(member)){
			                	Queue queue = Queue.findQueueByGroup(group.id, child.id);
								if(null == queue) {
									queue = new Queue();
									queue.group=group;
									queue.guest=child;							
									queue.time=MyUtil.now();
									queue.save();
								}
		                	}
		                	else
		                		msg+=childEmail+" 加入群組失敗。\n";
		                }
		                else {
							msg += childEmail+"與"+parentEmail+"配對失敗\n";
						}
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		        }finally{
		            try{
		                if ( fr != null )
		                    fr.close();
		                if ( br != null )
		                    br.close();    	
		            }catch(IOException ex){
		                ex.printStackTrace();
		            }
		        }
			}
		
		
			//刪除temp資料夾下的檔案(當下該檔案不會被刪除，待下次資源被釋放時才會被刪除)
			
			String[] list = des.list();
			for (String filepath : list) {
				File f = new File(Play.getFile("").getAbsolutePath() + "/public/temp/"+filepath);
				f.delete();
			}
			//return
			if(msg.equals("")){
            	msg="配對完成";
            }
			MemberManager.configure(msg);
		}

	
	
	@Restrict(ROLE.TEACHINGPLANNER)
	public static void activeChange(@Required Long id,@Required boolean active){
		MemberGroup group = MemberGroup.findById(id);
		group.active=active;
		group.save();
	}
	
	
	

}
