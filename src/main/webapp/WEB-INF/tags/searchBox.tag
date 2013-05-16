<%@attribute name="path" required="true"%>
<div id="searchBox" class="right">
  <form action="/repo/${path}" method="get">
    <input type="text" name="search"/>
    <input id="searchButton" class="submit" type="submit" value="Search"/>
  </form>
</div>