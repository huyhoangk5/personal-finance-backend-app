package com.finance.personal_finance_manager.controller;

import com.finance.personal_finance_manager.model.User;
import com.finance.personal_finance_manager.repository.UserRepository;
import com.finance.personal_finance_manager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody User user) {
        if (user.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body("Mật khẩu phải ít nhất 6 ký tự");
        }
        String result = userService.registerUser(user);
        if (result.contains("Lỗi")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        System.out.println("Login called with: " + credentials);
        String username = credentials.get("username");
        String password = credentials.get("password");
        Optional<User> user = userService.login(username, password);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        } else {
            return ResponseEntity.status(401).body("Sai tên đăng nhập hoặc mật khẩu!");
        }
    }

    @Autowired
    UserRepository userRepository;

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable Long userId, @RequestBody User userDetails) {
        Optional<User> optionalUser = userService.findById(userId);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = optionalUser.get();
        if (userDetails.getFullName() != null) user.setFullName(userDetails.getFullName());
        if (userDetails.getEmail() != null) {
            // Kiểm tra email đã tồn tại chưa (trừ chính user này)
            Optional<User> existingEmail = userRepository.findByEmail(userDetails.getEmail());
            if (existingEmail.isPresent() && !existingEmail.get().getUserId().equals(userId)) {
                return ResponseEntity.badRequest().body(null); // Hoặc trả về lỗi cụ thể
            }
            user.setEmail(userDetails.getEmail());
        }
        User updatedUser = userService.updateUser(user);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        Optional<User> userOpt = userService.authenticateGoogle(token);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(userOpt.get());
        } else {
            return ResponseEntity.status(401).body("Xác thực Google thất bại");
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> payload) {
        String userIdStr = payload.get("userId");
        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");
        if (userIdStr == null || oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Thiếu thông tin bắt buộc");
        }
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("Mật khẩu mới phải ít nhất 6 ký tự");
        }
        Long userId = Long.parseLong(userIdStr);
        boolean changed = userService.changePassword(userId, oldPassword, newPassword);
        if (changed) {
            return ResponseEntity.ok("Đổi mật khẩu thành công");
        } else {
            return ResponseEntity.status(400).body("Mật khẩu cũ không đúng hoặc user không tồn tại");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.isBlank() || !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            return ResponseEntity.badRequest().body("Email không hợp lệ");
        }
        String token = userService.createPasswordResetToken(email);
        if (token == null) {
            return ResponseEntity.badRequest().body("Email không tồn tại trong hệ thống");
        }
        userService.sendPasswordResetEmail(email, token);
        return ResponseEntity.ok("Email đặt lại mật khẩu đã được gửi");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        String newPassword = payload.get("newPassword");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("Token không hợp lệ");
        }
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("Mật khẩu mới phải ít nhất 6 ký tự");
        }
        boolean success = userService.resetPassword(token, newPassword);
        if (success) {
            return ResponseEntity.ok("Đặt lại mật khẩu thành công");
        } else {
            return ResponseEntity.badRequest().body("Token không hợp lệ hoặc đã hết hạn");
        }
    }
}