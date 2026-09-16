package cn.tinyue.console

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.CommentNavigatorBundle"

internal object CommentNavigatorBundle {
    private val bundle = DynamicBundle(CommentNavigatorBundle::class.java, BUNDLE)

    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): @Nls String =
        bundle.getMessage(key, *params)
}
