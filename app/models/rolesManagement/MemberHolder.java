package models.rolesManagement;

import java.util.Arrays;
import java.util.List;

import Utility.ROLE;

import models.deadbolt.Role;
import models.deadbolt.RoleHolder;

public class MemberHolder implements RoleHolder {

	@Override
	public List<? extends Role> getRoles() {
		return Arrays.asList(
				new MyRole(ROLE.PARENT),
				new MyRole(ROLE.GUEST)
		);
	}

}
