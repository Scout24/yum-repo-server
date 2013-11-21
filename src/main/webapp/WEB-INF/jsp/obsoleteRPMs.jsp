<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="util" uri="http://immobilienscout24.de/jsp-util" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<html>
<tags:header title="Obsolete RPMs in ${sourceRepo}" />
<body>
	<div id="content">
    <div>
      <div class="left">
        <tags:headlabel/>
        <h1 class="left">Obsolete RPMs in <strong>${sourceRepo}</strong> if ${targetRepo} is propagation target</h1>
      </div>
      <tags:logo />
    </div>
	  <ul class="tablelist">
        <li class="head">
            <span class="icon">&nbsp;</span>
            <span class="filename">Name&nbsp;</span>
            <span class="size">Size&nbsp;</span>
            <span class="action">action</span>
        </li>
		<c:forEach var="fileInfo" items="${obsoleteRPMs}" varStatus="status">
            <li id="rpm${status.index}">
                <span class="icon"><img src="/static/images/icons/rpm.gif"></span>
                <span class="filename">
                    ${fileInfo.location.href}
                          <c:if test="${fn:endsWith(fileInfo.location.href, '.rpm')}">
                              <img src="/static/images/icons/info.png" rel="/repo/${sourceRepo}/${fileInfo.location.href}/info.html" onclick="return false;" title="RPM Info" class="rpmInfo">
                          </c:if>
                </span>
                <span class="size">${fileInfo.formattedLength}</span>
                <span class="action">
                    <a href="#" onclick="yum.deleteRPM('rpm${status.index}','${sourceRepo}','${fileInfo.location.href}')" title="delete"><img src="/static/images/icons/trash.png"></a>
                </span>
            </li>
		</c:forEach>
	  </ul>
	</div>
  <tags:scripts />
</body>

</html>
