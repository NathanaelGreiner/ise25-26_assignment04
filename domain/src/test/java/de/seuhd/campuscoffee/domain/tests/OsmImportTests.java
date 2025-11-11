package de.seuhd.campuscoffee.domain.tests;

import de.seuhd.campuscoffee.domain.exceptions.OsmNodeMissingFieldsException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import de.seuhd.campuscoffee.domain.ports.PosDataService;
import de.seuhd.campuscoffee.domain.impl.PosServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the OSM import feature.
 * Tests the conversion of OpenStreetMap data to POS domain objects.
 */
@ExtendWith(MockitoExtension.class)
public class OsmImportTests {

    @Mock
    private PosDataService posDataService;

    @Mock
    private OsmDataService osmDataService;

    private PosServiceImpl posService;

    @BeforeEach
    void setUp() {
        posService = new PosServiceImpl(posDataService, osmDataService);
    }

    @Test
    void testImportOsmNodeRadaCoffee() {
        // Arrange - Create OSM node for Rada Coffee & Rösterei (node 5589879349)
        Map<String, String> tags = new HashMap<>();
        tags.put("name", "Rada Coffee & Rösterei");
        tags.put("amenity", "cafe");
        tags.put("addr:street", "Untere Straße");
        tags.put("addr:housenumber", "21");
        tags.put("addr:postcode", "69117");
        tags.put("addr:city", "Heidelberg");
        tags.put("description", "Caffé und Rösterei");

        OsmNode osmNode = OsmNode.builder()
                .nodeId(5589879349L)
                .tags(tags)
                .build();

        // Mock the OSM data service to return the node
        when(osmDataService.fetchNode(5589879349L)).thenReturn(osmNode);

        // Mock the POS data service to persist the POS
        Pos persistedPos = Pos.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .name("Rada Coffee & Rösterei")
                .description("Caffé und Rösterei")
                .type(PosType.CAFE)
                .campus(CampusType.ALTSTADT)
                .street("Untere Straße")
                .houseNumber("21")
                .postalCode(69117)
                .city("Heidelberg")
                .build();

        when(posDataService.upsert(any(Pos.class))).thenReturn(persistedPos);

        // Act
        Pos importedPos = posService.importFromOsmNode(5589879349L);

        // Assert
        assertThat(importedPos)
                .isNotNull()
                .satisfies(pos -> {
                    assertThat(pos.id()).isEqualTo(1L);
                    assertThat(pos.name()).isEqualTo("Rada Coffee & Rösterei");
                    assertThat(pos.description()).isEqualTo("Caffé und Rösterei");
                    assertThat(pos.type()).isEqualTo(PosType.CAFE);
                    assertThat(pos.street()).isEqualTo("Untere Straße");
                    assertThat(pos.houseNumber()).isEqualTo("21");
                    assertThat(pos.postalCode()).isEqualTo(69117);
                    assertThat(pos.city()).isEqualTo("Heidelberg");
                    assertThat(pos.campus()).isEqualTo(CampusType.ALTSTADT);
                });

        // Verify that the data services were called
        verify(osmDataService, times(1)).fetchNode(5589879349L);
        verify(posDataService, times(1)).upsert(any(Pos.class));
    }

    @Test
    void testImportOsmNodeMissingName() {
        // Arrange - Create OSM node without name
        Map<String, String> tags = new HashMap<>();
        tags.put("amenity", "cafe");
        tags.put("addr:street", "Untere Straße");
        tags.put("addr:housenumber", "21");
        tags.put("addr:postcode", "69117");
        tags.put("addr:city", "Heidelberg");

        OsmNode osmNode = OsmNode.builder()
                .nodeId(123456L)
                .tags(tags)
                .build();

        when(osmDataService.fetchNode(123456L)).thenReturn(osmNode);

        // Act & Assert
        assertThatThrownBy(() -> posService.importFromOsmNode(123456L))
                .isInstanceOf(OsmNodeMissingFieldsException.class);

        verify(posDataService, never()).upsert(any());
    }

    @Test
    void testImportOsmNodeMissingAddress() {
        // Arrange - Create OSM node without complete address
        Map<String, String> tags = new HashMap<>();
        tags.put("name", "Test Cafe");
        tags.put("amenity", "cafe");
        // Missing address fields
        tags.put("addr:city", "Heidelberg");

        OsmNode osmNode = OsmNode.builder()
                .nodeId(123456L)
                .tags(tags)
                .build();

        when(osmDataService.fetchNode(123456L)).thenReturn(osmNode);

        // Act & Assert
        assertThatThrownBy(() -> posService.importFromOsmNode(123456L))
                .isInstanceOf(OsmNodeMissingFieldsException.class);

        verify(posDataService, never()).upsert(any());
    }

    @Test
    void testImportOsmNodeInvalidPostalCode() {
        // Arrange - Create OSM node with invalid postal code
        Map<String, String> tags = new HashMap<>();
        tags.put("name", "Test Cafe");
        tags.put("amenity", "cafe");
        tags.put("addr:street", "Untere Straße");
        tags.put("addr:housenumber", "21");
        tags.put("addr:postcode", "invalid"); // Invalid postal code
        tags.put("addr:city", "Heidelberg");

        OsmNode osmNode = OsmNode.builder()
                .nodeId(123456L)
                .tags(tags)
                .build();

        when(osmDataService.fetchNode(123456L)).thenReturn(osmNode);

        // Act & Assert
        assertThatThrownBy(() -> posService.importFromOsmNode(123456L))
                .isInstanceOf(OsmNodeMissingFieldsException.class);

        verify(posDataService, never()).upsert(any());
    }

    @Test
    void testImportOsmNodeDetectsBakery() {
        // Arrange - Create OSM node for a bakery
        Map<String, String> tags = new HashMap<>();
        tags.put("name", "Test Bakery");
        tags.put("amenity", "bakery");
        tags.put("addr:street", "Main Street");
        tags.put("addr:housenumber", "10");
        tags.put("addr:postcode", "69115");
        tags.put("addr:city", "Heidelberg");

        OsmNode osmNode = OsmNode.builder()
                .nodeId(999999L)
                .tags(tags)
                .build();

        when(osmDataService.fetchNode(999999L)).thenReturn(osmNode);

        Pos persistedPos = Pos.builder()
                .id(2L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .name("Test Bakery")
                .description("bakery")
                .type(PosType.BAKERY)
                .campus(CampusType.ALTSTADT)
                .street("Main Street")
                .houseNumber("10")
                .postalCode(69115)
                .city("Heidelberg")
                .build();

        when(posDataService.upsert(any(Pos.class))).thenReturn(persistedPos);

        // Act
        Pos importedPos = posService.importFromOsmNode(999999L);

        // Assert - Should detect as BAKERY type
        assertThat(importedPos.type()).isEqualTo(PosType.BAKERY);
    }

    @Test
    void testOsmNodeTagHelpers() {
        // Arrange
        Map<String, String> tags = new HashMap<>();
        tags.put("name", "Test");
        tags.put("description", "Test Description");

        OsmNode osmNode = OsmNode.builder()
                .nodeId(1L)
                .tags(tags)
                .build();

        // Assert - Test convenience methods
        assertThat(osmNode.getTag("name")).isEqualTo("Test");
        assertThat(osmNode.getTag("description")).isEqualTo("Test Description");
        assertThat(osmNode.getTag("nonexistent")).isNull();
        
        assertThat(osmNode.hasTag("name")).isTrue();
        assertThat(osmNode.hasTag("nonexistent")).isFalse();
    }
}

