/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.preload;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class DumpDataIO {

  public static String serialize(Collection<DumpData> data) {
    StringBuilder sb = new StringBuilder();
    sb.append("<preloaded-classes-data>\n");

    for (DumpData d : data) {
      serialize(d, sb);
    }

    sb.append("</preloaded-classes-data>\n");
    return sb.toString();
  }

  /**
   * @param d
   * @param sb
   */
  private static void serialize(DumpData d, StringBuilder sb) {
    sb.append("<data package=\"" + d.packageName + "\" date=\"" +
            DateFormat.getDateTimeInstance().format(d.date) +"\">\n");

    for (Map.Entry<String, String> e : d.dumpData.entrySet()) {
      sb.append("<class name=\"" + e.getKey() + "\" classloader=\"" + e.getValue() + "\"/>\n");
    }

    sb.append("</data>\n");
  }

  public static Collection<DumpData> deserialize(File f) throws Exception {
    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setNamespaceAware(false);
    SAXParser saxParser = spf.newSAXParser();

    XMLReader xmlReader = saxParser.getXMLReader();
    DumpDataContentHandler ddch = new DumpDataContentHandler();
    xmlReader.setContentHandler(ddch);
    xmlReader.parse(new InputSource(new FileReader(f)));

    return ddch.data;
  }

  private static class DumpDataContentHandler extends DefaultHandler {
    Collection<DumpData> data = new LinkedList<DumpData>();
    DumpData openData = null;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
      if (qName.equals("data")) {
        if (openData != null) {
          throw new IllegalStateException();
        }
        String pkg = attributes.getValue("package");
        String dateString = attributes.getValue("date");

        if (pkg == null || dateString == null) {
          throw new IllegalArgumentException();
        }

        try {
          Date date = DateFormat.getDateTimeInstance().parse(dateString);
          openData = new DumpData(pkg, new HashMap<String, String>(), date);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      } else if (qName.equals("class")) {
        if (openData == null) {
          throw new IllegalStateException();
        }
        String className = attributes.getValue("name");
        String classLoader = attributes.getValue("classloader");

        if (className == null || classLoader == null) {
          throw new IllegalArgumentException();
        }

        openData.dumpData.put(className, classLoader.equals("null") ? null : classLoader);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (qName.equals("data")) {
        if (openData == null) {
          throw new IllegalStateException();
        }
        openData.countBootClassPath();

        data.add(openData);
        openData = null;
      }
    }
  }
}
