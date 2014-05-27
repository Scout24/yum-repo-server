<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@attribute name="path" required="true"%>
<div id="searchBox" class="right">
<c:if test="${isStatic}">
  <form action="<c:url value="/repo/${path}"/>" method="get">
    <input type="text" name="search"/>
    <input id="searchButton" class="submit" type="submit" value="Search"/>
  </form>
</c:if>

</div>