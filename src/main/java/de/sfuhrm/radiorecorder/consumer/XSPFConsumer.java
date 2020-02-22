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
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Consumer for XSPF playlist format URLs.
 *
 * @author Stephan Fuhrmann
 */
@Slf4j
public class XSPFConsumer extends AbstractConsumer implements Consumer<HttpConnection> {

    public final static String NS = "http://xspf.org/ns/0/";
    public final static String PREFIX = "x";

    public XSPFConsumer(ConsumerContext context) {
        super(context);
    }

    @Override
    protected void _accept(HttpConnection t) {
        try (InputStream is = t.getInputStream()) {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xp = factory.newXPath();
            xp.setNamespaceContext(new NamespaceContext() {
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
            InputSource inputSource = new InputSource(is);

            NodeList nl = (NodeList) xp.evaluate("/x:playlist/x:trackList/x:track/x:location", inputSource, XPathConstants.NODESET);

            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                String url = n.getTextContent();
                getConnectionHandler().consume(new URL(url));
            }
        } catch (XPathExpressionException ex) {
            throw new RadioException(false, ex);
        } catch (IOException ex) {
            log.warn("URL " + getContext().getUrl().toExternalForm() + " broke down", ex);
            throw new RadioException(true, ex);
        }
    }
}
