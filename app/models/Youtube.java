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

//toolkit中的 影片 youtube

@Entity
public class Youtube extends Model {
	
	//標題
	public String title;
	
	//說明
	@Lob
	public String intro;
	
	//網址
	public String url;
	
	//share
	public boolean share;
	
	//time
	public String createTime;
	
	//creator
	@OneToOne
	public Member creator;

	public static List<Youtube> findByCreator(Long id) {
		List<Youtube> youtubes = new ArrayList<Youtube>();
		Query q = JPA.em().createNativeQuery("select * from Youtube e where e.creator_id=" + id, Youtube.class);
		youtubes = q.getResultList();
		return youtubes;
	}
}
