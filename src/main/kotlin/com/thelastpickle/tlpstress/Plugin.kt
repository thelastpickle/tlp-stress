package com.thelastpickle.tlpstress

import com.thelastpickle.tlpstress.profiles.IStressProfile
import org.reflections.Reflections

/**
 * Wrapper for Stress Profile Plugins
 * Anything found in the class path will be returned.
 * TODO: Add a caching layer to prevent absurdly slow
 * reflection time
 */

data class Plugin (val name: String,
                   val cls: Class<out IStressProfile>,
                   val instance: IStressProfile) {

    companion object {

        fun getPlugins() : Map<String, Plugin> {
            val r = Reflections("com.thelastpickle.tlpstress")
            val modules = r.getSubTypesOf(IStressProfile::class.java)


            var result = sortedMapOf<String, Plugin>()

            for(m in modules) {
                val instance = m.getConstructor().newInstance()
//                val args = instance.getArguments()
                val tmp = Plugin(m.simpleName, m, instance)
                result[m.simpleName] = tmp
            }

            return result
        }



    }

    override fun toString() = name
}