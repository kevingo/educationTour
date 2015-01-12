package models;



import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.swing.JPanel;

import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;



@Entity
public class Question extends Model {
	
	//題型: 是非、選擇、問答題 
	public static final int STYLE_OX=0;
	public static final int STYLE_singleAnswer=1;
	public static final int STYLE_shortAnswer=2;
	
	//題型
	public int style;
	
	//題目
	@Lob
	public String title;
	
	//附件路徑
	public String attatchPath;
	
	//選項
	public String optionA;
	public String optionB;
	public String optionC;
	public String optionD;

	//分數(占分)
	public int score; 

	//解答
	public int answer;
	
	//解答說明
	@Lob
	public String illustration;
	
	@OneToOne
	public Member creator;

	public static void delQuesByExam(Long eid) {
		Query q = JPA.em().createNativeQuery("DELETE FROM Examine_Question WHERE Examine_id="+ eid);
		q.executeUpdate();
	}
}
