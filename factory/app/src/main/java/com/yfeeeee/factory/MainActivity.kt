// Pixel Factory - stub for early compile check. Developer: @yfeeeee
package com.yfeeeee.factory

import android.app.Activity
import android.os.Bundle
import com.yfeeeee.factory.core.*
import com.yfeeeee.factory.engine.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val g = Game(1)
        g.newGame()
        val r = Renderer(Sprites())
        if (g.world.N > 0 && r.W > 0) Save.VERSION
    }
}
