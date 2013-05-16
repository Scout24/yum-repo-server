<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="model" required="true" type="de.is24.infrastructure.gridfs.http.domain.Container" %>

<ul class="tableList">
  <li class="head">
    <span class="icon">&nbsp;</span>
      <span class="filename"><a href="?sortBy=name&order=${sortOrderName}">Name&nbsp;
        <img class="sortIcon" src="/static/images/icons/sort-${sortOrderDirectionName}.png"></a></span>
    <c:choose>
      <c:when test="${isStatic}">
        <span class="size"><a href="?sortBy=size&order=${sortOrderSize}">Size&nbsp;
          <img class="sortIcon" src="/static/images/icons/sort-${sortOrderDirectionSize}.png"></a></span>
        <span class="mtime"><a href="?sortBy=uploadDate&order=${sortOrderUploadDate}">Modified&nbsp;
          <img class="sortIcon" src="/static/images/icons/sort-${sortOrderDirectionUploadDate}.png"></a></span>
      </c:when>
      <c:otherwise>
        <span class="target"><a href="?sortBy=target&order=${sortOrderTarget}">Target&nbsp;
          <img class="sortIcon" src="/static/images/icons/sort-${sortOrderDirectionTarget}.png"></a></span>
      </c:otherwise>
    </c:choose>
  </li>
</ul>
<ul class="tableList">
  <li>
    <a href="../">
      <span class="icon"><img src="/static/images/icons/up.gif"></span>
      <span class="filename">../</span>
      <span class="size">&nbsp;</span>
    </a>
  </li>
</ul>