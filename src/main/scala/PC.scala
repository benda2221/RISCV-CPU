import chisel3._
import chisel3.util._

class PCIO extends Bundle {
    val jumpEn  = Input(Bool())
    val jumpTgt = Input(UInt(32.W))
    val pc      = Output(UInt(32.W))
    val stall   = Input(Bool())
}

class PC extends Module {
    val io = IO(new PCIO())

    val pc = RegInit(0x80000000L.U(32.W))

    when(io.jumpEn){
        pc := io.jumpTgt
    }.elsewhen(!io.stall){
        pc := pc + 4.U
    }
    io.pc := pc
}