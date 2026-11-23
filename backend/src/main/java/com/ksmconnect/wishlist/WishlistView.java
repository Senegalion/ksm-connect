package com.ksmconnect.wishlist;

import java.time.Instant;
import java.util.List;

public record WishlistView(boolean editable, int maxItems, int filledCount, List<Item> items) {

    public static final int MAX_ITEMS = 3;

    public record Item(int position, SuggestionType type, String url, String description, Instant updatedAt) {
        static Item of(GiftSuggestion suggestion) {
            return new Item(suggestion.getPosition(), suggestion.getType(), suggestion.getUrl(),
                    suggestion.getDescription(), suggestion.getUpdatedAt());
        }
    }

    static WishlistView of(boolean editable, List<GiftSuggestion> suggestions) {
        List<Item> items = suggestions.stream().map(Item::of).toList();
        return new WishlistView(editable, MAX_ITEMS, items.size(), items);
    }
}
