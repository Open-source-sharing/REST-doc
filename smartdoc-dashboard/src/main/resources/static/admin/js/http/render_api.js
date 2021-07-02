// Render http api detail page


// HttpURLBlock
function renderApiDetail(doc) {

    // APITitle
    $("#APITitle").val(doc.name)

    // APILastUpdateTime
    $("#APILastUpdateTime").val(formatTime(doc.lastUpdateTime))

    // APIURL
    $("#APIURL").val(doc.url)

    // APICURLCodeRequestSample
    $("#APICURLRequestSample").val(doc)

    // APICURLCodeResponseSample
    $("#APICURLCodeResponseSample").val()

    // APIJavaCodeRequestSample
    $("#APIJavaCodeRequestSample").val()

    // APIKotlinCodeRequestSample
    $("#APIKotlinCodeRequestSample").val()

    // APIPythonCodeRequestSample
    $("#APIPythonCodeRequestSample").val()

}

function formatTime(time){
    return time
}