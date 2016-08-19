// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.remote.http;

import static org.openqa.selenium.remote.DriverCommand.ACCEPT_ALERT;
import static org.openqa.selenium.remote.DriverCommand.DISMISS_ALERT;
import static org.openqa.selenium.remote.DriverCommand.EXECUTE_ASYNC_SCRIPT;
import static org.openqa.selenium.remote.DriverCommand.EXECUTE_SCRIPT;
import static org.openqa.selenium.remote.DriverCommand.GET_ALERT_TEXT;
import static org.openqa.selenium.remote.DriverCommand.GET_CURRENT_WINDOW_HANDLE;
import static org.openqa.selenium.remote.DriverCommand.GET_CURRENT_WINDOW_POSITION;
import static org.openqa.selenium.remote.DriverCommand.GET_CURRENT_WINDOW_SIZE;
import static org.openqa.selenium.remote.DriverCommand.GET_PAGE_SOURCE;
import static org.openqa.selenium.remote.DriverCommand.GET_WINDOW_HANDLES;
import static org.openqa.selenium.remote.DriverCommand.MAXIMIZE_CURRENT_WINDOW;
import static org.openqa.selenium.remote.DriverCommand.SET_ALERT_VALUE;
import static org.openqa.selenium.remote.DriverCommand.SET_CURRENT_WINDOW_POSITION;
import static org.openqa.selenium.remote.DriverCommand.SET_CURRENT_WINDOW_SIZE;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.internal.WebElementToJsonConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * A command codec that adheres to the W3C's WebDriver wire protocol.
 *
 * @see <a href="https://w3.org/tr/webdriver">W3C WebDriver spec</a>
 */
public class W3CHttpCommandCodec extends AbstractHttpCommandCodec {

  public W3CHttpCommandCodec() {

    defineCommand(EXECUTE_SCRIPT, post("/session/:sessionId/execute/sync"));
    defineCommand(EXECUTE_ASYNC_SCRIPT, post("/session/:sessionId/execute/async"));

    defineCommand(GET_PAGE_SOURCE, post("/session/:sessionId/execute/sync"));

    defineCommand(MAXIMIZE_CURRENT_WINDOW, post("/session/:sessionId/window/maximize"));
    defineCommand(GET_CURRENT_WINDOW_POSITION, get("/session/:sessionId/execute/sync"));
    defineCommand(SET_CURRENT_WINDOW_POSITION, post("/session/:sessionId/execute/sync"));
    defineCommand(GET_CURRENT_WINDOW_SIZE, get("/session/:sessionId/window/size"));
    defineCommand(SET_CURRENT_WINDOW_SIZE, post("/session/:sessionId/window/size"));
    defineCommand(GET_CURRENT_WINDOW_HANDLE, get("/session/:sessionId/window"));
    defineCommand(GET_WINDOW_HANDLES, get("/session/:sessionId/window/handles"));

    defineCommand(ACCEPT_ALERT, post("/session/:sessionId/alert/accept"));
    defineCommand(DISMISS_ALERT, post("/session/:sessionId/alert/dismiss"));
    defineCommand(GET_ALERT_TEXT, get("/session/:sessionId/alert/text"));
    defineCommand(SET_ALERT_VALUE, post("/session/:sessionId/alert/text"));
  }

  @Override
  protected Map<String, ?> amendParameters(String name, Map<String, ?> parameters) {
    switch (name) {
      case DriverCommand.FIND_CHILD_ELEMENT:
      case DriverCommand.FIND_CHILD_ELEMENTS:
      case DriverCommand.FIND_ELEMENT:
      case DriverCommand.FIND_ELEMENTS:
        String using = (String) parameters.get("using");
        String value = (String) parameters.get("value");

        Map<String, Object> toReturn = new HashMap<>();
        toReturn.putAll(parameters);

        switch (using) {
          case "class name":
            toReturn.put("using", "css selector");
            toReturn.put("value", "." + cssEscape(value));
            break;

          case "id":
            toReturn.put("using", "css selector");
            toReturn.put("value", "#" + cssEscape(value));
            break;

          case "link text":
            // Do nothing
            break;

          case "name":
            toReturn.put("using", "css selector");
            toReturn.put("value", "*[name='" + value + "']");
            break;

          case "partial link text":
            // Do nothing
            break;

          case "tag name":
            toReturn.put("using", "css selector");
            toReturn.put("value", "#" + cssEscape(value));
            break;

          case "xpath":
            // Do nothing
            break;
        }
        return toReturn;


      case GET_PAGE_SOURCE:
        return toScript(
          "var source = document.documentElement.outerHTML; \n" +
          "if (!source) { source = new XMLSerializer().serializeToString(document); }\n" +
          "return source;");

      case DriverCommand.GET_CURRENT_WINDOW_POSITION:
        return toScript("return {x: window.screenX, y: window.screenY}");

      case DriverCommand.SET_CURRENT_WINDOW_POSITION:
        return toScript(
          "window.screenX = arguments[0]; window.screenY = arguments[1]",
          parameters.get("x"),
          parameters.get("y"));

      default:
        return parameters;
    }
  }

  private Map<String, ?> toScript(String script, Object... args) {
    // Escape the quote marks
    script = script.replaceAll("\"", "\\\"");

    Iterable<Object> convertedArgs = Iterables.transform(
      Lists.newArrayList(args), new WebElementToJsonConverter());

    return ImmutableMap.of(
      "script", script,
      "args", Lists.newArrayList(convertedArgs));
  }

  private String cssEscape(String using) {
    using = using.replaceAll("(['\"\\\\#.:;,!?+<>=~*^$|%&@`{}\\-\\/\\[\\]\\(\\)])", "\\\\$1");
    if (using.length() > 0 && Character.isDigit(using.charAt(0))) {
      using = "\\" + Integer.toString(30 + Integer.parseInt(using.substring(0,1))) + " " + using.substring(1);
    }
    return using;
  }
}
