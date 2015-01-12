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
public class Map extends Model {
	
	//標題 (自訂欄位名稱)
	public String title;
	
	//景點名稱
	public String POIName;
	
	//說明
	@Lob
	public String intro;
	
	//經緯度
	public String lat;
	public String lng;
	
	//地址 電話
	public String address;
	public String phone;
	
	//停留開始時間
	public String startTime;
	
	//離開時間
	public String endTime;

	//share
	public boolean share;
	
	//time
	public String createTime;
	
	//creator
	@OneToOne
	public Member creator;

	public static List<Map> findByCreator(Long id) {
		List<Map> maps = new ArrayList<Map>();
		Query q = JPA.em().createNativeQuery("select * from Map e where e.creator_id=" + id, Map.class);
		maps = q.getResultList();
		return maps;
	}
}
