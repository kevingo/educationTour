package models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import play.data.validation.Required;
import play.db.jpa.Blob;
import play.db.jpa.JPA;
import play.db.jpa.Model;
import Utility.MyUtil;
import Utility.ROLE;


// 群組

@Entity
public class MemberGroup extends Model {

	
	@OneToOne
	@Required
	public Member creator;
	
	public String groupName;
	
	
	// 建立日期
	public String createDate = MyUtil.now();
	
	@ManyToMany
	public Set<Member> members;
	
	public boolean active=true; //可啟動或關閉 此群組
	
	public MemberGroup() {
		this.members = new HashSet();

	}
	
	public static List<MemberGroup> findByCreator(@Required Long id) {
		Member creator = Member.findById(id);
		Query q = JPA.em().createNativeQuery("select * from MemberGroup g where g.creator_id=" + creator.id, MemberGroup.class);				
		return (List<MemberGroup>)q.getResultList();		
	}
	
	/*
	public static List<MemberGroup> findByMember(Long id) {
		List<MemberGroup> listGroup = new ArrayList<MemberGroup>();
		Member user = Member.findById(id);
		Query q = JPA.em().createNativeQuery("select * from MemberGroup_Member g where g.members_id=" + user.id);				
		if(q.getResultList().size()>0){
			for (int i=0 ; i<q.getResultList().size() ; i++) {
				Long group_id =(long) 0;
				group_id= Long.parseLong(q.getResultList().get(i).toString());
				if(group_id>0){
					q = JPA.em().createNativeQuery("select * from MemberGroup g where g.id=" + group_id, MemberGroup.class);	
					if(q.getResultList().size()>0 && !listGroup.contains((MemberGroup)q.getResultList().get(0)))
						listGroup.add((MemberGroup) q.getResultList().get(0));
				}
			}
			
			if(listGroup.size()>0)
				return listGroup;
			else 
				return null;
		}
		else
			return null;
	}
	*/
	
	public static List<MemberGroup> findByMember(Long id) {
		List<MemberGroup> listGroup = new ArrayList<MemberGroup>();
		Query q = JPA.em().createNativeQuery("select * from MemberGroup m where m.id IN " +
				"(select MemberGroup_id from MemberGroup_Member mm where mm.members_id=" + id + ")", MemberGroup.class);
		listGroup = q.getResultList();
		return listGroup;
	}

	public static MemberGroup findByNameAndCreator(String groupName, Long creator_id) {
		Query q = JPA.em().createNativeQuery("select * from MemberGroup g where g.creator_id=" + creator_id+" and g.groupName='"+groupName+"'", MemberGroup.class);
		MemberGroup group = null;
		if(q.getResultList().size()>0)
			group=(MemberGroup) q.getResultList().get(0);
		return group;
	}
}


