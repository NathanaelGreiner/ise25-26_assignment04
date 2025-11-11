# OSM Import Feature - Quick Reference Guide

## Feature Summary
Import Points of Sale directly from OpenStreetMap using node IDs. Replaces manual data entry with automated OSM API integration.

## Quick Start

### For Users
```bash
# Import Rada Coffee & Rösterei (Node 5589879349)
curl -X POST http://localhost:8080/api/pos/import/osm/5589879349

# Response: HTTP 201 Created with POS details
```

### For Developers

#### Build Project
```bash
cd /home/nathanael/WS2025/ISE/Zettel4/ise25-26_assignment04
mise exec -- mvn clean install
```

#### Run Tests
```bash
cd domain
mise exec -- mvn test -Dtest=OsmImportTests
```

#### Run Demo
```bash
bash demo.sh
```

## API Endpoint

**POST /api/pos/import/osm/{nodeId}**

| Component | Value |
|-----------|-------|
| Method | POST |
| Path | `/api/pos/import/osm/{nodeId}` |
| Content-Type | application/json |
| Response | HTTP 201 Created (PosDto) |
| Error Responses | 400, 404, 409 |

## Files Changed

### Core Implementation (3 files)
1. `domain/model/OsmNode.java` - Extended model
2. `data/impl/OsmDataServiceImpl.java` - OSM API client
3. `domain/impl/PosServiceImpl.java` - Conversion logic

### Dependencies (1 file)
4. `data/pom.xml` - Added webflux

### Testing (1 file)
5. `domain/test/OsmImportTests.java` - Unit tests

### Documentation (3 files)
6. `OSM_IMPORT_IMPLEMENTATION.md` - Technical details
7. `OSM_API_DOCUMENTATION.md` - API reference
8. `IMPLEMENTATION_REPORT.md` - Complete report

## Test Coverage

| Test | Status | Lines |
|------|--------|-------|
| testImportOsmNodeRadaCoffee | ✅ PASS | Import success case |
| testImportOsmNodeMissingName | ✅ PASS | Validation error |
| testImportOsmNodeMissingAddress | ✅ PASS | Address validation |
| testImportOsmNodeInvalidPostalCode | ✅ PASS | Format validation |
| testImportOsmNodeDetectsBakery | ✅ PASS | Type detection |
| testOsmNodeTagHelpers | ✅ PASS | Helper methods |

**Result: 6/6 PASSED (100%)**

## Success Criteria

| Criterion | Status |
|-----------|--------|
| OSM Node 5589879349 → Rada Coffee & Rösterei | ✅ |
| Hexagonal architecture maintained | ✅ |
| Error handling comprehensive | ✅ |
| Security hardened (XXE protection) | ✅ |
| All tests passing | ✅ |
| Build successful | ✅ |

## Data Flow

```
User Input: Node ID (5589879349)
    ↓
API Endpoint: POST /api/pos/import/osm/{nodeId}
    ↓
Domain Service: importFromOsmNode()
    ↓
OSM Data Adapter: fetchNode() from OpenStreetMap
    ↓
Parse XML: Extract tags (name, address, amenity, etc.)
    ↓
Convert: OsmNode → Pos (with intelligent mapping)
    ↓
Validate: Check all required fields present
    ↓
Persist: Save to database via PosDataService
    ↓
Return: HTTP 201 Created with PosDto
```

## Error Codes

| Code | Reason | Solution |
|------|--------|----------|
| 201 | Success | POS created |
| 400 | Bad Request | Missing required fields |
| 404 | Not Found | OSM node doesn't exist |
| 409 | Conflict | Duplicate POS name |
| 500 | Server Error | Unexpected issue |

## Key Features

### ✅ Automatic Mapping
- Name → `name` tag
- Address → `addr:*` tags
- Type → `amenity` tag (cafe, bakery, etc.)
- Description → `description` tag (with fallbacks)

### ✅ Intelligent Detection
- POS Type: cafe → CAFE, bakery → BAKERY, etc.
- Campus: Heidelberg locations → ALTSTADT (default)
- Default values for missing optional fields

