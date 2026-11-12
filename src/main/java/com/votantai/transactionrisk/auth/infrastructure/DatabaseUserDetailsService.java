package com.votantai.transactionrisk.auth.infrastructure;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.votantai.transactionrisk.auth.application.UsernameNormalizer;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var account = userRepository.findByUsername(UsernameNormalizer.normalize(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.withUsername(account.username())
                .password(account.passwordHash())
                .disabled(!account.enabled())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name())))
                .build();
    }
}
