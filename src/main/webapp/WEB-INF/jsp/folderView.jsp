<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%--@elvariable id="model" type="de.is24.infrastructure.gridfs.http.domain.Container"--%>

<html>
  <tags:header title="Index of ${model.path}/"/>
<body>
  <div id="content">
    <div>
      <div class="left">
        <tags:headlabel/>
        <h1>Index of <strong>${model.path}/</strong></h1>
      </div>
      <tags:logo/>
      <tags:searchBox path="${model.path}"/>
    </div>
    
    <tags:repoListHeader model="${model}"/>
    <tags:repoListEntries model="${model}"/>
    <tags:footer model="${model}"/>
  
  </div>
  <tags:scripts/>
</body>
</html>
