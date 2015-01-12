package models.rolesManagement;

import java.util.Arrays;
import java.util.List;

import Utility.ROLE;

import models.deadbolt.Role;
import models.deadbolt.RoleHolder;

public class PlatformOwnerHolder implements RoleHolder {

	@Override
	public List<? extends Role> getRoles() {
		return Arrays.asList(
				new MyRole(ROLE.Q_ADMIN),
				new MyRole(ROLE.Q_SHOWPIECE),
				new MyRole(ROLE.Q_TEACHER),
				new MyRole(ROLE.Q_VOLUNTEER),
				new MyRole(ROLE.Q_USER),
				new MyRole(ROLE.ADMIN),
				new MyRole(ROLE.PARENT),
				//new MyRole(ROLE.STUDENT),
				new MyRole(ROLE.TEACHINGPLANNER),
				new MyRole(ROLE.GUEST)
				
		);
	}

}
