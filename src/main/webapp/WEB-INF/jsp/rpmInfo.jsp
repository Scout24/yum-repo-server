<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<div>
    <ul class="tableList name-value-pairs">
        <li>
            <span class="label">Name</span><span class="value">${model.name}</span>
        </li>
        <li>
            <span class="label">Version</span><span class="value">${model.version.ver}</span>
        </li>
        <li>
            <span class="label">Vendor</span><span class="value">${model.packageFormat.vendor}</span>
        </li>
        <li>
            <span class="label">Release</span><span class="value">${model.version.rel}</span>
        </li>
        <li>
            <span class="label">Build Date</span><span class="value"><fmt:formatDate type="both" value="${model.time.buildAsDate}" /></span>
        </li>
        <li>
            <span class="label">Build Host</span><span class="value">${model.packageFormat.buildHost}</span>
        </li>
        <li>
            <span class="label">Group</span><span class="value">${model.packageFormat.group}</span>
        </li>
        <li>
            <span class="label">Source RPM</span><span class="value">${model.packageFormat.sourceRpm}</span>
        </li>
        <li>
            <span class="label">Size</span><span class="value">${model.size.packagedAsString}</span>
        </li>
        <li>
            <span class="label">License</span><span class="value">${model.packageFormat.license}</span>
        </li>
        <li>
            <span class="label">Packager</span><span class="value long">${model.packager}</span>
        </li>
        <li>
            <span class="label">Summary</span><span class="value long">${fn:replace(model.summary, "
", '<br />')}</span>
        </li>
        <li>
            <span class="label">Description</span>
            <span class="value big">${fn:replace(model.description, "
", '<br />')}</span>
        </li>
        <li>
            <span class="label">Provides</span>
            <span class="value">
                <c:forEach items="${model.packageFormat.provides}" var="entry">
                    ${entry.name}<br />
                </c:forEach>
            </span>
        </li>
        <li>
            <span class="label">Obsoletes</span>
            <span class="value">
                <c:forEach items="${model.packageFormat.obsoletes}" var="entry">
                    ${entry.name}<br />
                </c:forEach>
            </span>
        </li>
        <li>
            <span class="label">Requires</span>
            <span class="value">
                <c:forEach items="${model.packageFormat.requires}" var="entry">
                    ${entry.name}<br />
                </c:forEach>
            </span>
        </li>
        <li>
            <span class="label">Conflicts</span>
            <span class="value">
                <c:forEach items="${model.packageFormat.conflicts}" var="entry">
                    ${entry.name}<br />
                </c:forEach>
            </span>
        </li>
       
        <li>
            <span class="label"><a href="#" onclick="$('table.files').toggleClass('hidden'); return false;">Files</a></span>
            <span class="value big">
                <table class="files hidden">
                    <thead>
                    <tr>
                        <th>Filename</th>
                        <%--
                        <th>Size</th>
                        <th>User</th>
                        <th>Group</th>
                        --%>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${model.packageDirs}" var="dir">
                        <c:forEach items="${dir.files}" var="file">
                            <tr>
                                <td>
                                    ${dir.name}/${file.name}
                                    <c:if test="${file.type == DIR}">(dir)</c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:forEach>
                    </tbody>
                </table>
            </span>
        </li>
    </ul>
</div>