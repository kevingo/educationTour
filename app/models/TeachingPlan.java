package models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import org.apache.commons.lang.StringEscapeUtils;

import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;

// 教案

@Entity
public class TeachingPlan extends Model {

	public static int out = 0;		// 下架
	public static int ok = 1;		// 上架
	//以下狀態暫時用不到，目前只分上下架
	public static int progress = 2; // 審核中
	public static int reject = 3;	// 退回
	public static int draft = 4;	// 草稿
	
	//admin權限管理-上下架and刪除 (預設開啟)
	public boolean enable = true;
	
	@OneToOne
	@Required
	public Member creator;
	public String createDate;
	
	//教案名稱
	public String name;
	
	//老師控制是否公開
	public boolean publish;
	
	//是否分享(開放引用)
	public boolean share;

	//標籤
	@ManyToMany
	public Set<Tag> tags;
		
	//主題課程
	public String subject;
	
	//單元課程
	public String unit;
	
	// 簡介
	@Lob
	public String introduction;
	
	//for 封面(從行前中後其一中抓第一張圖片當封面)
	@OneToOne
	public Photo photo;
	
	//行前
	@OneToOne
	public TeachingPlanBefore tpBefore;
	
	//行中
	@OneToOne
	public TeachingPlanNow tpNow;
	
	//行後
	@OneToOne
	public TeachingPlanAfter tpAfter;
	
	public int viewCount = 0;

	public TeachingPlan() {
		
		this.tags = new HashSet();
		/*
		this.files = new HashSet();
		this.uri = new HashSet();
		this.pois = new HashSet();
		*/
	}
	
	public static TeachingPlan findById(Long id){
		Query q = JPA.em().createNativeQuery("select * from TeachingPlan t where t.id=" + id, TeachingPlan.class);				
		TeachingPlan m = null;
		if(q.getResultList().size()>0)
			m = (TeachingPlan) q.getResultList().get(0);	
		return m; 
	}
	
	public static List<TeachingPlan> listByCreator(Long userId) {
		Query q = JPA.em().createNativeQuery("select * from TeachingPlan t where t.creator_id=" + userId, TeachingPlan.class);				
		return (List<TeachingPlan>)q.getResultList();
	}
	
	public static List<TeachingPlan> listByPublishAndUserId(Long userId, Boolean publish) {
		Query q = JPA.em().createNativeQuery("select * from TeachingPlan t where t.enable=1 and t.creator_id=" + userId + " and t.publish=" + publish, TeachingPlan.class);				
		return (List<TeachingPlan>)q.getResultList();
	}

	public static List<TeachingPlan> listPublishTp() {
		Query q = JPA.em().createNativeQuery("select * from TeachingPlan t where t.publish=1 and t.enable=1", TeachingPlan.class);
		try {
			return (List<TeachingPlan>)q.getResultList();
		} catch (Exception e) {
			return  null;
		}
	}
	
	// num : -1 , for all
	public static List<TeachingPlan> listHotAndPublishTpDESC(int num) {
		String query = "";
		if(num<=-1)
			query = "select * from TeachingPlan t where t.publish=1 and t.enable=1 order by viewCount DESC";
		else
			query = "select * from TeachingPlan t where t.publish=1 and t.enable=1 order by viewCount DESC limit " + num;
		Query q = JPA.em().createNativeQuery(query, TeachingPlan.class);
		return q.getResultList();
	}
	
	public static List<TeachingPlan> listPublishTpByAdmin() {
		Query q = JPA.em().createNativeQuery("select * from TeachingPlan t where t.publish=1", TeachingPlan.class);
		try {
			return (List<TeachingPlan>)q.getResultList();
		} catch (Exception e) {
			return  null;
		}
	}
	
	public static List<TeachingPlan> findByTag(Tag tag){
		List<TeachingPlan> listTPs = new ArrayList<TeachingPlan>();
		Query q = JPA.em().createNativeQuery("select * from TeachingPlan t where t.publish=1 and t.enable=1", TeachingPlan.class);
		List<TeachingPlan> allTPs =  (List<TeachingPlan>) q.getResultList();
		if(allTPs!=null){
			for (TeachingPlan tp : allTPs) {
				if(tp.tags.contains(tag))
					listTPs.add(tp);
			}
		}
		return listTPs;
	}

	public static List<TeachingPlan> findByKeyword(String keyword) {
		keyword = StringEscapeUtils.escapeSql(keyword);
		List<TeachingPlan> listTPs = null;
		Query q = JPA.em().createNativeQuery("select * from TeachingPlan t where t.name like '%" + keyword+"%' and t.publish=1 and t.enable=1", TeachingPlan.class);
		listTPs = q.getResultList();
		return listTPs;
		
	}

	public static List<TeachingPlan> listMyFavoTp(Long userId) {
		List<TeachingPlan> tps = new ArrayList<TeachingPlan>();
		Query q = JPA.em().createNativeQuery("select * from TeachingPlan t where t.id IN (select favoriteTPs_id from Member_TeachingPlan m where m.Member_id=" + userId + ")", TeachingPlan.class);		
		if(null!=q.getResultList())
			tps = q.getResultList();
		return tps;
	}

	
}
