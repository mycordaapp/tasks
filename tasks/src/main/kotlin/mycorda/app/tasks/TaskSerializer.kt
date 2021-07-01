package mycorda.app.tasks

//
//
//import java.io.File
//import java.io.IOException
//import java.util.*
//import kotlin.reflect.KClass
//import com.fasterxml.jackson.databind.JsonNode
//import com.fasterxml.jackson.module.kotlin.KotlinModule
//import net.corda.ccl.commons.model.NotRequired
//import java.net.URL
//
//
//class TaskSerializer() {
//    private val mapper: ObjectMapper
//
//    init {
//        mapper = ObjectMapper()
//        val module = KotlinModule()
//        module.addSerializer(File::class.java, FileSerializer())
//        module.addDeserializer(File::class.java, FileDeserializer())
//        mapper.registerModule(module)
//    }
//
//
//    fun deserialize(data: String, taskClazz: KClass<out Task>): Any? {
//        val reflections = TaskReflections(taskClazz)
//        val paramClazz = reflections.paramClass()
//
//        return doDeserialize(data, reflections, paramClazz)
//    }
//
//    fun deserializeResult(data: String, taskClazz: KClass<out Task>): Any? {
//        val reflections = TaskReflections(taskClazz)
//        val clazz = reflections.resultClass()
//
//        return doDeserialize(data, reflections, clazz)
//    }
//
//    fun deserializeResult(data: String, taskClazz: KClass<out Task>, clazz: KClass<out Any>): Any? {
//        val reflections = TaskReflections(taskClazz)
//        //val clazz = reflections.resultClass()
//
//        return doDeserialize(data, reflections, clazz)
//    }
//
//    private fun doDeserialize(data: String, reflections: TaskReflections, clazz: KClass<out Any>): Any? {
//        if (data.isBlank() && reflections.isParamOptional()) {
//            return null
//        }
//
//        if (TaskReflections.isUnit(clazz)) {
//            if (data.isNotBlank()) throw RuntimeException("doDeserialize found data '$data' when Unit / Nothing is expected")
//            return Unit
//        }
//
//        if (TaskReflections.isNotRequired(clazz)) {
//            if (data.isNotBlank()) throw RuntimeException("doDeserialize found data '$data' when NotRequired is expected")
//            return NotRequired.instance()
//        }
//
//        return if (TaskReflections.isScalar(clazz)) {
//            when (clazz.simpleName) {
//                "Int" -> data.toInt()
//                "Long" -> data.toLong()
//                "BigDecimal" -> data.toBigDecimal()
//                "Boolean" -> data.toBoolean()
//                "Double" -> data.toDouble()
//                "Float" -> data.toFloat()
//                "String" -> data
//                "UUID" -> UUID.fromString(data)
//                "URL" -> URL(data)
//                "File" -> createTmpFile(data)
//                else -> throw RuntimeException("Don't know about scalar ${clazz.simpleName}")
//            }
//        } else {
//            mapper.readValue(data, clazz.java)
//        }
//    }
//
//    fun serializeResult(data: Any, prettyPrint: Boolean = false): String {
//        val clazz = data::class
//
//        if (data is Unit) return ""
//        if (data is Nothing) return ""
//        if (data is NotRequired) return ""
//        if (data == null) return ""
//
//        return if (TaskReflections.isScalar(clazz)) {
//            when (clazz.simpleName) {
//                "Int" -> data.toString()
//                "Long" -> data.toString()
//                "BigDecimal" -> data.toString()
//                "Boolean" -> data.toString()
//                "Double" -> data.toString()
//                "Float" -> data.toString()
//                "String" -> data.toString()
//                "UUID" -> data.toString()
//                "URL" -> data.toString()
//                "File" -> fileToBase64Reference(data as File)
//                else -> throw RuntimeException("Don't know about scalar ${clazz.simpleName}")
//            }
//        } else {
//            if (prettyPrint) {
//                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data)
//            } else {
//                mapper.writeValueAsString(data)
//            }
//        }
//    }
//
//    fun isScalar(data: Any): Boolean {
//        return TaskReflections.isScalar(data::class)
//    }
//
//    fun createTmpFile(base64: String): File {
//        val dir = createTempDir()
//        val name = FileParam.fileName(base64)
//        val f = File(dir, name)
//        f.writeBytes(FileParam.loadBytes(base64))
//        return f
//    }
//
//}
//
//class FileSerializer @JvmOverloads constructor(t: Class<File>? = null) : StdSerializer<File>(t) {
//
//    @Throws(IOException::class, JsonProcessingException::class)
//    override fun serialize(
//            value: File, jgen: JsonGenerator, provider: SerializerProvider) {
//        jgen.writeString(FileParam.fileToBase64Reference(value))
//    }
//}
//
//class FileDeserializer @JvmOverloads constructor(t: Class<File>? = null) : StdDeserializer<File>(t) {
//    override fun deserialize(jp: JsonParser?, ctxt: DeserializationContext?): File {
//        val node = jp!!.getCodec().readTree(jp) as JsonNode
//        return FileParam.loadFile(node.textValue())
//    }
//
//}
//
