package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import org.apache.commons.lang.StringEscapeUtils;
import org.bouncycastle.jce.provider.JDKDSASigner.noneDSA;

import com.mysql.jdbc.PreparedStatement;

import play.db.jpa.Blob;
import play.db.jpa.JPA;
import play.db.jpa.Model;
import Utility.ROLE;

@Entity
public class Member extends Model {

	//用來控制帳號的開關
	public Boolean enable =true;
	
	
	public String email;
	public String username;
	public String password;
	public boolean isFacebookUser;
	
	public String birthday;
	public String phone;

	//大頭貼
	@OneToOne
	public Photo photo;
	//心情小語 (介紹)
	public String mood;
	
	public ArrayList roleList;

	public String sex;
	
	public String [] resume;
	public String education;
		
	public int visit = 0;	
	// 於DB中部存放任何資料，僅拿來做暫存資料 ，傳送至前端使用，跳選頁面後即消失
	public String tempData;
	
	//link 親子關係
	@ManyToMany
	public Set<Member> relationMember;
	
	public String schoolName;//學校名稱
	public String className; //班級名稱
	
	//老師上傳的審核資料
	@OneToOne
	public Attatch attatch;
	
	//老師收款銀行資料 (銀行代碼與帳號)
	public String bankCode;
	public String accountATM;
	
	public String tname;
	
	//老師的標籤
	@ManyToMany
	public Set<Tag> tags;
	
	
	//會員收藏的教案 與 戶外活動
	@ManyToMany
	public Set<TeachingPlan> favoriteTPs;
	@ManyToMany
	public Set<OutdoorActivity> favoriteOAs = new HashSet<OutdoorActivity>();
	
	
	//Quanta
	public boolean isQuantaUser; 
	//true:大人 , false:小孩
	public boolean isAdult; 
	
	//time
	public String createTime;
	
	
	public Member(String email, String username, String roleName) {
		this(email, "", username, roleName, true);
	}
	
	public Member (){
		this.roleList = new ArrayList();
		this.isFacebookUser=false;
		this.isQuantaUser=false;
	}
	
	//平台註冊成員
	public Member(String email, String password, String username, String roleName, boolean isFacebook) {
		this.email = email;
		this.username = username;
		this.sex = "";
		this.education = "尚未填寫";
		this.password = password;		
		this.isFacebookUser = isFacebook;	
		this.roleList = new ArrayList();
		visit = 1;		
		if(roleName==null) roleName = ROLE.PARENT;
		
		roleList.add(roleName);
		create();		
	}

	
	//廣達  admin,志工  ,游於藝 (帳號給學校)
	public Member(String email, String password , String roleName, boolean isQuantaUser,String schoolOrUserName)  {
		this.email = email;
		this.password = password;	
		if(roleName==ROLE.Q_VOLUNTEER)
			this.username = schoolOrUserName;
		else if(roleName==ROLE.Q_SHOWPIECE)
			this.schoolName=schoolOrUserName;
		else if(roleName==ROLE.Q_ADMIN)
			this.username = schoolOrUserName;
		else if(roleName==ROLE.Q_TEACHER)
			this.username=schoolOrUserName;
		this.isFacebookUser = false;	
		this.isQuantaUser=isQuantaUser;
		this.isAdult=true;
		this.roleList = new ArrayList();
		roleList.add(roleName);
		visit = 1;
		create();	
	}

	public static Member findUser(String email) {
		Query q = JPA.em().createNativeQuery("select * from Member m where m.email='" + email + "'", Member.class);
//		Query q = JPA.em().createQuery("FROM Member WHERE email=?1", Member.class).setParameter(1, email);		
		//Query q = JPA.em().createNativeQuery("select * from Member m where m.email= ?", Member.class);
		//List<Member> list = q.setParameter(1, email).getResultList();
		Member m = null;
		if(q.getResultList().size()>0)
			m = (Member) q.getResultList().get(0);			
		return m;  
	}
	
