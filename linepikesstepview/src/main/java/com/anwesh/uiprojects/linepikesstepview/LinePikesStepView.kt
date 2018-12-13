package com.anwesh.uiprojects.linepikesstepview

/**
 * Created by anweshmishra on 14/12/18.
 */
import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.content.Context
import android.app.Activity

val nodes : Int = 5
val lines : Int = 4
val scDiv : Double = 0.51
val scGap : Float = 0.05f
val color : Int = Color.parseColor("#2980b9")
val strokeFactor : Int = 90
val sizeFactor : Float = 2.4f
val DELAY : Long = 25

fun Int.getInverse()  : Float = 1f / this

fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.getInverse(), Math.max(0f, this - i * n.getInverse())) * n

fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()

fun Float.mirrorValue(a : Int, b : Int) : Float = (1 - scaleFactor()) * a.getInverse() + scaleFactor() * b.getInverse()

fun Float.updateScale(dir : Float, a : Int, b : Int) : Float = dir * scGap * mirrorValue(a, b)

fun Canvas.drawLPSNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val size : Float = gap / sizeFactor
    val xGap = size / (lines + 1)
    paint.color = color
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    save()
    translate(w/2, gap * (i + 1))
    for (j in 0..1) {
        val scj : Float = sc1.divideScale(j, 2)
        save()
        rotate(-j * (90f + 90f * sc2))
        drawLine(0f, 0f, size, 0f, paint)
        for (k in 0..(lines - 1)) {
            val sck : Float = scj.divideScale(k, lines)
            save()
            translate(xGap * (k + 1), 0f)
            drawLine(0f, 0f, 0f, -xGap * sck * (1 - 2 * j), paint)
            restore()
        }
        restore()
    }
    restore()
}

class LinePikesStepView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {
        fun update(cb : (Float) -> Unit) {
            scale += scale.updateScale(dir, lines * 2, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {
        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(DELAY)
                    view.invalidate()
                } catch (ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class LPSNode(var i : Int, val state : State = State()) {

        private var next : LPSNode? = null

        private var prev : LPSNode? = null

        init {
            addNeighbor()
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawLPSNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = LPSNode(i + 1)
                next?.prev = this
            }
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : LPSNode {
            var curr : LPSNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class LinePikesStep(var i : Int) {

        private var dir : Int = 1

        private val root : LPSNode = LPSNode(0)
        private var curr : LPSNode = root

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, sc ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, sc)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : LinePikesStepView) {

        private val animator : Animator = Animator(view)
        private val lps : LinePikesStep = LinePikesStep(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(Color.parseColor("#BDBDBD"))
            lps.draw(canvas, paint)
            animator.animate {
                lps.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            lps.startUpdating {
                animator.start()
            }
        }
    }

    companion object {
        fun create(activity: Activity) : LinePikesStepView {
            val view : LinePikesStepView = LinePikesStepView(activity)
            activity.setContentView(view)
            return view
        }
    }
}