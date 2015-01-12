package models;

import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.TableGenerator;

import java.util.ArrayList;

import javax.persistence.Entity;
import javax.persistence.Query;

import Utility.MyUtil;


import play.db.jpa.JPA;
import play.db.jpa.Model;
import play.db.jpa.GenericModel;
import play.db.jpa.Model;

// 聯絡我們

@Entity

public class Contact extends GenericModel {
	
	//處理狀態 (未處理 ,處理中,已處理)
	public static final String STATUS_Unhandle="未處理";
	public static final String STATUS_Handling="處理中";
	public static final String STATUS_Done="已處理";

	
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "generate")
	@TableGenerator(name="generate", initialValue = 1, allocationSize = 1)
	public Long id;
	
	
	// 姓名 email 電話 內容
	public String category;
	public String name;
	public String email;
	public String phone;
	public String subject;
	public String content;
	public String time=MyUtil.now();
	public String status = STATUS_Unhandle;
	
	public Boolean replied=false;
	public String re_time ;
	public String re_content;
	public int re_count = 0;
	@OneToOne
	public Member reviewer;
	
	public static List<Contact> findUnreplyList() {
		Query q = JPA.em().createNativeQuery("select * from Contact t where t.replied=" + Boolean.FALSE + " order by t.id DESC" , Contact.class);
		return (List<Contact>)q.getResultList();
	}

	public static List<Contact> findByReviewer(Long reviewerId) {
		Query q = JPA.em().createNativeQuery("select * from Contact t where t.reviewer_id=" + reviewerId, Contact.class);
		return (List<Contact>)q.getResultList();
	}

	
}
