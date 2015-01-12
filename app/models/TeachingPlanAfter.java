package models;

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
public class TeachingPlanAfter extends Model {
	//順序
	public String orderNumber;
	
	//
	@Lob
	public String illustration;
	
	@OneToOne
	public Member creator;
}
