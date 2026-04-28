<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ attribute name="title" required="false" %>
<!DOCTYPE html>
<html>
<head>
<title>${title}</title>
<%@ include file="/template/includes.jsp" %>
</head>
<body>
<%@ include file="/template/header.jsp" %>
<div id="mainDiv">
    <jsp:doBody/>   <%-- this is the yield --%>
</div>
</body>
</html>