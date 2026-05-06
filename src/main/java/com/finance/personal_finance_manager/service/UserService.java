package com.finance.personal_finance_manager.service;

import com.finance.personal_finance_manager.model.PasswordResetToken;
import com.finance.personal_finance_manager.model.User;
import com.finance.personal_finance_manager.repository.PasswordResetTokenRepository;
import com.finance.personal_finance_manager.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Các method cũ giữ nguyên
    public String registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            return "Lỗi: Tên đăng nhập đã tồn tại!";
        }
        // Mã hóa mật khẩu trước khi lưu
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return "Đăng ký thành công!";
    }

    public Optional<User> login(String loginInput, String password) {
        // Kiểm tra xem loginInput có phải email không
        Optional<User> userOpt = userRepository.findByEmail(loginInput);
        if (userOpt.isEmpty()) {
            // Nếu không phải email, tìm theo username
            userOpt = userRepository.findByUsername(loginInput);
        }
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    // Xác thực Google với token thật
    public Optional<User> authenticateGoogle(String idTokenString) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList("923508787768-tirtvocpu20jrba6khna61ppbqjv3idj.apps.googleusercontent.com"))
                .build();
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                User user = userRepository.findByUsername(email).orElse(null);
                if (user == null) {
                    user = new User();
                    user.setUsername(email);
                    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Mã hóa
                    user.setEmail(email);
                    user.setFullName(name);
                    user = userRepository.save(user);
                }
                return Optional.of(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(oldPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private JavaMailSender mailSender;

    // Phương thức tạo token reset password
    @Transactional
    public String createPasswordResetToken(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return null;
        User user = userOpt.get();
        passwordResetTokenRepository.deleteByUser(user);
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(null, token, user, LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(resetToken);
        return token;
    }

    // Gửi email (giả lập in ra console, có thể tích hợp thật)
    public void sendPasswordResetEmail(String email, String token) {
        String resetLink = "http://localhost:5173/reset-password?token=" + token;
        System.out.println("Link đặt lại mật khẩu: " + resetLink);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Đặt lại mật khẩu Finance Manager");
            message.setText("Click vào link để đặt lại mật khẩu: " + resetLink);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Gửi email thất bại: " + e.getMessage());
            // Không throw exception
        }
    }

    // Xác nhận token và đặt lại mật khẩu
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || tokenOpt.get().getExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }
        User user = tokenOpt.get().getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.delete(tokenOpt.get());
        return true;
    }
}