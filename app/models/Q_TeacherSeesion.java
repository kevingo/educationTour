package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Query;

import Utility.ROLE;

import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import play.db.jpa.Model;

@Entity
public class Q_TeacherSeesion extends Model {

	public String sessionName;
	public String date;
	public String time;
	public String location;
	public int visit = 0; //參與人數
	
	@ManyToMany
	public Set<Attatch> files;
	
	public Q_TeacherSeesion( String date ,String time , String sessionName, String location) {
		this.sessionName=sessionName;
		this.date = date;
		this.time=time;
		this.location=location;		
		visit = 0;	
		create();
	}

	
	public static List<Q_TeacherSeesion> findSessionByName(String sessionName) {
		Query q = JPA.em().createNativeQuery("select * from Q_TeacherSeesion s where s.sessionName='" + sessionName + "'", Q_TeacherSeesion.class);
		List<Q_TeacherSeesion> s = null;
		if(q.getResultList().size()>0)
			s = q.getResultList();			
		return s; 
	}



	public static Q_TeacherSeesion findByDateAndName(String sessionDate , String sessionName) {
		Query q = JPA.em().createNativeQuery("select * from Q_TeacherSeesion s where s.sessionName='" + sessionName + "' and s.date='"+sessionDate+"'", Q_TeacherSeesion.class);
		Q_TeacherSeesion s = null;
		if(q.getResultList().size()>0)
			s = (Q_TeacherSeesion) q.getResultList().get(0);			
		return s; 
	}
	
	
}
