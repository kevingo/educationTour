package controllers;

import java.util.ArrayList;

import Utility.ROLE;
import models.Member;
import models.deadbolt.RoleHolder;
import models.rolesManagement.GuestHolder;
import models.rolesManagement.MemberHolder;
import models.rolesManagement.PlatformOwnerHolder;
import models.rolesManagement.Q_AdminHolder;
import models.rolesManagement.Q_ShowPieceHolder;
import models.rolesManagement.Q_TeacherHolder;
import models.rolesManagement.Q_VolunteerHolder;
import models.rolesManagement.StudentHolder;
import models.rolesManagement.TeachingPlannerHolder;
import play.mvc.Controller;

import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.DeadboltHandler;
import controllers.deadbolt.ExternalizedRestrictionsAccessor;
import controllers.deadbolt.RestrictedResourcesHandler;

public class MyRoleHandler extends Controller implements DeadboltHandler {

	String roleType = "";
	String quanta_admin_email = "quanta@gmail.com";
	
	@Override
	public void beforeRoleCheck() {

		if(session.contains("email")) {
			String email = session.get("email");
			Member user = Member.findUser(email);
			
			if(user!=null){
				if(null == user.roleList) {
					user.roleList = new ArrayList();
					user.save();
				}
				
				if(user.checkRole(ROLE.ADMIN))
					roleType = ROLE.ADMIN;
				else if(user.checkRole(ROLE.TEACHINGPLANNER))
					roleType = ROLE.TEACHINGPLANNER;
				else if(user.checkRole(ROLE.STUDENT))
					roleType = ROLE.STUDENT;
				else if(user.checkRole(ROLE.PARENT))
					roleType = ROLE.PARENT;
				else if(user.checkRole(ROLE.Q_ADMIN))
					roleType = ROLE.Q_ADMIN;
				else if(user.checkRole(ROLE.Q_SHOWPIECE))
					roleType = ROLE.Q_SHOWPIECE;
				else if(user.checkRole(ROLE.Q_TEACHER))
					roleType = ROLE.Q_TEACHER;
				else if(user.checkRole(ROLE.Q_VOLUNTEER))
					roleType = ROLE.Q_VOLUNTEER;
				else 
					roleType = ROLE.GUEST;	
			}
			
		} 
		else 
			roleType = ROLE.GUEST;		
	}

	@Override
	public RoleHolder getRoleHolder() {

		if(roleType.equals(ROLE.ADMIN))
			return new PlatformOwnerHolder();
		else if(roleType.equals(ROLE.TEACHINGPLANNER))
			return new TeachingPlannerHolder();
		else if(roleType.equals(ROLE.PARENT))
			return new MemberHolder();
		else if(roleType.equals(ROLE.STUDENT))
			return new StudentHolder();
		else if(roleType.equals(ROLE.GUEST))
			return new GuestHolder();
		else if(roleType.equals(ROLE.Q_ADMIN))
			return new Q_AdminHolder();
		else if(roleType.equals(ROLE.Q_TEACHER))
			return new Q_TeacherHolder();
		else if(roleType.equals(ROLE.Q_SHOWPIECE))
			return new Q_ShowPieceHolder();
		else if(roleType.equals(ROLE.Q_VOLUNTEER))
			return new Q_VolunteerHolder();
		else
			return null;
	}
	

	@Override
	public void onAccessFailure(String controllerClassName) {
		Deadbolt.forbidden();
	}

	@Override
	public ExternalizedRestrictionsAccessor getExternalizedRestrictionsAccessor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RestrictedResourcesHandler getRestrictedResourcesHandler() {
		// TODO Auto-generated method stub
		return null;
	}

}
