<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<c:choose>
  <c:when test="${viewName eq 'static'}">
    <span class="headlabel active">Static</span><a class="headlabel" href="/repo/virtual/" title="">Virtual</a><a class="headlabel" href="/maintenance/" title="">Maintenance</a>
  </c:when>
  <c:when test="${viewName eq 'virtual'}">
    <a class="headlabel" href="/repo/">Static</a><span class="headlabel active">Virtual</span><a class="headlabel" href="/maintenance/" title="">Maintenance</a>
  </c:when>
  <c:otherwise>
    <a class="headlabel" href="/repo/">Static</a><a class="headlabel" href="/repo/virtual/" title="">Virtual</a><span class="headlabel">Maintenance</span>
  </c:otherwise>
</c:choose>
