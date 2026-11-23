package com.ksmconnect.user;

import com.ksmconnect.security.AuthenticatedUser;
import com.ksmconnect.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/me")
class ProfileController {

    private final ProfileService profileService;

    ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    record ProfileView(UUID id, String email, String firstName, String lastName, String phoneNumber, Set<Role> roles) {
        static ProfileView of(User user) {
            return new ProfileView(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                    user.getPhoneNumber(), user.getRoles());
        }
    }

    record ProfileUpdate(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Size(max = 30) String phoneNumber) {
    }

    record AccountDeletion(@NotBlank @Size(max = 256) String password) {
        @Override
        public String toString() {
            return "AccountDeletion[redacted]";
        }
    }

    @GetMapping
    ProfileView profile(@CurrentUser AuthenticatedUser principal) {
        return ProfileView.of(profileService.get(principal.id()));
    }

    @PutMapping
    ProfileView update(@CurrentUser AuthenticatedUser principal, @Valid @RequestBody ProfileUpdate request) {
        return ProfileView.of(profileService.update(principal.id(),
                new PersonName(request.firstName(), request.lastName()), request.phoneNumber()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentUser AuthenticatedUser principal, @Valid @RequestBody AccountDeletion request,
                HttpServletRequest httpRequest) {
        profileService.delete(principal.id(), request.password());
        httpRequest.getSession().invalidate();
    }
}
