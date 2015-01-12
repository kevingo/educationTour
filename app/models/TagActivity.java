package models;



import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Query;

import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;



@Entity
public class TagActivity extends Model {
	
	//大項名稱(找活動,找教案,找老師)
	public String name;

	public static TagActivity findByName(String activity) {
		Query q = JPA.em().createNativeQuery("select * from TagActivity a where a.name=\"" + activity+ "\"" , TagActivity.class);			
		TagActivity m = null;
		if(q.getResultList().size()>0)
			m = (TagActivity) q.getResultList().get(0);	
		return m; 
	}
	

}
