package com.ksmconnect.wishlist;

import com.ksmconnect.security.AuthenticatedUser;
import com.ksmconnect.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/wishlist")
class WishlistController {

    private final WishlistService wishlistService;

    WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    record SuggestionRequest(
            @NotNull SuggestionType type,
            @Size(max = SuggestionContent.MAX_URL_LENGTH) String url,
            @Size(max = SuggestionContent.MAX_DESCRIPTION_LENGTH) String description) {
    }

    @GetMapping
    WishlistView myWishlist(@CurrentUser AuthenticatedUser principal, @PathVariable UUID eventId) {
        return wishlistService.myWishlist(eventId, principal.id());
    }

    @PutMapping("/{position}")
    WishlistView save(@CurrentUser AuthenticatedUser principal, @PathVariable UUID eventId,
                      @PathVariable int position, @Valid @RequestBody SuggestionRequest request) {
        return wishlistService.save(eventId, principal.id(), position, request.type(), request.url(),
                request.description());
    }

    @DeleteMapping("/{position}")
    WishlistView remove(@CurrentUser AuthenticatedUser principal, @PathVariable UUID eventId,
                        @PathVariable int position) {
        return wishlistService.remove(eventId, principal.id(), position);
    }
}
