package smartdoc.dashboard.controller.console.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.web.bind.annotation.*
import restdoc.rpc.client.common.model.ApplicationType
import smartdoc.dashboard.controller.console.model.*
import smartdoc.dashboard.core.ApiResponse
import smartdoc.dashboard.core.ApiStandard
import smartdoc.dashboard.core.ok
import smartdoc.dashboard.model.HTTP_DOCUMENT_COLLECTION
import smartdoc.dashboard.model.ProjectType
import smartdoc.dashboard.model.Resource
import smartdoc.dashboard.model.doc.DocType
import smartdoc.dashboard.repository.DubboDocumentRepository
import smartdoc.dashboard.repository.HttpDocumentRepository
import smartdoc.dashboard.repository.ProjectRepository
import smartdoc.dashboard.repository.ResourceRepository
import smartdoc.dashboard.util.IDUtil
import smartdoc.dashboard.util.IDUtil.now
import javax.validation.Valid

/**
 *
 */
@RestController
class ResourceController {

    @Autowired
    lateinit var resourceRepository: ResourceRepository

    @Autowired
    lateinit var httpDocumentRepository: HttpDocumentRepository

    @Autowired
    lateinit var dubboDocumentRepository: DubboDocumentRepository

    @Autowired
    lateinit var projectRepository: ProjectRepository

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @PostMapping("/{projectId}/resource")
    @smartdoc.dashboard.base.auth.Verify(role = ["SYS_ADMIN"])
    fun create(@PathVariable projectId: String, @Valid @RequestBody dto: CreateResourceDto): ApiResponse {
        val resource = Resource(
                id = IDUtil.id(),
                tag = dto.tag,
                name = dto.name,
                pid = dto.pid,
                projectId = projectId,
                createTime = now(),
                createBy = "System")
        resourceRepository.save(resource)
        return ok(resource)
    }

    /**
     * View Data Wrapper
     * @sample Resource
     */
    @RequestMapping("/{projectId}/resource/dtree")
    @smartdoc.dashboard.base.auth.Verify(role = ["*"])
    fun getResourceDTree(@PathVariable projectId: String,
                         @RequestParam at: ApplicationType,
                         @RequestParam(defaultValue = "false") onlyQueryResource: Boolean): DTreeApiResponse {

        val project = projectRepository.findById(projectId).orElseThrow { ApiStandard.INVALID_REQUEST.instanceError() }

        val rootNode = DTreeNodeVO(
                id = "root",
                title = project.name,
                parentId = "0",
                nodeType = NodeType.RESOURCE,
                spread = true,
                iconClass = "dtree-icon-weibiaoti5")

        val resourceQuery = Query(Criteria("projectId").`is`(projectId))
        val resources = resourceRepository.list(resourceQuery)
                .sortedWith(compareBy({ it.order }, { it.createTime }))

        val resourceIds = resources.map { it.id }.toMutableList()
        resourceIds.add("root")

        val resourceNodes = resources.map {
            DTreeNodeVO(id = it.id!!,
                    title = it.name!!,
                    parentId = it.pid!!,
                    nodeType = NodeType.RESOURCE,
                    iconClass = "dtree-icon-weibiaoti5")
        }

        val nodes = mutableListOf<DTreeNodeVO>()
        nodes.addAll(resourceNodes)

        if (!onlyQueryResource) {
            val docQuery = Query(Criteria("projectId").`is`(projectId).and("resource").`in`(resourceIds))
            val apiNodes = if (at == ApplicationType.REST_WEB) {
                docQuery.fields()
                        .include("_id")
                        .include("createTime")
                        .include("name")
                        .include("order")
                        .include("docType")
                        .include("resource")

                val docs = mongoTemplate.find(docQuery, DocPojo::class.java, HTTP_DOCUMENT_COLLECTION)
                        .sortedWith(compareBy({ it.order }, { it.createTime }))

                docs.map {
                    DTreeNodeVO(id = it._id,
                            title = it.name,
                            parentId = it.resource,
                            nodeType = if (it.docType == DocType.API) NodeType.API else NodeType.WIKI,iconClass = "dtree-icon-normal-file"
                    )
                }
            } else {
                docQuery.fields().exclude("paramDescriptors").exclude("returnValueDescriptor")
                val dubboDocs = dubboDocumentRepository.list(docQuery)
                dubboDocs.map {
                    DTreeNodeVO(id = it.id,
                            title = it.name,
                            parentId = it.resource,
                            nodeType = if (it.docType == DocType.API) NodeType.API else NodeType.WIKI)
                }
            }
            nodes.addAll(apiNodes)
        }
        reverseCalTree(nodes, rootNode)

        return DTreeApiResponse(data = mutableListOf(rootNode))
    }

