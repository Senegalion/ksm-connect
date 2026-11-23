package com.ksmconnect.security;

import com.ksmconnect.user.EmailAddresses;
import com.ksmconnect.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KsmUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public KsmUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmail(EmailAddresses.normalize(email))
                .map(AuthenticatedUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown account"));
    }
}
