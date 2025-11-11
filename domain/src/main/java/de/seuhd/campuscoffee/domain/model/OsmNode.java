package de.seuhd.campuscoffee.domain.model;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an OpenStreetMap node with relevant Point of Sale information.
 * This is the domain model for OSM data before it is converted to a POS object.
 *
 * @param nodeId The OpenStreetMap node ID.
 * @param tags A map of OSM tags (name, address:street, address:housenumber, etc.)
 */
@Builder(toBuilder = true)
public record OsmNode(
        @NonNull Long nodeId,
        @NonNull Map<String, String> tags
) {
    public OsmNode {
        // Defensive copy to ensure immutability
        tags = new HashMap<>(tags);
    }

    /**
     * Convenience method to get a tag value by key.
     *
     * @param key the tag key
     * @return the tag value, or null if not present
     */
    @Nullable
    public String getTag(String key) {
        return tags.get(key);
    }

    /**
     * Convenience method to check if a tag exists.
     *
     * @param key the tag key
     * @return true if the tag exists, false otherwise
     */
    public boolean hasTag(String key) {
        return tags.containsKey(key);
    }
}
