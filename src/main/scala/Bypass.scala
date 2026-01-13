import chisel3._
import chisel3.util._

class BypassIO extends Bundle {
    val instPkgEX       = Input(new InstructionPackage())
    val instPkgLS       = Input(new InstructionPackage())
    val instPkgWB       = Input(new InstructionPackage())
    val src1BypassEn    = Output(Bool())
    val src2BypassEn    = Output(Bool())
    val src1BypassData  = Output(UInt(32.W))
    val src2BypassData  = Output(UInt(32.W))
}

class Bypass extends Module {
    val io = IO(new BypassIO())

    io.src1BypassEn := (io.instPkgEX.rs1 === io.instPkgWB.rd && io.instPkgWB.rdVld) || (io.instPkgEX.rs1 === io.instPkgLS.rd && io.instPkgLS.rdVld)
    io.src2BypassEn := (io.instPkgEX.rs2 === io.instPkgWB.rd && io.instPkgWB.rdVld) || (io.instPkgEX.rs2 === io.instPkgLS.rd && io.instPkgLS.rdVld)
    io.src1BypassData := Mux(io.instPkgEX.rs1 === io.instPkgLS.rd && io.instPkgLS.rdVld, io.instPkgLS.aluResult, io.instPkgWB.aluResult)
    io.src2BypassData := Mux(io.instPkgEX.rs2 === io.instPkgWB.rd && io.instPkgWB.rdVld, io.instPkgWB.aluResult, io.instPkgLS.aluResult)
    
}