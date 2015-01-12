package models;



import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Query;

import org.apache.ivy.ant.IvyPublish.PublishArtifact;

import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;

//toolkit中的 地圖

@Entity
public class HistoryNode extends Model {
	
	//屬於哪一個apply
	@ManyToOne
	public Apply apply;
	
	//點名的時間
	public String time;
	
	//經緯度
	public String lat;
	public String lng;

	//地址
	public String address;
	
	//此筆紀錄是否已通知家長
	public boolean notifyParent;
}
