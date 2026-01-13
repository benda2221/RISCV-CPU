import chisel3._
import chisel3.util._

class RegisterFileIO extends Bundle {
    val rs1     = Input(UInt(5.W))
    val rs2     = Input(UInt(5.W))
    val rd      = Input(UInt(5.W))
    val rdVld   = Input(Bool())
    val rdData  = Input(UInt(32.W))
    val rs1Data = Output(UInt(32.W))
    val rs2Data = Output(UInt(32.W))
}

class RegisterFile extends Module {
    val io = IO(new RegisterFileIO())

    val regs = RegInit(VecInit.fill(32)(0.U(32.W)))

    when(io.rdVld){
        regs(io.rd) := io.rdData
    }
    io.rs1Data := Mux(io.rs1 === io.rd && io.rdVld, io.rdData, regs(io.rs1))
    io.rs2Data := Mux(io.rs2 === io.rd && io.rdVld, io.rdData, regs(io.rs2))
}