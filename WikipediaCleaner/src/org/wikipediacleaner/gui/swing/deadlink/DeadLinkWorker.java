/*
 *  WPCleaner: A tool to help on Wikipedia maintenance tasks.
 *  Copyright (C) 2019  Nicolas Vervelle
 *
 *  See README.txt file for licensing information.
 */


package org.wikipediacleaner.gui.swing.deadlink;

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.JTextComponent;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikipediacleaner.api.API;
import org.wikipediacleaner.api.APIException;
import org.wikipediacleaner.api.APIFactory;
import org.wikipediacleaner.api.HttpUtils;
import org.wikipediacleaner.api.constants.EnumWikipedia;
import org.wikipediacleaner.api.data.Page;
import org.wikipediacleaner.api.data.PageElementExternalLink;
import org.wikipediacleaner.gui.swing.basic.BasicWindow;
import org.wikipediacleaner.gui.swing.basic.BasicWorker;
import org.wikipediacleaner.gui.swing.basic.Utilities;
import org.wikipediacleaner.i18n.GT;


/**
 * Worker for checking dead links in articles.
 */
public class DeadLinkWorker extends BasicWorker {

  /** Logs */
  private final Logger log = LoggerFactory.getLogger(DeadLinkWorker.class);

  /** List of pages */
  private final List<Page> pages;

  /** Text pane */
  private final JTextComponent textPane;

  /** List of dead links */
  private List<DeadLink> errors; 

  /**
   * @param wiki Wiki.
   * @param window Window
   * @param pages List of pages to check.
   * @param textPane Text pane.
   */
  public DeadLinkWorker(
      EnumWikipedia wiki, BasicWindow window,
      List<Page> pages, JTextComponent textPane) {
    super(wiki, window);
    this.pages = pages;
    this.textPane = textPane;
  }

  /* (non-Javadoc)
   * @see org.wikipediacleaner.gui.swing.utils.SwingWorker#finished()
   */
  @Override
  public void finished() {
    super.finished();
    Object result = get();
    Component parent = (getWindow() != null) ? getWindow().getParentComponent() : null;
    if (!(result instanceof Throwable)) {
      if ((errors != null) && !errors.isEmpty()) {
        DeadLinkWindow.createDeadLinkWindow(getWikipedia(), errors, textPane);
      } else {
        Utilities.displayInformationMessage(parent, GT._T("No dead links were found."));
      }
    } else {
      Utilities.displayError(parent, (Throwable) result);
    }
  }

  /* (non-Javadoc)
   * @see org.wikipediacleaner.gui.swing.utils.SwingWorker#construct()
   */
  @Override
  public Object construct() {
    try {
      errors = new ArrayList<>();
      API api = APIFactory.getAPI();
      if (textPane == null) {
        setText(GT._T("Retrieving page contents"));
        api.retrieveContents(getWikipedia(), pages, false, false);
      } else {
        for (Page page : pages) {
          page.setContents(textPane.getText());
        }
      }
      HttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
      HttpClient client = new HttpClient(manager);
      for (Page page : pages) {
        setText(GT._T("Analyzing {0}", page.getTitle()));
        List<PageElementExternalLink> links = page.getAnalysis(page.getContents(), false).getExternalLinks();
        if (links != null) {
          for (PageElementExternalLink link : links) {
            String url = link.getLink();
            setText(GT._T("Analyzing {0}", url));
            DeadLink deadLink = null;
            try {
              HttpMethod method = HttpUtils.createHttpMethod(url, null, true);
              int questionIndex = url.indexOf('?');
              if (questionIndex > 0) {
                method.setQueryString(url.substring(questionIndex + 1));
              }
              method.setRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
              method.setRequestHeader("Accept-Encoding", "gzip, deflate");
              method.setRequestHeader("Accept-Language", "en-US,en");
              method.setRequestHeader("Cache-Control", "no-cache");
              method.setRequestHeader("Connection", "keep-alive");
              method.setRequestHeader("Pragma", "no-cache");
              method.setRequestHeader("Upgrade-Insecure-Requests", "1");
              //method.setRequestHeader("Content-Type", "text/html");
              method.setRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36");
              int statusCode = client.executeMethod(method);
              if (statusCode != HttpStatus.SC_OK) {
                deadLink = new DeadLink(page.getTitle(), link, statusCode);
              }
            } catch (IOException e) {
              log.error("Exception when accessing " + url + ": " + e.getMessage());
              deadLink = new DeadLink(page.getTitle(), link, e.getMessage());
            }
            if (deadLink != null) {
              errors.add(deadLink);
            }
          }
        }
      }
    } catch (APIException e) {
      return e;
    }
    return errors;
  }
}
