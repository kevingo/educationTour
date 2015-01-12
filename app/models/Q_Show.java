package models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Query;

import Utility.MyUtil;
import Utility.ROLE;

import play.db.jpa.JPA;
import play.db.jpa.Model;

@Entity
public class Q_Show extends Model {

	public String showName; //展覽名稱
	public String serialNumber; //序號
	public String year; //展出年分


	public Q_Show(String showName) {
		
		this.showName = showName;
		String serialNumber = MyUtil.GenerateShowSerialNumber();
		this.serialNumber = serialNumber;
		//取得時間
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy");
		Date date = new Date();
		String strDate = sdFormat.format(date);
		this.year = strDate;
		
		create();
		
		//建立展覽的QRCode (用於 貼在 交接文件上 以方便列出此次展覽應有的展品)
		MyUtil.genShowQRCode(serialNumber);
	}


	public static boolean IsExistBySerialNumber(String serialNumber) {
		Query q = JPA.em().createNativeQuery("select * from Q_Show s where s.serialNumber='" + serialNumber + "'", Q_Show.class);
		
		if(q.getResultList().size()>0)
			return true;
		else			
			return false;
	}


	public static boolean IsExistByName(String showName) {
		Query q = JPA.em().createNativeQuery("select * from Q_Show s where s.showName='" + showName + "'", Q_Show.class);
		
		if(q.getResultList().size()>0)
			return true;
		else			
			return false;
	}


	public static Q_Show findByName(String showName) {
		Query q = JPA.em().createNativeQuery("select * from Q_Show s where s.showName='" + showName + "'", Q_Show.class);
		Q_Show m = null;
		if(q.getResultList().size()>0)
			m = (Q_Show) q.getResultList().get(0);	
		return m; 
	}


	public static Q_Show findShowBySerialNumber(String serialNumber) {
		Query q = JPA.em().createNativeQuery("select * from Q_Show s where s.serialNumber='" + serialNumber + "'", Q_Show.class);
		Q_Show m = null;
		if(q.getResultList().size()>0)
			m = (Q_Show) q.getResultList().get(0);			
		return m; 
	}

}
