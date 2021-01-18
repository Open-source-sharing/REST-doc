package restdoc.web.model.doc.http


/**
 * HeaderFieldDescriptor
 * @author Maple
 */
data class HeaderFieldDescriptor(
        val field: String,
        var value: List<String>,
        var description: String? = null,
        val optional: Boolean = false
)