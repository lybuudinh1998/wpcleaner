/*
 *  WikipediaCleaner: A tool to help on Wikipedia maintenance tasks.
 *  Copyright (C) 2012  Nicolas Vervelle
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.wikipediacleaner.api.request.xml;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.wikipediacleaner.api.APIException;
import org.wikipediacleaner.api.constants.EnumWikipedia;
import org.wikipediacleaner.api.data.RecentChange;
import org.wikipediacleaner.api.request.ApiRecentChangesResult;
import org.wikipediacleaner.api.request.ApiRequest;
import org.wikipediacleaner.api.request.ConnectionInformation;


/**
 * MediaWiki API XML recent changes results.
 */
public class ApiXmlRecentChangesResult extends ApiXmlResult implements ApiRecentChangesResult {

  /**
   * @param wiki Wiki on which requests are made.
   * @param httpClient HTTP client for making requests.
   * @param connection Connection information.
   */
  public ApiXmlRecentChangesResult(
      EnumWikipedia wiki,
      HttpClient httpClient,
      ConnectionInformation connection) {
    super(wiki, httpClient, connection);
  }

  /**
   * Execute recent changes request.
   * 
   * @param properties Properties defining request.
   * @param recentChanges The list of recent changes to be filled.
   * @return The timestamp to use as a starting point for the next call.
   * @throws APIException
   */
  public String executeRecentChanges(
      Map<String, String> properties,
      List<RecentChange> recentChanges) throws APIException {
    String nextStart = null;
    try {
      Element root = getRoot(properties, ApiRequest.MAX_ATTEMPTS);

      // Get recent changes list
      XPath xpa = XPath.newInstance("/api/query/recentchanges/rc");
      List results = xpa.selectNodes(root);
      Iterator iter = results.iterator();
      while (iter.hasNext()) {
        Element currentNode = (Element) iter.next();
        boolean isAnonymous = currentNode.getAttribute("anon") != null;
        boolean isBot = currentNode.getAttribute("bot") != null;
        boolean isMinor = currentNode.getAttribute("minor") != null;
        boolean isNew = currentNode.getAttribute("new") != null;
        String comment = currentNode.getAttributeValue("comment");
        String ns = currentNode.getAttributeValue("ns");
        String pageId = currentNode.getAttributeValue("pageid");
        String rcid = currentNode.getAttributeValue("rcid");
        String timestamp = currentNode.getAttributeValue("timestamp");
        String title = currentNode.getAttributeValue("title");
        String type = currentNode.getAttributeValue("type");
        String user = currentNode.getAttributeValue("user");
        try {
          RecentChange rc = new RecentChange(
              Integer.valueOf(rcid), Integer.valueOf(ns),
              title, Integer.valueOf(pageId));
          rc.setAnonymous(isAnonymous);
          rc.setBot(isBot);
          rc.setComment(comment);
          rc.setMinor(isMinor);
          rc.setNew(isNew);
          rc.setTimestamp(timestamp);
          rc.setType(type);
          rc.setUser(user);
          recentChanges.add(0, rc);
        } catch (NumberFormatException e) {
          log.error("Error loading recent changes", e);
        }
      }

      // Get start for next request
      XPath xpaNext = XPath.newInstance("/api/query-continue/recentchanges");
      Element node = (Element) xpaNext.selectSingleNode(root);
      if (node != null) {
        nextStart = node.getAttributeValue("rcstart");
      }
    } catch (JDOMException e) {
      log.error("Error loading recent changes", e);
      throw new APIException("Error parsing XML", e);
    }

    return nextStart;
  }
}
