package com.referralcrm.api;

import com.referralcrm.domain.AppUser;
import com.referralcrm.repository.AppUserRepository;
import com.referralcrm.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
    private final AppUserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthController(AppUserRepository users, PasswordEncoder encoder, JwtService jwt) { this.users=users; this.encoder=encoder; this.jwt=jwt; }
    public record RegisterRequest(@NotBlank @Size(max=160) String fullName, @NotBlank @Email @Size(max=320) String email, @NotBlank @Size(min=12,max=72) String password) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> register(@Valid @RequestBody RegisterRequest r) {
        String email=r.email().trim().toLowerCase(Locale.ROOT);
        if(users.existsByEmail(email)) throw new ApiException(HttpStatus.CONFLICT,"An account with this email already exists");
        AppUser u=new AppUser(); u.setEmail(email); u.setFullName(r.fullName().trim()); u.setPasswordHash(encoder.encode(r.password())); users.save(u); return token(u);
    }
    @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody LoginRequest r) {
        AppUser u=users.findByEmailAndDeletedAtIsNull(r.email().trim().toLowerCase(Locale.ROOT)).orElseThrow(() -> new BadCredentialsException("Email or password is incorrect"));
        if(!encoder.matches(r.password(),u.getPasswordHash())) throw new BadCredentialsException("Invalid credentials"); return token(u);
    }
    @GetMapping("/me") public Map<String,Object> me(Authentication authentication) {
        UUID userId=(UUID)authentication.getPrincipal();
        AppUser u=users.findById(userId).filter(x->x.getDeletedAt()==null).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Session is no longer valid"));
        return Map.of("id",u.getId(),"email",u.getEmail(),"fullName",u.getFullName());
    }
    private Map<String,Object> token(AppUser u) { return Map.of("accessToken",jwt.issue(u.getId(),u.getEmail()),"tokenType","Bearer","expiresInSeconds",jwt.expirationSeconds(),"user",Map.of("id",u.getId(),"email",u.getEmail(),"fullName",u.getFullName())); }
}
