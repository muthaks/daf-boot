package za.co.rmb.approvalforumdrools.security.filter;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

public final class AuthenticatedUserDetails implements Principal {

    private final String username;
    private final Set<String> roles;

    public AuthenticatedUserDetails(String username, Set<String> roles) {
        this.username = username;
        this.roles = Collections.unmodifiableSet(roles);
    }

    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public String getName() {
        return username;
    }

}