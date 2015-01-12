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

//toolkit中的 純文字 (多行)

@Entity
public class Text extends Model {
	
	//標題
	public String title;
	
	//說明
	@Lob
	public String intro;
	
	//share
	public boolean share;
	
	//time
	public String createTime;
	
	//creator
	@OneToOne
	public Member creator;

	public static List<Text> findByCreator(Long id) {
		List<Text> texts = new ArrayList<Text>();
		Query q = JPA.em().createNativeQuery("select * from Text e where e.creator_id=" + id, Text.class);
		texts = q.getResultList();
		return texts;
	}
	
}
