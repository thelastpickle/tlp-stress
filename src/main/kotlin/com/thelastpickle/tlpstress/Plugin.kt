package com.thelastpickle.tlpstress

import com.thelastpickle.tlpstress.profiles.IStressProfile
import org.reflections.Reflections

data class Plugin (val name: String,
                   val cls: Class<out IStressProfile>,
                   val arguments: Any) {

    companion object {

        fun getPlugins() : Map<String, Plugin> {
            val r = Reflections()
            val modules = r.getSubTypesOf(IStressProfile::class.java)

            var result = mutableMapOf<String, Plugin>()

            for(m in modules) {
                val args = m.getConstructor().newInstance().getArguments()
                val tmp = Plugin(m.simpleName, m, args)
                result[m.simpleName] = tmp
            }

            return result
        }
    }
}