import chisel3._
import chisel3.util._

class InstructionPackage extends Bundle {
    val vld  = Bool()
    val inst = UInt(32.W)
    val pc   = UInt(32.W)
    // register file
    val rdVld = Bool()
    val rd    = UInt(5.W)
    val rs1   = UInt(5.W)
    val rs2   = UInt(5.W)
    // immediate
    val imm   = UInt(32.W)
    // operation code: 7: store; 6: load; 5: jump
    val op    = UInt(7.W)
    val aluSrc1 = UInt(3.W)
    val aluSrc2 = UInt(3.W)

    // data
    val rs1Data = UInt(32.W)
    val rs2Data = UInt(32.W)
    val aluResult = UInt(32.W)
    val memResult = UInt(32.W)
}