	public static Member findById(Long id) {
		Query q = JPA.em().createNativeQuery("select * from Member m where  m.id=" +id , Member.class);
		Member m = null;
		if(q.getResultList().size()>0)
			m = (Member) q.getResultList().get(0);			
		return m; 
	}
	
	public static Member findByIdAndDisable(Long id) {
		Query q = JPA.em().createNativeQuery("select * from Member m where m.enable = false and m.id=" +id , Member.class);
		Member m = null;
		if(q.getResultList().size()>0)
			m = (Member) q.getResultList().get(0);			
		return m; 
	}
	
	public static Member findUserByName(String username) {
		Query q = JPA.em().createNativeQuery("select * from Member m where m.username='" + username + "'", Member.class);
		Member m = null;
		if(q.getResultList().size()>0)
			m = (Member) q.getResultList().get(0);			
		return m; 
	}
	
	public static boolean addToFavorite(String type, Long userId, Long id) {
		
		boolean added = false;
		
		if(type.equals("tp")) {
			Query q = JPA.em().createNativeQuery("select * from Member_TeachingPlan m where m.member_id=" + userId + " and m.favoriteTPs_id=" + id);
			if(q.getResultList().size() != 0)
				added = true;
		} 
		
		if(type.equals("oa")) {
			Query q = JPA.em().createNativeQuery("select * from Member_OutdoorActivity m where m.member_id=" + userId + " and m.favoriteOAs_id=" + id);
			if(q.getResultList().size() != 0)
				added = true;
		}
			
		return added; 
	}
	
	public void addRole(String roleName) {
		roleList.add(roleName);
	}
	
	public void removeRole(String roleName) {
		try {
			roleList.remove(roleName);
		} catch(Exception e) {			
			System.out.println(roleName + " not exist! " + e.toString());
		}
	}
	
	public ArrayList getRoleList() {
		return roleList;
	}
	
	public boolean checkRole(String roleName) {
		return roleList.contains(roleName);
	}
	
	public static Member connect(String email, String password) {
		return find("byEmailAndPassword", email, password).first();
	}
	
	public static Member login(String email, String password) {
		Member member = Member.findUser(email);
		
		if(null==member)
			return null;
		else {
			if(member.password.equals(password)) {
				return member;
			} else {
				return null;
			}
		}
	}
	
	//找 是否是廣達之會員
	public static List<Member> findMemberByisQuantaUser(Boolean isQuantaUser){
		Query q = JPA.em().createNativeQuery("select * from Member m where m.isQuantaUser=" + isQuantaUser, Member.class);
		List<Member> m = null;
		if(q.getResultList().size()>0)
			m = q.getResultList();			
		return m; 
	}
	
	public static List<Member> findMemberByRole(String roleName) {
		
		//先取得全部member，再用orm來判斷role
		Query q = JPA.em().createNativeQuery("select * from Member m where m.enable=1", Member.class);		
		List<Member> m = q.getResultList();
		List<Member> members = new ArrayList<Member>();
		for (Member member : m) {
			if(member.roleList!=null && member.roleList.contains(roleName)){
				members.add(member);
			}
		}
		return members; 
	}
	
	public static List<Member> findMemberByViewCount(int num) {
		String query = "";
		if(num == -1)
			query = "select * from Member m order by visit DESC";
		else if(num>0)
			query = "SELECT * from Member m order by visit DESC limit " + num;
		else
			query = "select * from Member m order by visit DESC";
		List<Member> m = new ArrayList<Member>();
		Query q = JPA.em().createNativeQuery(query , Member.class);
		m = q.getResultList();
		return m;
	}
	
	public static List<Member> findMemberByRoleByAdmin(String roleName) {
		
		//先取得全部member，再用orm來判斷role
		Query q = JPA.em().createNativeQuery("select * from Member m", Member.class);		
		List<Member> m = q.getResultList();
		List<Member> members = new ArrayList<Member>();
		for (Member member : m) {
			if(member.roleList!=null && member.roleList.contains(roleName)){
				members.add(member);
			}
		}
		return members; 
	}

