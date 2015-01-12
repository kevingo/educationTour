package models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import org.apache.commons.lang.StringEscapeUtils;

import com.sun.org.apache.bcel.internal.generic.NEW;


import Utility.MyUtil;

import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;

//戶外活動 

@Entity
public class OutdoorActivity extends Model {
	 
	public static final int STATUS_NotBegin = 0;   // 尚未開始報名
	public static final int STATUS_Progress = 1;  // 開放報名中
	public static final int STATUS_Finish = 2;    // 已截止報名

	
	//平台管理者用 (控制開關,預設開啟)
	public boolean enable = true;
	
	//建立者(老師)與建立日期
	@OneToOne
	@Required
	public Member creator;
	public String createDate;
	
	//是否公開
	public boolean publish;
	//報名狀態
	public int status;

	//活動名稱
	public String name;
	
	//標籤
	@ManyToMany
	public Set<Tag> tags;
	
	//活動時間
	public String fromDate;
	public String toDate;
	public String fromTime;
	public String toTime;
	
	//報名截止日期
	public String regDue;
	
	//報名人數限制
	public int attendeeUpperLimit;
	public int attendeeLowerLimit;
	
	//集合點	
	public String gatherPoint;
	
	//活動費用(價格)
	public int price;
	
	//活動簡介
	@Lob
	public String introduction; 
	
	//元件的順序， 用代碼,id表示    ,且用 "##" 隔開
	//例如: map,1##youtube,4##photo,7  =>代表 地圖的id 1 接著是 youtube的id 4 ,依此類推
	//一共有 map, youtube ,text , photo , attatch , link ,
	public String componentOrder;
	
	
	//引用的教案 用 OA include TP 這張table儲存
	@ManyToMany
	public Set<OAIncludeTP> tps;

	//評量
	@OneToMany
	public Set<Examine> examines;
		
	//封面照片
	@ManyToMany
	public Set<Photo> photos;
	
	//NFC配對元件
	@OneToMany
	public Set<NFC> nfc;

	
	public int viewCount = 0;

	public static List<OutdoorActivity> listByCreator(Long userId) {
		Query q = JPA.em().createNativeQuery("select * from OutdoorActivity where creator_id=" + userId, OutdoorActivity.class);
		return (List<OutdoorActivity>)q.getResultList();
	}
	
	public static List<OutdoorActivity> listByStatusAndUserId(Long userId, int status) {
		Query q = JPA.em().createNativeQuery("select * from OutdoorActivity where enable=1 and creator_id=" + userId + " and status=" + status, OutdoorActivity.class);
		return (List<OutdoorActivity>)q.getResultList();
	}
	
	public static List<OutdoorActivity> listTodayOAByEmail(String email){
		Member creator = Member.findUser(email);
		String date = MyUtil.getDate();
		//重組時間格式 ，以符合SQL內儲存的格式 (eg:20130808 => 2013-08-08)
		date =date.substring(0,4)+"-"+date.substring(4,6)+"-"+date.substring(6,8);
		Query q = JPA.em().createNativeQuery("select * from OutdoorActivity where creator_id=" + creator.id +" and  fromDate='"+date+"'", OutdoorActivity.class);
		return (List<OutdoorActivity>)q.getResultList();
	}
	
	public static List<OutdoorActivity> listPublishOA() {
		Query q = JPA.em().createNativeQuery("select * from OutdoorActivity o where o.publish=1 and o.enable=1", OutdoorActivity.class); // 狀態:1是上架
		return (List<OutdoorActivity>)q.getResultList();
	}	
	
//	public static List<OutdoorActivity> listPublishOAWithKw() {
//		
//	}
	
	public static List<OutdoorActivity> listPublishOAByAdmin() {
		Query q = JPA.em().createNativeQuery("select * from OutdoorActivity o where o.publish=1", OutdoorActivity.class); // 狀態:1是上架
		return (List<OutdoorActivity>)q.getResultList();
	}
	
	public static int getCurrApplyCount(Long oaid) {
		Query q = JPA.em().createNativeQuery("select count(*) from Apply a where a.oa_id=" + oaid);
		int num = 0;
		if(q.getResultList().size()>0)
			num = Integer.parseInt(String.valueOf(q.getResultList().get(0)));
		return num;
	}
	
	public static List<OutdoorActivity> findHotOutdoorActivity(){
		List<OutdoorActivity> allOA = findAll();
		List<OutdoorActivity> hotOA = new ArrayList<OutdoorActivity>();
		if(allOA.size()>=2) {
			hotOA.addAll(allOA.subList(allOA.size()-2, allOA.size()));
			
			for (OutdoorActivity oa : allOA) {
				int newCount = getCurrApplyCount(oa.id);
				//第一個node count 與第二個node count
				int hoterCount =  getCurrApplyCount(hotOA.get(0).id);
				int hotCount =  getCurrApplyCount(hotOA.get(1).id);
				if(newCount>hoterCount){
					//清除hotOA,重新先加入newNode ,再加入原node1
					OutdoorActivity node1 = OutdoorActivity.findById(hotOA.get(0).id);
					hotOA.clear();
					hotOA.add(oa);
					hotOA.add(node1);
				}
				else if(newCount>hotCount){
					//替換掉node2
					hotOA.remove(1);
					hotOA.add(oa);
				}
			}
		}
		else
			hotOA.addAll(allOA);
		
		return hotOA;
	}

