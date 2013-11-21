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
      <tags:headlabel/>
      <h1 class="left">Maintenance options </h1>
      <tags:logo />
    </div>
    <div id="forms">
      <div id="obsoletes"> Search for obsolete RPMs assuming a repo propagation chain from 
        <form action="obsolete" method="GET">
          <input type="text" name="sourceRepo" title="source repo">
          to
          <input type="text" name="targetRepo" title="target repo">
          <input type="submit" value="go" name="go" title="go" >
        </form>
      </div>
    </div>
  <tags:scripts />
</body>

</html>
