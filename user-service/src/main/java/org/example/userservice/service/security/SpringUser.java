package org.example.userservice.service.security;

import lombok.Getter;
import org.example.userservice.entity.User;
import org.example.userservice.entity.UserType;
import org.springframework.security.core.authority.AuthorityUtils;

@Getter
public class SpringUser extends org.springframework.security.core.userdetails.User {

    private final User user;

    @Override
    public boolean isEnabled() {
        return user.getUserType() == UserType.ACTIVE || user.getUserType() == UserType.ADMIN;
    }

    public SpringUser(User user) {
        super(user.getUsername(),
                user.getPassword(),
                AuthorityUtils.createAuthorityList(user.getUserType().name()));
        this.user = user;
    }
}