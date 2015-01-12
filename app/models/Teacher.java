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
public class Teacher extends Model {
	
	//標題 (自訂欄位名稱)
	public String title;
	
	//老師姓名
	public String teacherName;
	//學經歷
	@Lob
	public String education; 
	
	//簡短文字說明
	@Lob
	public String introduction;
	
	//老師封面圖片
	@OneToOne
	public Photo photo;
	
	//share
	public boolean share;
	
	//time
	public String createTime;
	
	//creator
	@OneToOne
	public Member creator;

	public static List<Teacher> findByCreator(Long id) {
		List<Teacher> teachers = new ArrayList<Teacher>();
		Query q = JPA.em().createNativeQuery("select * from Teacher e where e.creator_id=" + id, Teacher.class);
		teachers = q.getResultList();
		return teachers;
	}
	
}
