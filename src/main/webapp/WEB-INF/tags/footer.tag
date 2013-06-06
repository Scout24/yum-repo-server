<%@ attribute name="model" required="true" type="de.is24.infrastructure.gridfs.http.domain.Container" %>

<ul class="tableList">
  <li class="footer">
    <span class="icon">&nbsp;</span>
    <span class="filename">&nbsp;</span>
    <span class="size"><strong>${model.formattedTotalSize}</strong></span>
  </li>
</ul>
<div id="versionInfoContainer" class="versionInfo"><span>Version: ${appVersion.version}</span></div>