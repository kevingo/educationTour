package models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import Utility.MyUtil;

import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;

//戶外活動 

@Entity
public class OAIncludeTP extends Model {
	 

	//引用的TP
	@OneToOne
	public TeachingPlan tp;
	
	//此教案之titleName
	public String citeTitle;
	
	//額外補充說明
	@Lob
	public String intro;
	
	//time
	public String createTime;


	public static List<OAIncludeTP> findByOA(Long oaid) {
		//先找出該oa
		OutdoorActivity oa = OutdoorActivity.findById(oaid);
		List<OAIncludeTP> listOAIncludeTP = new ArrayList<OAIncludeTP>();
		listOAIncludeTP.addAll(oa.tps);
		return listOAIncludeTP;
	}
	
	public static List<OAIncludeTP> findByTP(Long tpid) {
		Query q = JPA.em().createNativeQuery("select * from OAIncludeTP o where o.tp_id=" + tpid, OAIncludeTP.class);
		
		List<OAIncludeTP> listOAIncludeTP = null;
		listOAIncludeTP = (List<OAIncludeTP>)q.getResultList();
		return listOAIncludeTP;
	}
	
}