<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<div>
  <ul class="tableList name-value-pairs">
    <li>
      <span class="label">Name</span>
      <span class="value">${repo.name}</span>
    </li>
    <li>
      <span class="label">External</span>
      <span class="value"><input id="externalSwitch" type="checkbox" class="externalSwitch" ${repo.external ? 'checked="checked"' : ''} /></span>
    </li>
    <li>
      <span class="label">Target</span>
      <span class="value">
        <input id="currentTarget" type="hidden" value="${repo.target}" />
        <select id="targetRepos" ${repo.external ? 'style="display:none;"' : ''} class="round" onchange="$('#saveButton').show()">
        </select>
        <input type="text" id="targetUrl" value="${repo.external ? repo.target : ''}" ${repo.external ? '' : 'style="display:none;"'} class="round" onchange="$('#saveButton').show()"/>
      </span>
    </li>
    <li>
      <span class="label">Actions</span>
      <span class="value">
        <button id="saveButton" class="submit" onclick="return yum.saveVirtualRepo('${repo.name}');" style="display: none;">Save</button>
        <img id="saveSpinner" src="/static/images/wait.gif" style="display: none;" />
      </span>
    </li>
  </ul>
</div>