	public static List<Member> findByKeyword(String keyword) {
		keyword = StringEscapeUtils.escapeSql(keyword);
		Query q = JPA.em().createNativeQuery("select * from Member m where m.username like '%" + keyword+ "%'", Member.class);
		List<Member> m = new ArrayList<Member>();
		if(q.getResultList().size()>0)
			m = (List<Member>) q.getResultList();		
		return m; 
	}

	public static List<Member> findUncheckTeacher() {
		Query q = JPA.em().createNativeQuery("select * from Member m where m.attatch_id is not null", Member.class);
		List<Member> hasAttatchMember = (List<Member>) q.getResultList();
		List<Member> listUncheckMember = new ArrayList<Member>();
		
		if(hasAttatchMember!=null){
			for (Member member : hasAttatchMember) {
				if(!member.roleList.contains(ROLE.TEACHINGPLANNER))
					listUncheckMember.add(member);
			}
		}
		return listUncheckMember;
	}

	public static List<Member> findRelations(Long userId) {
		List<Member> relations = new ArrayList<Member>();
		Query q = JPA.em().createNativeQuery("select * from Member m where m.id IN (select relationMember_id from Member_Member where Member_id="+ userId +")", Member.class);
		relations = q.getResultList();
		return relations;
	}
	
	public static List<Member> findTeacherByChildren(List<Member> my_children) {
		List<Member> teachers = new ArrayList<Member>();
		for(int i=0 ; i<my_children.size() ; i++) {
			Member child = my_children.get(i);
			List<MemberGroup> groups = MemberGroup.findByMember(child.id);
			for(int j=0 ; j<groups.size() ; j++) {
				MemberGroup group = groups.get(j);
				teachers.add(group.creator);
			}
		}
		
		return teachers;
	}
	 
	public static List<Member> findMemberByKeyword(String schoolName,String fullCassName, String email) {
		if(schoolName==null||schoolName.equals("")){
			schoolName="'%%'";
		}	
		else
			schoolName="'%"+schoolName+"%'";
		
		if(fullCassName==null||fullCassName.equals(""))
			fullCassName="'%%'";
		else
			fullCassName="'%"+fullCassName+"%'";
		
		if(email==null||email.equals(""))
			email="'%%'";
		else
			email="'%"+email+"%'";
		
		schoolName = StringEscapeUtils.escapeSql(schoolName);
		fullCassName = StringEscapeUtils.escapeSql(fullCassName);
		email = StringEscapeUtils.escapeSql(email);
		List<Member> members = null;
		Query q = JPA.em().createNativeQuery("select * from Member m where (m.schoolName like " + schoolName + " and m.className like "+ fullCassName+ " and m.email like "+email+" )", Member.class);
		
		if(q.getResultList().size()>0)
			members = q.getResultList();
		return members;
	}

	public static List<Member> findByTag(Tag tag) {
		List<Member> teachers = new ArrayList<Member>();
		List<Member> members = Member.findAll();
		for (Member member : members) {
			if(member.tags.contains(tag) && !teachers.contains(member))
				teachers.add(member);
		}
		return teachers;
	}

	public static List<Member> findByFavoriteOA(Long oaId) {
		List<Member> AllMember = Member.findAll();
		List<Member> members = new ArrayList<Member>();
		OutdoorActivity oa = OutdoorActivity.findById(oaId);
		if(oa!=null){
			for (Member member : AllMember) {
				if(member.favoriteOAs.contains(oa))
					members.add(member);
			}
		}
		return members;	
	}
	
	public static List<Member> findByFavoriteTP(Long tpId) {
		List<Member> AllMember = Member.findAll();
		List<Member> members = new ArrayList<Member>();
		TeachingPlan tp = TeachingPlan.findById(tpId);
		if(tp!=null){
			for (Member member : AllMember) {
				if(member.favoriteTPs.contains(tp))
					members.add(member);
			}
		}
		return members;	
	}

	
}
