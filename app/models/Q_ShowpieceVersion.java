package models;

import javax.persistence.Entity;
import javax.persistence.Query;

import play.db.jpa.JPA;
import play.db.jpa.Model;

@Entity
public class Q_ShowpieceVersion extends Model {

	public String tableName; // Table 名稱
	public String version;   //版本
	public String time;      // 更新日期
	public Q_ShowpieceVersion(String tableName, String version, String time) {
		this.tableName=tableName;
		this.version=version;
		this.time=time;
		create();
	}
	public static Q_ShowpieceVersion findVersionByName(String name) {
		Query q = JPA.em().createNativeQuery("select * from Q_ShowpieceVersion s where s.tableName='" + name + "'", Q_ShowpieceVersion.class);
		Q_ShowpieceVersion m = null;
		if(q.getResultList().size()>0)
			m = (Q_ShowpieceVersion) q.getResultList().get(0);			
		return m; 
	}
}
