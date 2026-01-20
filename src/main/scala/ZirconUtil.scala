package ZirconUtil

import chisel3._
import chisel3.util._

object ZirconUtil {
    // Reverse bit order
    def Reverse(x: UInt): UInt = {
        val width = x.getWidth
        val reversed = Wire(Vec(width, Bool()))
        for (i <- 0 until width) {
            reversed(i) := x(width - 1 - i)
        }
        reversed.asUInt
    }
    
    // Reverse log2 (count leading zeros)
    def Log2Rev(x: UInt): UInt = {
        val width = x.getWidth
        val reversed = Reverse(x)
        val leadingZeros = PriorityEncoder(reversed)
        leadingZeros.asUInt
    }
    
    // Sign extend
    def SE(x: UInt, targetWidth: Int): UInt = {
        val width = x.getWidth
        require(width <= targetWidth, s"Source width $width must be <= target width $targetWidth")
        if (width == targetWidth) {
            x
        } else {
            Fill(targetWidth - width, x(width - 1)) ## x
        }
    }
    
    // Zero extend
    def ZE(x: UInt, targetWidth: Int): UInt = {
        val width = x.getWidth
        require(width <= targetWidth, s"Source width $width must be <= target width $targetWidth")
        if (width == targetWidth) {
            x
        } else {
            0.U((targetWidth - width).W) ## x
        }
    }
}

