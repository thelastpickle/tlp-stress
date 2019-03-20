package com.thelastpickle.tlpstress.generators

import org.apache.logging.log4j.kotlin.logger
import org.reflections.Reflections

data class FunctionDescription(val name: String,
                               val description: String)

class AnnotationMissingException(val name: Class<out FieldGenerator>) : Exception()

/**
 * Finds all the available functions and tracks them by name
 */
class FunctionLoader : Iterable<FunctionDescription> {

    override fun iterator(): Iterator<FunctionDescription> {
        return object : Iterator<FunctionDescription> {
            val iter = map.iterator()

            override fun hasNext() = iter.hasNext()

            override fun next(): FunctionDescription {
                val tmp = iter.next()
                val annotation = tmp.value.getAnnotation(Function::class.java) ?: throw AnnotationMissingException(tmp.value)

                return FunctionDescription(annotation.name, annotation.description)
            }

        }
    }

    class FunctionNotFound(val name: String) : Exception()

    val map : MutableMap<String, Class<out FieldGenerator>> = mutableMapOf()

    init {
        val r = Reflections("com.thelastpickle.tlpstress")
        log.debug { "Getting FieldGenerator subtypes" }
        val modules = r.getSubTypesOf(FieldGenerator::class.java)


        modules.forEach {
            log.debug { "Getting annotations for $it" }

            val annotation = it.getAnnotation(Function::class.java) ?: throw AnnotationMissingException(it)

            val name = annotation.name
            map[name] = it
        }
    }

    companion object {
        val log = logger()

    }


    /**
     * Returns an instance of the requested class
     */
    fun getInstance(name: String) : FieldGenerator {
        val tmp = map[name]
        val result = tmp?.newInstance() ?: throw FunctionNotFound(name)
        return result
    }

    /**
     *
     */
    fun getInstance(func: ParsedFieldFunction) : FieldGenerator {
        val tmp = getInstance(func.name)
        tmp.setParameters(func.args)
        return tmp
    }



}