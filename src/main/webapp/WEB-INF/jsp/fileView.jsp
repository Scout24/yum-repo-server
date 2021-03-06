<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="en">
<tags:header title="Index of ${model.path}/" />
<body>
  <div id="content">
    <div>
      <div class="left">
        <tags:headlabel />
        <h1>Index of <strong>${model.path}/</strong></h1>
      </div>
      <tags:logo />
      <tags:searchBox path="${model.path}" />
    </div>
    <ul class="tablelist">
      <li class="head">
        <span class="icon">&nbsp;</span>
        <span class="filename"><a href="?sortBy=name&order=${sortOrderName}">Name&nbsp;<img class="sortIcon" src="<c:url value="/static/images/icons/sort-${sortOrderDirectionName}.png"/>"></a></span>
        <span class="size"><a href="?sortBy=size&order=${sortOrderSize}">Size&nbsp;<img class="sortIcon" src="<c:url value="/static/images/icons/sort-${sortOrderDirectionSize}.png"/>"></a></span>
        <span class="mtime"><a href="?sortBy=uploadDate&order=${sortOrderUploadDate}">Modified&nbsp;<img class="sortIcon" src="<c:url value="/static/images/icons/sort-${sortOrderDirectionUploadDate}.png"/>"></a></span>
      </li>
      <li>
        <a href="../">
          <span class="icon"><img src="<c:url value="/static/images/icons/up.gif"/>"></span>
          <span class="filename">../</span>
          <span class="size">&nbsp;</span>
          <span class="mtime">&nbsp;</span>
        </a>
      </li>
			<c:forEach var="fileInfo" items="${model.items}" varStatus="loop">
        <li id="item_${loop.index}">
          <a href="${fileInfo.filename}">
            <span class="icon"><img src="<c:url value="/static/images/icons/rpm.gif"/>"></span>
            <span class="filename">
                ${fileInfo.filename}
                <c:if test="${fn:endsWith(fileInfo.filename, '.rpm')}">
                    <img src="<c:url value="/static/images/icons/info.png" />" rel="${fileInfo.filename}/info.html" onclick="return false;" title="RPM Info" class="rpmInfo">
                    <img src="<c:url value="/static/images/icons/trash.png" />" class="delete-icon show-on-mouseover" onclick="return yum.deleteSavelyRPM('item_${loop.index}', '','${fileInfo.filename}')" title="Delete ${fileInfo.filename}" />
                </c:if>
            </span>
            <span class="size">${fileInfo.formattedLength}</span>
            <span class="mtime"><fmt:formatDate pattern="dd.MM.yyyy HH:mm:ss" value="${fileInfo.lastModified}" /></span>
          </a>
        </li>
			</c:forEach>
      <li class="footer">
        <span class="icon">&nbsp;</span>
        <span class="filename">&nbsp;</span>
        <span class="size"><strong>${model.formattedTotalSize}</strong></span>
      </li>
		</ul>
	</div>
    <tags:scripts />
</body>

</html>
