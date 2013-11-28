<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="util" uri="http://immobilienscout24.de/jsp-util" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<html>
<tags:header title="Maintenance options" />
<body>
	<div id="content">
    <div>
      <div class="left">
        <tags:headlabel/>
        <h1>Maintenance options </h1>
      </div>
      <tags:logo />
    </div>
    <div id="forms">
      <div id="obsoletes">
        <h3>Search for obsolete RPMs assuming a repo propagation chain from</h3>
        <form action="obsolete" method="GET">
          <input type="text" name="sourceRepo" title="source repo" class="round" />
          to
          <input type="text" name="targetRepo" title="target repo" class="round" />
          <input type="submit" value="go" name="go" title="go" class="round" />
        </form>
      </div>
      <div id="propagatables">
        <h3>Search for RPMs that could be propagated given a propagation chain from</h3>
        <form action="propagatable" method="GET">
          <input type="text" name="sourceRepo" title="source repo" class="round" />
          to
          <input type="text" name="targetRepo" title="target repo" class="round" />
          <input type="submit" value="go" name="go" title="go" class="round" />
        </form>
      </div>
    </div>
  <tags:scripts />
</body>

</html>
