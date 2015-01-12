package models;



import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Query;

import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;

//toolkit中的 地圖

@Entity
public class ViewCountHistory extends Model {
	
	public static final int MemberView=0;
	public static final int TeachingPlanView=1;
	public static final int OutdoorActivityView=2;
	
	//ip
	public String ip;
	
	//看哪一個內容 ()
	public int contentType;
	
	//內容的id
	public Long contentId;
	
	//日期
	public String date;

	public static ViewCountHistory findByIpAndContent(String ip, int contentType, Long contentId) {
		Query q = JPA.em().createNativeQuery("select * from ViewCountHistory v where v.ip='"+ip+"' and v.contentType="+contentType+" and contentId="+contentId, ViewCountHistory.class);
		ViewCountHistory view = null;
		if(q.getResultList().size()>0)
			view=(ViewCountHistory)q.getResultList().get(0);
		return view;
	}

}
