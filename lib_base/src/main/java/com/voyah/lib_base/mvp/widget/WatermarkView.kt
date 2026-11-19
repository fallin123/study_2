package com.voyah.lib_base.mvp.widget// WatermarkView.kt
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class WatermarkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33000000") // 默认水印颜色
        textSize = 48f                        // 默认字体大小
        isAntiAlias = true
    }

    // 水印参数
    private var watermarkText = "Watermark"
    private var angle = -30f          // 旋转角度（默认-30度）
    private var hSpacing = 200f       // 水平间距
    private var vSpacing = 200f       // 垂直间距

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawWatermark(canvas, watermarkText, width, height)
    }

    private fun drawWatermark(canvas: Canvas, text: String, width: Int, height: Int) {
        val textWidth = paint.measureText(text)
        val textHeight = paint.descent() - paint.ascent()

        canvas.save()
        // 创建包含旋转的矩阵
        val matrix = Matrix().apply {
            preRotate(angle)
        }
        canvas.concat(matrix)

        // 计算绘制起点
        val startX = -width * 0.8f
        val startY = -height * 0.2f

        var y = startY
        while (y < height * 1.5) {
            var x = startX
            while (x < width * 1.5) {
                canvas.drawText(text, x, y, paint)
                x += textWidth + hSpacing
            }
            y += textHeight + vSpacing
        }

        canvas.restore()
    }

    // 更新水印参数的方法
    fun updateWatermark(
        text: String,
        color: Int = paint.color,
        textSize: Float = paint.textSize,
        angle: Float = this.angle,
        hSpacing: Float = this.hSpacing,
        vSpacing: Float = this.vSpacing
    ) {
        this.watermarkText = text
        this.angle = angle
        this.hSpacing = hSpacing
        this.vSpacing = vSpacing
        paint.color = color
        paint.textSize = textSize
        invalidate()
    }
}




