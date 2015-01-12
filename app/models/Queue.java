package models;

import java.sql.Time;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;


import Utility.MyUtil;
import Utility.ROLE;

import play.db.jpa.JPA;
import play.db.jpa.Model;

//用來儲存待確認之邀請  親子關係、班群

@Entity
public class Queue extends Model {
	
	//發出邀請者
	@OneToOne
	public Member master;
	
	//被邀請者
	@OneToOne
	public Member guest;
	
	//申請加入之班群
	@OneToOne
	public MemberGroup group;
	
	public String time;
	

	//找出尚未被接受之邀請  (親子關係配對)
	public static List<Queue> findRelationByMaster(Long master) {
		Query q = JPA.em().createNativeQuery("select * from Queue q where q.group_id is null and q.master_id=" + master, Queue.class);
		
		List<Queue> result = null;
		result =  (List<Queue>)q.getResultList();
		return result;
	}
	
	//找出尚未接受之邀請 (親子關係配對)
	public static List<Queue> findRelationByGuest(Long guest) {
		Query q = JPA.em().createNativeQuery("select * from Queue q where q.group_id is null and q.guest_id=" + guest, Queue.class);
		
		List<Queue> result = null;
		if(q.getResultList().size()>0)
			result =  (List<Queue>)q.getResultList();
		return result;
	}
	
	//找出尚未被接受之邀請 (班群) [發出邀請者為group,被邀請者為member] (以Group的角度找出)
	public static List<Queue> findGroupByGuestAndGroup(Long group) {
		Query q = JPA.em().createNativeQuery("select * from Queue q where q.master_id is null and q.group_id=" + group, Queue.class);
		
		List<Queue> result = null;
		result =  (List<Queue>)q.getResultList();
		return result;
	}
	
	//找出尚未接受之邀請 (班群) [發出邀請者為member,被邀請者為group] (以Group的角度找出)
	public static List<Queue> findGroupByMasterAndGroup(Long group) {
		Query q = JPA.em().createNativeQuery("select * from Queue q where q.guest_id is null and q.group_id=" + group, Queue.class);
		
		List<Queue> result = null;
		result =  (List<Queue>)q.getResultList();
		return result;
	}
	
	//找出尚未被接受之邀請 (班群) [發出邀請者為Master,被邀請者為group] (以Member的角度找出)
	public static List<Queue> findGroupByMasterAndMember(Long memberId){
		Query q = JPA.em().createNativeQuery("select * from Queue q where q.guest_id is null and q.master_id=" + memberId, Queue.class);
		
		List<Queue> result = null;
		result =  (List<Queue>)q.getResultList();
		return result;
	}
	
	///找出尚未接受之邀請 (班群) [發出邀請者為group,被邀請者為member](以Member的角度找出)
		public static List<Queue> findGroupByGuestAndMember(Long memberId) {
			Query q = JPA.em().createNativeQuery("select * from Queue q where q.master_id is null and q.guest_id=" + memberId, Queue.class);
			
			List<Queue> result = null;
			result =  (List<Queue>)q.getResultList();
			return result;
		}

	public static Queue findQueueByRelation(Long master_id, Long guest_id) {
		Query q = JPA.em().createNativeQuery("select * from Queue q where q.group_id is null and q.master_id=" + master_id+" and q.guest_id="+guest_id, Queue.class);
		
		Queue result = null;
		if(q.getResultList().size()>0)
			result =  (Queue) q.getResultList().get(0);
		return result;
	}

	public static Queue findQueueByGroup(Long group_id, Long member_id) {
		Query q = JPA.em().createNativeQuery("select * from Queue q where q.group_id=" + group_id+" and (q.guest_id="+member_id+" or q.master_id="+member_id+")", Queue.class);
		
		Queue result = null;
		if(q.getResultList().size()>0)
			result =  (Queue) q.getResultList().get(0);
		return result;
	}

	public static List<Queue> findGroupByGroupId(Long groupId) {
		Query q = JPA.em().createNativeQuery("select * from Queue q where q.group_id=" + groupId, Queue.class);
		
		List<Queue> result = null;
		result =  (List<Queue>)q.getResultList();
		return result;
	}
	
	public static boolean hasUnReplyRelationMember(Long guestId) {
		boolean hasUnReply = false;
		Query q = JPA.em().createNativeQuery("SELECT * from Queue q where q.guest_id=" + guestId, Queue.class);
		if(q.getResultList() != null && q.getResultList().size()>0)
			hasUnReply = true;
		return hasUnReply;
	}
	
	
}