    private fun reverseCalTree(nodes: MutableList<DTreeNodeVO>, pNode: DTreeNodeVO) {
        val childNodes = nodes.filter { it.parentId == pNode.id }.toMutableList()

        pNode.children.addAll(childNodes)
        for (childNode in childNodes) {
            if (childNode.nodeType == NodeType.RESOURCE) {
                reverseCalTree(nodes, childNode)
            }
        }
    }

    @GetMapping("/{projectId}/resource/flatten")
    fun getFlattenResource(@PathVariable projectId: String): Any {
        val resources = resourceRepository.list(Query(Criteria("projectId").`is`(projectId)))
        val navNodes = resources.map {
            NavNode(id = it.id!!,
                    title = it.name!!,
                    field = "name",
                    children = null,
                    pid = it.pid!!)
        }.toMutableList()

        val rootNav = NavNode(
                id = "root",
                title = "一级目录(虚拟)",
                field = "title",
                children = mutableListOf(),
                href = null,
                pid = "0",
                checked = true)

        navNodes.add(rootNav)

        return navNodes
    }

    /**
     *
     * TODO   code review
     */
    @GetMapping("/{projectId}/resource/tree")
    fun getTree(@PathVariable projectId: String, @RequestParam(required = false, defaultValue = "false") onlyResource: Boolean): ApiResponse {
        val resources = resourceRepository.list(Query(Criteria("projectId").`is`(projectId)))

        val navNodes = resources.map {
            NavNode(id = it.id!!,
                    title = it.name!!,
                    field = "name",
                    children = null,
                    pid = it.pid!!)
        }

        val rootNav = NavNode(
                id = "root",
                title = "一级目录",
                field = "title",
                children = mutableListOf(),
                href = null,
                pid = "0",
                checked = true)

        findChild(rootNav, navNodes)

        if (onlyResource) return ok(mutableListOf(rootNav))
        val allNode = mutableListOf<NavNode>()

        allNode.add(rootNav)
        allNode.addAll(navNodes)

        val nodeIds = allNode.map { it.id }.toMutableList()

        val docs = httpDocumentRepository.list(Query(Criteria("resource").`in`(nodeIds).and("projectId").`is`(projectId)))

        for (navNode in allNode) {
            val childrenDocNode: MutableList<NavNode> = docs
                    .filter { navNode.id == it.resource }
                    .map {
                        val node = NavNode(
                                id = it.id!!,
                                title = it.name!!,
                                field = "",
                                children = mutableListOf(),
                                href = null,
                                pid = navNode.id,
                                spread = true,
                                checked = false)

                        node.type = if (DocType.API == it.docType) NodeType.API else NodeType.WIKI
                        node
                    }.toMutableList()

            if (navNode.children != null) {
                navNode.children!!.addAll(childrenDocNode)
            } else {
                navNode.children = childrenDocNode
            }
        }
        return ok(mutableListOf(rootNav))
    }

    @smartdoc.dashboard.base.auth.Verify(role = ["SYS_ADMIN"])
    @DeleteMapping("/{projectId}/resource/{id}")
    fun delete(@PathVariable id: String, @PathVariable projectId: String): ApiResponse {
        val project = projectRepository.findById(projectId)
                .orElseThrow { ApiStandard.BAD_REQUEST.instanceError("${projectId}项目不存在") }

        val quantity = if (project.type == ProjectType.REST_WEB) {
            httpDocumentRepository.count(Query(Criteria("resource").`is`(id)))
        } else if (project.type == ProjectType.DUBBO) {
            dubboDocumentRepository.count(Query(Criteria("resource").`is`(id)))
        } else 0

        if (quantity != 0L) ApiStandard.BAD_REQUEST.error("当前目录下存在关联的文档，无法删除")
        resourceRepository.deleteById(id)
        return ok()
    }

    @PatchMapping("/resource")
    @smartdoc.dashboard.base.auth.Verify(role = ["SYS_ADMIN"])
    fun patch(@RequestBody @Valid dto: UpdateNodeDto): ApiResponse {
        val updateResult = resourceRepository.update(Query().addCriteria(Criteria("_id").`is`(dto.id)),
                Update().set("name", dto.name)
                        .set("order", dto.order)
                        .set("pid", dto.pid
                        )
        )
        return ok()
    }
}