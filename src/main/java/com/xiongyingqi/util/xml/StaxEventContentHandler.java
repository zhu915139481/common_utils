/*
 * Copyright 2002-2012 the original author or authors.
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

package com.xiongyingqi.util.xml;

import com.xiongyingqi.util.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventConsumer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * SAX {@code ContentHandler} that transforms callback calls to {@code XMLEvent}s
 * and writes them to a {@code XMLEventConsumer}.
 *
 * @author Arjen Poutsma
 * @see javax.xml.stream.events.XMLEvent
 * @see javax.xml.stream.util.XMLEventConsumer
 * @since 3.0
 */
class StaxEventContentHandler extends AbstractStaxContentHandler {

    private final XMLEventFactory eventFactory;

    private final XMLEventConsumer eventConsumer;


    /**
     * Construct a new instance of the {@code StaxEventContentHandler} that writes to the given
     * {@code XMLEventConsumer}. A default {@code XMLEventFactory} will be created.
     *
     * @param consumer the consumer to write events to
     */
    StaxEventContentHandler(XMLEventConsumer consumer) {
        this.eventFactory = XMLEventFactory.newInstance();
        this.eventConsumer = consumer;
    }

    /**
     * Construct a new instance of the {@code StaxEventContentHandler} that uses the given
     * event factory to create events and writes to the given {@code XMLEventConsumer}.
     *
     * @param consumer the consumer to write events to
     * @param factory  the factory used to create events
     */
    StaxEventContentHandler(XMLEventConsumer consumer, XMLEventFactory factory) {
        this.eventFactory = factory;
        this.eventConsumer = consumer;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        if (locator != null) {
            this.eventFactory.setLocation(new LocatorLocationAdapter(locator));
        }
    }

    @Override
    protected void startDocumentInternal() throws XMLStreamException {
        consumeEvent(this.eventFactory.createStartDocument());
    }

    @Override
    protected void endDocumentInternal() throws XMLStreamException {
        consumeEvent(this.eventFactory.createEndDocument());
    }

    @Override
    protected void startElementInternal(QName name, Attributes atts, org.springframework.util.xml.SimpleNamespaceContext namespaceContext)
            throws XMLStreamException {

        List<Attribute> attributes = getAttributes(atts);
        List<Namespace> namespaces = createNamespaces(namespaceContext);
        consumeEvent(this.eventFactory.createStartElement(name, attributes.iterator(),
                (namespaces != null ? namespaces.iterator() : null)));
    }

    @Override
    protected void endElementInternal(QName name, org.springframework.util.xml.SimpleNamespaceContext namespaceContext) throws XMLStreamException {
        List<Namespace> namespaces = createNamespaces(namespaceContext);
        consumeEvent(this.eventFactory.createEndElement(name, namespaces != null ? namespaces.iterator() : null));
    }

    @Override
    protected void charactersInternal(char[] ch, int start, int length) throws XMLStreamException {
        consumeEvent(this.eventFactory.createCharacters(new String(ch, start, length)));
    }

    @Override
    protected void ignorableWhitespaceInternal(char[] ch, int start, int length) throws XMLStreamException {
        consumeEvent(this.eventFactory.createIgnorableSpace(new String(ch, start, length)));
    }

    @Override
    protected void processingInstructionInternal(String target, String data) throws XMLStreamException {
        consumeEvent(this.eventFactory.createProcessingInstruction(target, data));
    }

    private void consumeEvent(XMLEvent event) throws XMLStreamException {
        this.eventConsumer.add(event);
    }

    /**
     * Create and return a list of {@code NameSpace} objects from the {@code NamespaceContext}.
     */
    private List<Namespace> createNamespaces(org.springframework.util.xml.SimpleNamespaceContext namespaceContext) {
        if (namespaceContext == null) {
            return null;
        }

        List<Namespace> namespaces = new ArrayList<Namespace>();
        String defaultNamespaceUri = namespaceContext.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);
        if (StringUtils.hasLength(defaultNamespaceUri)) {
            namespaces.add(this.eventFactory.createNamespace(defaultNamespaceUri));
        }
        for (Iterator<String> iterator = namespaceContext.getBoundPrefixes(); iterator.hasNext(); ) {
            String prefix = iterator.next();
            String namespaceUri = namespaceContext.getNamespaceURI(prefix);
            namespaces.add(this.eventFactory.createNamespace(prefix, namespaceUri));
        }
        return namespaces;
    }

    private List<Attribute> getAttributes(Attributes attributes) {
        List<Attribute> list = new ArrayList<Attribute>();
        for (int i = 0; i < attributes.getLength(); i++) {
            QName name = toQName(attributes.getURI(i), attributes.getQName(i));
            if (!("xmlns".equals(name.getLocalPart()) || "xmlns".equals(name.getPrefix()))) {
                list.add(this.eventFactory.createAttribute(name, attributes.getValue(i)));
            }
        }
        return list;
    }

    /* No operation */
    @Override
    protected void skippedEntityInternal(String name) throws XMLStreamException {
    }


    private static final class LocatorLocationAdapter implements Location {

        private final Locator locator;

        public LocatorLocationAdapter(Locator locator) {
            this.locator = locator;
        }

        @Override
        public int getLineNumber() {
            return this.locator.getLineNumber();
        }

        @Override
        public int getColumnNumber() {
            return this.locator.getColumnNumber();
        }

        @Override
        public int getCharacterOffset() {
            return -1;
        }

        @Override
        public String getPublicId() {
            return this.locator.getPublicId();
        }

        @Override
        public String getSystemId() {
            return this.locator.getSystemId();
        }
    }

}
