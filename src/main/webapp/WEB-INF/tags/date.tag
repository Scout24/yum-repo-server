<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@attribute name="date" required="true" type="java.util.Date" %>
<c:choose>
    <c:when test="${date eq null}">
        unknwon
    </c:when>
    <c:otherwise>
        <fmt:formatDate type="both" value="${date}" />
    </c:otherwise>
</c:choose>