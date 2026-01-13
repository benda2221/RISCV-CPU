import chisel3._
import chisel3.util._

object BranchOp {
    val BEQ     = 0x10.U(5.W)
    val BNE     = 0x11.U(5.W)
    val JALR    = 0x12.U(5.W)
    val JAL     = 0x13.U(5.W)
    val BLT     = 0x14.U(5.W)
    val BGE     = 0x15.U(5.W)
    val BLTU    = 0x16.U(5.W)
    val BGEU    = 0x17.U(5.W)
}

class BranchIO extends Bundle {
    val src1     = Input(UInt(32.W))
    val src2     = Input(UInt(32.W))
    val op       = Input(UInt(5.W))
    val pc       = Input(UInt(32.W))
    val imm      = Input(UInt(32.W))
    val jumpEn  = Output(Bool())
    val jumpTgt = Output(UInt(32.W))
}

class Branch extends Module {
    val io = IO(new BranchIO())

    val jumpEn = WireDefault(false.B)
    val jumpTgt = WireDefault(io.pc + io.imm)
    import BranchOp._

    switch(io.op){
        is(BEQ){
            jumpEn := io.src1 === io.src2
        }
        is(BNE){
            jumpEn := io.src1 =/= io.src2
        }
        is(JALR){
            jumpEn := true.B
            jumpTgt := io.src1 + io.imm
        }
        is(JAL){
            jumpEn := true.B
        }
        is(BLT){
            jumpEn := io.src1.asSInt < io.src2.asSInt
        }
        is(BGE){
            jumpEn := io.src1.asSInt >= io.src2.asSInt
        }
        is(BLTU){
            jumpEn := io.src1.asUInt < io.src2.asUInt
        }
        is(BGEU){
            jumpEn := io.src1.asUInt >= io.src2.asUInt
        }
    }
    io.jumpEn := jumpEn
    io.jumpTgt := jumpTgt

}