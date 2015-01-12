package models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;

import play.data.validation.Required;
import play.db.jpa.Blob;
import play.db.jpa.JPA;
import play.db.jpa.Model;
import Utility.MyUtil;
import Utility.ROLE;


// 群組

@Entity
public class Q_VolunteerGroup extends Model {

	
	@OneToOne
	@Required
	public Member creator;
	
	public String groupName;
	
	// 建立日期
	public String createDate = MyUtil.now();
	
	@OneToMany
	public Set<Member> members;
	
	public boolean active=true; //可啟動或關閉 此群組
	
	public Q_VolunteerGroup() {
		this.members = new HashSet();

	}
	/*
	public static Q_VolunteerGroup findGroupByCreator(@Required Long id) {
		Member creator = Member.findById(id);
		Query q = JPA.em().createNativeQuery("select * from q_volunteergroup g where g.creator_id=" + creator.id, Q_VolunteerGroup.class);				
		if(q.getResultList().size()>0)
			return (Q_VolunteerGroup)q.getResultList().get(0);
		else
			return null;
	}
	*/
	public static Q_VolunteerGroup findGroupByMember(@Required Long id) {
		Member member = Member.findById(id);
		Query q = JPA.em().createNativeQuery("select Q_VolunteerGroup_id from Q_VolunteerGroup_Member g where g.members_id=" + member.id);				
		Long group_id=(long) 0;

		if(q.getResultList().size()>0)
			group_id=Long.parseLong(q.getResultList().get(0).toString());
		
		if(group_id>0){
			q = JPA.em().createNativeQuery("select * from Q_VolunteerGroup g where g.id=" + group_id, Q_VolunteerGroup.class);	
			if(q.getResultList().size()>0)
				return (Q_VolunteerGroup)q.getResultList().get(0);	
		}
		
		return null;
	}
	public static List<Q_VolunteerGroup> findGroupByCreator(Long m_id) {
		Query q = JPA.em().createNativeQuery("select * from Q_VolunteerGroup g where g.creator_id=" + m_id, Q_VolunteerGroup.class);	
		List<Q_VolunteerGroup> listGroups = null;
		
		if(q.getResultList().size()>0)
			return listGroups = q.getResultList();	
		else 
			return listGroups;
	}
	
}


