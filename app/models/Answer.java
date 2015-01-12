package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Query;

import com.itextpdf.text.pdf.PdfStructTreeController.returnType;

import play.db.jpa.JPA;
import play.db.jpa.Model;

@Entity
public class Answer extends Model {

	@ManyToOne
	public Apply apply;
	
	@ManyToOne
	public OutdoorActivity oa;
	
	@ManyToOne
	public Examine examine;
	
	public String ans=""; //學生的回答,該次OA的課後評量答案(以##分割 eg: ##true##false##A##B##xxxx##)
	
	 //作答時間
	public String responseTime;
	 
	 
	 
	public Boolean do_response = false; 
	public Boolean do_comment = false; 
	
	public int score;
	//問答題的分數
	public int bonusScore;
	
	//批改時間
	//老師的回答,該次OA的課後評量答案(以##分割[,questionId,commentTime] eg: ,1,2012-xx-xx xxxx##,5,2012-xx-xx xxxx##,2,2012-xx-xx xxxx##)
	@Lob
	public String commentTime=",";
	
	//老師的回答,該次OA的課後評量答案(以##分割[,questionId,comment] eg: ,1,HI##,5,hello##,2,厲害##,8,xxxx##)
	@Lob
	public String teacher_comment=",";
	
	public static int getCurrTakerCount(Long oaid, Long examineId) {
		int s = 0;
		Query q = JPA.em().createNativeQuery("select count(*) from Answer a where a.examine_id=" + examineId + " and a.oa_id=" + oaid);
		if(q.getResultList().size()>0)
			s = Integer.parseInt(String.valueOf(q.getResultList().get(0)));
		return s;
	}
	
	//待閱卷(老師尚未批改完成)
	public static int getUnCommentCount(Long oaid, Long examineId) {
		int count = 0;
		Query q = JPA.em().createNativeQuery("select count(*) from Answer a where a.examine_id=" + examineId + " and a.oa_id=" + oaid + " and a.do_comment=0");
		if(q.getResultList().size()>0)
			count = Integer.parseInt(String.valueOf(q.getResultList().get(0)));
		return count;
	}

	public static Answer findAnsByExamAndApply(Long examId, Long applyId) {		
		Answer ans = null;
		Query q = JPA.em().createNativeQuery("select * from Answer a where a.examine_id=" + examId + " and a.apply_id=" + applyId, Answer.class);
		if(q.getResultList().size()>0)
			ans = (Answer)q.getResultList().get(0);
		return ans;
	}

	public static List<Answer> findAnsByExam(Long id) {
		List<Answer> ans = null;
		Query q = JPA.em().createNativeQuery("select * from Answer a where a.examine_id=" + id , Answer.class);
		if(q.getResultList().size()>0)
			ans = q.getResultList();
		return ans;
	}

	public static List<Answer> findAnsByCreator(Long id) {
		List<Apply> applies = Apply.findApplyByAttendantMember(id);
		List<Answer> ans = new ArrayList<Answer>();
		for (Apply apply : applies) {
			Query q = JPA.em().createNativeQuery("select * from Answer a where a.apply_id=" + apply.id , Answer.class);
			if(q.getResultList().size()>0)
				ans.addAll(q.getResultList());
		}
		return ans;
	}

	public static List<Answer> findAnswerByOa(Long oaId) {
		List<Apply> listApply = Apply.findApplyByOa(oaId);
		List<Answer> listAnswer = new ArrayList<Answer>();
		for (Apply apply : listApply) {
			List<Answer> ans = Answer.findAnsByApply(apply.id);
			if(ans!=null)
				listAnswer.addAll(ans);
		}
		return listAnswer;
		
	}

	public static List<Answer> findAnsByApply(Long applyId) {
		List<Answer> ans = null;
		Query q = JPA.em().createNativeQuery("select * from Answer a where a.apply_id=" + applyId , Answer.class);
		if(q.getResultList().size()>0)
			ans = q.getResultList();
		return ans;
	}
}
