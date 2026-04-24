package com.gout.security;

import com.gout.dao.UserRepository;
import com.gout.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        // CRIT-001: 탈퇴(DELETED) 또는 정지(SUSPENDED) 사용자는 남아있는 JWT 로 접근 불가.
        if (user.getStatus() == User.Status.DELETED) {
            throw new UsernameNotFoundException("User withdrawn: " + userId);
        }
        if (user.getStatus() == User.Status.SUSPENDED) {
            throw new UsernameNotFoundException("계정이 정지되었습니다: " + userId);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getId(),
                user.getPassword() != null ? user.getPassword() : "",
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
