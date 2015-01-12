package models;

import java.sql.Time;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import org.apache.commons.lang.SerializationUtils;
import org.codehaus.groovy.tools.shell.commands.ShowCommand;
import org.eclipse.jdt.core.dom.ThisExpression;

import com.sun.org.apache.bcel.internal.generic.LNEG;


import Utility.MyUtil;
import Utility.ROLE;

import play.db.jpa.JPA;
import play.db.jpa.Model;



@Entity
public class Q_Showpiece extends Model {
	
	@OneToOne 
	public Q_Show show; //展覽
	public String showpieceName;
	public String serialNumber; //展品唯一識別碼
	public String information;     //說明 (額外備註)
	@OneToOne
	public Member reciever; //所在位置(接收方)
	@OneToOne
	public Member sender;   //送出方
	public String status;
	@OneToMany
	public Set<Q_ShowpieceHistoryNode> historyNodes; //歷程記錄

	
	public Q_Showpiece(String showName, String showpieceName,String serialNumber,String status, String information) {
		
		Q_Show show= Q_Show.findByName(showName);
		if(show==null){
			show = new Q_Show(showName);
			//新增版號 (+0.1)
			//預定刪除 只須留一個版號即可
			Q_ShowpieceVersion showVersion = Q_ShowpieceVersion.findVersionByName("q_show");
			double version = Double.parseDouble(showVersion.version)+0.1;
			DecimalFormat df = new DecimalFormat("#.#");   
			showVersion.version=String.valueOf(df.format(version));
			showVersion.time=MyUtil.now();
			showVersion.save();
		}
				
		this.show = show;
		this.showpieceName = showpieceName;
		this.serialNumber = serialNumber;
		this.information=information;
		//初始設定為廣達admin
		this.sender =(Member) Member.findUser("quanta@gmail.com");
		this.reciever =(Member) Member.findUser("quanta@gmail.com");
		this.status=status;
		this.historyNodes = new HashSet();
		
		Q_ShowpieceHistoryNode node = new Q_ShowpieceHistoryNode();
		
		node.member=sender;
		node.recieveTime=MyUtil.now();
		node.recieveStatus=status;
		node.sendTime=MyUtil.now();
		node.sendStatus=status;
		node.save();
		this.historyNodes.add(node);

		
		create();
	}
		

	//透過展覽ID找到所有的展品
	public static List<Q_Showpiece> findShowpieceByShow(Long show_id) {
		Query q = JPA.em().createNativeQuery("select * from Q_Showpiece s where s.show_id='" + show_id + "'", Q_Showpiece.class);
		
		
		List<Q_Showpiece> result = null;
		result =  (List<Q_Showpiece>)q.getResultList();
		return result;
	}
	
	//透過展覽serial number 找到該次所有的展品
	public static List<Q_Showpiece> findShowpieceByShowSerialNumber(String serialNumber) {
		//先找到show id
		Q_Show show = Q_Show.findShowBySerialNumber(serialNumber);
		if(show!=null){
			Query q = JPA.em().createNativeQuery("select * from Q_Showpiece s where s.show_id='" + show.id + "'", Q_Showpiece.class);
			
			try {
				return (List<Q_Showpiece>)q.getResultList();
			} catch (Exception e) {
				return  null;
			}
		}
		else {
			return null;
		}
	}
	
	//透過展品名稱找到特定的展品
	public static Q_Showpiece findShowpieceByShowpieceName(String showpieceName) {
		Query q = JPA.em().createNativeQuery("select * from Q_Showpiece s where s.showpieceName='" + showpieceName + "'", Q_Showpiece.class);
		
		Q_Showpiece s = null;
		if(q.getResultList().size()>0)
			s = (Q_Showpiece) q.getResultList().get(0);			
		return s; 
	}
	
	public static Q_Showpiece findShowpieceByShowpieceSerialNumber(String serialNumber) {
		Query q = JPA.em().createNativeQuery("select * from Q_Showpiece s where s.serialNumber='" + serialNumber + "'", Q_Showpiece.class);
		Q_Showpiece s = null;
		if(q.getResultList().size()>0)
			s = (Q_Showpiece) q.getResultList().get(0);			
		return s; 
	}

	public static boolean IsExistBySerialNumber(String serialNumber) {
		Query q = JPA.em().createNativeQuery("select * from Q_Showpiece s where s.serialNumber='" + serialNumber + "'", Q_Showpiece.class);
		
		if(q.getResultList().size()>0)
			return true;
		else			
			return false;
	}


	public static Q_Showpiece findShowpieceByShowAndShowpiece(String showName,String showpieceName) {
		//先找到show id
		Q_Show show = Q_Show.findByName(showName);

		if(show==null)
			return null;
		Query q = JPA.em().createNativeQuery("select * from Q_Showpiece s where s.showpieceName='" + showpieceName + "' and s.show_id='"+show.id+"'", Q_Showpiece.class);
		Q_Showpiece s = null;
		if(q.getResultList().size()>0)
			s = (Q_Showpiece) q.getResultList().get(0);		
		return s; 
	}


	


	
	
	
	
}
