package de.is24.infrastructure.gridfs.http.web.jsp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class JspUtil {

  public static String urlEncode(String value, String charset) throws UnsupportedEncodingException {
    return URLEncoder.encode(value, charset);
  }

  public static String urlEncode(String value) throws UnsupportedEncodingException {
    return URLEncoder.encode(value, "UTF-8");
  }
}