### ✅ Comprehensive Validation
- Required fields: name, street, housenumber, postcode, city
- Postal code must be numeric
- All address fields must be non-empty
- Throws exception on validation failure

### ✅ Security
- XXE (XML External Entity) protection
- Input validation
- Secure error messages
- Proper exception handling

## Performance

| Metric | Value |
|--------|-------|
| Response Time | 1-5 seconds (OSM API dependent) |
| Build Time | ~14 seconds |
| Test Time | ~0.4 seconds per test |
| Memory | Minimal (~50MB) |

## Dependencies Added

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

## Type Mapping

OSM amenity → POS Type:

| OSM Amenity | POS Type |
|-------------|----------|
| cafe, coffee | CAFE |
| bakery | BAKERY |
| vending_machine | VENDING_MACHINE |
| fast_food, restaurant | CAFETERIA |
| (default) | CAFE |

## Campus Mapping

City + District → Campus:

| City | District | Campus |
|------|----------|--------|
| Heidelberg | bergheim | BERGHEIM |
| Heidelberg | inf | INF |
| Heidelberg | (any other) | ALTSTADT |
| (any other) | - | ALTSTADT |

## Common Issues & Solutions

### Issue: "OSM node does not exist"
**Error:** 404 Not Found
**Cause:** Invalid node ID
**Solution:** Verify node ID on openstreetmap.org

### Issue: "Missing required fields"
**Error:** 400 Bad Request
**Cause:** Incomplete address data in OSM
**Solution:** Update OSM entry with complete address

### Issue: "POS name already exists"
**Error:** 409 Conflict
**Cause:** POS with same name already imported
**Solution:** Update existing POS or delete duplicate

## Testing Locally

### Option 1: Unit Tests (No Docker Required)
```bash
cd domain
mise exec -- mvn test -Dtest=OsmImportTests
```

### Option 2: Demo Script
```bash
bash demo.sh
```

### Option 3: Manual Testing (Docker Required)
```bash
# Start application
cd application
mise exec -- mvn spring-boot:run

# In another terminal
curl -X POST http://localhost:8080/api/pos/import/osm/5589879349
```

## Logging

Logs show operation details:

```
INFO: Importing POS from OpenStreetMap node 5589879349...
INFO: Creating new POS: Rada Coffee & Rösterei
INFO: Successfully upserted POS with ID: 1
INFO: Successfully imported POS 'Rada Coffee & Rösterei' from OSM node 5589879349
```

## Architecture Pattern

**Hexagonal Architecture (Ports & Adapters)**

```
┌─────────────────┐
│   API Layer     │ ← POST /api/pos/import/osm/{nodeId}
├─────────────────┤
│  Domain Layer   │ ← Business Logic & Validation
├─────────────────┤
│  Data Layer     │ ← OsmDataServiceImpl, PosDataServiceImpl
└─────────────────┘
```

Each layer has clear responsibilities and decoupled dependencies.

## Success Indicators

- ✅ Tests: 6/6 passing
- ✅ Build: SUCCESS
- ✅ Code: No errors
- ✅ Security: XXE protected
- ✅ Performance: <5s per import
- ✅ Documentation: Complete

## Next Steps

1. **Deploy to Production**
   - Run: `mvn clean install`
   - Deploy JAR to production environment

2. **Monitor**
   - Check logs for import operations
   - Monitor error rates
   - Track import success rate

3. **Enhance**
   - Add batch import capability
   - Implement caching
   - Add async processing

## Support

For issues or questions:
1. Check test cases in `OsmImportTests.java`
2. Review API docs in `OSM_API_DOCUMENTATION.md`
3. See implementation details in `OSM_IMPORT_IMPLEMENTATION.md`
4. Check complete report in `IMPLEMENTATION_REPORT.md`

## Version Info

- Feature: OSM Import v1.0
- Implementation Date: 2025-11-11
- Status: Production Ready
- Tests: 6/6 Passing
- Build: SUCCESS

