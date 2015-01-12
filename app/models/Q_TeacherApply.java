package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import Utility.ROLE;

import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;

@Entity
public class Q_TeacherApply extends Model {
	
	public static final int NoFood=0;
	public static final int Meat=1;
	public static final int Meatless = 2; 
	
	@OneToOne
	@Required
	public Q_TeacherSeesion session; // 場次(廣達)
	
	//所屬縣市
	public String belongArea;
	
	//單位名稱
	public String departmentName;
	
	//姓名
	public String username;
	
	//身分證字號
	public String userId;
	
	//電話
	public String phone;
	//email
	public String email;
	//餐點(葷素無)
	public int food;
	
	//唯一識別碼
	public String serialNumber;
	
	public String checkinTime;
	public String checkoutTime;
	public String totalTime;

	
	
	public Q_TeacherApply(String userId, String username, String phone,String email, int food) {
		this.userId=userId;
		this.username=username;
		this.phone=phone;
		this.email=email;
		this.food=food;
		create();		
	}

	public static List<Q_TeacherApply> findApplyByUser(String userId) {
		Query q = JPA.em().createNativeQuery("select * from Q_TeacherApply a where a.userId='" + userId + "'", Q_TeacherApply.class);
		return (List<Q_TeacherApply>)q.getResultList();
	}
	
	public static List<Q_TeacherApply> findApplyBySession(Long session_id) {
		Query q = JPA.em().createNativeQuery("select * from Q_TeacherApply a where a.session_id='" + session_id + "'", Q_TeacherApply.class);
		return (List<Q_TeacherApply>)q.getResultList();
	}
	
	public static Q_TeacherApply findApplyBySessionAndUser(Long session_id,String userId) {
		Query q = JPA.em().createNativeQuery("select * from Q_TeacherApply a where a.session_id='" + session_id + "' and a.userId='"+userId+"'", Q_TeacherApply.class);
		Q_TeacherApply a = null;
		if(q.getResultList().size()>0)
			a = (Q_TeacherApply) q.getResultList().get(0);			
		return a; 
	}
	
	public static Q_TeacherApply findApplyByDateAndUser(String date,String userId) {
		Query q = JPA.em().createNativeQuery("select * from Q_TeacherApply a where  a.userId='"+userId+"'", Q_TeacherApply.class);
		List<Q_TeacherApply> listApply = null;
		Q_TeacherApply a = null;
		if(q.getResultList().size()>0)
			listApply = (List<Q_TeacherApply>)q.getResultList();	
		for (Q_TeacherApply apply : listApply) {
			if(apply.session.date.equals(date)){
				a =apply;
				break;
			}	
		}
		return a; 
	}

	public static boolean IsExistBySerialNumber(String serialNumber) {
		Query q = JPA.em().createNativeQuery("select * from Q_TeacherApply a where a.serialNumber='" + serialNumber + "'", Q_TeacherApply.class);
		
		if(q.getResultList().size()>0)
			return true;
		else			
			return false;
	}
	
	//計算總人數(實到)
	public static int countArrivedApplyBySession(Long session_id){
		Query q = JPA.em().createNativeQuery("select * from Q_TeacherApply a where a.checkinTime is not null and a.session_id=" + session_id , Q_TeacherApply.class);
		int count=0;
		if(q.getResultList().size()>0)
			count =q.getResultList().size();
		return count;
	}
	
	//計算葷食(素食)總人數(應到)
	public static int countMeatApplyBySession(Long session_id , int food){
		Query q = JPA.em().createNativeQuery("select * from Q_TeacherApply a where  a.session_id='" + session_id + "' and a.food="+food, Q_TeacherApply.class);
		int count=0;
		if(q.getResultList().size()>0)
			count =q.getResultList().size();
		return count;
	}
	
	//計算葷食(素食)總人數(實到)
	public static int countMeatArrivedApplyBySession(Long session_id, int food){
		Query q = JPA.em().createNativeQuery("select * from Q_TeacherApply a where a.checkinTime is not null and  a.session_id='" + session_id + "' and a.food="+food, Q_TeacherApply.class);
		int count=0;
		if(q.getResultList().size()>0)
			count =q.getResultList().size();
		return count;
	}
	
	
}