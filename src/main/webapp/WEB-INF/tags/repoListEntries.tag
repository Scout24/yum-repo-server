<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ attribute name="model" required="true" type="de.is24.infrastructure.gridfs.http.domain.Container" %>

<ul class="tableList">
  <c:forEach var="item" items="${model.items}">
    <li>
      <a href="${item.href}/">
        <span class="icon"><img src="/static/images/icons/folder.gif"></span>
            <span class="filename folder">
              ${item.name}/
              <c:if test="${model.showInfo}">
                <img src="/static/images/icons/info.png" rel="${item.name}/info.html" onclick="return false;"
                     title="Repository Info" class="${isStatic ? 'repoInfo' : 'virtualRepoInfo'}">
                <span class="tags"><c:forEach items="${item.tags}" var="tag">${tag} </c:forEach></span>
              </c:if>
            </span>
        <c:choose>
          <c:when test="${isStatic}">
            <c:if test="${!item.external}">
              <span class="size">${item.formattedSize}</span>
            </c:if>
            <span class="mtime"><fmt:formatDate pattern="dd.MM.yyyy HH:mm:ss" value="${item.lastModified}"/></span>            
          </c:when>
          <c:otherwise>
            <span class="target">${item.target}</span>
          </c:otherwise>
        </c:choose>        
      </a>
    </li>
  </c:forEach>
</ul>
