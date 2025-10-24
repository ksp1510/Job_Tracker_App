package com.jobtracker.config;

import com.jobtracker.model.User;
import com.jobtracker.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract user info from Google
        String email = oAuth2User.getAttribute("email");
        String givenName = oAuth2User.getAttribute("given_name");
        String familyName = oAuth2User.getAttribute("family_name");
        String name = oAuth2User.getAttribute("name");

        // Parse names if not provided separately
        if (givenName == null && name != null) {
            String[] names = name.split(" ", 2);
            givenName = names[0];
            familyName = names.length > 1 ? names[1] : "";
        }

        // Find or create user
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            // Create new user
            user = User.builder()
                    .email(email)
                    .firstName(givenName != null ? givenName : "User")
                    .lastName(familyName != null ? familyName : "")
                    .authProvider("GOOGLE")
                    .build();
            user = userRepository.save(user);
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUserId());

        // Redirect to frontend with token
        String redirectUrl = String.format("http://localhost:3000/auth/oauth2/callback?token=%s&firstName=%s&lastName=%s",
                token, user.getFirstName(), user.getLastName());

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
