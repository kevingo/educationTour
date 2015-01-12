package models;

import javax.persistence.Entity;
import javax.persistence.Query;

import play.db.jpa.JPA;
import play.db.jpa.Model;

@Entity
public class NFC  extends Model {

	public String title;
	
	public String componentOrder;
	
	// rule: nfc_id, ex: nfc_1
	public String serialNumber;

	public static NFC findBySerailNumber(String serial) {
		Query q = JPA.em().createNativeQuery("select * from NFC n where n.serialNumber='"+serial+"'", NFC.class);
		NFC nfc =null;
		if(q.getResultList().size()>0)
			nfc=(NFC)q.getResultList().get(0);
		return nfc;
	}
	
}
