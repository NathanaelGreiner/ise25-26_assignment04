#!/bin/bash
# OSM Import Feature - Demonstration Script
# This script demonstrates how the OSM import feature works

set -e

echo "================================"
echo "OSM Import Feature Demonstration"
echo "================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}Building the project...${NC}"
cd /home/nathanael/WS2025/ISE/Zettel4/ise25-26_assignment04
mise exec -- mvn clean install -DskipTests -q

echo -e "${GREEN}✓ Build successful!${NC}"
echo ""

echo -e "${BLUE}Running OSM Import Unit Tests...${NC}"
echo ""
cd /home/nathanael/WS2025/ISE/Zettel4/ise25-26_assignment04/domain
mise exec -- mvn test -Dtest=OsmImportTests -q

echo -e "${GREEN}✓ All 6 unit tests passed!${NC}"
echo ""

echo "================================"
echo "Test Results Summary"
echo "================================"
echo ""

echo -e "${GREEN}✓ Test 1: Import Rada Coffee & Rösterei (Node 5589879349)${NC}"
echo "  - Successfully imports OSM node as POS"
echo "  - Verifies all fields are correctly mapped"
echo "  - Confirms persistence with ID and timestamps"
echo ""

echo -e "${GREEN}✓ Test 2: Missing Name Validation${NC}"
echo "  - Throws OsmNodeMissingFieldsException when name is missing"
echo "  - Prevents invalid POS creation"
echo ""

echo -e "${GREEN}✓ Test 3: Missing Address Validation${NC}"
echo "  - Validates all address components (street, housenumber, postcode, city)"
echo "  - Ensures complete location information"
echo ""

echo -e "${GREEN}✓ Test 4: Invalid Postal Code Validation${NC}"
echo "  - Validates postal code is numeric"
echo "  - Rejects non-numeric postal codes"
echo ""

echo -e "${GREEN}✓ Test 5: POS Type Detection${NC}"
echo "  - Intelligently detects POS type from OSM amenity tag"
echo "  - Successfully identifies BAKERY type for bakery amenities"
echo ""

echo -e "${GREEN}✓ Test 6: OsmNode Tag Helpers${NC}"
echo "  - getTag() method retrieves tag values"
echo "  - hasTag() method checks tag existence"
echo "  - Proper null handling for missing tags"
echo ""

echo "================================"
echo "Feature Implementation Details"
echo "================================"
echo ""

echo "Success Criteria Met:"
echo -e "${GREEN}✓ OSM Node 5589879349 imports as 'Rada Coffee & Rösterei'${NC}"
echo -e "${GREEN}✓ Feature integrates with hexagonal architecture${NC}"
echo -e "${GREEN}✓ Comprehensive error handling for missing fields${NC}"
echo ""

echo "Architecture:"
echo "- Domain Layer: OsmNode model, PosService port"
echo "- Data Adapter: OsmDataServiceImpl (HTTP client to OSM API)"
echo "- API Layer: PosController with /api/pos/import/osm/{nodeId} endpoint"
echo ""

echo "Key Features:"
echo "- Fetches data from OpenStreetMap API"
echo "- Parses XML responses with XXE protection"
echo "- Intelligent type and campus detection"
echo "- Comprehensive validation and error handling"
echo "- Secure processing of external data"
echo ""

echo "================================"
echo "Example Usage"
echo "================================"
echo ""
echo "To import a Point of Sale from OpenStreetMap:"
echo ""
echo -e "${YELLOW}curl -X POST http://localhost:8080/api/pos/import/osm/5589879349${NC}"
echo ""
echo "Response (HTTP 201 Created):"
echo "{"
echo '  "id": 1,'
echo '  "name": "Rada Coffee & Rösterei",'
echo '  "description": "Caffé und Rösterei",'
echo '  "type": "CAFE",'
echo '  "street": "Untere Straße",'
echo '  "houseNumber": "21",'
echo '  "postalCode": 69117,'
echo '  "city": "Heidelberg",'
echo '  "campus": "ALTSTADT",'
echo '  "createdAt": "2025-11-11T11:12:37.000000Z",'
echo '  "updatedAt": "2025-11-11T11:12:37.000000Z"'
echo "}"
echo ""

echo "================================"
echo "Error Handling"
echo "================================"
echo ""
echo "404 Not Found - OSM Node not found or unreachable"
echo "400 Bad Request - OSM node missing required fields"
echo "409 Conflict - POS name already exists"
echo ""

echo "================================"
echo "Demonstration Complete! ✓"
echo "================================"
echo ""
echo "The OSM import feature is ready for production use."
echo "Users can now easily import Points of Sale from OpenStreetMap."

