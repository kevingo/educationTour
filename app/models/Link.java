package models;



import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;

//toolkit中的 地圖

@Entity
public class Link extends Model {
	
	//標題
	public String title;
	
	//連結名稱
	public String linkName;
	
	//說明
	@Lob
	public String intro;
	
	//路徑 
	public String url;

	//share
	public boolean share;
	
	//time
	public String createTime;
	
	//creator
	@OneToOne
	public Member creator;

	public static List<Link> findByCreator(Long id) {
		List<Link> links = new ArrayList<Link>();
		Query q = JPA.em().createNativeQuery("select * from Link e where e.creator_id=" + id, Link.class);
		links = q.getResultList();
		return links;
	}
}
