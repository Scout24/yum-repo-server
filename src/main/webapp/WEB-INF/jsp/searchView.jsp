<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<html>
<tags:header title="Search in ${model.path}/" />
<body>
	<div id="content">
    <div>
      <h1 class="left">Search in <strong>${model.path}/</strong></h1>
      <tags:logo />
      <tags:searchBox path="${model.path}" />
    </div>
		<ul class="tablelist">
			<li class="head">
				<span class="icon">&nbsp;</span>
				<span class="filename"><a href="?search=${searchBy}&sortBy=name&order=${sortOrderName}">Name&nbsp;<img class="sortIcon" src="/static/images/icons/sort-${sortOrderDirectionName}.png"></a></span>
				<span class="size"><a href="?search=${searchBy}&sortBy=size&order=${sortOrderSize}">Size&nbsp;<img class="sortIcon" src="/static/images/icons/sort-${sortOrderDirectionSize}.png"></a></span>
				<span class="mtime"><a href="?search=${searchBy}&sortBy=uploadDate&order=${sortOrderUploadDate}">Modified&nbsp;<img class="sortIcon" src="/static/images/icons/sort-${sortOrderDirectionUploadDate}.png"></a></span> 
				<span class="repo"><a href="?search=${searchBy}&sortBy=repo&order=${sortOrderRepo}">Repo&nbsp;<img class="sortIcon" src="/static/images/icons/sort-${sortOrderDirectionRepo}.png"></a></span>
			</li>
      <li>
        <a href="/repo/${model.path}">
          <span class="icon"><img src="/static/images/icons/up.gif"></span>
          <span class="filename">../</span>
          <span class="size">&nbsp;</span>
          <span class="mtime">&nbsp;</span>
          <span class="repo">&nbsp;</span>
        </a>
      </li>
			<c:forEach var="fileInfo" items="${model.items}">
        <li>
          <a href="/repo/${fileInfo.repo}/${fileInfo.arch}/${fileInfo.filename}">
            <span class="icon"><img src="/static/images/icons/rpm.gif"></span>
            <span class="filename">
                ${fileInfo.filename}
                      <c:if test="${fn:endsWith(fileInfo.filename, '.rpm')}">
                          <img src="/static/images/icons/info.png" rel="/repo/${fileInfo.repo}/${fileInfo.arch}/${fileInfo.filename}/info.html" onclick="return false;" title="RPM Info" class="rpmInfo">
                      </c:if>
                  </span>
            <span class="size">${fileInfo.formattedLength}</span>
            <span class="mtime"><fmt:formatDate pattern="dd.MM.yyyy HH:mm:ss" value="${fileInfo.lastModified}" /></span>
            <span class="repo">${fileInfo.repo}</span>
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
