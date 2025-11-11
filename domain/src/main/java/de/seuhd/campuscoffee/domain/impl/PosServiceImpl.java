package de.seuhd.campuscoffee.domain.impl;

import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeMissingFieldsException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import de.seuhd.campuscoffee.domain.ports.PosDataService;
import de.seuhd.campuscoffee.domain.ports.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Implementation of the POS service that handles business logic related to POS entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosServiceImpl implements PosService {
    private final PosDataService posDataService;
    private final OsmDataService osmDataService;

    @Override
    public void clear() {
        log.warn("Clearing all POS data");
        posDataService.clear();
    }

    @Override
    public @NonNull List<Pos> getAll() {
        log.debug("Retrieving all POS");
        return posDataService.getAll();
    }

    @Override
    public @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException {
        log.debug("Retrieving POS with ID: {}", id);
        return posDataService.getById(id);
    }

    @Override
    public @NonNull Pos upsert(@NonNull Pos pos) throws PosNotFoundException {
        if (pos.id() == null) {
            // Create new POS
            log.info("Creating new POS: {}", pos.name());
            return performUpsert(pos);
        } else {
            // Update existing POS
            log.info("Updating POS with ID: {}", pos.id());
            // POS ID must be set
            Objects.requireNonNull(pos.id());
            // POS must exist in the database before the update
            posDataService.getById(pos.id());
            return performUpsert(pos);
        }
    }

    @Override
    public @NonNull Pos importFromOsmNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Importing POS from OpenStreetMap node {}...", nodeId);

        // Fetch the OSM node data using the port
        OsmNode osmNode = osmDataService.fetchNode(nodeId);

        // Convert OSM node to POS domain object and upsert it
        // TODO: Implement the actual conversion (the response is currently hard-coded).
        Pos savedPos = upsert(convertOsmNodeToPos(osmNode));
        log.info("Successfully imported POS '{}' from OSM node {}", savedPos.name(), nodeId);

        return savedPos;
    }

    /**
     * Converts an OSM node to a POS domain object.
     * Extracts relevant fields from OSM tags and maps them to POS attributes.
     * Uses intelligent defaults for missing fields.
     *
     * @param osmNode the OSM node with tags
     * @return a new POS object with data extracted from OSM tags
     * @throws OsmNodeMissingFieldsException if required fields are missing
     */
    private @NonNull Pos convertOsmNodeToPos(@NonNull OsmNode osmNode) throws OsmNodeMissingFieldsException {
        // Extract required fields
        String name = osmNode.getTag("name");
        if (name == null || name.isBlank()) {
            log.warn("OSM node {} missing required 'name' tag", osmNode.nodeId());
            throw new OsmNodeMissingFieldsException(osmNode.nodeId());
        }

        // Extract address components
        String street = osmNode.getTag("addr:street");
        String houseNumber = osmNode.getTag("addr:housenumber");
        String postalCode = osmNode.getTag("addr:postcode");
        String city = osmNode.getTag("addr:city");

        // Validate critical address fields
        if (street == null || street.isBlank() ||
            houseNumber == null || houseNumber.isBlank() ||
            postalCode == null || postalCode.isBlank() ||
            city == null || city.isBlank()) {
            log.warn("OSM node {} missing required address fields", osmNode.nodeId());
            throw new OsmNodeMissingFieldsException(osmNode.nodeId());
        }

        // Extract optional fields with defaults
        String description = osmNode.getTag("description");
        if (description == null || description.isBlank()) {
            description = osmNode.getTag("amenity");
            if (description == null || description.isBlank()) {
                description = name; // Use name as fallback
            }
        }

        // Determine POS type from OSM tags
        PosType posType = determinePosType(osmNode);

        // Determine campus type from address or default to ALTSTADT
        CampusType campusType = determineCampusType(city, osmNode);

        // Parse postal code
        Integer postalCodeInt;
        try {
            postalCodeInt = Integer.parseInt(postalCode);
        } catch (NumberFormatException e) {
            log.warn("OSM node {} has invalid postal code: {}", osmNode.nodeId(), postalCode);
            throw new OsmNodeMissingFieldsException(osmNode.nodeId());
        }

        log.debug("Successfully converted OSM node {} to POS: name='{}', street='{}', city='{}'",
                osmNode.nodeId(), name, street, city);

        return Pos.builder()
                .name(name)
                .description(description)
                .type(posType)
                .campus(campusType)
                .street(street)
                .houseNumber(houseNumber)
                .postalCode(postalCodeInt)
                .city(city)
                .build();
    }

    /**
     * Determines the POS type from OSM tags.
     * Maps OSM amenity values to POS types.
     *
     * @param osmNode the OSM node
     * @return the determined POS type, or CAFE as default
     */
    private PosType determinePosType(@NonNull OsmNode osmNode) {
        String amenity = osmNode.getTag("amenity");
        String shop = osmNode.getTag("shop");

        if (amenity != null) {
            return switch (amenity.toLowerCase()) {
                case "cafe" -> PosType.CAFE;
                case "coffee" -> PosType.CAFE;
                case "bakery" -> PosType.BAKERY;
                case "vending_machine" -> PosType.VENDING_MACHINE;
                case "fast_food" -> PosType.CAFETERIA;
                case "restaurant" -> PosType.CAFETERIA;
                default -> PosType.CAFE;
            };
        }

        if (shop != null && shop.toLowerCase().contains("bakery")) {
            return PosType.BAKERY;
        }

        // Default to CAFE for any coffee-related OSM entry
        return PosType.CAFE;
    }

    /**
     * Determines the campus type from city information and OSM tags.
     * Currently defaults to ALTSTADT for Heidelberg and uses other campus types for known regions.
     *
     * @param city the city name from OSM
     * @param osmNode the OSM node
     * @return the determined campus type
     */
    private CampusType determineCampusType(@NonNull String city, @NonNull OsmNode osmNode) {
        if (!city.equalsIgnoreCase("Heidelberg")) {
            return CampusType.ALTSTADT; // Default for non-Heidelberg cities
        }

        // For Heidelberg, check district or default to ALTSTADT
        String district = osmNode.getTag("addr:district");
        if (district != null) {
            return switch (district.toLowerCase()) {
                case "bergheim" -> CampusType.BERGHEIM;
                case "inf" -> CampusType.INF;
                case "neuenheim" -> CampusType.INF;
                default -> CampusType.ALTSTADT;
            };
        }

        return CampusType.ALTSTADT; // Default campus
    }

    /**
     * Performs the actual upsert operation with consistent error handling and logging.
     * Database constraint enforces name uniqueness - data layer will throw DuplicatePosNameException if violated.
     * JPA lifecycle callbacks (@PrePersist/@PreUpdate) set timestamps automatically.
     *
     * @param pos the POS to upsert
     * @return the persisted POS with updated ID and timestamps
     * @throws DuplicatePosNameException if a POS with the same name already exists
     */
    private @NonNull Pos performUpsert(@NonNull Pos pos) throws DuplicatePosNameException {
        try {
            Pos upsertedPos = posDataService.upsert(pos);
            log.info("Successfully upserted POS with ID: {}", upsertedPos.id());
            return upsertedPos;
        } catch (DuplicatePosNameException e) {
            log.error("Error upserting POS '{}': {}", pos.name(), e.getMessage());
            throw e;
        }
    }
}
