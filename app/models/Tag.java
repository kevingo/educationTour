package models;



import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Query;


import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;



@Entity
public class Tag extends Model {
	
	//名稱
	public String name;
	
	//顏色色碼
	public String color;
	
	//啟用
	public boolean enable;

	public static Tag findByNameAndCategory(String tagName ,Long cateid) {
		TagCategory tagCategory = TagCategory.findById(cateid);
		if(tagCategory!=null){
			Tag tag = null;
			for (Tag t : tagCategory.tags) {
				if(t.name==tagName)
					tag=t;
			}
			
			return tag;
		}
		else
			return null;
	}
	
	public static Tag findTagByName(String tagName) {
		Tag t = null;
		Query q = JPA.em().createNativeQuery("select * from Tag t where t.name=" + "\"" + tagName + "\"", Tag.class);
		if(q.getResultList().size()>0)
			t = (Tag) q.getResultList().get(0);
		return t;
	}

	public static List<Tag> findByTagActivity(String activity) {
		List<TagCategory> listCate = TagCategory.findByActivity(activity);
		List<Tag> listTags = new ArrayList<Tag>();
		for (TagCategory cate : listCate) {
			for (Tag tag : cate.tags) {
				if(!listTags.contains(tag))
					listTags.add(tag);
			}
		}
		
		return listTags;
	}
	
	public static List<Tag> findTagByOA(Long oaid) {
		List<Tag> tags = null;
		Query q = JPA.em().createNativeQuery("select * from Tag t where t.id IN (" +
				"select tags_id from OutdoorActivity_Tag tt where tt.OutdoorActivity_id=" + oaid + ")", Tag.class);		
		tags = (List<Tag>)q.getResultList();
		return tags;
	}
	
	public static List<Tag> findTagsByTeachingPlan(Long tid) {
		List<Tag> tags = null;
		Query q = JPA.em().createNativeQuery("select * from Tag t where t.id IN (" +
				"select tags_id from TeachingPlan_Tag tt where tt.TeachingPlan_id=" + tid + ")", Tag.class);		
		tags = (List<Tag>)q.getResultList();
		return tags;
	}

}
