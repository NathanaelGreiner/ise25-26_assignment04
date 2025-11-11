package de.seuhd.campuscoffee.data.impl;

import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * OSM import service that fetches data from the OpenStreetMap API.
 */
@Service
@Slf4j
class OsmDataServiceImpl implements OsmDataService {
    private static final String OSM_API_BASE_URL = "https://www.openstreetmap.org/api/0.6";
    private static final String USER_AGENT = "CampusCoffee/1.0 (+https://github.com/se-ubt/ise25-26_campus-coffee)";

    private final RestTemplate restTemplate;

    public OsmDataServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public @NonNull OsmNode fetchNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Fetching OSM node {} from API...", nodeId);

        try {
            String url = OSM_API_BASE_URL + "/node/" + nodeId;
            String xmlResponse = restTemplate.getForObject(url, String.class);

            if (xmlResponse == null || xmlResponse.isEmpty()) {
                log.warn("Empty response from OSM API for node {}", nodeId);
                throw new OsmNodeNotFoundException(nodeId);
            }

            return parseOsmXml(xmlResponse, nodeId);
        } catch (RestClientException e) {
            log.error("Failed to fetch OSM node {}: {}", nodeId, e.getMessage());
            throw new OsmNodeNotFoundException(nodeId);
        }
    }

    /**
     * Parses XML response from OpenStreetMap API.
     * Expected format:
     * <osm>
     *   <node id="..." lat="..." lon="...">
     *     <tag k="name" v="..."/>
     *     <tag k="address:street" v="..."/>
     *     ...
     *   </node>
     * </osm>
     *
     * @param xmlResponse the XML response from OSM API
     * @param nodeId the expected node ID
     * @return parsed OsmNode with tags
     * @throws OsmNodeNotFoundException if parsing fails or node not found
     */
    private OsmNode parseOsmXml(String xmlResponse, Long nodeId) throws OsmNodeNotFoundException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes()));

            NodeList nodeElements = doc.getElementsByTagName("node");
            if (nodeElements.getLength() == 0) {
                log.warn("No node element found in OSM API response for node {}", nodeId);
                throw new OsmNodeNotFoundException(nodeId);
            }

            Element nodeElement = (Element) nodeElements.item(0);
            String foundId = nodeElement.getAttribute("id");
            if (!foundId.equals(nodeId.toString())) {
                log.warn("Mismatched node ID: expected {}, got {}", nodeId, foundId);
                throw new OsmNodeNotFoundException(nodeId);
            }

            // Parse tags
            Map<String, String> tags = new HashMap<>();
            NodeList tagElements = nodeElement.getElementsByTagName("tag");
            for (int i = 0; i < tagElements.getLength(); i++) {
                Element tagElement = (Element) tagElements.item(i);
                String key = tagElement.getAttribute("k");
                String value = tagElement.getAttribute("v");
                tags.put(key, value);
            }

            log.debug("Successfully parsed OSM node {} with {} tags", nodeId, tags.size());
            return OsmNode.builder()
                    .nodeId(nodeId)
                    .tags(tags)
                    .build();

        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("Error parsing OSM XML response: {}", e.getMessage(), e);
            throw new OsmNodeNotFoundException(nodeId);
        }
    }
}
