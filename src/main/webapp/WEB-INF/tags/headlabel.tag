<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<c:choose>
  <c:when test="${isStatic}">
    <span class="headlabel active">Static</span><a class="headlabel" href="/repo/virtual/" title="">Virtual</a>
  </c:when>
  <c:otherwise>
    <a class="headlabel" href="/repo/">Static</a><span class="headlabel active">Virtual</span>
  </c:otherwise>
</c:choose>
