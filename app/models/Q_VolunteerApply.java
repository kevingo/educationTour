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
public class Q_VolunteerApply extends Model {
	
	@OneToOne
	@Required
	public Member member; // 報名者(廣達)
	
	@OneToOne
	@Required
	public Q_VolunteerSeesion session; // 場次(廣達)
	
	public String checkinTime;
	public String checkoutTime;
	public String totalTime;

	
	
	public static List<Q_VolunteerApply> findApplyByUser(Long member_id) {
		Query q = JPA.em().createNativeQuery("select * from Q_VolunteerApply a where a.member_id='" + member_id + "'", Q_VolunteerApply.class);
		return (List<Q_VolunteerApply>)q.getResultList();
	}
	
	public static List<Q_VolunteerApply> findApplyBySession(Long session_id) {
		Query q = JPA.em().createNativeQuery("select * from Q_VolunteerApply a where a.session_id='" + session_id + "'", Q_VolunteerApply.class);
		return (List<Q_VolunteerApply>)q.getResultList();
	}
	
	public static Q_VolunteerApply findApplyBySessionAndUser(Long session_id,Long member_id) {
		Query q = JPA.em().createNativeQuery("select * from Q_VolunteerApply a where a.session_id='" + session_id + "' and a.member_id='"+member_id+"'", Q_VolunteerApply.class);
		Q_VolunteerApply a = null;
		if(q.getResultList().size()>0)
			a = (Q_VolunteerApply) q.getResultList().get(0);			
		return a; 
	}
	
	
}