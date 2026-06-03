package org.example.userservice.service.security;

import lombok.RequiredArgsConstructor;
import org.example.userservice.repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(SpringUser::new)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));
    }
}