package models;



import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;

//toolkit中的 引用元件

@Entity
public class Cite extends Model {
	
	
	//引用的類型，決定要去哪一張Table抓資料
	public String style;
	
	//該table之id
	public Long tid;
	
	//該元件之title
	public String citeTitle;
	
	//說明
	@Lob
	public String intro;
	
	//引用來自哪一個教案
	@OneToOne
	public TeachingPlan citeTP;
	
	//time
	public String createTime;

	public static List<Cite> findByTP(Long id) {
		Query q = JPA.em().createNativeQuery("select * from Cite c where c.citeTP_id=" + id, Cite.class);				
		return (List<Cite>)q.getResultList();
	}
	
	
	
	
}
