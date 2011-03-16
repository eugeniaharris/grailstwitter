<html>
<head>
    <meta name="layout" content="main"/>
</head>
<body>
    <div id="searchResults">
    <g:if test="${searchResult.results}">
        <g:each var="person" in="${searchResult?.results}">
        <div class="name">
            ${person.realName} <g:link id="${person.id}" class="followLink" action="follow" controller="status">follow</g:link>
        </div>
        </g:each>
    </g:if>
    <g:else>
        No matches
    </g:else>
    </div>
</body>
</html>
