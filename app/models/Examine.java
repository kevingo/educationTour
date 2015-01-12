package models;



import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import org.apache.commons.lang.StringEscapeUtils;

import Utility.ROLE;
import play.Logger;
import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;

@Entity
public class Examine extends Model {
	
	//評量狀態: 未開始、進行中尚未結束已結束、已結束 
	public static final int unstart=0;
	public static final int progress=1;
	public static final int finished=2;
	
	//建立者
	@ManyToOne
	public Member creator;
	
	//評量名稱
	public String title;
	
	//公開/不公開
	public boolean publish;
	
	//狀態
	public int status;
	
	//開始作答日期
	public String startDate; 

	//結束作答日期
	public String endDate;
	
	//開始作答時間
	public String startTime; 

	//結束作答時間
	public String endTime;
	
	//評量介紹
	@Lob
	public String introduction;
	
	//作答前，答題說明
	@Lob
	public String questionExplain;
	
	//作答後，評後說明
	@Lob
	public String illustration;
	
	//題目 & 順序(3##5##1##...etc)
	@ManyToMany
	public Set<Question> questions;
	public String orderNumber="";
	
	/*
	public Examine(){
		this.questions = (Set<Question>) new LinkedHashMap();
	}
*/
	public static List<Examine> findExamineByKeywordAndOA(String keyword,Long oaid) {
		keyword = StringEscapeUtils.escapeSql(keyword);
		Query q = JPA.em().createNativeQuery("select * from Apply a where a.oa_id=" + oaid + "and  title like '%"+ keyword + "%'", Examine.class);
		
		if(oaid==0){
			q =JPA.em().createNativeQuery("select * from Apply a where title like '%"+ keyword + "%'", Examine.class);
		}
		
		List<Examine> listExame = null;
		listExame = q.getResultList();
		return listExame;
	}
	
	public static List<Examine> findExamineByOa(Long oaid, String role) {
		String query = "";
		if(role.equals(ROLE.STUDENT))
			query = "select * from Examine e where e.publish=1 AND e.id IN " +
					"(select examines_id from OutdoorActivity_Examine oe where oe.OutdoorActivity_id="+ oaid +")";
		else
			query = "select * from Examine e where e.id IN " +
					"(select examines_id from OutdoorActivity_Examine oe where oe.OutdoorActivity_id="+ oaid +")";
		Query q = JPA.em().createNativeQuery(query, Examine.class);
		return q.getResultList();
	}

	public static List<Examine> findExamineByOaidAndKwOnExamineName(Long oaid, String kw) {
		kw = StringEscapeUtils.escapeSql(kw);
		List<Examine> examine = new ArrayList<Examine>();
		Query q = JPA.em().createNativeQuery("select * from Examine e where e.id IN (select examines_id from OutdoorActivity_Examine oe where oe.OutdoorActivity_id="+oaid+")" +  
				" and e.title like '%" + kw+ "%'", Examine.class);
		Logger.info("sql=" + "select * from Examine e where e.id IN (select examines_id from OutdoorActivity_Examine oe where oe.OutdoorActivity_id="+oaid+")" +  
				" and e.title like '%" + kw+ "%'");
		examine = q.getResultList();
		return examine;
	}
	
	public static List<Examine> findExamineByAttendId(Long attendId) {
		List<Examine> exams = new ArrayList<Examine>();
		Query q = JPA.em().createNativeQuery("select * from Examine e where e.publish=1 AND e.id IN " +
				"(select examines_id from OutdoorActivity_Examine oe where oe.OutdoorActivity_id IN " +
				"(select oa_id from Apply a where a.attendantMember_id="+attendId+"))", Examine.class);
		exams = q.getResultList();
		return exams;
	}
	
	public static List<Examine> findExamineByCreatorAndStatus(Long creatorId, int status) {
		List<Examine> exams = new ArrayList<Examine>();
		Query q = JPA.em().createNativeQuery("select * from Examine e where e.creator_id=" + 
				creatorId + " and e.status=" + status, Examine.class);
		exams = q.getResultList();
		return exams;
	}

	public static List<Examine> findExamineByCreator(Long creatorId) {
		List<Examine> exams = new ArrayList<Examine>();
		Query q = JPA.em().createNativeQuery("select * from Examine e where e.creator_id=" + creatorId, Examine.class);
		exams = q.getResultList();
		return exams;
	}
	
}
