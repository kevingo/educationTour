package models;



import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;


@Entity
public class TagCategory extends Model {
	
	//類別名稱
	public String name;
	
	//大項
	@OneToOne
	public TagActivity tagActivity;
	
	//細項標籤
	@OneToMany
	public Set<Tag> tags;

	public static TagCategory findByNameAndActivity(String categoryName , String activity) {
		TagActivity tagActivity = TagActivity.findByName(activity);
		if(tagActivity!=null){
			Query q = JPA.em().createNativeQuery("select * from TagCategory c where c.tagActivity_id=" + tagActivity.id + " and c.name=\"" + categoryName +"\"" , TagCategory.class);
			TagCategory m = null;
			if(q.getResultList().size()>0)
				m = (TagCategory) q.getResultList().get(0);	
			return m; 
		}
		else 
			return null;
	}

	public static List<TagCategory> findByActivity(String activity) {
		TagActivity tagActivity = TagActivity.findByName(activity);
		if(tagActivity!=null){
			Query q = JPA.em().createNativeQuery("select * from TagCategory c where c.tagActivity_id=" + tagActivity.id , TagCategory.class);
			return (List<TagCategory>)q.getResultList();	
		}
		else 
			return null;
	}

	public static TagCategory findByTag(Tag selectedTag) {
		List<TagCategory> listAllCategory = null;
		Query q = JPA.em().createNativeQuery("select * from TagCategory c ", TagCategory.class);
		listAllCategory = (List<TagCategory>)q.getResultList();
		if(listAllCategory!=null && listAllCategory.size()>0){
			for (TagCategory tagCategory : listAllCategory) {
				if(tagCategory.tags.contains(selectedTag))
					return tagCategory;
			}
		}
		
		return null;
		
	}
	

}
