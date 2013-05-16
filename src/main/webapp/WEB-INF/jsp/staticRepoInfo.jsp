<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%--@elvariable id="repo" type="de.is24.infrastructure.gridfs.http.domain.RepoEntry"--%>
<div>
    <ul class="tableList name-value-pairs">
        <li>
            <span class="label">Name</span>
            <span class="value" id="yumRepoName" name="${repo.name}">${repo.name}</span>
        </li>
        <li>
            <span class="label">Last Modified</span>
            <span class="value"><tags:date date="${repo.lastModified}" /></span>
        </li>
        <li>
            <span class="label">Scheduled</span>
            <span class="value">
                <input type="checkbox" class="scheduledSwitch" name="${repo.name}" ${repo.type == 'SCHEDULED' ? 'checked="checked"' : ''} />
            </span>
        </li>
        <li>
            <span class="label">Last Metadata</span>
            <span class="value"><tags:date date="${repo.lastMetadataGeneration}" /></span>
        </li>
        <li>
          <span class="label">Max. Keep RPMs</span>
          <span class="value"><span id="maxKeepRpmsValue" name="${repo.name}">${repo.maxKeepRpms}</span><div id="maxKeepRpmsSlider"></div></span>
        </li>
        <li style="clear:left">
		  <span class="label">Tags</span>
        </li>
    </ul>
	<div style="display:block">
		<span id="tags" style="display:none"><c:forEach items="${repo.tags}" var="tag"><span class="tag" style="margin-right:5px">${tag}</span></c:forEach></span>
		<input id="tagsInput" type="text" /><button id="saveTagButton" class="submit" onclick="return yum.resetTags();" style="margin-left:3px;">Save Tags</button>
	</div>
</div>