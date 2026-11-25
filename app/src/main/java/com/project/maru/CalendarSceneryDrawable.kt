package com.project.maru

import android.graphics.*
import android.graphics.drawable.Drawable

// Native vector decoration: stays crisp at every density and never covers the date controls.
internal class CalendarSceneryDrawable(private val landscape: Boolean=false) : Drawable() {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(canvas: Canvas) {
        val checkpoint=canvas.save()
        canvas.translate(bounds.left.toFloat(),bounds.top.toFloat())
        canvas.scale(bounds.width()/320f,bounds.height()/(if(landscape) 28f else 76f))
        fun color(hex: String) { paint.shader=null; paint.color=Color.parseColor(hex); paint.style=Paint.Style.FILL }
        if(landscape) {
            color("#DCE8BA")
            canvas.drawPath(Path().apply { moveTo(0f,28f); lineTo(0f,19f); cubicTo(50f,-2f,95f,31f,164f,16f); cubicTo(220f,4f,270f,26f,320f,13f); lineTo(320f,28f); close() },paint)
            color("#C5D99A"); canvas.drawOval(0f,10f,18f,31f,paint); canvas.drawOval(10f,5f,29f,32f,paint)
            color("#B5D9BC"); canvas.drawRoundRect(270f,3f,287f,23f,8f,8f,paint)
            color("#F5B9C2"); canvas.drawRoundRect(293f,0f,314f,25f,10f,10f,paint)
            color("#C49E86"); canvas.drawRoundRect(277f,13f,279f,28f,1f,1f,paint); canvas.drawRoundRect(302f,12f,304f,28f,1f,1f,paint)
        } else {
            color("#FFD0DF")
            paint.shader=LinearGradient(0f,0f,0f,76f,Color.parseColor("#FFC8D9"),Color.parseColor("#FFE4ED"),Shader.TileMode.CLAMP)
            canvas.drawPath(Path().apply { moveTo(0f,76f); lineTo(0f,64f); cubicTo(28f,36f,58f,39f,80f,60f); cubicTo(119f,29f,151f,34f,177f,61f); cubicTo(217f,26f,248f,39f,264f,57f); cubicTo(287f,38f,306f,45f,320f,60f); lineTo(320f,76f); close() },paint)
            color("#FFD879"); canvas.drawCircle(23f,28f,11f,paint)
            paint.strokeWidth=3f; paint.strokeCap=Paint.Cap.ROUND
            for(index in 0..7) {
                val angle=Math.PI*index/4
                canvas.drawLine(23f+(16*Math.cos(angle)).toFloat(),28f+(16*Math.sin(angle)).toFloat(),23f+(20*Math.cos(angle)).toFloat(),28f+(20*Math.sin(angle)).toFloat(),paint)
            }
            fun cloud(x: Float,y: Float) {
                color("#DCECF7"); canvas.drawRoundRect(x,y+6,x+32,y+19,7f,7f,paint); canvas.drawCircle(x+14,y+7,9f,paint); canvas.drawCircle(x+24,y+10,7f,paint)
                color("#FFFFFF"); canvas.drawRoundRect(x+2,y+8,x+30,y+17,6f,6f,paint); canvas.drawCircle(x+14,y+7,7f,paint); canvas.drawCircle(x+24,y+10,5f,paint)
            }
            cloud(5f,51f); cloud(279f,15f)
        }
        canvas.restoreToCount(checkpoint)
    }
    override fun setAlpha(alpha: Int) { paint.alpha=alpha; invalidateSelf() }
    override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter=filter; invalidateSelf() }
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