	public static List<OutdoorActivity> findByKeyword(String keyword) {
		keyword = StringEscapeUtils.escapeSql(keyword);
		List<OutdoorActivity> listOAs = null;
		String query = "select * from OutdoorActivity o where o.publish=1 and o.enable=1 AND" +
				" o.name like " + "'%" + keyword + "%'";
		Query q = JPA.em().createNativeQuery(query, OutdoorActivity.class);
		listOAs = q.getResultList(); 
		return  listOAs;
	}

	public static List<OutdoorActivity> findByTag(Long tagId) {
		Tag tag = Tag.findById(tagId);
		List<OutdoorActivity> listAllOA = null;
		List<OutdoorActivity> listOA = new ArrayList<OutdoorActivity>();
		
		Query q = JPA.em().createNativeQuery("select * from OutdoorActivity o where o.publish=1 and o.enable=1", OutdoorActivity.class);
		listAllOA = q.getResultList();
		if(listAllOA.size()>0){
			for (OutdoorActivity oa : listAllOA) {
				if(oa.tags.contains(tag) && !listOA.contains(oa))
					listOA.add(oa);
			}
			return  listOA;
		}
		else {
			return null;
		}
	}

	public static List<OutdoorActivity> findAllByPublish(boolean publish) {
		int publishInt = (publish)?1:0;
		Query q = JPA.em().createNativeQuery("select * from OutdoorActivity o where o.publish="+publishInt+" and o.enable=1", OutdoorActivity.class); 
		return (List<OutdoorActivity>)q.getResultList();
	}

	public static List<OutdoorActivity> listMyFavoOa(Long userId) {
		List<OutdoorActivity> oas = new ArrayList<OutdoorActivity>();
		Query q = JPA.em().createNativeQuery("select * from OutdoorActivity o where o.id IN(select favoriteOAs_id from Member_OutdoorActivity m where m.Member_id=" + userId + ")", OutdoorActivity.class);
		if(null!=q.getResultList())		
			oas = q.getResultList();
		return oas;
	}

	public static List<OutdoorActivity> listChildOAByStatus(Long child_id, int statusProgress) {
		List<OutdoorActivity> oas = new ArrayList<OutdoorActivity>();
		Query q = JPA.em().createNativeQuery("select * from OutdoorActivity o where o.enable=1 and o.id IN (select oa_id from Apply a where a.attendantMember_id=" + child_id + ") and o.status=" + statusProgress + "", OutdoorActivity.class);
		oas = q.getResultList();
		return oas;
	}
	
	public static List<OutdoorActivity> listChildOAByStatus(Long child_id) {
		List<OutdoorActivity> oas = new ArrayList<OutdoorActivity>();
		Query q = JPA.em().createNativeQuery("select * from OutdoorActivity o where o.id IN (select oa_id from Apply a where a.attendantMember_id=" + child_id + ")", OutdoorActivity.class);
		oas = q.getResultList();
		return oas;
	}

	public static List<OutdoorActivity> listMyOaByStatus(Long userId,
			int statusProgress) {
		List<OutdoorActivity> my_oa = new ArrayList<OutdoorActivity>();
		Query q = JPA.em().createNativeQuery("select * from OutdoorActivity o where o.enable=1 and o.id IN" +
				"(select a.oa_id from Apply a where a.attendantMember_id="+ userId + ") and o.status=" + statusProgress, OutdoorActivity.class);
		my_oa = q.getResultList();
		return my_oa;
	}
	
	public static List<OutdoorActivity> listMyJoinOa(Long userid) {
		List<OutdoorActivity> my_join_oa = new ArrayList<OutdoorActivity>();
		Query q = JPA.em().createNativeQuery("select * from OutdoorActivity o where o.id IN(select a.oa_id from Apply a where a.attendantMember_id="+userid+")", OutdoorActivity.class);
		my_join_oa = q.getResultList();
		return my_join_oa;
	}

	public static OutdoorActivity findOAByExamID(Long examId) {
		Query q = JPA.em().createNativeQuery("select * from OutdoorActivity o where o.id IN (select OutdoorActivity_id from OutdoorActivity_Examine oe where oe.examines_id="+examId+")", OutdoorActivity.class);
		OutdoorActivity oa =null;
		if(q.getResultList().size()>0)
		 oa=(OutdoorActivity)q.getResultList().get(0);
		return oa;
	}

	public static List<OutdoorActivity> findByCiteTP(Long CiteId) {
		List<OutdoorActivity> listOAs = null;
		String query = "select * from OutdoorActivity o where o.componentOrder like '%cite," + CiteId + "##%'";
		Query q = JPA.em().createNativeQuery(query, OutdoorActivity.class);
		listOAs = q.getResultList(); 
		return  listOAs;
	}

	public static OutdoorActivity findByNFC(String serialNumber) {
		//取得NFC ID
		NFC nfc = NFC.findBySerailNumber(serialNumber);
		OutdoorActivity oa=null;
		if(nfc!=null){
			List<OutdoorActivity> allOA = OutdoorActivity.findAll();
			for (OutdoorActivity o : allOA) {
				if(o.nfc.contains(nfc))
					oa=o;
			}
		}
		return oa;
	}
	
}