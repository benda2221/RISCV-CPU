package ZirconConfig

import chisel3._

object EXEOp {
    // ALU operations
    val ADD     = 0x0.U(5.W)
    val SLL     = 0x1.U(5.W)
    val SLT     = 0x2.U(5.W)
    val SLTU    = 0x3.U(5.W)
    val XOR     = 0x4.U(5.W)
    val SRL     = 0x5.U(5.W)
    val OR      = 0x6.U(5.W)
    val AND     = 0x7.U(5.W)
    val SUB     = 0x8.U(5.W)
    val SRA     = 0xd.U(5.W)
    
    // Branch operations
    val BEQ     = 0x10.U(5.W)
    val BNE     = 0x11.U(5.W)
    val JALR    = 0x12.U(5.W)
    val JAL     = 0x13.U(5.W)
    val BLT     = 0x14.U(5.W)
    val BGE     = 0x15.U(5.W)
    val BLTU    = 0x16.U(5.W)
    val BGEU    = 0x17.U(5.W)
    
    // Multiply operations
    val MUL     = 0x0.U(4.W)
    val MULH    = 0x1.U(4.W)
    val MULHSU  = 0x2.U(4.W)
    val MULHU   = 0x3.U(4.W)
    
    // Divide operations
    val DIV     = 0x4.U(4.W)
    val DIVU    = 0x5.U(4.W)
    val REM     = 0x6.U(4.W)
    val REMU    = 0x7.U(4.W)
}

