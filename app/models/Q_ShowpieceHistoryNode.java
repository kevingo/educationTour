package models;

import java.math.BigInteger;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import org.apache.commons.lang.SerializationUtils;
import org.eclipse.jdt.core.dom.ThisExpression;

import com.sun.org.apache.regexp.internal.recompile;


import Utility.ROLE;

import play.Logger;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;



@Entity
public class Q_ShowpieceHistoryNode extends Model {
	
	@OneToOne
	@Required
	public Member member;
	
	public String recieveTime;
	
	public String sendTime;
	
	public String recieveStatus;
	public String sendStatus;

	public static Q_ShowpieceHistoryNode findByMemberAndShowpiece(Long member_id, Long showpiece_id) {
		
		Query q = JPA.em().createNativeQuery("select historyNodes_id from Q_Showpiece_Q_ShowpieceHistoryNode s where s.Q_Showpiece_id=" + showpiece_id );
		//先找到showpiece id
		List<BigInteger> listNode =(List<BigInteger>) q.getResultList();
		Q_ShowpieceHistoryNode s = null;
		for (BigInteger node_id : listNode) {
			q = JPA.em().createNativeQuery("select * from Q_ShowpieceHistoryNode s where s.member_id=" + member_id + " and s.id="+node_id.longValue(),Q_ShowpieceHistoryNode.class);
			
			if(q.getResultList().size()>0){
				s = (Q_ShowpieceHistoryNode) q.getResultList().get(0);
				break;
			}
		}
		return s;
				
		
	} 

}
