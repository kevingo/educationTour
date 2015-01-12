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
public class Attatch extends Model {
	
	//標題
	public String title;
	
	//說明
	@Lob
	public String intro;
	
	//路徑位置 與檔案名稱
	public String path;
	public String fileName;
	
	//share
	public boolean share;
	
	//time
	public String createTime;
	
	//creator
	@OneToOne
	public Member creator;

	public static List<Attatch> findByCreator(Long id) {
		List<Attatch> attatchs = new ArrayList<Attatch>();
		Query q = JPA.em().createNativeQuery("select * from Attatch e where e.creator_id=" + id, Attatch.class);
		attatchs = q.getResultList();
		return attatchs;
	}
	

}
