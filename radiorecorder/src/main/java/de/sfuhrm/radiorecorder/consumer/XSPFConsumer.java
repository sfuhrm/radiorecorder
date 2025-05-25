/*
 * Copyright 2017 Stephan Fuhrmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sfuhrm.radiorecorder.consumer;

import de.sfuhrm.radiorecorder.ConsumerContext;
import de.sfuhrm.radiorecorder.http.HttpConnection;
import de.sfuhrm.radiorecorder.RadioException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Consumer for XSPF playlist format URLs, hardened against XXE attacks.
 *
 * @author Stephan Fuhrmann
 */
@Slf4j
public class XSPFConsumer extends AbstractConsumer implements Consumer<HttpConnection> {

    /** The XML namespace of the playlist. */
    private static final String NS = "http://xspf.org/ns/0/";

    /** The internal XML namespace prefix to use. */
    private static final String PREFIX = "x";

    /** Constructor.
     * @param context the context to work in.
     */
    public XSPFConsumer(ConsumerContext context) {
        super(context);
    }

    @Override
    protected void _accept(HttpConnection t) {
        try (InputStream is = t.getInputStream()) {
            // Create a secure DocumentBuilderFactory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Disable external entities and DTD processing
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(is);

            // Set up XPath with namespace context
            XPathFactory xPathFactory = XPathFactory.newInstance();
            xPathFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            XPath xpath = xPathFactory.newXPath();
            xpath.setNamespaceContext(new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    return prefix.equals(PREFIX) ? NS : XMLConstants.NULL_NS_URI;
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    return NS.equals(namespaceURI) ? PREFIX : null;
                }

                @Override
                public Iterator<String> getPrefixes(String namespaceURI) {
                    return namespaceURI.equals(NS) ? Collections.singletonList(PREFIX).iterator() : Collections.emptyIterator();
                }
            });

            // Evaluate XPath expression
            NodeList nl = (NodeList) xpath.evaluate("/x:playlist/x:trackList/x:track/x:location", document, XPathConstants.NODESET);

            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                String url = n.getTextContent();
                getConnectionHandler().consume(URI.create(url));
            }
        } catch (ParserConfigurationException | SAXException | XPathExpressionException |
                 XPathFactoryConfigurationException ex) {
            throw new RadioException(false, ex);
        } catch (IOException ex) {
            log.warn("URL {} broke down", getContext().getUri().toASCIIString(), ex);
            throw new RadioException(true, ex);
        }
    }
}