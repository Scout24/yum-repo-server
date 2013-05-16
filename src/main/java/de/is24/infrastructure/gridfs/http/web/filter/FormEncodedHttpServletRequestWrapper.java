package de.is24.infrastructure.gridfs.http.web.filter;

import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.Arrays.asList;
import static java.util.Collections.enumeration;
import static java.util.Collections.list;
import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.lang.ArrayUtils.addAll;


public class FormEncodedHttpServletRequestWrapper extends HttpServletRequestWrapper {
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";

  private Map<String, String[]> parameters;

  public FormEncodedHttpServletRequestWrapper(HttpServletRequest request, FormHttpMessageConverter messageConverter) {
    super(request);
    this.parameters = mergeParameters(messageConverter);
  }

  @Override
  public String getHeader(String name) {
    if (CONTENT_TYPE.equals(name)) {
      return CONTENT_TYPE_FORM_URLENCODED;
    }

    return super.getHeader(name);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    Set<String> headerNames = new LinkedHashSet<String>(list(super.getHeaderNames()));
    headerNames.add(CONTENT_TYPE);
    return enumeration(headerNames);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    if (CONTENT_TYPE.equals(name)) {
      return enumeration(asList(CONTENT_TYPE_FORM_URLENCODED));
    }
    return super.getHeaders(name);
  }

  @Override
  public String getParameter(String name) {
    String[] value = parameters.get(name);
    if ((value != null) && (value.length > 0)) {
      return value[0];
    }

    return null;
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return parameters;
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return enumeration(parameters.keySet());
  }

  @Override
  public String[] getParameterValues(String name) {
    return parameters.get(name);
  }

  private Map<String, String[]> mergeParameters(FormHttpMessageConverter messageConverter) {
    Map<String, String[]> mergedParameterMap = new HashMap<String, String[]>(super.getParameterMap());

    mergeParameters(mergedParameterMap, parseRequestBody(messageConverter));

    return unmodifiableMap(mergedParameterMap);
  }

  private void mergeParameters(Map<String, String[]> mergedParameterMap,
                               Set<Map.Entry<String, List<String>>> requestBodyParameters) {
    for (Map.Entry<String, List<String>> entry : requestBodyParameters) {
      String[] existingParameterValue = mergedParameterMap.get(entry.getKey());
      if (existingParameterValue == null) {
        mergedParameterMap.put(entry.getKey(), entry.getValue().toArray(new String[0]));
      } else {
        String[] newParameterValue = (String[]) addAll(existingParameterValue, entry.getValue().toArray(new String[0]));
        mergedParameterMap.put(entry.getKey(), newParameterValue);
      }
    }
  }

  private Set<Map.Entry<String, List<String>>> parseRequestBody(FormHttpMessageConverter messageConverter) {
    try {
      ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(this) {
        @Override
        public InputStream getBody() throws IOException {
          return FormEncodedHttpServletRequestWrapper.this.getInputStream();
        }
      };
      return messageConverter.read(null, inputMessage).entrySet();
    } catch (IOException e) {
      throw new BadRequestException("Could not parse form url encoded request.", e);
    }
  }
}
