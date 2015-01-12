package models;

import java.util.ArrayList;

import javax.persistence.Entity;
import javax.persistence.Query;

import Utility.ROLE;

import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import play.db.jpa.Model;

@Entity
public class Q_VolunteerSeesion extends Model {

	public String sessionName;
	public String date;
	public String serialNumber;

	public int visit = 0; //參與人數
	
	public Q_VolunteerSeesion(String sessionName, String date, String serialNumber) {
		this.sessionName=sessionName;
		this.date = date;
		this.serialNumber=serialNumber;
		
		
		visit = 0;
		
		create();
	}

	public static Q_VolunteerSeesion findSessionByserialNumber(String serialNumber) {
		Query q = JPA.em().createNativeQuery("select * from Q_VolunteerSeesion s where s.serialNumber='" + serialNumber + "'", Q_VolunteerSeesion.class);
		Q_VolunteerSeesion s = null;
		if(q.getResultList().size()>0)
			s = (Q_VolunteerSeesion) q.getResultList().get(0);			
		return s; 
	}
	
	public static Q_VolunteerSeesion findSessionByName(String sessionName) {
		Query q = JPA.em().createNativeQuery("select * from Q_VolunteerSeesion s where s.sessionName='" + sessionName + "'", Q_VolunteerSeesion.class);
		Q_VolunteerSeesion s = null;
		if(q.getResultList().size()>0)
			s = (Q_VolunteerSeesion) q.getResultList().get(0);			
		return s; 
	}

	public static boolean IsExistBySerialNumber(String serialNumber) {
		Query q = JPA.em().createNativeQuery("select * from Q_VolunteerSeesion s where s.serialNumber='" + serialNumber + "'", Q_VolunteerSeesion.class);
		if(q.getResultList().size()>0)
			return true;
		else			
			return false;
	}

	public static Q_VolunteerSeesion findByNameAndDate(String sessionName, String sessionDate) {
		Query q = JPA.em().createNativeQuery("select * from Q_VolunteerSeesion s where s.sessionName='" + sessionName + "' and s.date='"+sessionDate+"'", Q_VolunteerSeesion.class);
		Q_VolunteerSeesion s = null;
		if(q.getResultList().size()>0)
			s = (Q_VolunteerSeesion) q.getResultList().get(0);			
		return s; 
	}
	
	
}
