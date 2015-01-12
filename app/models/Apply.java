package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

import org.apache.commons.lang.StringEscapeUtils;

import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;

@Entity
public class Apply extends Model {
	
	public static final int Child=0;
	public static final int Adult=1;
	
	//status
	public static final int UnConfirm=0;// 老師邀請學生，學生家長尚未同意
	public static final int Unpay=1;
	public static final int Validate=2;
	public static final int Payed=3;
	public static final int Reject=4; //被拒絕參加
	

	// 戶外活動
	@OneToOne
	@Required
	public OutdoorActivity oa; 
	
	// 報名者
	@OneToOne
	@Required
	public Member regMember; 
	
	// 被報名者(參加活動的小朋友)
	@OneToOne
	public Member attendantMember; 
	
	// 身分證字號
	@Required
	public String userid;
	
	//電話(若member中有電話資料則從member中帶出)
	@Required
	public String phone;
	
	// 報名的姓名(手動輸入)
	@Required
	public String username; 	 
	// 出生年月日
	@Required
	public String birth; 
	
	//狀態
	public int status = Unpay; //是否已經付費的狀態  預設"未付費"  
	
	//匯款帳戶
	public String ATMaccount;  

	// 報名時間
	public String applyTime;

	
	//點名紀錄
	@OneToMany
	public Set<HistoryNode> nodes;
	
	public static Apply findApplyById(Long id)
	{
		Query query = JPA.em().createNativeQuery("select * from Apply a where a.id=" + id, Apply.class);
		Apply apply = null;
		if(query.getResultList().size()>0)
			apply=(Apply)query.getResultList().get(0);
		return apply;
	}

	public static List<Apply> findMyApply(Long oaid, Long uid) {
		Query q = JPA.em().createNativeQuery("select * from Apply a where a.oa_id=" + oaid + " and a.regMember_id=" + uid, Apply.class);		
		return (List<Apply>)q.getResultList();
	}
	
	public static Apply findApplyByOAandAttendMember(Long oaid, Long userId) {
		Query q = JPA.em().createNativeQuery("select * from Apply a where a.oa_id=" + oaid + " and a.attendantMember_id=" + userId, Apply.class);
		Apply apply = null;
		if(q.getResultList().size()>0)
			apply=(Apply)q.getResultList().get(0);
		return apply;
	}
	
	public static List<Apply> findApplyByOaidAndKwOnUserName(Long oaid, String kw) {
		kw = StringEscapeUtils.escapeSql(kw);
		List<Apply> applies = new ArrayList<Apply>();
		Query q = JPA.em().createNativeQuery("select * from Apply a where a.oa_id=" + oaid + " and a.username like '%" + kw+ "%'", Apply.class);
		applies = q.getResultList();
		return applies;
	}

	public static List<Apply> findApplyByOa(Long oaid) {
		Query q = JPA.em().createNativeQuery("select * from Apply a where a.oa_id=" + oaid, Apply.class);
		return (List<Apply>)q.getResultList();
	}
	
	public static int countApplyByOa(Long oaid) {
		
		Query q = JPA.em().createNativeQuery("select * from Apply a where a.oa_id=" + oaid, Apply.class);
		List<Apply> listApply = (List<Apply>)q.getResultList();
		if(listApply!=null)
			return listApply.size();
		else
			return 0; 
	}

	public static List<Apply> findApplyGroupByUser(Long id) {
		Query q = JPA.em().createNativeQuery("select * from Apply a where a.regMember_id=" + id + " group by oaid", Apply.class);
		return (List<Apply>)q.getResultList();
	}
	
	public static List<Apply> findApplyByUser(Long id) {
		Query q = JPA.em().createNativeQuery("select * from Apply a where a.regMember_id=" + id, Apply.class);
		return (List<Apply>)q.getResultList();
	}
	
	public static List<Apply> findApplyByAttendantMember(Long id) {
		Query q = JPA.em().createNativeQuery("select * from Apply a where a.attendantMember_id=" + id, Apply.class);
		return (List<Apply>)q.getResultList();
	}
	
	public static List<Apply> findApplyByOAByStatus(Long oaid, int status){
		Query q = JPA.em().createNativeQuery("select * from Apply a where a.oa_id=" + oaid + " and a.status="+status , Apply.class);
		return (List<Apply>)q.getResultList();
	}

	// check apply by attendant id and userid(身分證字號)
	public static boolean notApply(Long oaid, Long attendant_id, String userId) {		
		boolean notapply = true;
		String sql = "select * from Apply a where a.oa_id=" + oaid + " and a.attendantMember_id=" + attendant_id + " and a.userid='" + userId + "'";
		Query q = JPA.em().createNativeQuery(sql , Apply.class);
		if(q.getResultList().size()>0) {
			notapply = false;
		}
		return notapply;
	}

//	public static List<Apply> findApplyByOaAndRegMember(Long oaid,
//			Long member_id) {
//		List<Apply> applies = new ArrayList<Apply>();
//		Query q = JPA.em().createNativeQuery("select * from Apply a where a.oa_id=" + oaid + " and a.regMember_id=" + member_id, Apply.class);
//		applies = q.getResultList();
//		return applies;
//	}

	public static String getATMbyOaAndMember(Long oaid, Long regMember_id) {
		Query q = JPA.em().createNativeQuery("select ATMaccount from Apply a where a.oa_id=" + oaid + " and a.regMember_id=" + regMember_id);
		if(q.getResultList().size()>0)
			return q.getResultList().get(0).toString();
		else
			return "尚未輸入"; 
	}

	public static void batchDelete(List<Apply> listApply){
		for (Apply apply : listApply) {
			JPA.em().remove(JPA.em().merge(apply));
		}
	}
}
