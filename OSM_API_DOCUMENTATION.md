# OSM Import API Documentation

## Endpoint: Import POS from OpenStreetMap

### Request
```http
POST /api/pos/import/osm/{nodeId}
Content-Type: application/json
```

### Path Parameters
| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `nodeId` | Long | OpenStreetMap node identifier | `5589879349` |

### Response (HTTP 201 Created)

**Success Response:**
```json
{
  "id": 1,
  "name": "Rada Coffee & Rösterei",
  "description": "Caffé und Rösterei",
  "type": "CAFE",
  "street": "Untere Straße",
  "houseNumber": "21",
  "postalCode": 69117,
  "city": "Heidelberg",
  "campus": "ALTSTADT",
  "createdAt": "2025-11-11T11:12:37Z",
  "updatedAt": "2025-11-11T11:12:37Z"
}
```

### HTTP Status Codes

| Status | Description | Reason |
|--------|-------------|--------|
| 201 | Created | POS successfully imported and persisted |
| 400 | Bad Request | OSM node missing required fields (name, address) |
| 404 | Not Found | OSM node with given ID not found or unreachable |
| 409 | Conflict | POS name already exists (duplicate) |
| 500 | Internal Server Error | Unexpected server error |

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Unique POS identifier in the system |
| `name` | String | Point of Sale name |
| `description` | String | Description or amenity type |
| `type` | String | POS type (CAFE, BAKERY, VENDING_MACHINE, CAFETERIA) |
| `street` | String | Street name |
| `houseNumber` | String | House number (may include suffix like "21a") |
| `postalCode` | Integer | Postal code |
| `city` | String | City name |
| `campus` | String | Campus location (ALTSTADT, BERGHEIM, INF) |
| `createdAt` | String | ISO 8601 timestamp of creation |
| `updatedAt` | String | ISO 8601 timestamp of last update |

## Examples

### Example 1: Successful Import

**Request:**
```bash
curl -X POST http://localhost:8080/api/pos/import/osm/5589879349 \
  -H "Content-Type: application/json"
```

**Response (201 Created):**
```json
{
  "id": 42,
  "name": "Rada Coffee & Rösterei",
  "description": "Caffé und Rösterei",
  "type": "CAFE",
  "street": "Untere Straße",
  "houseNumber": "21",
  "postalCode": 69117,
  "city": "Heidelberg",
  "campus": "ALTSTADT",
  "createdAt": "2025-11-11T11:30:00Z",
  "updatedAt": "2025-11-11T11:30:00Z"
}
```

### Example 2: OSM Node Not Found

**Request:**
```bash
curl -X POST http://localhost:8080/api/pos/import/osm/999999999 \
  -H "Content-Type: application/json"
```

**Response (404 Not Found):**
```json
{
  "timestamp": "2025-11-11T11:35:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "The OpenStreetMap node with ID 999999999 does not exist.",
  "path": "/api/pos/import/osm/999999999"
}
```

### Example 3: Missing Required Fields

**Request:**
```bash
curl -X POST http://localhost:8080/api/pos/import/osm/123456 \
  -H "Content-Type: application/json"
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2025-11-11T11:40:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "The OpenStreetMap node with ID 123456 does not have the required fields.",
  "path": "/api/pos/import/osm/123456"
}
```

### Example 4: Duplicate POS Name

**Request:**
```bash
curl -X POST http://localhost:8080/api/pos/import/osm/5589879349 \
  -H "Content-Type: application/json"
```

**Response (409 Conflict) - if POS already exists:**
```json
{
  "timestamp": "2025-11-11T11:45:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "A POS with the name 'Rada Coffee & Rösterei' already exists.",
  "path": "/api/pos/import/osm/5589879349"
}
```

## OpenStreetMap Data Requirements

For a successful import, the OSM node must contain the following tags:

### Required Tags
- `name`: Name of the location
- `addr:street`: Street name
- `addr:housenumber`: House number
- `addr:postcode`: Postal code (must be numeric)
- `addr:city`: City name

### Optional Tags (for enhanced data)
- `amenity`: Type of amenity (cafe, bakery, etc.)
- `description`: Description of the location
- `addr:district`: District (for campus type detection)

### Example OSM Node (XML)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<osm version="0.6" generator="CGImap">
  <node id="5589879349" changeset="123456" timestamp="2025-01-01T00:00:00Z" user="mapper" uid="12345">
    <tag k="name" v="Rada Coffee & Rösterei"/>
    <tag k="amenity" v="cafe"/>
    <tag k="addr:street" v="Untere Straße"/>
    <tag k="addr:housenumber" v="21"/>
    <tag k="addr:postcode" v="69117"/>
    <tag k="addr:city" v="Heidelberg"/>
    <tag k="description" v="Caffé und Rösterei"/>
    <tag k="addr:district" v="Altstadt"/>
  </node>
</osm>
```

## Business Rules

1. **Unique Names**: POS names must be unique. Attempting to import a duplicate name will result in a 409 Conflict error.

2. **Address Completeness**: All address components must be present and non-empty to successfully import.

3. **Valid Postal Code**: The postal code must be numeric and parseable as an integer.

4. **Type Detection**: POS type is automatically determined from the OSM `amenity` tag:
   - `cafe`, `coffee` → CAFE
   - `bakery` → BAKERY
   - `vending_machine` → VENDING_MACHINE
   - `fast_food`, `restaurant` → CAFETERIA
   - Default → CAFE

5. **Campus Assignment**: Campus type is assigned based on the city and district:
   - Heidelberg → ALTSTADT (default) or determined by district tag
   - Non-Heidelberg cities → ALTSTADT (default)

## Error Scenarios

### Scenario 1: Invalid Node ID Format
If the node ID is not a valid number, the API will return a 400 error.

### Scenario 2: Network Error
If the OSM API is unreachable, a 404 error will be returned.

### Scenario 3: Malformed OSM Response
If the OSM API returns invalid XML, a 404 error will be returned.

### Scenario 4: Postal Code Parsing Error
If the postal code is not numeric, a 400 error will be returned with message about missing fields.

## Performance Considerations

- Each import makes a synchronous HTTP request to the OpenStreetMap API
- OSM API rate limit: Typically 1 request per second per IP
- Response time depends on OSM API availability and network latency
- Typical response time: 1-5 seconds

## Security

- **XXE Protection**: XML parsing is protected against XXE (XML External Entity) attacks
- **Input Validation**: All required fields are validated before processing
- **Error Messages**: Error messages do not leak sensitive system information
- **User-Agent**: Requests to OSM API include a proper User-Agent header

## Related Endpoints

### Get All POS
```http
GET /api/pos
```

### Get POS by ID
```http
GET /api/pos/{id}
```

### Create POS Manually
```http
POST /api/pos
Content-Type: application/json

{
  "name": "Coffee Shop",
  "description": "Great coffee",
  "type": "CAFE",
  "street": "Main Street",
  "houseNumber": "42",
  "postalCode": 69115,
  "city": "Heidelberg",
  "campus": "ALTSTADT"
}
```

### Update POS
```http
PUT /api/pos/{id}
Content-Type: application/json

{
  "id": 42,
  "name": "Updated Name",
  "description": "Updated description",
  "type": "CAFE",
  "street": "Main Street",
  "houseNumber": "42",
  "postalCode": 69115,
  "city": "Heidelberg",
  "campus": "ALTSTADT"
}
```

## Implementation Notes

- The import operation is idempotent for duplicate names (updates existing record)
- Timestamps are automatically set during creation and update
- The operation is atomic - either fully succeeds or fully fails
- No partial data is persisted on failure

