<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="util" uri="http://immobilienscout24.de/jsp-util" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<html>
<tags:header title="Obsolete RPMs in ${sourceRepo} if ${targetRepo} is propagation target" />
<body>
	<div id="content">
    <div>
      <div class="left">
        <tags:headlabel/>
        <h1>Obsolete RPMs in <strong>${sourceRepo}</strong> if ${targetRepo} is propagation target</h1>
      </div>
      <tags:logo />
    </div>
	  <ul class="tablelist">
      <li>
        <a href="/maintenance/">
          <span class="icon"><img src="/static/images/icons/up.gif"></span>
          <span class="filename">back to maintenance Options</span>
          <span class="size">&nbsp;</span>
          <span class="action">&nbsp;</span>
        </a>
      </li>
      <c:if test="${obsoleteRPMs.size() > 0}">
        <li>
          <a href="#" onclick="yum.deleteObsoleteRPMs('${targetRepo}','${sourceRepo}')">
            <span class="icon"><img src="/static/images/icons/trash.png"></span>
            <span class="filename">Trigger deletion of all obsolete RPMs </span>
            <span class="size">&nbsp;</span>
            <span class="action">&nbsp;</span>
          </a>
        </li>
      </c:if>
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
